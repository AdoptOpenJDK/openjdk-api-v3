package net.adoptopenjdk.api.v3.dataSources.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import net.adoptopenjdk.api.v3.dataSources.filters.BinaryFilter
import net.adoptopenjdk.api.v3.dataSources.filters.ReleaseFilter
import net.adoptopenjdk.api.v3.models.Release
import java.time.LocalDateTime

class Releases {

    @JsonProperty("nodes")
    val nodes: Map<String, Release>

    constructor(nodes: List<Release>) {
        this.nodes = nodes.map { it.id to it }.toMap()
    }

    @JsonCreator
    constructor(@JsonProperty("nodes") nodes: Map<String, Release>) {
        this.nodes = nodes
    }

    fun filterBinaries(binaryFilter: BinaryFilter): Releases {
        val filtered = getReleases()
                .map {
                    Release(it, it.binaries.filter { binaryFilter.test(it) }.toTypedArray())
                }
        return Releases(filtered.toList())
    }

    @JsonIgnore
    fun getReleases(filter: ReleaseFilter): Sequence<Release> {
        return getReleases()
                .filter {
                    return@filter filter.test(it)
                }
    }

    @JsonIgnore
    fun getReleases(): Sequence<Release> {
        return nodes.values.sortedWith(TIME_SORTER).asSequence()
    }

    fun retain(ids: List<String>): Releases {
        return Releases(nodes.filterKeys { ids.contains(it) })
    }

    fun hasReleaseId(id: String): Boolean {
        return nodes.containsKey(id)
    }

    fun hasReleaseBeenUpdated(id: String, updatedAt: LocalDateTime): Boolean {
        return nodes.get(id)?.updated_at?.equals(updatedAt) ?: true
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
        val TIME_SORTER: Comparator<Release> = Comparator.comparing { release: Release -> release.timestamp }
    }
}
