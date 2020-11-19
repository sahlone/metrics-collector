package com.sahlone.mc.handler

import arrow.core.Either
import arrow.core.flatMap
import com.github.sahlone.klogging.TracingContext
import com.sahlone.kson.data.mapper.Json
import com.sahlone.mc.events.SensorDataPublisher
import com.sahlone.mc.handler.SensorDataHandler.Companion.SensorData
import com.sahlone.mc.models.Error
import com.sahlone.mc.models.EventType
import com.sahlone.mc.models.MetricName
import com.sahlone.mc.models.SensorMeasurement
import com.sahlone.mc.utils.uuidPath
import java.time.Instant
import org.http4k.core.Request

class SensorDataHandler(private val sensorDataPublisher: SensorDataPublisher) :
    CommandHandler<SensorData, SensorMeasurement>(
        Json.jsonReads(),
        Json.jsonWrites()
    ) {
    override fun handle(
        tracingContext: TracingContext,
        request: Request,
        data: SensorData
    ): Either<Error, SensorMeasurement> =
        request.uuidPath(tracingContext, "uuid").flatMap {
            val eventData = SensorMeasurement(it, MetricName.Co2, data.co2, data.time)
            sensorDataPublisher.send(
                tracingContext,
                EventType.SENSOR_DATA_PUT,
                eventData.sensorId,
                eventData.sensorId,
                eventData
            )
        }.map {
            it.data
        }

    companion object {
        data class SensorData(val co2: Int, val time: Instant)
    }
}
