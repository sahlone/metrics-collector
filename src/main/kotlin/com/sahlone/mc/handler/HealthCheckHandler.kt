package com.sahlone.mc.handler

import arrow.core.Either
import arrow.core.Try
import com.github.sahlone.klogging.TracingContext
import com.sahlone.kson.data.mapper.IntJsonWrites
import com.sahlone.kson.data.mapper.JsonWrites
import com.sahlone.kson.data.mapper.at
import com.sahlone.mc.models.Error
import org.http4k.core.Request
import org.http4k.core.Status

object HealthCheckHandler : UnitCommandHandler<HealthCheckResult>(
    HealthCheckResult.JsonWrites) {
    override fun handle(
        tracingContext: TracingContext,
        request: Request
    ): Either<Error, HealthCheckResult> =
        Try.just(HealthCheckPassed()).toEither { Error.Companion.InternalServerError(tracingContext) }
}

sealed class HealthCheckResult {

    companion object {
        internal val JsonWrites: JsonWrites<HealthCheckResult> = {
            IntJsonWrites(it.status.code) at "code"
        }
    }

    abstract val status: Status
}

data class HealthCheckFailed(override val status: Status = Status.SERVICE_UNAVAILABLE) : HealthCheckResult()
data class HealthCheckPassed(override val status: Status = Status.OK) : HealthCheckResult()
