package com.sahlone.mc.models

import com.github.sahlone.klogging.TracingContext
import com.sahlone.kson.data.mapper.Json
import com.sahlone.kson.data.mapper.JsonWrites
import com.sahlone.kson.data.mapper.StringJsonWrites
import com.sahlone.kson.data.mapper.at
import org.http4k.core.Status

sealed class Error {
    abstract val trace: TracingContext
    abstract val code: String
    abstract val message: String
    abstract val status: Status

    companion object {

        data class InternalServerError(
            override val trace: TracingContext,
            override val message: String = "Server is not able to process the request"
        ) : Error() {
            override val code = "InternalServerError"
            override val status = Status.INTERNAL_SERVER_ERROR
        }

        data class NotFound(override val trace: TracingContext, val entityId: String) : Error() {
            override val code = "NotFound"
            override val message = "Entity with id $entityId not found"
            override val status = Status.NOT_FOUND
        }

        data class InvalidRequestData(override val trace: TracingContext, override val message: String) : Error() {
            override val code = "InvalidRequestData"
            override val status = Status.UNPROCESSABLE_ENTITY
        }

        val writes: JsonWrites<Error> = {
            with(it) {
                Json(
                    StringJsonWrites(trace.correlationId) at "traceId",
                    StringJsonWrites(code) at "code",
                    StringJsonWrites(message) at "message"
                )
            }
        }
    }
}
