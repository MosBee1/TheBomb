package io.github.mosbee1.thebomb.data.model

data class TimerPreset(
    val slot: Int,
    val amount: Int,
    val unit: DurationUnit,
) {
    val durationMillis: Long
        get() = unit.toMillis(amount)

    companion object {
        const val SLOT_COUNT = 4

        fun defaults(): List<TimerPreset> = listOf(
            TimerPreset(slot = 1, amount = 10, unit = DurationUnit.MINUTES),
            TimerPreset(slot = 2, amount = 1, unit = DurationUnit.HOURS),
            TimerPreset(slot = 3, amount = 1, unit = DurationUnit.DAYS),
            TimerPreset(slot = 4, amount = 1, unit = DurationUnit.WEEKS),
        )
    }
}
