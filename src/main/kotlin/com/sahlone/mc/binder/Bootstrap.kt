package com.sahlone.mc.binder

import arrow.core.Try
import arrow.core.recoverWith
import arrow.data.NonEmptyList
import com.github.sahlone.klogging.TracingContext
import com.github.sahlone.klogging.createContextualLogger
import com.sahlone.mc.modules.DatabaseModule
import com.sahlone.mc.modules.DbMigrations
import com.sahlone.mc.modules.HealthCheckModule
import kotlin.system.exitProcess
import org.http4k.routing.RoutingHttpHandler
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.get
import org.koin.core.module.Module

abstract class AppConfig {
    abstract val port: Int
}

class Bootstrap<T : AppConfig>(
    val config: (TracingContext) -> Try<T>,
    val initModules: (T) -> NonEmptyList<Module>,
    val port: (T) -> Int
) : KoinComponent {
    private val logger = createContextualLogger()
    operator fun invoke(): Try<Unit> {
        val tracingContext = TracingContext()
        return config(tracingContext)
            .map { appConfig ->
                koinModules(appConfig)
                appConfig
            }.flatMap { appConfig ->
                get<DbMigrations>()
                    .migrate(tracingContext).map {
                        appConfig
                    }
            }.map { appConfig ->
                val router: RoutingHttpHandler = get()
                val serverPort = port(appConfig)
                val server = router.asServer(Netty(serverPort)).start()
                logger.debug(TracingContext(), { "port" to serverPort }) { "Service started successfully" }
                server.block()
            }.recoverWith {
                logger.errorWithCause(tracingContext, it) { "Error starting service" }
                exitProcess(1)
            }
    }

    private fun koinModules(config: T) =
        startKoin {
            createEagerInstances()
            modules(
                HealthCheckModule()
            )
            modules(
                initModules(config).all
            )
            modules(
                DatabaseModule()
            )
        }
}
