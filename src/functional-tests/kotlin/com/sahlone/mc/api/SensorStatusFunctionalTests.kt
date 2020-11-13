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

class SensorStatusFunctionalTests : ShouldSpec({

    val putPath = "sensors/{uuid}/measurements"
    val statusPath = "sensors/{uuid}"

    data class SensorData(val co2: Int, val time: Instant)
    "Sensor status endpoint" {
        should("successfully report sensor status for ok") {
            val sensorId = UUID.randomUUID().toString()
            repeat(10) {
                val data = SensorData((Math.random() * 2000).toInt(), Instant.now())
                withRestFixtures(data = data, path = putPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                }
            }
            eventually(TEST_WAIT_DURATION) {
                withGetRestFixtures(statusPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                    val json = JsonPath.from(body.asString())
                    json.getString("status") shouldBe "OK"
                }
            }
        }

        should("successfully report sensor status for warn") {
            val sensorId = UUID.randomUUID().toString()
            val data = SensorData(2401, Instant.now())
            withRestFixtures(data = data, path = putPath.replace("{uuid}", sensorId)) {
                statusCode shouldBe Status.OK.code
            }

            eventually(TEST_WAIT_DURATION) {
                withGetRestFixtures(statusPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                    val json = JsonPath.from(body.asString())
                    json.getString("status") shouldBe "WARN"
                }
            }
        }

        should("successfully report sensor status for warn for 2 threshold breaches") {
            val sensorId = UUID.randomUUID().toString()
            repeat(2) {
                val data = SensorData(2401 + it, Instant.now())
                withRestFixtures(data = data, path = putPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                }
            }

            eventually(TEST_WAIT_DURATION) {
                withGetRestFixtures(statusPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                    val json = JsonPath.from(body.asString())
                    json.getString("status") shouldBe "WARN"
                }
            }
        }
        should("successfully report sensor status for Alert") {
            val sensorId = UUID.randomUUID().toString()
            repeat(3) {
                val data = SensorData(3201 + it, Instant.now())
                withRestFixtures(data = data, path = putPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                }
            }

            eventually(TEST_WAIT_DURATION) {
                withGetRestFixtures(statusPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                    val json = JsonPath.from(body.asString())
                    json.getString("status") shouldBe "ALERT"
                }
            }
        }

        should("successfully report sensor status Ok After alert is closed") {
            val sensorId = UUID.randomUUID().toString()
            repeat(3) {
                val data = SensorData(3201 + it, Instant.now())
                withRestFixtures(data = data, path = putPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                }
            }

            eventually(TEST_WAIT_DURATION) {
                withGetRestFixtures(statusPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                    val json = JsonPath.from(body.asString())
                    json.getString("status") shouldBe "ALERT"
                }
            }
            repeat(3) {
                val data = SensorData(1800 + it, Instant.now())
                withRestFixtures(data = data, path = putPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                }
            }
            eventually(TEST_WAIT_DURATION) {
                withGetRestFixtures(statusPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                    val json = JsonPath.from(body.asString())
                    json.getString("status") shouldBe "OK"
                }
            }
        }

        should("successfully give 404 for non existent sensor") {
            val sensorId = UUID.randomUUID().toString()
            withGetRestFixtures(statusPath.replace("{uuid}", sensorId)) {
                statusCode shouldBe Status.NOT_FOUND.code
            }
        }
    }
})
