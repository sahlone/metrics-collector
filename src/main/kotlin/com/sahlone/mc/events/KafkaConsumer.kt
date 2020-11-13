package com.sahlone.mc.events

import arrow.core.Either
import arrow.core.Try
import arrow.core.recover
import com.github.sahlone.klogging.TracingContext
import com.github.sahlone.klogging.createContextualLogger
import com.sahlone.kson.data.mapper.Json
import com.sahlone.kson.data.mapper.JsonError
import com.sahlone.kson.data.mapper.JsonSuccess
import com.sahlone.kson.data.mapper.JsonValue
import com.sahlone.mc.config.KafkaConfig
import com.sahlone.mc.models.Error
import com.sahlone.mc.models.EventMeta
import com.sahlone.mc.models.SensorEvent
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kafka.common.KafkaException
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException

class KafkaConsumer(
    private val config: KafkaConfig,
    private val handler: (TracingContext, EventMeta, JsonValue) -> Either<Error, Unit>
) : Runnable {
    private val tracingContext = TracingContext()
    private val logger = createContextualLogger()
    private val shutdown: AtomicBoolean = AtomicBoolean(false)
    private val started: CountDownLatch = CountDownLatch(1)
    private val shutdownLatch: CountDownLatch = CountDownLatch(1)
    private val configMap: Map<String, String> = mapOf(
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "${config.autoCommit}",
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to config.autoOffsetReset,
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to config.bootstrapServers,
        ConsumerConfig.CLIENT_ID_CONFIG to "${UUID.randomUUID()}",
        ConsumerConfig.GROUP_ID_CONFIG to config.groupId,
        ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to "${config.sessionTimeoutMs}",
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "${config.maxPollRecords}",
        ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG to "${config.heartbeatIntervalMs}",
        ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG to "${config.requestTimeoutMs}",
        ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "${config.maxPollIntervalMs}",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringDeserializer",
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringDeserializer"
    )
    private val thread: Thread =
        Thread(ThreadGroup("kafka-consumer"), this, "kafka-consumer-${config.groupId}-${Math.random()}")
    private val consumer = KafkaConsumer<String, String>(configMap)

    fun start() {
        logger.debug(tracingContext, { "config" to config }) { "Initializing kafka consumer" }
        thread.isDaemon = true
        thread.start()
        if (!started.await(config.maxPollIntervalMs, TimeUnit.MILLISECONDS)) {
            logger.debug(tracingContext, { "config" to config }) { "Error starting kafka consumer" }
            throw KafkaException("Error staring kafka consumer $config")
        }
    }

    override fun run() {

        Try {
            consumer.subscribe(listOf(config.topic))
            logger.debug(tracingContext, { "config" to config }) { "kafka consumer started" }
            started.countDown()
            while (!shutdown.get()) {
                Try {
                    (1..config.minPollsPerCommit).map {
                        val records = consumer.poll(Duration.ofMillis(Long.MAX_VALUE))
                        records.forEach { record ->
                            val tracingContext: TracingContext =
                                record.key()?.let { TracingContext(it) } ?: tracingContext
                            handleRecords(tracingContext, record)
                        }
                    }
                    commitSync(tracingContext)
                }.recover {
                    logger.errorWithCause(tracingContext, it) { "Error in kafka consumer" }
                }
            }
        }.recover {
            logger.errorWithCause(tracingContext, it) { "Error in kafka consumer" }
        }.map {
            consumer.close()
            shutdownLatch.countDown()
        }
    }

    private fun handleRecords(tracingContext: TracingContext, record: ConsumerRecord<String, String>) =
        Try {
            val parseResult = Json.parse(record.value())
                .flatMap {
                    SensorEvent.reads(it)
                }
            when (parseResult) {
                is JsonSuccess -> {
                    val (meta, data) = parseResult.value
                    when (handler(tracingContext, meta, data)) {
                        is Either.Left -> {
                            logger.error(tracingContext,
                                { "meta" to meta }, { "data" to data }) {
                                "Error handling sensor event for aggregateId:" +
                                    "${meta.aggregateId},entityId:${meta.entityId}"
                            }
                        }
                        is Either.Right -> Unit
                    }
                }

                is JsonError -> logger.error(
                    tracingContext,
                    { "event" to record.value() },
                    { "err" to parseResult.errors.all.toString() }) { "Error parsing sensor event." }
            }
        }.recover {
            logger.errorWithCause(
                tracingContext, it, { "record" to record.value() }) { "Error handling sensor event" }
        }

    private fun commitSync(tracingContext: TracingContext, retryCount: Int = 3): Try<Unit> = Try {
        consumer.commitSync()
    }.recover {
        when (it) {
            is WakeupException ->
                if (retryCount > 0) {
                    commitSync(
                        tracingContext, retryCount - 1
                    )
                } else {
                    logger.errorWithCause(
                        tracingContext,
                        it,
                        { "groupId" to config.groupId }) { "Error committing kafka offset" }
                }
            else ->
                logger.errorWithCause(
                    tracingContext,
                    it,
                    { "groupId" to config.groupId }) { "Error committing kafka offset" }
        }
    }

    fun shutdown(tracingContext: TracingContext) = Try {
        shutdown.set(true)
        shutdownLatch.await(config.maxPollIntervalMs, TimeUnit.MILLISECONDS)
        logger.debug(tracingContext, { "groupId" to config.groupId }) { "Shutting down kafka consumer" }
    }.recover {
        logger.errorWithCause(tracingContext, it) { "Error shutting down kafka consumer" }
    }
}
