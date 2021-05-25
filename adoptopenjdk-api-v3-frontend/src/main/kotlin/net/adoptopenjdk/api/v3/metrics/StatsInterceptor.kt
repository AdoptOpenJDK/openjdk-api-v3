package net.adoptopenjdk.api.v3.metrics

import io.smallrye.metrics.ExtendedMetadata
import io.smallrye.metrics.MetricRegistries
import org.eclipse.microprofile.metrics.Gauge
import org.eclipse.microprofile.metrics.MetricRegistry
import org.eclipse.microprofile.metrics.MetricType
import org.eclipse.microprofile.metrics.MetricUnits
import org.jboss.resteasy.core.interception.jaxrs.PostMatchContainerRequestContext
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.ext.Provider

@Provider
class StatsInterceptor : ContainerRequestFilter {

    private val stats = mutableMapOf<String, DayMeter>()
    private val all = DayMeter()

    init {
        val metricRegistry = MetricRegistries
            .get(MetricRegistry.Type.APPLICATION)

        addGuage(metricRegistry, "1 hour rate") { all.getOneHourRate() }
        addGuage(metricRegistry, "1 day rate") { all.getOneDayRate() }
        addGuage(metricRegistry, "7 day rate") { all.getSevenDayRate() }
        addGuage(metricRegistry, "1 hour rate total") { all.getTotalRequestsPerHour() }
        addGuage(metricRegistry, "mean") { all.getMeanRate() }
        addGuage(metricRegistry, "count") { all.count }
        addGuage(metricRegistry, "upTimeMin") { TimeUnit.MILLISECONDS.toMinutes(ManagementFactory.getRuntimeMXBean().uptime).toDouble() }
    }

    override fun filter(requestContext: ContainerRequestContext?) {
        if (requestContext is PostMatchContainerRequestContext) {
            val path = requestContext.resourceMethod.method.declaringClass.simpleName + "." + requestContext.resourceMethod.method.name

            var stat = stats[path]
            if (stat == null) {
                stat = DayMeter()
                stats[path] = stat

                val metricRegistry = MetricRegistries
                    .get(MetricRegistry.Type.APPLICATION)

                addGuage(metricRegistry, "$path 1 hour rate") { stat.getOneHourRate() }
                addGuage(metricRegistry, "$path 1 day rate") { stat.getOneDayRate() }
                addGuage(metricRegistry, "$path 7 day rate") { stat.getSevenDayRate() }
                addGuage(metricRegistry, "$path 1 hour rate total") { stat.getTotalRequestsPerHour() }
                addGuage(metricRegistry, "$path mean") { stat.getMeanRate() }
                addGuage(metricRegistry, "$path count") { stat.count }
            }
            stat.mark()
            all.mark()
        }
    }

    private fun addGuage(metricRegistry: MetricRegistry, name: String, getter: () -> Number) {
        metricRegistry.register(
            ExtendedMetadata(
                name,
                MetricType.GAUGE,
                MetricUnits.NONE,
                name,
                true
            ),
            object : Gauge<Number> {
                override fun getValue(): Number = getter()
            }
        )
    }
}
