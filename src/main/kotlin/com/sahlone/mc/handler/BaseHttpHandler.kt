package com.sahlone.mc.handler

import arrow.core.Either
import arrow.core.Try
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.toOption
import arrow.data.NonEmptyList
import arrow.data.nel
import com.github.sahlone.klogging.TracingContext
import com.github.sahlone.klogging.createContextualLogger
import com.sahlone.kson.data.mapper.JsValidationError
import com.sahlone.kson.data.mapper.Json
import com.sahlone.kson.data.mapper.JsonReads
import com.sahlone.kson.data.mapper.JsonWrites
import com.sahlone.kson.data.mapper.NotAJsonValue
import com.sahlone.kson.data.mapper.unsafeFix
import com.sahlone.mc.models.Error
import com.sahlone.mc.models.Error.Companion.InvalidRequestData
import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.Header

typealias RequestHandler = (Request) -> Response

abstract class CommandHandler<IN, OUT>(
    private val reads: JsonReads<IN>,
    private val writes: JsonWrites<OUT>,
    private val responseStatus: Status = Status.OK,
    private val contentType: ContentType = ContentType.APPLICATION_JSON
) : BaseRequestHandler(), RequestHandler {

    abstract fun handle(tracingContext: TracingContext, request: Request, data: IN): Either<Error, OUT>

    override fun invoke(request: Request): Response =
        withRequestTracing(request) { tracingContext ->
            val result =
                parseJsonRequest(tracingContext, request, reads)
                    .flatMap {
                        handle(tracingContext, request, it)
                    }
            evaluateResult(result, writes, responseStatus, contentType)
        }
}

abstract class UnitCommandHandler<OUT>(
    private val writes: JsonWrites<OUT>,
    private val responseStatus: Status = Status.OK,
    private val contentType: ContentType = ContentType.APPLICATION_JSON
) : BaseRequestHandler(), RequestHandler {

    abstract fun handle(tracingContext: TracingContext, request: Request): Either<Error, OUT>

    override fun invoke(request: Request): Response =
        withRequestTracing(request) { tracingContext ->
            val result = handle(tracingContext, request)
            evaluateResult(result, writes, responseStatus, contentType)
        }
}

open class BaseRequestHandler {

    companion object {
        val logger = createContextualLogger()
        const val TRACE_ID_HEADER = "X-RequestId"
    }

    protected fun <OUT> evaluateResult(
        data: Either<Error, OUT>,
        writes: JsonWrites<OUT>,
        status: Status,
        contentType: ContentType
    ): Response {
        return when (val result = data.map {
            it.toResponse(status, writes, contentType)
        }) {
            is Either.Right -> result.b
            is Either.Left -> {
                result.a.toResponse()
            }
        }
    }

    protected fun <IN> parseJsonRequest(
        tracingContext: TracingContext,
        request: Request,
        reads: JsonReads<IN>
    ): Either<Error, IN> = Try {
        Json.parse(request.bodyString())
    }.toEither().mapLeft {
        logger.errorWithCause(
            tracingContext,
            it,
            { "request" to request }) { "Error parsing  json body for request" }
        JsValidationError(NotAJsonValue).toApiErrors(tracingContext)
    }.flatMap { json ->
        json.flatMap {
            reads(it)
        }.toEither().mapLeft { it.toAPIErrors(tracingContext) }
    }

    protected fun withRequestTracing(request: Request, block: (TracingContext) -> Response): Response =
        request.header(TRACE_ID_HEADER).toOption().map {
            TracingContext(it)
        }.getOrElse {
            TracingContext()
        }.let { tracingContext ->
            Try {
                block(tracingContext)
            }.getOrElse {
                logger.errorWithCause(tracingContext, it, { "request" to request }) { "Error service request" }
                Error.Companion.InternalServerError(tracingContext).toResponse()
            }.also {
                logger.debug(
                    tracingContext,
                    { "request" to request },
                    { "response" to it }) { "http trace" }
            }
        }
}

fun JsValidationError.toApiErrors(tracingContext: TracingContext) = this.nel().toAPIErrors(tracingContext)

fun NonEmptyList<JsValidationError>.toAPIErrors(tracingContext: TracingContext): Error {
    val message = this@toAPIErrors.all.joinToString {
        if (it.path.value.isBlank()) {
            it.errorDetail.show()
        } else {
            "${it.errorDetail.show()} : ${it.path.value}"
        }
    }
    return InvalidRequestData(tracingContext, message)
}

fun Error.toResponse(): Response =
    this.toResponse(this@toResponse.status, Error.writes, ContentType.APPLICATION_JSON)

fun <T> T.toResponse(status: Status, writes: JsonWrites<T>, contentType: ContentType) =
    Response(status)
        .with(
            Header.CONTENT_TYPE of contentType,
            {
                it.body(Json.stringify(writes(this)).unsafeFix())
            }
        )
