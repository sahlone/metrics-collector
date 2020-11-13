package com.sahlone.mc.binder

import com.sahlone.mc.handler.SensorAlarmsHandler
import com.sahlone.mc.handler.SensorDataHandler
import com.sahlone.mc.handler.SensorMetricsHandler
import com.sahlone.mc.handler.SensorStatusHandler
import org.koin.dsl.module

object HandlerModule {
    operator fun invoke() =
        module {
            single {
                SensorAlarmsHandler(get())
            }
            single {
                SensorDataHandler(get())
            }
            single {
                SensorMetricsHandler(get())
            }
            single {
                SensorStatusHandler(get())
            }
        }
}
