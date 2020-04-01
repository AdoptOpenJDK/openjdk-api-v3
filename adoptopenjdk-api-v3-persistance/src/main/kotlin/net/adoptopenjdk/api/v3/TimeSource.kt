package net.adoptopenjdk.api.v3

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object TimeSource {
    public val ZONE = ZoneId.of("Z")
    fun now(): ZonedDateTime = ZonedDateTime.now(ZONE).truncatedTo(ChronoUnit.SECONDS)
    fun date(): LocalDate = LocalDate.now(ZONE)
}