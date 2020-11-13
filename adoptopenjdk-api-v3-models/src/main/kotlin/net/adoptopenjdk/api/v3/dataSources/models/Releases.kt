package net.adoptopenjdk.api.v3.dataSources.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import net.adoptopenjdk.api.v3.dataSources.SortMethod
import net.adoptopenjdk.api.v3.dataSources.SortOrder
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.VersionData
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
/* ktlint-enable no-wildcard-imports */
import java.util.*
import java.util.function.Predicate

class Releases {

    @JsonProperty("nodes")
    val nodes: Map<String, Release>

    @JsonIgnore
    val nodeList: TreeSet<Release> = TreeSet(VERSION_THEN_TIME_SORTER)
    private val nodeListTimeThenVersion: TreeSet<Release> = TreeSet(TIME_THEN_VERSION_SORTER)

    constructor(nodes: List<Release>) {
        this.nodes = nodes
            .map { it.id to it }
            .toMap()
        nodeList.addAll(nodes)
        nodeListTimeThenVersion.addAll(nodes)
    }

    @JsonCreator
    constructor(@JsonProperty("nodes") nodes: Map<String, Release>) {
        this.nodes = nodes
        nodeList.addAll(nodes.values)
        nodeListTimeThenVersion.addAll(nodes.values)
    }

    @JsonIgnore
    fun getReleases(filter: Predicate<Release>, sortOrder: SortOrder, sortMethod: SortMethod): Sequence<Release> {
        return getReleases(sortOrder, sortMethod)
            .filter {
                return@filter filter.test(it)
            }
    }

    @JsonIgnore
    fun getReleases(sortOrder: SortOrder, sortMethod: SortMethod): Sequence<Release> {
        val list = when (sortMethod) {
            SortMethod.DATE -> nodeListTimeThenVersion
            SortMethod.DEFAULT -> nodeList
        }

        val nodes = if (sortOrder == SortOrder.ASC) list.iterator() else list.descendingIterator()

        return nodes.asSequence()
    }

    @JsonIgnore
    fun getReleases(): Sequence<Release> {
        return getReleases(SortOrder.ASC, SortMethod.DEFAULT)
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
        val PRE_SORTER = compareBy<VersionData, String?>(nullsLast()) { it.pre }

        // Cant use the default sort as we want to ignore optional
        val VERSION_COMPARATOR = compareBy<VersionData> { it.major }
            .thenBy { it.minor }
            .thenBy { it.security }
            .then(PRE_SORTER)
            .thenBy { it.build }
            .thenBy { it.adopt_build_number }

        private val TIME_COMPARATOR = compareBy { release: Release -> release.timestamp }

        val RELEASE_COMPARATOR = compareBy<Release, VersionData>(VERSION_COMPARATOR, { it.version_data })

        private val RELEASE_NAME_AND_ID_COMPARATOR = compareBy { release: Release -> release.release_name }
            .thenComparing { release: Release -> release.id }

        val VERSION_THEN_TIME_SORTER: Comparator<Release> =
            RELEASE_COMPARATOR
                .then(TIME_COMPARATOR)
                .then(RELEASE_NAME_AND_ID_COMPARATOR)

        val TIME_THEN_VERSION_SORTER: Comparator<Release> =
            TIME_COMPARATOR
                .then(RELEASE_COMPARATOR)
                .then(RELEASE_NAME_AND_ID_COMPARATOR)
    }
}
