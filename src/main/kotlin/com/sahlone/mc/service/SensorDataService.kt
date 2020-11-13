package com.sahlone.mc.service

import com.github.sahlone.klogging.TracingContext
import com.github.sahlone.klogging.createContextualLogger
import com.sahlone.mc.models.SensorMeasurement
import com.sahlone.mc.repository.Repository

class SensorDataService(private val repository: Repository) {

    private val logger = createContextualLogger()
    fun putSensorMeasurementData(tracingContext: TracingContext, sensorMeasurement: SensorMeasurement) {
        if (!repository.existSensorDataForTime(sensorMeasurement.sensorId, sensorMeasurement.timestamp)) {
            repository.insertSensorData(sensorMeasurement).also {
                logger.debug(tracingContext, { "sensor_data" to sensorMeasurement }) {
                    "Sensor Data updated for sensorId:${sensorMeasurement.sensorId}"
                }
            }
        }
    }
}
