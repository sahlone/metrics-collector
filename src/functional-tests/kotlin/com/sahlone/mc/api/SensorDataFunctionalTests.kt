package com.sahlone.mc.api

import com.sahlone.mc.utils.withRestFixtures
import io.kotlintest.matchers.string.beEmpty
import io.kotlintest.shouldBe
import io.kotlintest.shouldNot
import io.kotlintest.specs.ShouldSpec
import io.restassured.path.json.JsonPath
import java.time.Instant
import java.util.UUID
import org.http4k.core.Status

class SensorDataFunctionalTests : ShouldSpec({

    val path = "sensors/{uuid}/measurements"

    data class SensorData(val co2: Int, val time: Instant)
    "Sensor data input endpoint" {
        should("successfully save sensor measurements") {
            val data = SensorData(200, Instant.now())
            withRestFixtures(data = data, path = path.replace("{uuid}", UUID.randomUUID().toString())) {
                statusCode shouldBe Status.OK.code
                val json = JsonPath.from(body.asString())
                json.getString("sensorId") shouldNot beEmpty()
                json.getString("metric") shouldBe "Co2"
                json.getInt("value") shouldBe 200
                json.getString("timestamp") shouldBe data.time.toString()
            }
        }
    }
})
