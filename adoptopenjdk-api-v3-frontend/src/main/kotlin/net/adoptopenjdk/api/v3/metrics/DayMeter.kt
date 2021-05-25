package net.adoptopenjdk.api.v3.metrics

import io.smallrye.metrics.app.Clock
import io.smallrye.metrics.app.EWMA
import org.eclipse.microprofile.metrics.Counting
import org.eclipse.microprofile.metrics.Metric
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder

/*
Based on MeterImpl from smallrye-metrics
 */
class DayMeter : Metric, Counting {

    companion object {
        private val TICK_INTERVAL = TimeUnit.MINUTES.toNanos(1)

        private const val INTERVAL = 1.0
        private const val MINUTES_PER_DAY = 60.0 * 24.0
        private const val MINUTES_PER_WEEK = 7.0 * 60.0 * 24.0
        private const val MINUTES_PER_HOUR = 1.0
        private const val M0_ALPHA = 2.0 / ((MINUTES_PER_HOUR / INTERVAL) + 1.0)
        private const val M1_ALPHA = 2.0 / ((MINUTES_PER_DAY / INTERVAL) + 1.0)
        private const val M7_ALPHA = 2.0 / ((MINUTES_PER_WEEK / INTERVAL) + 1.0)
    }

    private val h1Rate = EWMA(M0_ALPHA, INTERVAL.toLong(), TimeUnit.MINUTES)
    private val d1Rate = EWMA(M1_ALPHA, INTERVAL.toLong(), TimeUnit.MINUTES)
    private val d7Rate = EWMA(M7_ALPHA, INTERVAL.toLong(), TimeUnit.MINUTES)

    private val count = LongAdder()
    private val startTime: Long = 0
    private val clock: Clock = Clock.defaultClock()
    private val lastTick: AtomicLong = AtomicLong(clock.tick)

    fun mark() {
        tickIfNecessary()
        count.add(1)
        h1Rate.update(1)
        d1Rate.update(1)
        d7Rate.update(1)
    }

    private fun tickIfNecessary() {
        val oldTick = lastTick.get()
        val newTick = clock.tick
        val age = newTick - oldTick
        if (age > TICK_INTERVAL) {
            val newIntervalStartTick = newTick - age % TICK_INTERVAL
            if (lastTick.compareAndSet(oldTick, newIntervalStartTick)) {
                val requiredTicks = age / TICK_INTERVAL
                for (i in 0 until requiredTicks) {
                    h1Rate.tick()
                    d1Rate.tick()
                    d7Rate.tick()
                }
            }
        }
    }

    override fun getCount(): Long {
        return count.sum()
    }

    fun getOneHourRate(): Double {
        tickIfNecessary()
        return h1Rate.getRate(TimeUnit.MINUTES)
    }

    fun getMeanRate(): Double {
        return if (count.toLong() == 0L) {
            0.0
        } else {
            val elapsed = clock.tick - startTime
            count.toDouble() / elapsed.toDouble() * TimeUnit.MINUTES.toNanos(1)
        }
    }

    fun getTotalRequestsPerHour(): Double {
        return count.toDouble() / ((1 + TimeUnit.MILLISECONDS.toMinutes(ManagementFactory.getRuntimeMXBean().uptime).toDouble()) / 60.0)
    }

    fun getOneDayRate(): Double {
        tickIfNecessary()
        return d1Rate.getRate(TimeUnit.MINUTES)
    }

    fun getSevenDayRate(): Double {
        tickIfNecessary()
        return d7Rate.getRate(TimeUnit.MINUTES)
    }
}
