package net.adoptopenjdk.api.v3.mapping

import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.models.Release
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

abstract class ReleaseMapper {
    abstract suspend fun toAdoptRelease(release: GHRelease): Release?


    companion object {
        fun parseDate(date: String): ZonedDateTime {
            return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(date))
                    .atZone(ZoneId.of("Z"))

        }
    }

}