package com.sahlone.mc.config

import arrow.core.Try
import arrow.core.recoverWith
import arrow.instances.`try`.applicativeError.raiseError
import com.github.sahlone.klogging.TracingContext
import com.github.sahlone.klogging.createContextualLogger
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

object ValidatedMetricsCollectorConfig {

    private val logger = createContextualLogger()

    operator fun invoke(tracingContext: TracingContext, configFile: String? = null): Try<MetricsCollectorConfig> = Try {
        val config = if (configFile.isNullOrBlank()) {
            ConfigFactory.load().getConfig("metricsCollector")
        } else {
            ConfigFactory.load(configFile).getConfig("metricsCollector")
        }
        val dbConfig = config.extract<DbConfig>("dbConfig")
        val port = config.getInt("port")
        val kafkaConfig = config.extract<KafkaConfig>("kafka")
        MetricsCollectorConfig(
            port = port,
            dbConfig = dbConfig,
            kafkaConfig = kafkaConfig
        ).also {
            logger.debug(tracingContext, { "config" to it }) { "App Configuration loaded" }
        }
    }.recoverWith { error ->
        logger.errorWithCause(tracingContext, error) { "Error create App config" }
        error.raiseError()
    }
}
