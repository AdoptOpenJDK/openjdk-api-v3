package net.adoptopenjdk.api.v3.dataSources.persitence

import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease

interface ApiPersistence {
    suspend fun updateAllRepos(repos: AdoptRepos)
    suspend fun readReleaseData(featureVersion: Int): FeatureRelease
}
