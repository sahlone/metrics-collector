package com.sahlone.mc.repository

import java.util.UUID
import org.jetbrains.exposed.dao.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.joda.time.DateTime

object SensorDataTable : UUIDTable("sensor_data", "sd_id") {
    val sensorId: Column<UUID> = uuid("sd_sensor_id")
    val metricName: Column<String> = text("sd_metric_name")
    val metricValue: Column<Int> = integer("sd_metric_value")
    val timestamp: Column<DateTime> = datetime("sd_timestamp")
    val created: Column<DateTime> = datetime("_created")
}

object SensorAlarmsTable : UUIDTable("sensor_alarms", "sa_id") {
    val sensorId: Column<UUID> = uuid("sa_sensor_id")
    val start: Column<DateTime> = datetime("sa_start")
    val end: Column<DateTime?> = datetime("sa_end").nullable()
    val state: Column<String> = text("sa_state")
}

object SensorStatusTable : UUIDTable("sensor_status", "ss_id") {
    val sensorId: Column<UUID> = uuid("ss_sensor_id")
    val state: Column<String> = text("ss_state")
    val created: Column<DateTime> = datetime("ss_created")
    val modified: Column<DateTime> = datetime("ss_modified")
}

object SensorMetricsTable : UUIDTable("sensor_metrics", "sm_id") {
    val sensorId: Column<UUID> = uuid("sm_sensor_id")
    val avg: Column<Double> = double("sm_avg")
    val max: Column<Int> = integer("sm_max")
    val count: Column<Int> = integer("sm_count")
    val created: Column<DateTime> = datetime("sm_created")
    val modified: Column<DateTime> = datetime("sm_modified")
}
