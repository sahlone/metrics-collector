package com.sahlone.mc.api

import com.sahlone.mc.utils.TEST_WAIT_DURATION
import com.sahlone.mc.utils.withGetRestFixtures
import com.sahlone.mc.utils.withRestFixtures
import io.kotlintest.eventually
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import io.restassured.path.json.JsonPath
import java.time.Instant
import java.util.UUID
import org.http4k.core.Status

class SensorMetricsFunctionalTests : ShouldSpec({

    val putPath = "sensors/{uuid}/measurements"
    val metricsPath = "sensors/{uuid}/metrics"

    data class SensorData(val co2: Int, val time: Instant)
    "Sensor metrics endpoint" {
        should("successfully report sensor metrics") {
            val sensorId = UUID.randomUUID().toString()
            repeat(11) {
                val data = SensorData(2000 + it, Instant.now())
                withRestFixtures(data = data, path = putPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                }
            }
            eventually(TEST_WAIT_DURATION) {
                withGetRestFixtures(metricsPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                    val json = JsonPath.from(body.asString())
                    json.getInt("maxLast30Days") shouldBe 2010
                    json.getDouble("avgLast30Days").toInt() shouldBe 2005
                }
            }
        }

        should("successfully give 404 for non existent sensor") {
            val sensorId = UUID.randomUUID().toString()
            withGetRestFixtures(metricsPath.replace("{uuid}", sensorId)) {
                statusCode shouldBe Status.NOT_FOUND.code
            }
        }
    }
})
