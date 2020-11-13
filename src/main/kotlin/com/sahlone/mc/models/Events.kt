package com.sahlone.mc.models

import com.github.sahlone.klogging.TracingContext
import com.sahlone.kson.data.mapper.Json
import com.sahlone.kson.data.mapper.JsonObject
import com.sahlone.kson.data.mapper.JsonObjectReads
import com.sahlone.kson.data.mapper.JsonReads
import com.sahlone.kson.data.mapper.JsonResult
import com.sahlone.kson.data.mapper.JsonValue
import com.sahlone.kson.data.mapper.JsonWrites
import com.sahlone.kson.data.mapper.at
import java.time.Instant
import java.util.UUID

data class SensorEvent<T : EventData>(val meta: EventMeta, val data: T) {
    companion object {
        inline fun <reified T : EventData> read(): JsonReads<SensorEvent<T>> = {
            it(
                Json.jsonReads<EventMeta>() at "meta",
                Json.jsonReads<T>() at "data"
            ) { a, b -> SensorEvent(a, b) }
        }

        val reads: (JsonValue) -> JsonResult<Pair<EventMeta, JsonObject>> = {
            it(
                Json.jsonReads<EventMeta>() at "meta",
                JsonObjectReads at "data"
            ) { a, b -> Pair(a, b) }
        }

        inline fun <reified T : EventData> writes(): JsonWrites<SensorEvent<T>> = {
            Json(
                Json.jsonWrites<EventMeta>()(it.meta) at "meta",
                Json.jsonWrites<T>()(it.data) at "data"
            )
        }
    }
}

data class EventMeta(
    val id: UUID,
    val aggregateId: UUID,
    val entityId: UUID,
    val eventType: EventType,
    val occurredAt: Instant,
    val tracingContext: TracingContext,
    val version: String = "v1.0"
)

sealed class EventData {

    companion object {

        inline fun <reified T : EventData> reads(): JsonReads<T> = Json.jsonReads()

        fun <T : EventData> writes(): JsonWrites<T> = {
            Json.jsonWrites<T>()(it)
        }
    }
}

data class SensorMeasurement(
    val sensorId: UUID,
    val metric: MetricName,
    val value: Int,
    val timestamp: Instant
) : EventData()

enum class EventType {
    SENSOR_DATA_PUT
}
