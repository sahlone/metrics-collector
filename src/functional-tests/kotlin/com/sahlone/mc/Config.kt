package com.sahlone.mc

import com.sahlone.mc.config.DbConfig
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

object Config {

    fun getDatabaseConfig(): DbConfig = loadConfig().extract("dbConfig")

    private fun loadConfig(): Config {
        return ConfigFactory.load().getConfig("metricsCollector")
    }
}
