package com.sahlone.mc.binder

import com.sahlone.mc.events.KafkaConsumer
import com.sahlone.mc.events.SensorDataConsumer
import com.sahlone.mc.events.SensorDataPublisher
import com.sahlone.mc.repository.Repository
import com.sahlone.mc.service.SensorAlertService
import com.sahlone.mc.service.SensorDataService
import com.sahlone.mc.service.SensorMetricsService
import com.sahlone.mc.service.SensorStatusService
import org.koin.dsl.module

object CoreModule {
    operator fun invoke() =
        module {
            single {
                Repository(get())
            }
            single {
                SensorAlertService(get())
            }
            single {
                SensorMetricsService(get())
            }
            single {
                SensorDataPublisher(get())
            }
            single {
                SensorDataService(get())
            }
            single {
                SensorStatusService(get())
            }
            single {
                val sensorDataConsumer = SensorDataConsumer(get(), get(), get(), get())
                val kafkaConsumer = KafkaConsumer(get(), sensorDataConsumer)
                kafkaConsumer.start()
                sensorDataConsumer
            }
        }
}
