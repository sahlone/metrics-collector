package com.sahlone.mc.binder

import com.sahlone.mc.config.MetricsCollectorConfig
import org.koin.dsl.module

object ConfigModule {

    operator fun invoke(metricsCollectorConfig: MetricsCollectorConfig) =
        module {
            single { metricsCollectorConfig }
            single { metricsCollectorConfig.dbConfig }
            single { metricsCollectorConfig.kafkaConfig }
        }
}
