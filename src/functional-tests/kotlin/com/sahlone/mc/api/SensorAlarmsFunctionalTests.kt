package com.sahlone.mc.api

import com.sahlone.mc.utils.TEST_WAIT_DURATION
import com.sahlone.mc.utils.withGetRestFixtures
import com.sahlone.mc.utils.withRestFixtures
import io.kotlintest.eventually
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.ShouldSpec
import io.restassured.path.json.JsonPath
import java.time.Instant
import java.util.Objects
import java.util.UUID
import org.http4k.core.Status

class SensorAlarmsFunctionalTests : ShouldSpec({

    val putPath = "sensors/{uuid}/measurements"
    val alertsPath = "sensors/{uuid}/alerts"

    data class SensorData(val co2: Int, val time: Instant)
    "Sensor alarms endpoint" {
        should("create an alarm for 3 consecutive alert metrics") {
            val sensorId = UUID.randomUUID().toString()
            repeat(4) {
                val data = SensorData(3241 + it, Instant.now())
                withRestFixtures(data = data, path = putPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                }
            }
            eventually(TEST_WAIT_DURATION) {
                withGetRestFixtures(alertsPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                    val json = JsonPath.from(body.asString())
                    json.getList<JsonPath>("$").size shouldBe 1
                    json.getList<Objects>("$").withIndex().forEach {
                        val index = it.index
                        json.getString("[$index].endTime") shouldBe null
                        json.getInt("[$index].measurement1") shouldBe 3243
                        json.getInt("[$index].measurement2") shouldBe 3242
                        json.getInt("[$index].measurement3") shouldBe 3241
                    }
                }
            }
        }

        should("close an alarm for 3 consecutive ok metrics after an alert") {
            val sensorId = UUID.randomUUID().toString()
            repeat(3) {
                val data = SensorData(3212 + it, Instant.now())
                withRestFixtures(data = data, path = putPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                }
            }
            repeat(2) {
                val data = SensorData(1281 + it, Instant.now())
                withRestFixtures(data = data, path = putPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                }
            }
            val closedData = SensorData(1000, Instant.now())
            withRestFixtures(data = closedData, path = putPath.replace("{uuid}", sensorId)) {}
            eventually(TEST_WAIT_DURATION) {
                withGetRestFixtures(alertsPath.replace("{uuid}", sensorId)) {
                    statusCode shouldBe Status.OK.code
                    val json = JsonPath.from(body.asString())
                    json.getList<JsonPath>("$").size shouldBe 1
                    json.getList<Objects>("$").withIndex().forEach {
                        val index = it.index
                        json.getString("[$index].endTime") shouldBe closedData.time.toString()
                    }
                }
            }
        }
        should("open and close multiple alarms") {
            val sensorId = UUID.randomUUID().toString()
            repeat(5) {

                repeat(3) {
                    val data = SensorData(4000 + it, Instant.now())
                    withRestFixtures(data = data, path = putPath.replace("{uuid}", sensorId)) {
                        statusCode shouldBe Status.OK.code
                    }
                }
                repeat(3) {
                    val data = SensorData(1200 + it, Instant.now())
                    withRestFixtures(data = data, path = putPath.replace("{uuid}", sensorId)) {
                        statusCode shouldBe Status.OK.code
                    }
                }
                eventually(TEST_WAIT_DURATION) {
                    withGetRestFixtures(alertsPath.replace("{uuid}", sensorId)) {
                        statusCode shouldBe Status.OK.code
                        val json = JsonPath.from(body.asString())
                        json.getList<JsonPath>("$").size shouldBe it
                        json.getList<Objects>("$").withIndex().forEach {
                            val index = it.index
                            json.getString("[$index].endTime") shouldNotBe null
                        }
                    }
                }
            }
        }
        should("successfully give empty list for non existent sensor") {
            val sensorId = UUID.randomUUID().toString()
            withGetRestFixtures(alertsPath.replace("{uuid}", sensorId)) {
                statusCode shouldBe Status.OK.code
                val json = JsonPath.from(body.asString())
                json.getList<JsonPath>("$").size shouldBe 0
            }
        }
    }
})
