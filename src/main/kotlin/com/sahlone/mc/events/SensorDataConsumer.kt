package com.sahlone.mc.events

import arrow.core.Either
import com.github.sahlone.klogging.TracingContext
import com.github.sahlone.klogging.createContextualLogger
import com.sahlone.kson.data.mapper.Json
import com.sahlone.kson.data.mapper.JsonValue
import com.sahlone.mc.handler.toAPIErrors
import com.sahlone.mc.models.Error
import com.sahlone.mc.models.EventMeta
import com.sahlone.mc.models.EventType
import com.sahlone.mc.models.SensorMeasurement
import com.sahlone.mc.service.SensorAlertService
import com.sahlone.mc.service.SensorDataService
import com.sahlone.mc.service.SensorMetricsService
import com.sahlone.mc.service.SensorStatusService

class SensorDataConsumer(
    private val sensorDataService: SensorDataService,
    private val sensorStatusService: SensorStatusService,
    private val sensorMetricsService: SensorMetricsService,
    private val sensorAlertService: SensorAlertService
) : (TracingContext, EventMeta, JsonValue) -> Either<Error, Unit> {

    private val logger = createContextualLogger()

    override fun invoke(
        tracingContext: TracingContext,
        eventMeta: EventMeta,
        eventData: JsonValue
    ): Either<Error, Unit> {
        logger.info(tracingContext) { "Processing sensor event: $eventMeta" }
        return when (eventMeta.eventType) {
            EventType.SENSOR_DATA_PUT -> {
                Json.jsonReads<SensorMeasurement>()(eventData).toEither().mapLeft {
                    logger.debug(
                        tracingContext,
                        { "event" to eventData },
                        { "error" to it }) { "Error deserializing sensor event" }
                    it.toAPIErrors(tracingContext)
                }.map {
                    sensorDataService.putSensorMeasurementData(tracingContext, it)
                    sensorStatusService.updateSensorStatus(tracingContext, it.sensorId)
                    sensorMetricsService.updateSensorMetrics(tracingContext, it.sensorId)
                    sensorAlertService.updateSensorAlerts(tracingContext, it.sensorId)
                    logger.debug(tracingContext, { "event" to it }) { "Successfully processed sensor event" }
                }
            }
        }
    }
}
