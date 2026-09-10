package io.github.mosbee1.thebomb.util

import kotlin.math.roundToInt

object DurationFormatter {

    fun remainingFor(targetMillis: Long, nowMillis: Long): String =
        countdown(targetMillis - nowMillis)

    fun countdown(remainingMillis: Long): String {
        if (remainingMillis <= 0L) return "0m"
        val totalMinutes = (remainingMillis + 59_999L) / 60_000L
        val totalHours = totalMinutes / 60L
        val totalDays = totalHours / 24L
        val weeks = totalDays / 7L
        return when {
            weeks > 0L -> "${weeks}w ${totalDays % 7L}d"
            totalDays > 0L -> "${totalDays}d ${totalHours % 24L}h"
            totalHours > 0L -> "${totalHours}h ${totalMinutes % 60L}m"
            else -> "${totalMinutes}m"
        }
    }
}

object ByteSizeFormatter {

    private const val KB = 1024L
    private const val MB = KB * 1024L
    private const val GB = MB * 1024L

    fun format(bytes: Long): String = when {
        bytes < KB -> "$bytes B"
        bytes < MB -> "${oneDecimal(bytes.toDouble() / KB)} KB"
        bytes < GB -> "${oneDecimal(bytes.toDouble() / MB)} MB"
        else -> "${oneDecimal(bytes.toDouble() / GB)} GB"
    }

    private fun oneDecimal(value: Double): String {
        val tenths = (value * 10.0).roundToInt()
        return if (tenths % 10 == 0) {
            (tenths / 10).toString()
        } else {
            "${tenths / 10}.${tenths % 10}"
        }
    }
}
