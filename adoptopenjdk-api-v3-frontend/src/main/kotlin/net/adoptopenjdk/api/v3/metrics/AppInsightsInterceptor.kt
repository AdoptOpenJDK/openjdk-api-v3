package net.adoptopenjdk.api.v3.metrics

import com.microsoft.applicationinsights.TelemetryClient
import com.microsoft.applicationinsights.telemetry.RequestTelemetry
import org.eclipse.microprofile.metrics.annotation.Timed
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.annotation.Priority
import javax.interceptor.AroundInvoke
import javax.interceptor.Interceptor
import javax.interceptor.InvocationContext
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

@Timed
@Suppress("unused")
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 10)
class AppInsightsInterceptor {
    private val telemetryClient: TelemetryClient

    init {
        telemetryClient = loadTelemetryClient()
    }

    private fun loadTelemetryClient(): TelemetryClient {
        val path = saveConfigToFile()
        try {
            return TelemetryClient()
        } finally {
            val tmpDir = System.getProperty("java.io.tmpdir")
            val isTmpDir = path?.startsWith(tmpDir)
            if (isTmpDir != null && isTmpDir) {
                path.toFile().deleteRecursively()
            }
        }
    }

    // Unfortunate hack to get around that we seem unable to load ApplicationInsights.xml from the classpath
    // from inside the module
    private fun saveConfigToFile(): Path? {
        val inputStream = AppInsightsInterceptor::class.java.classLoader.getResourceAsStream("ApplicationInsights.xml")

        inputStream?.use {
            val configPath = Files.createTempDirectory("appInsightsConfig").toAbsolutePath()
            val configFile = File(configPath.toString(), "ApplicationInsights.xml")
            configFile.deleteOnExit()
            configPath.toFile().deleteOnExit()
            System.setProperty("applicationinsights.configurationDirectory", configPath.toString())

            val outputStream = FileOutputStream(configFile)

            outputStream.use { fileOut ->
                inputStream.copyTo(fileOut)
            }

            return configPath
        }
        return null
    }

    @AroundInvoke
    @Throws(Exception::class)
    fun timedMethod(context: InvocationContext): Any? {
        var response: Any? = null
        var success = true
        var status = 200
        var exception: Throwable? = null

        val methodName = context.method.toGenericString()
        val start = System.nanoTime()
        try {
            response = context.proceed()
            when (response) {
                null -> {
                    status = 404
                    success = false
                }
                is Response -> {
                    status = response.status
                    success = response.status < 400
                }
                else -> {
                    status = 200
                    success = true
                }
            }
        } catch (e: Throwable) {
            exception = e
            success = false
            status = if (e is WebApplicationException) {
                e.response.status
            } else {
                500
            }
        } finally {
            try {
                val duration = (System.nanoTime() - start) / 1000000
                val requestTelemetry = RequestTelemetry(
                    methodName,
                    Date(),
                    duration,
                    status.toString(),
                    success
                )
                telemetryClient.trackRequest(requestTelemetry)
            } finally {
                if (exception != null) {
                    throw exception
                }
                return response
            }
        }
    }
}
