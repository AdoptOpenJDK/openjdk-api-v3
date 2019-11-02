package net.adoptopenjdk.api.v3.dataSources.github.graphql.clients

import io.aexp.nodes.graphql.GraphQLRequestEntity
import io.aexp.nodes.graphql.GraphQLResponseEntity
import io.aexp.nodes.graphql.GraphQLTemplate
import io.aexp.nodes.graphql.Variable
import io.aexp.nodes.graphql.exceptions.GraphQLException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.HasRateLimit
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit
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

    protected fun request(query: String): GraphQLRequestEntity.RequestBuilder {
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

        printRateLimit(result)

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

    private fun <F : HasRateLimit> printRateLimit(result: GraphQLResponseEntity<F>) {
        val rateLimitData = result.response.rateLimit

        if (rateLimitData.remaining < 1000) {
            LOGGER.info("Remaining data getting low ${rateLimitData.remaining} ${rateLimitData.cost}")
        }
        LOGGER.info("RateLimit ${rateLimitData.remaining} ${rateLimitData.cost}")
    }

    protected suspend fun <F : HasRateLimit> queryApi(requestEntityBuilder: GraphQLRequestEntity.RequestBuilder, cursor: String?, clazz: Class<F>): GraphQLResponseEntity<F>? {

        requestEntityBuilder.variables(Variable("cursorPointer", cursor))
        val query = requestEntityBuilder.build()

        var result: GraphQLResponseEntity<F>? = null
        var retryCount = 0
        while (result == null) {
            try {
                GlobalScope.async {
                    result = GraphQLTemplate(Int.MAX_VALUE, Int.MAX_VALUE).query(query, clazz)
                }.await()
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