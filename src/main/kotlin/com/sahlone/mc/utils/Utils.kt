package com.sahlone.mc.utils

import arrow.core.Either
import arrow.core.Try
import com.github.sahlone.klogging.TracingContext
import com.sahlone.mc.models.Error
import com.sahlone.mc.models.Error.Companion.InvalidRequestData
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.http4k.core.Request
import org.http4k.routing.path
import org.joda.time.DateTime

fun Request.uuidPath(tracingContext: TracingContext, path: String): Either<Error, UUID> = Try {
    UUID.fromString(this.path(path))
}.toEither {
    InvalidRequestData(tracingContext, "$path is not a valid uuid")
}

fun Instant.toDateTime(): DateTime = DateTime.parse(this@toDateTime.toString())
fun LocalDate.toDateTime(): DateTime = DateTime.parse(this@toDateTime.toString())
