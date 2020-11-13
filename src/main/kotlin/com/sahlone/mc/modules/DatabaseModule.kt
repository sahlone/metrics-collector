package com.sahlone.mc.modules

import com.sahlone.mc.config.DbConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module
import org.koin.dsl.module

object DatabaseModule {
    operator fun invoke(): Module =
        module {
            single<DataSource> {
                val dbConfig: DbConfig = get()
                val config = HikariConfig()
                config.jdbcUrl = dbConfig.jdbcUrl
                config.username = dbConfig.username
                config.password = dbConfig.password
                HikariDataSource(config)
            }

            single {
                val ds: DataSource = get()
                Database.connect(ds)
            }

            single {
                val ds: DataSource = get()
                DbMigrations("classpath:/db", ds)
            }
        }
}
