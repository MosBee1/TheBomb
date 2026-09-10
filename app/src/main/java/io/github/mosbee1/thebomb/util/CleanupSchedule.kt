package io.github.mosbee1.thebomb.util

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure date math for the daily blast. Candidates and "now" are both
 * ZonedDateTime, so isAfter compares absolute instants - correct across
 * DST transitions.
 */
object CleanupSchedule {

    private const val MINUTES_PER_DAY = 24 * 60

    /** The first occurrence of the time-of-day STRICTLY AFTER [afterMillis]. */
    fun nextOccurrenceAfter(afterMillis: Long, timeOfDayMinutes: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(afterMillis).atZone(zone)
        val safe = timeOfDayMinutes.coerceIn(0, MINUTES_PER_DAY - 1)
        val time = LocalTime.of(safe / 60, safe % 60)
        var candidate = now.toLocalDate().atTime(time).atZone(zone)
        if (!candidate.isAfter(now)) {
            candidate = now.toLocalDate().plusDays(1).atTime(time).atZone(zone)
        }
        return candidate.toInstant().toEpochMilli()
    }

    fun isCleanupDue(
        nowMillis: Long,
        timeOfDayMinutes: Int,
        lastHandledAt: Long,
    ): Boolean = nowMillis >= nextOccurrenceAfter(lastHandledAt, timeOfDayMinutes)
}