package com.sahlone.mc.filter

import arrow.core.Try
import arrow.core.getOrElse
import com.github.sahlone.klogging.TracingContext
import com.github.sahlone.klogging.createContextualLogger
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status

open class HttpException(val status: Status, errorMessage: String = status.description) : RuntimeException(errorMessage)

fun createErrorResponse(status: Status) =
    Response(status)

object HttpExceptionFilter {

    private val logger = createContextualLogger()

    operator fun invoke() = Filter { next ->
        {
            Try {
                next(it)
            }.getOrElse { error ->
                logger.errorWithCause(TracingContext(), error) { "Uncaught error " }
                when (error) {
                    is HttpException -> {
                        createErrorResponse(
                            error.status
                        )
                    }
                    else -> {
                        createErrorResponse(
                            Status.INTERNAL_SERVER_ERROR
                        )
                    }
                }
            }
        }
    }
}
