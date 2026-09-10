package io.github.mosbee1.thebomb.util

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

object CleanupSchedule {

    private const val MINUTES_PER_DAY = 24 * 60

    fun nextOccurrenceAfter(afterMillis: Long, timeOfDayMinutes: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(afterMillis).atZone(zone)
        val safe = timeOfDayMinutes.coerceIn(0, MINUTES_PER_DAY - 1)
        val time = LocalTime.of(safe / 60, safe % 60)
        var candidate = now.toLocalDate().atTime(time)
        if (!candidate.isAfter(now)) {
            candidate = now.toLocalDate().plusDays(1).atTime(time)
        }
        return candidate.atZone(zone).toInstant().toEpochMilli()
    }

    fun isCleanupDue(
        nowMillis: Long,
        timeOfDayMinutes: Int,
        lastHandledAt: Long,
    ): Boolean = nowMillis >= nextOccurrenceAfter(lastHandledAt, timeOfDayMinutes)
}
