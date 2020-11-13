package com.sahlone.mc.routes

import com.sahlone.mc.filter.HttpExceptionFilter
import com.sahlone.mc.handler.HealthCheckHandler
import com.sahlone.mc.handler.SensorAlarmsHandler
import com.sahlone.mc.handler.SensorDataHandler
import com.sahlone.mc.handler.SensorMetricsHandler
import com.sahlone.mc.handler.SensorStatusHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.RequestContexts
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

class Router(
    private val healthCheckHandler: HealthCheckHandler,
    private val sensorDataHandler: SensorDataHandler,
    private val sensorStatusHandler: SensorStatusHandler,
    private val sensorMetricsHandler: SensorMetricsHandler,
    private val sensorAlarmsHandler: SensorAlarmsHandler
) {

    private val contexts = RequestContexts()

    private val apiVersioningRouter: (RoutingHttpHandler) -> RoutingHttpHandler = {
        routes(
            "/api/v1" bind it
        )
    }
    val healthCheckRouter: (HealthCheckHandler) -> RoutingHttpHandler = {
        routes(
            "/health" bind GET to it,
            "/health/_extended" bind GET to it
        )
    }

    val publicApiRouter: () -> RoutingHttpHandler = {
        routes(
            "/sensors/{uuid}/measurements" bind POST to sensorDataHandler,
            "/sensors/{uuid}" bind GET to sensorStatusHandler,
            "/sensors/{uuid}/metrics" bind GET to sensorMetricsHandler,
            "/sensors/{uuid}/alerts" bind GET to sensorAlarmsHandler
        )
    }

    fun asHandler(): RoutingHttpHandler =
        HttpExceptionFilter()
            .then(ServerFilters.InitialiseRequestContext(contexts))
            .then(
                routes(
                    healthCheckRouter(healthCheckHandler),
                    apiVersioningRouter(
                        publicApiRouter()
                    )
                )
            )
}
