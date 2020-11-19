package com.sahlone.mc.handler

import arrow.core.Either
import arrow.core.flatMap
import com.github.sahlone.klogging.TracingContext
import com.sahlone.kson.data.mapper.Json
import com.sahlone.mc.handler.SensorStatusHandler.Companion.SensorStatusResponse
import com.sahlone.mc.models.Error
import com.sahlone.mc.models.Error.Companion.NotFound
import com.sahlone.mc.repository.Repository
import com.sahlone.mc.utils.uuidPath
import org.http4k.core.Request

class SensorStatusHandler(private val repository: Repository) :
    UnitCommandHandler<SensorStatusResponse>(
        Json.jsonWrites()
    ) {
    companion object {
        data class SensorStatusResponse(val status: String)
    }

    override fun handle(
        tracingContext: TracingContext,
        request: Request
    ): Either<Error, SensorStatusResponse> =
        request.uuidPath(tracingContext, "uuid").flatMap { sensorId ->
            repository.getSensorStatus(sensorId).toEither {
                NotFound(tracingContext, "sensor")
            }.map {
                SensorStatusResponse(it.state.name)
            }
        }
}
