package com.sahlone.mc.handler

import arrow.core.Either
import com.github.sahlone.klogging.TracingContext
import com.sahlone.kson.data.mapper.Json
import com.sahlone.kson.data.mapper.JsonArrayWrites
import com.sahlone.mc.handler.SensorAlarmsHandler.Companion.SensorAlarmsResponse
import com.sahlone.mc.models.Error
import com.sahlone.mc.service.SensorAlertService
import com.sahlone.mc.utils.uuidPath
import java.time.Instant
import org.http4k.core.Request

class SensorAlarmsHandler(private val sensorAlarmService: SensorAlertService) :
    UnitCommandHandler<List<SensorAlarmsResponse>>(JsonArrayWrites(Json.jsonWrites())) {
    companion object {
        data class SensorAlarmsResponse(
            val startTime: Instant,
            val endTime: Instant?,
            val measurement1: Int,
            val measurement2: Int,
            val measurement3: Int
        )
    }

    override fun handle(
        tracingContext: TracingContext,
        request: Request
    ): Either<Error, List<SensorAlarmsResponse>> =
        request.uuidPath(tracingContext, "uuid").map { sensorId ->
            sensorAlarmService.getSensorAlarms(sensorId).map {
                SensorAlarmsResponse(
                    it.alarm.start,
                    it.alarm.end,
                    it.measurements[0].value,
                    it.measurements[1].value,
                    it.measurements[2].value
                )
            }
        }
}
