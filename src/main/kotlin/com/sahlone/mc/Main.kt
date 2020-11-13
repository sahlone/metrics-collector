package com.sahlone.mc

import arrow.data.NonEmptyList
import com.sahlone.mc.binder.Bootstrap
import com.sahlone.mc.binder.ConfigModule
import com.sahlone.mc.binder.CoreModule
import com.sahlone.mc.binder.HandlerModule
import com.sahlone.mc.binder.RouterModule
import com.sahlone.mc.config.ValidatedMetricsCollectorConfig

fun main() {
    Bootstrap({
        ValidatedMetricsCollectorConfig(it)
    }, {
        NonEmptyList.of(
            ConfigModule(it),
            CoreModule(),
            HandlerModule(),
            RouterModule()
        )
    }, { it.port })()
}
