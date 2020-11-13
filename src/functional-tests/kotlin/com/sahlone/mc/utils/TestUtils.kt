package com.sahlone.mc.utils

import io.restassured.RestAssured
import io.restassured.http.Method
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import java.time.Duration

val TEST_WAIT_DURATION: Duration = Duration.ofSeconds(10)
private const val baseUrl = "http://localhost:8080/api/v1"
fun <T, U> withRestFixtures(
    data: T,
    path: String,
    method: Method = Method.POST,
    block: RequestSpecification.() -> Unit = {},
    block1: Response.() -> U
): U =
    with(
        RestAssured.given()
            .header("Content-Type", "application/json")
            .body(data)
    ) {
        block()
        this
    }.request(method, "$baseUrl/$path").block1()

fun <U> withGetRestFixtures(
    path: String,
    block: RequestSpecification.() -> Unit = {},
    block1: Response.() -> U
): U =
    with(
        RestAssured.given()
            .header("Content-Type", "application/json")
    ) {
        block()
        this
    }.get("$baseUrl/$path").block1()

fun <U> withDeleteRestFixtures(
    path: String,
    block: RequestSpecification.() -> Unit = {},
    block1: Response.() -> U
): U =
    with(RestAssured.given()) {
        block()
        this
    }.delete("$baseUrl/$path").block1()
