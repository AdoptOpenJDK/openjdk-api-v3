package net.adoptopenjdk.api.v3.mapping

import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.models.Release
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

abstract class ReleaseMapper {
    abstract suspend fun toAdoptRelease(release: GHRelease): Release


    fun parseDate(date: String) =
            Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(date)).atZone(ZoneId.of("UTC")).toLocalDateTime()

}