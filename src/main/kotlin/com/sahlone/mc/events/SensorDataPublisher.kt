package com.sahlone.mc.events

import arrow.core.Either
import arrow.core.Option
import arrow.core.Try
import com.github.sahlone.klogging.TracingContext
import com.github.sahlone.klogging.createContextualLogger
import com.sahlone.kson.data.mapper.Json
import com.sahlone.kson.data.mapper.unsafeFix
import com.sahlone.mc.config.KafkaConfig
import com.sahlone.mc.models.Error
import com.sahlone.mc.models.Error.Companion.InternalServerError
import com.sahlone.mc.models.EventData
import com.sahlone.mc.models.EventMeta
import com.sahlone.mc.models.EventType
import com.sahlone.mc.models.SensorEvent
import java.time.Instant
import java.util.UUID
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord

class SensorDataPublisher(val config: KafkaConfig) {

    val logger = createContextualLogger()
    private val configMap: Map<String, String> = mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to config.bootstrapServers,
        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to "${config.idempotence}",
        ProducerConfig.LINGER_MS_CONFIG to "1",
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer"
    )
    val producer = KafkaProducer<String, String>(configMap)

    inline fun <reified T : EventData> send(
        tracingContext: TracingContext,
        eventType: EventType,
        aggregateId: UUID,
        entityId: UUID,
        data: T
    ): Either<Error, SensorEvent<T>> =
        Try {
            val messageId = UUID.randomUUID()
            val event = SensorEvent(
                EventMeta(messageId, aggregateId, entityId, eventType, Instant.now(), tracingContext),
                data
            )
            producer.send(
                ProducerRecord<String, String>(
                    config.topic,
                    event.meta.aggregateId.toString(),
                    Json.stringify(SensorEvent.writes<T>()(event)).unsafeFix()
                )
            ) { record, error ->
                Option.fromNullable(error).map {
                    logger.errorWithCause(tracingContext, error,
                        { "event" to event }) { "Error sending sensor event to kafka" }
                }
                Option.fromNullable(record).map {
                    logger.debug(
                        tracingContext,
                        { "eventType" to eventType },
                        { "event" to event }) { "Successfully sent sensor event to kafka" }
                }
            }
            event
        }.toEither().mapLeft<Error> {
            logger.errorWithCause(
                tracingContext,
                it,
                { "eventType" to eventType },
                { "data" to data }) { "Error sending  sensor event to kafka" }
            InternalServerError(tracingContext, "Error sending sensor event to kafka")
        }
}
