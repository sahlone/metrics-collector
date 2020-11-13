package com.sahlone.mc.models

import java.time.Instant
import java.util.UUID

const val ALERT_THRESHOLD_VALUE = 2000
const val ALERT_THRESHOLD_NUMBER = 3
const val NO_OF_DAYS_THRESHOLD = 30L

enum class MetricName {
    Co2
}

enum class SensorState {
    OK,
    WARN,
    ALERT
}

enum class AlarmState {
    OPEN,
    CLOSED
}

data class SensorStatus(val sensorId: UUID, val state: SensorState)
data class SensorAlarm(val id: UUID, val sensorId: UUID, val start: Instant, val end: Instant?, val state: AlarmState)
data class SensorMetrics(
    val id: UUID,
    val sensorId: UUID,
    val avg: Double,
    val max: Int,
    val count: Int,
    val created: Instant,
    val modified: Instant
)

data class SensorAlarmData(val alarm: SensorAlarm, val measurements: List<SensorMeasurement>)
