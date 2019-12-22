package net.adoptopenjdk.api.v3.models

import java.time.LocalDateTime

abstract class DbStatsEntry<T>(
        val date: LocalDateTime
) {
    abstract fun getMetric(): Long
    abstract fun getId(): T
}