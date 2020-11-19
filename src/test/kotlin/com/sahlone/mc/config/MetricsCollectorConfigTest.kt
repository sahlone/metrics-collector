package com.sahlone.mc.config

import arrow.core.getOrElse
import com.github.sahlone.klogging.TracingContext
import io.kotlintest.fail
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec

class MetricsCollectorConfigTest : ShouldSpec({

    should("load config if all the values are valid") {
        val config =
            ValidatedMetricsCollectorConfig.invoke(TracingContext(), "local.conf")
                .getOrElse { fail("config should be loaded") }
        config.port shouldBe 8080
        config.dbConfig.jdbcUrl shouldBe "jdbc:postgresql://localhost:5432/metrics_collector"
        config.dbConfig.username shouldBe "metrics_collector"
        config.dbConfig.password shouldBe "metrics_collector"
        config.kafkaConfig.bootstrapServers shouldBe "localhost:9092"
        config.kafkaConfig.groupId shouldBe "metrics-collector"
    }
    should("fail to load config if values are valid") {
        val config =
            ValidatedMetricsCollectorConfig.invoke(TracingContext(), "application.conf")
        config.isSuccess() shouldBe false
    }
})
