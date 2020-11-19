package com.sahlone.mc.filter

import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

class ExceptionFilterTest : ShouldSpec({
    val filter = HttpExceptionFilter()

    should("catch an exception from downstream handlers") {
        val exceptionResponseProvider: (Request) -> Response = {
            throw IllegalArgumentException()
        }
        val requestHandler =
            filter(exceptionResponseProvider)

        val response = requestHandler(Request(Method.GET, ""))
        response.status shouldBe Status.INTERNAL_SERVER_ERROR
        response.bodyString() shouldBe ""
    }

    should("give correct status code for HttpException") {
        val exceptionResponseProvider: (Request) -> Response = {
            throw HttpException(Status.BAD_GATEWAY, "some error message")
        }
        val requestHandler =
            filter(exceptionResponseProvider)

        val response = requestHandler(Request(Method.GET, ""))
        response.status shouldBe Status.BAD_GATEWAY
    }
})
