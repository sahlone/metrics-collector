package com.sahlone.mc.repository

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import com.sahlone.mc.models.ALERT_THRESHOLD_NUMBER
import com.sahlone.mc.models.ALERT_THRESHOLD_VALUE
import com.sahlone.mc.models.AlarmState
import com.sahlone.mc.models.MetricName
import com.sahlone.mc.models.SensorAlarm
import com.sahlone.mc.models.SensorMeasurement
import com.sahlone.mc.models.SensorMetrics
import com.sahlone.mc.models.SensorState
import com.sahlone.mc.models.SensorStatus
import com.sahlone.mc.utils.toDateTime
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.avg
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.joda.time.DateTime

class Repository(private val database: Database) {
    fun insertSensorData(sensorMeasurement: SensorMeasurement) {
        transaction(database) {
            SensorDataTable.insert { table ->
                table[id] = EntityID(
                    UUID.randomUUID(),
                    SensorDataTable
                )
                table[sensorId] = sensorMeasurement.sensorId
                table[metricName] = sensorMeasurement.metric.name
                table[metricValue] = sensorMeasurement.value
                table[timestamp] = sensorMeasurement.timestamp.toDateTime()
            }
        }
    }

    fun existSensorDataForTime(sensor: UUID, metricTime: Instant) = transaction(database) {
        with(SensorDataTable) {
            select {
                sensorId eq sensor and
                    timestamp.eq(metricTime.toDateTime())
            }.firstOrNull().toOption().fold({ false }, { true })
        }
    }

    fun getLastThreeSensorReadings(sensor: UUID) = transaction(database) {
        with(SensorDataTable) {
            select {
                sensorId eq sensor
            }.orderBy(timestamp to SortOrder.DESC, created to SortOrder.DESC).limit(3).map { row ->
                SensorMeasurement(
                    row[sensorId],
                    MetricName.valueOf(row[metricName]),
                    row[metricValue],
                    Instant.ofEpochMilli(row[timestamp].millis)
                )
            }
        }
    }
    fun getLatestSensorReading(sensor: UUID) = transaction(database) {
        with(SensorDataTable) {
            select {
                sensorId eq sensor
            }.orderBy(created to SortOrder.DESC).limit(3).map { row ->
                SensorMeasurement(
                    row[sensorId],
                    MetricName.valueOf(row[metricName]),
                    row[metricValue],
                    Instant.ofEpochMilli(row[timestamp].millis)
                )
            }.firstOrNull()
        }
    }

    fun deleteSensorData(sensor: UUID, offsetDate: LocalDate) = transaction {
        with(SensorDataTable) {
            deleteWhere {
                timestamp.less(offsetDate.toDateTime()) and sensorId.eq(sensor)
            }
        }
    }

    fun calculateSensorMetrics(sensor: UUID, offsetDate: LocalDate) = transaction(database) {
        with(SensorDataTable) {
            val count = metricValue.count().alias("count")
            val max = metricValue.max().alias("max")
            val avg = metricValue.avg().alias("avg")
            slice(sensorId, count, max, avg)
                .select {
                    sensorId eq sensor and
                        timestamp.greaterEq(offsetDate.toDateTime())
                }.groupBy(sensorId).map { row ->
                    SensorMetrics(
                        sensor,
                        sensor,
                        row[avg]?.toDouble() ?: 0.0,
                        row[max] ?: 0,
                        row[count],
                        Instant.now(),
                        Instant.now()
                    )
                }
        }.firstOrNull().toOption().getOrElse {
            SensorMetrics(UUID.randomUUID(), sensor, 0.0, 0, 0, Instant.now(), Instant.now())
        }
    }

    fun getSensorStatus(sensor: UUID): Option<SensorStatus> = transaction(database) {
        with(SensorStatusTable) {
            select {
                sensorId eq sensor
            }.limit(1).map {
                SensorStatus(it[sensorId], SensorState.valueOf(it[state]))
            }.firstOrNull().toOption()
        }
    }

    fun setSensorStatus(sensorStatus: SensorStatus) =
        transaction(database) {
            with(SensorStatusTable) {
                getSensorStatus(sensorStatus.sensorId).fold({
                    insert { table ->
                        table[id] = EntityID(
                            UUID.randomUUID(),
                            SensorStatusTable
                        )
                        table[sensorId] = sensorStatus.sensorId
                        table[state] = sensorStatus.state.name
                        table[created] = DateTime.now()
                        table[modified] = DateTime.now()
                    }
                }, {
                    update({ sensorId eq sensorStatus.sensorId }) { table ->
                        table[state] = sensorStatus.state.name
                        table[modified] = DateTime.now()
                    }
                })
            }
        }

    fun saveAlarm(sensorAlarm: SensorAlarm) = transaction(database) {
        with(SensorAlarmsTable) {
            insert { table ->
                table[id] = EntityID(sensorAlarm.id, SensorAlarmsTable)
                table[sensorId] = sensorAlarm.sensorId
                table[start] = DateTime(sensorAlarm.start.toEpochMilli())
                table[state] = sensorAlarm.state.name
            }
        }
    }

    fun updateAlarm(sensorAlarm: SensorAlarm) = transaction(database) {
        with(SensorAlarmsTable) {
            update({ id eq sensorAlarm.id }) { table ->
                table[end] = sensorAlarm.end?.toDateTime()
                table[state] = sensorAlarm.state.name
            }
        }
    }

    fun getAlarms(sensor: UUID, alarmState: AlarmState) = transaction(database) {
        with(SensorAlarmsTable) {
            select {
                sensorId eq sensor and
                    state.eq(alarmState.name)
            }.map {
                SensorAlarm(
                    it[id].value,
                    it[sensorId],
                    Instant.ofEpochMilli(it[start].millis),
                    if (it[end] != null) Instant.ofEpochMilli(it[end]?.millis ?: 0) else null,
                    AlarmState.valueOf(it[state])
                )
            }
        }
    }

    fun getAlarmMeasurements(alarm: SensorAlarm) = transaction(database) {
        with(SensorDataTable) {
            select {
                sensorId eq alarm.sensorId and
                    timestamp.lessEq(alarm.start.toDateTime()) and
                    metricValue.greaterEq(ALERT_THRESHOLD_VALUE)
            }.orderBy(timestamp to SortOrder.DESC, created to SortOrder.DESC).limit(ALERT_THRESHOLD_NUMBER)
                .map { row ->
                    SensorMeasurement(
                        row[sensorId],
                        MetricName.valueOf(row[metricName]),
                        row[metricValue],
                        Instant.ofEpochMilli(row[timestamp].millis)
                    )
                }
        }
    }

    fun getSensorMetrics(sensor: UUID) = transaction(database) {
        with(SensorMetricsTable) {
            select {
                sensorId eq sensor
            }.map {
                SensorMetrics(
                    it[id].value,
                    it[sensorId],
                    it[avg],
                    it[max],
                    it[count],
                    Instant.ofEpochMilli(it[created].millis),
                    Instant.ofEpochMilli(it[modified].millis)
                )
            }.firstOrNull().toOption()
        }
    }

    fun updateSensorMetrics(sensorMetrics: SensorMetrics) =
        transaction(database) {
            with(SensorMetricsTable) {
                getSensorMetrics(sensorMetrics.sensorId).fold({
                    insert { table ->
                        table[id] = EntityID(UUID.randomUUID(), SensorMetricsTable)
                        table[sensorId] = sensorMetrics.sensorId
                        table[avg] = sensorMetrics.avg
                        table[max] = sensorMetrics.max
                        table[count] = sensorMetrics.count
                        table[created] = sensorMetrics.created.toDateTime()
                        table[modified] = sensorMetrics.modified.toDateTime()
                    }
                }, {
                    update({ sensorId eq sensorMetrics.sensorId }) { table ->
                        table[avg] = sensorMetrics.avg
                        table[max] = sensorMetrics.max
                        table[count] = sensorMetrics.count
                        table[modified] = sensorMetrics.modified.toDateTime()
                    }
                })
            }
        }
}
