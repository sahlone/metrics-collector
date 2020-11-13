package com.sahlone.mc.handler

import arrow.core.Either
import arrow.core.flatMap
import com.github.sahlone.klogging.TracingContext
import com.sahlone.kson.data.mapper.Json
import com.sahlone.mc.models.Error.Companion.NotFound
import com.sahlone.mc.handler.SensorMetricsHandler.Companion.SensorMetricsResponse
import com.sahlone.mc.models.Error
import com.sahlone.mc.repository.Repository
import com.sahlone.mc.utils.uuidPath
import org.http4k.core.Request

class SensorMetricsHandler(private val repository: Repository) :
    UnitCommandHandler<SensorMetricsResponse>(Json.jsonWrites()) {

    companion object {
        data class SensorMetricsResponse(val maxLast30Days: Int, val avgLast30Days: Double)
    }

    override fun handle(
        tracingContext: TracingContext,
        request: Request
    ): Either<Error, SensorMetricsResponse> =
        request.uuidPath(tracingContext, "uuid").flatMap { sensorId ->
            repository.getSensorMetrics(sensorId).toEither {
                NotFound(tracingContext, "sensor")
            }.map {
                SensorMetricsResponse(it.max, it.avg)
            }
        }
}
