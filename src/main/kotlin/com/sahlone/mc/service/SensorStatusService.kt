package com.sahlone.mc.service

import arrow.core.getOrElse
import com.github.sahlone.klogging.TracingContext
import com.github.sahlone.klogging.createContextualLogger
import com.sahlone.mc.models.ALERT_THRESHOLD_NUMBER
import com.sahlone.mc.models.ALERT_THRESHOLD_VALUE
import com.sahlone.mc.models.SensorMeasurement
import com.sahlone.mc.models.SensorState
import com.sahlone.mc.models.SensorStatus
import com.sahlone.mc.repository.Repository
import java.util.UUID

class SensorStatusService(private val repository: Repository) {

    private val logger = createContextualLogger()

    fun updateSensorStatus(tracingContext: TracingContext, sensorId: UUID) {
        val sensorStatus = repository.getSensorStatus(sensorId).getOrElse {
            SensorStatus(sensorId, SensorState.OK)
        }
        val measurements = repository.getLastThreeSensorReadings(sensorId)
        val newState =
            when (sensorStatus.state) {
                SensorState.OK, SensorState.WARN -> {
                    transitionNonAlertState(measurements)
                }
                SensorState.ALERT -> {
                    transitionAlertState(measurements)
                }
            }
        repository.setSensorStatus(sensorStatus.copy(state = newState)).also {
            logger.debug(tracingContext) {
                "Updated sensor status for $sensorId to $newState"
            }
        }
    }

    private fun transitionNonAlertState(measurements: List<SensorMeasurement>): SensorState {
        val warnMeasurements = measurements.filter { it.value > ALERT_THRESHOLD_VALUE }
        return when {
            warnMeasurements.size >= ALERT_THRESHOLD_NUMBER -> {
                SensorState.ALERT
            }
            measurements.isNotEmpty() && measurements[0].value > ALERT_THRESHOLD_VALUE -> {
                SensorState.WARN
            }
            else -> {
                SensorState.OK
            }
        }
    }

    private fun transitionAlertState(measurements: List<SensorMeasurement>): SensorState {
        val okMeasurements = measurements.filter { it.value < ALERT_THRESHOLD_VALUE }
        return if (okMeasurements.size >= ALERT_THRESHOLD_NUMBER) {
            SensorState.OK
        } else {
            SensorState.ALERT
        }
    }
}
