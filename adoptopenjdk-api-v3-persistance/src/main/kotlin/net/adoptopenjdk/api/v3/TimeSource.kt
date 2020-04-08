package net.adoptopenjdk.api.v3

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object TimeSource {
    fun now(): ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS)
    fun date(): LocalDate = LocalDate.now(ZoneOffset.UTC)
}
