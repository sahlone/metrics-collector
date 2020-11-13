package com.sahlone.mc.modules

import arrow.core.Try
import arrow.core.recoverWith
import arrow.instances.`try`.applicativeError.raiseError
import com.github.sahlone.klogging.TracingContext
import com.github.sahlone.klogging.createContextualLogger
import javax.sql.DataSource
import org.flywaydb.core.Flyway

class DbMigrations(
    private val location: String,
    private val ds: DataSource
) {

    private val logger = createContextualLogger()

    fun migrate(tracingContext: TracingContext) = Try {
        logger.debug(tracingContext) { "Applying database migrations" }
        with(Flyway.configure()) {
            baselineOnMigrate(false)
            locations(location)
            dataSource(ds).load().migrate()
        }
        logger.debug(tracingContext) { "Database migrations applied" }
    }.recoverWith {
        logger.errorWithCause(tracingContext, it) { "Database migrations failed" }
        it.raiseError()
    }
}
