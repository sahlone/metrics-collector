package com.sahlone.mc.config

import com.sahlone.mc.binder.AppConfig

data class KafkaConfig(
    val bootstrapServers: String,
    val groupId: String,
    val clientId: String,
    val topic: String,
    val autoOffsetReset: String,
    val sessionTimeoutMs: Long,
    val maxPollRecords: Int,
    val heartbeatIntervalMs: Long,
    val requestTimeoutMs: Long,
    val autoCommit: Boolean,
    val maxPollIntervalMs: Long,
    val minPollsPerCommit: Int,
    val idempotence: Boolean
)
data class DbConfig(val jdbcUrl: String, val username: String, val password: String)

data class MetricsCollectorConfig(
    override val port: Int,
    val dbConfig: DbConfig,
    val kafkaConfig: KafkaConfig
) : AppConfig()
