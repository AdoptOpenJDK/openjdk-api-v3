package net.adoptopenjdk.api.v3.dataSources.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import net.adoptopenjdk.api.v3.dataSources.SortOrder
import net.adoptopenjdk.api.v3.dataSources.filters.ReleaseFilter
import net.adoptopenjdk.api.v3.models.Release
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class Releases {

    @JsonProperty("nodes")
    val nodes: Map<String, Release>

    @JsonIgnore
    val nodeList: TreeSet<Release> = TreeSet(VERSION_THEN_TIME_SORTER)

    constructor(nodes: List<Release>) {
        this.nodes = nodes
                .map { it.id to it }
                .toMap()
        nodeList.addAll(nodes)
    }

    @JsonCreator
    constructor(@JsonProperty("nodes") nodes: Map<String, Release>) {
        this.nodes = nodes
        nodeList.addAll(nodes.values)
    }

    @JsonIgnore
    fun getReleases(filter: ReleaseFilter, sortOrder: SortOrder): Sequence<Release> {
        return getReleases(sortOrder)
                .filter {
                    return@filter filter.test(it)
                }
    }

    @JsonIgnore
    fun getReleases(sortOrder: SortOrder): Sequence<Release> {
        val nodes = if (sortOrder == SortOrder.ASC) nodeList.iterator() else nodeList.descendingIterator()

        return nodes.asSequence()
    }

    @JsonIgnore
    fun getReleases(): Sequence<Release> {
        return getReleases(SortOrder.ASC)
    }

    fun retain(githubId: List<GithubId>): Releases {
        return Releases(nodes.filterKeys { adoptId -> githubId.any { adoptId.startsWith(it.githubId) } })
    }

    fun hasReleaseId(githubId: GithubId): Boolean {
        return nodes
                .any { it.key.startsWith(githubId.githubId) }
    }

    fun hasReleaseBeenUpdated(githubId: GithubId, updatedAt: ZonedDateTime): Boolean {
        return nodes
                .filter { it.key.startsWith(githubId.githubId) }
                .any {
                    ChronoUnit.SECONDS.between(it.value.updated_at, updatedAt) != 0L
                }
    }

    fun add(newReleases: List<Release>): Releases {
        return Releases(nodes.plus(newReleases.map { it.id to it }))
    }

    fun remove(id: String): Releases {
        return Releases(nodes.minus(id))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Releases

        if (nodes != other.nodes) return false

        return true
    }

    override fun hashCode(): Int {
        return nodes.hashCode()
    }

    companion object {
        //Cant use the default sort as we want to ignore optional
        val VERSION_COMPARATOR = compareBy<Release> { it.version_data.major }
                .thenBy { it.version_data.minor }
                .thenBy { it.version_data.security }
                .thenBy { it.version_data.pre }
                .thenBy { it.version_data.build }
                .thenBy { it.version_data.adopt_build_number }


        val VERSION_THEN_TIME_SORTER: Comparator<Release> =
                VERSION_COMPARATOR
                        .thenComparing { release: Release -> release.timestamp }
                        .thenComparing { release: Release -> release.release_name }
                        .thenComparing { release: Release -> release.id }
    }
}
