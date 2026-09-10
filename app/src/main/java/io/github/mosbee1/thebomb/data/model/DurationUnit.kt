package io.github.mosbee1.thebomb.data.model

enum class DurationUnit(
    val millisPerUnit: Long,
    val sliderRange: IntRange,
) {
    MINUTES(60_000L, 1..59),
    HOURS(3_600_000L, 1..23),
    DAYS(86_400_000L, 1..30),
    WEEKS(604_800_000L, 1..8);

    fun toMillis(amount: Int): Long = millisPerUnit * amount.coerceAtLeast(0)

    companion object {
        fun fromNameOrDefault(name: String?): DurationUnit =
            entries.firstOrNull { it.name == name } ?: MINUTES
    }
}
