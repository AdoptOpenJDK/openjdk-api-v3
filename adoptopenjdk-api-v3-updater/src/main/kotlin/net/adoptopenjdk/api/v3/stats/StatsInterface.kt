package net.adoptopenjdk.api.v3.stats

import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos

class StatsInterface {

    private val githubDownloadStatsCalculator: GithubDownloadStatsCalculator = GithubDownloadStatsCalculator()
    private val dockerStatsInterface: DockerStatsInterface = DockerStatsInterface()

    suspend fun update(repos: AdoptRepos) {
        githubDownloadStatsCalculator.saveStats(repos)
        dockerStatsInterface.updateDb()
    }

}