package com.sahlone.mc.modules

import com.sahlone.mc.handler.HealthCheckHandler
import org.koin.dsl.module

object HealthCheckModule {

    operator fun invoke() =
        module {
            single {
                HealthCheckHandler
            }
        }
}
