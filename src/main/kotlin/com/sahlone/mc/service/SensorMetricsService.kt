package com.sahlone.mc.service

import com.github.sahlone.klogging.TracingContext
import com.github.sahlone.klogging.createContextualLogger
import com.sahlone.mc.models.NO_OF_DAYS_THRESHOLD
import com.sahlone.mc.models.SensorMetrics
import com.sahlone.mc.repository.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class SensorMetricsService(private val repository: Repository) {

    private val logger = createContextualLogger()

    fun updateSensorMetrics(tracingContext: TracingContext, sensorId: UUID) {
        repository.getSensorMetrics(sensorId).toEither {
            repository.updateSensorMetrics(
                recalculateMetrics(sensorId)
            )
        }.map { sm ->
            val today = LocalDate.now()
            val lastModified = LocalDateTime.ofInstant(sm.modified, ZoneOffset.UTC).toLocalDate()
            val sensorMetrics = if (lastModified.isBefore(today)) {
                recalculateMetrics(sensorId)
            } else {
                sm
            }
            repository.getLatestSensorReading(sensorId)?.let {
                val latestReadingTimestamp = LocalDateTime.ofInstant(it.timestamp, ZoneOffset.UTC).toLocalDate()
                if (latestReadingTimestamp.isAfter(today.minusDays(NO_OF_DAYS_THRESHOLD - 1))) {
                    val newMax =
                        if (it.value > sensorMetrics.max) it.value else sensorMetrics.max
                    val newCount = sensorMetrics.count + 1
                    val newAvg = (it.value + sensorMetrics.avg * sensorMetrics.count) / newCount
                    val updatedMetrics = sensorMetrics.copy(
                        avg = newAvg,
                        count = newCount,
                        max = newMax
                    )
                    repository.updateSensorMetrics(updatedMetrics)
                    logger.debug(tracingContext, { "sensor_metrics" to updatedMetrics }) {
                        "sensor metrics updated for sensor $sensorId"
                    }
                }
            }
        }
    }

    private fun recalculateMetrics(sensorId: UUID): SensorMetrics {
        val today = LocalDate.now()
        val offset = today.minusDays(NO_OF_DAYS_THRESHOLD)
        repository.deleteSensorData(sensorId, offset)
        return repository.calculateSensorMetrics(sensorId, offset)
    }
}
