package net.adoptopenjdk.api.v3.dataSources.github.graphql.clients

import io.aexp.nodes.graphql.GraphQLRequestEntity
import io.aexp.nodes.graphql.GraphQLResponseEntity
import io.aexp.nodes.graphql.GraphQLTemplate
import io.aexp.nodes.graphql.Variable
import io.aexp.nodes.graphql.exceptions.GraphQLException
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.adoptopenjdk.api.v3.HttpClientFactory
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.HasRateLimit
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.system.exitProcess


open class GraphQLGitHubInterface {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    // GH limit 500,000 nodes per request
    // total nodes:
    //  50 releases
    //  50 releases * 100 assets
    // = 50 + 50 * 100
    // = 5050

    protected val OWNER = "AdoptOpenJDK"

    private val BASE_URL = "https://api.github.com/graphql"
    private val TOKEN: String = readToken()
    private val THRESHOLD_START = System.getenv("GITHUB_THRESHOLD")?.toFloatOrNull() ?: 1000f
    private val THRESHOLD_HARD_FLOOR = System.getenv("GITHUB_THRESHOLD_HARD_FLOOR")?.toFloatOrNull() ?: 200f

    fun request(query: String): GraphQLRequestEntity.RequestBuilder {
        return GraphQLRequestEntity.Builder()
                .url(BASE_URL)
                .headers(mapOf(
                        "Authorization" to "Bearer $TOKEN"
                ))
                .request(query.trimIndent().replace("\n", ""))

    }

    protected suspend fun <E, F : HasRateLimit> getAll(
            requestEntityBuilder: GraphQLRequestEntity.RequestBuilder,

            extract: suspend (F) -> List<E>,
            hasNext: (F) -> Boolean,
            getCursor: (F) -> String?,

            initialCursor: String? = null,
            response: F? = null,
            clazz: Class<F>
    ): List<E> {
        var cursor = initialCursor

        if (response != null) {
            if (!hasNext(response)) {
                return listOf()
            } else {
                cursor = getCursor(response)
            }
        }

        val result: GraphQLResponseEntity<F>? = queryApi(requestEntityBuilder, cursor, clazz)

        if (result == null || repoDoesNotExist(result)) return listOf()

        selfRateLimit(result)

        val newData = extract(result.response)

        val more = getAll(requestEntityBuilder, extract, hasNext, getCursor, initialCursor, result.response, clazz)

        return newData.plus(more)
    }

    private fun <F : HasRateLimit> repoDoesNotExist(result: GraphQLResponseEntity<F>): Boolean {
        if (result.errors != null && result.errors.isNotEmpty()) {
            if (result.errors[0].message.contains("Could not resolve to a Repository")) {
                return true
            }

            result.errors.forEach {
                LOGGER.warn(it.message)
            }
        }
        return false
    }

    private suspend fun <F : HasRateLimit> selfRateLimit(result: GraphQLResponseEntity<F>) {
        val rateLimitData = result.response.rateLimit
        if (rateLimitData.remaining < THRESHOLD_START) {
            var quota = getRemainingQuota()
            do {
                val delayTime = max(10, quota.second)
                LOGGER.info("Remaining data getting low $quota ${rateLimitData.cost} $delayTime")
                delay(1000 * delayTime)

                quota = getRemainingQuota()
            } while (quota.first < THRESHOLD_START)
        }
        LOGGER.info("RateLimit ${rateLimitData.remaining} ${rateLimitData.cost}")
    }

    private suspend fun getRemainingQuota(): Pair<Int, Long> {
        try {
            return suspendCoroutine { continuation ->
                try {
                    val request = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.github.com/rate_limit"))
                            .setHeader("Authorization", "token $TOKEN")
                            .build()

                    HttpClientFactory
                            .getHttpClient()
                            .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .orTimeout(5, TimeUnit.SECONDS)
                            .handle { result, error ->

                                if (error != null) {
                                    continuation.resumeWithException(error)
                                } else if (result?.body() == null) {
                                    continuation.resumeWithException(Exception("Failed to read remaining quota"))
                                } else {
                                    try {
                                        continuation.resume(processResponse(result))
                                    } catch (e: Exception) {
                                        continuation.resumeWithException(e)
                                    }
                                }
                            }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to read remaining quota", e)
            return Pair(0, 100)
        }
    }

    private fun processResponse(result: HttpResponse<String>): Pair<Int, Long> {
        val json = JsonObject(result.body())
        val remainingQuota = json.getJsonObject("resources")
                ?.getJsonObject("graphql")
                ?.getInteger("remaining")
        val resetTime = json.getJsonObject("resources")
                ?.getJsonObject("graphql")
                ?.getLong("reset")

        if (resetTime != null && remainingQuota != null) {
            val delayTime = if (remainingQuota > THRESHOLD_HARD_FLOOR) {
                // scale delay, sleep for 1 second at rate limit == 1000
                // then scale up to 400 seconds at rate limit == 1
                (400f * (THRESHOLD_START - remainingQuota) / THRESHOLD_START).toLong()
            } else {
                val reset = LocalDateTime.ofEpochSecond(resetTime, 0, ZoneOffset.UTC)
                LOGGER.info("Remaining quota VERY LOW $remainingQuota delaying til $reset")
                ChronoUnit.SECONDS.between(LocalDateTime.now(ZoneOffset.UTC), reset)
            }

            return Pair(remainingQuota, delayTime)
        } else {
            throw Exception("Unable to parse graphql data")
        }
    }

    protected suspend fun <F : HasRateLimit> queryApi(requestEntityBuilder: GraphQLRequestEntity.RequestBuilder, cursor: String?, clazz: Class<F>): GraphQLResponseEntity<F>? {

        requestEntityBuilder.variables(Variable("cursorPointer", cursor))
        val query = requestEntityBuilder.build()

        var result: GraphQLResponseEntity<F>? = null
        var retryCount = 0
        while (result == null) {
            try {
                withContext(Dispatchers.Default) {
                    result = GraphQLTemplate(Int.MAX_VALUE, Int.MAX_VALUE).query(query, clazz)
                }
            } catch (e: GraphQLException) {
                if (e.status == "403" || e.status == "502") {
                    // Normally get these due to tmp ban due to rate limiting
                    LOGGER.info("Retrying ${e.status} ${retryCount++}")
                    if (retryCount == 20) {
                        printError(query, cursor)
                        return null
                    }
                    delay((TimeUnit.SECONDS.toMillis(2) * retryCount))
                } else {
                    printError(query, cursor)
                    return null
                }
            } catch (e: Exception) {
                LOGGER.error("Query failed", e)
                return null
            }
        }
        return result
    }


    private fun printError(query: GraphQLRequestEntity?, cursor: String?) {
        LOGGER.warn("Retry limit hit $query")
        LOGGER.warn("Cursor $cursor")
    }


    private fun readToken(): String {
        var token = System.getenv("GITHUB_TOKEN")
        if (token == null) {
            token = System.getProperty("GITHUB_TOKEN")
        }

        if (token == null) {

            val userHome = System.getProperty("user.home")

            // e.g /home/foo/.adopt_api/token.properties
            val propertiesFile = File(userHome + File.separator + ".adopt_api" + File.separator + "token.properties")

            if (propertiesFile.exists()) {

                val properties = Properties()
                properties.load(Files.newInputStream(propertiesFile.toPath()))
                token = properties.getProperty("token")
            }

        }
        if (token == null) {
            LOGGER.error("Could not find GITHUB_TOKEN")
            exitProcess(1)
        }
        return token
    }
}