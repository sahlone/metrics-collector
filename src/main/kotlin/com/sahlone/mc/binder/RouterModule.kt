package com.sahlone.mc.binder

import com.sahlone.mc.handler.HealthCheckHandler
import com.sahlone.mc.handler.SensorAlarmsHandler
import com.sahlone.mc.handler.SensorDataHandler
import com.sahlone.mc.handler.SensorMetricsHandler
import com.sahlone.mc.handler.SensorStatusHandler
import com.sahlone.mc.routes.Router
import com.sahlone.mc.events.SensorDataConsumer
import org.koin.dsl.module

object RouterModule {
    operator fun invoke() =
        module {
            single {
                val healthCheckHandler: HealthCheckHandler = get()
                val sensorDataHandler: SensorDataHandler = get()
                val sensorMetricsHandler: SensorMetricsHandler = get()
                val sensorAlarmsHandler: SensorAlarmsHandler = get()
                val sensorStatusHandler: SensorStatusHandler = get()
                get<SensorDataConsumer>()
                Router(
                    healthCheckHandler,
                    sensorDataHandler,
                    sensorStatusHandler,
                    sensorMetricsHandler,
                    sensorAlarmsHandler
                ).asHandler()
            }
        }
}
