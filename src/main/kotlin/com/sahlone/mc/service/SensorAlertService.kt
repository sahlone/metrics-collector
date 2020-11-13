package com.sahlone.mc.service

import arrow.core.getOrElse
import com.github.sahlone.klogging.TracingContext
import com.github.sahlone.klogging.createContextualLogger
import com.sahlone.mc.models.AlarmState
import com.sahlone.mc.models.NO_OF_DAYS_THRESHOLD
import com.sahlone.mc.models.SensorAlarm
import com.sahlone.mc.models.SensorAlarmData
import com.sahlone.mc.models.SensorState
import com.sahlone.mc.models.SensorStatus
import com.sahlone.mc.repository.Repository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class SensorAlertService(private val repository: Repository) {

    private val logger = createContextualLogger()
    fun getSensorAlarms(sensor: UUID): List<SensorAlarmData> {
        val offset = Instant.ofEpochSecond(LocalDate.now().minusDays(NO_OF_DAYS_THRESHOLD).toEpochDay())
        return (repository.getAlarms(sensor, AlarmState.OPEN) + repository.getAlarms(
            sensor,
            AlarmState.CLOSED
        )).filter { it.start.isAfter(offset) }.map {
            val measurements = repository.getAlarmMeasurements(it)
            SensorAlarmData(it, measurements)
        }
    }

    fun updateSensorAlerts(
        tracingContext: TracingContext,
        sensorId: UUID
    ) {
        val newState = repository.getSensorStatus(sensorId).getOrElse {
            SensorStatus(sensorId, SensorState.OK)
        }.state
        val openAlarms = repository.getAlarms(sensorId, AlarmState.OPEN)

        val measurements = repository.getLastThreeSensorReadings(sensorId)
        if (newState != SensorState.ALERT) {
            openAlarms.map { alarm ->
                val endTime = measurements.maxBy { it.timestamp }!!.timestamp
                alarm.copy(end = endTime, state = AlarmState.CLOSED)
            }.map { alarm ->
                repository.updateAlarm(alarm).also {
                    logger.debug(tracingContext) { "Alarm ${alarm.id} closed for sensorId : $sensorId" }
                }
            }
        } else {
            if (openAlarms.isEmpty()) {
                val startTime = measurements.maxBy { it.timestamp }?.timestamp!!
                val alarm = SensorAlarm(
                    UUID.randomUUID(),
                    sensorId,
                    startTime,
                    Instant.MAX,
                    AlarmState.OPEN
                )
                repository.saveAlarm(alarm).also {
                    logger.debug(tracingContext) { "Alarm ${alarm.id} created for sensorId : $sensorId" }
                }
            }
        }
    }
}
