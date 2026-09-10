package io.github.mosbee1.thebomb.data.model

data class AppSettings(
    val janitorEnabled: Boolean = false,
    val autoArchiveEnabled: Boolean = false,
    val popupEnabled: Boolean = true,
    val cleanupReminderEnabled: Boolean = false,
    val cleanupTimeMinutes: Int = 23 * 60 + 30,
    val presets: List<TimerPreset> = TimerPreset.defaults(),
    val lastCleanupHandledAt: Long = 0L,
    val onboardingCompleted: Boolean = false,
) {
    fun preset(slot: Int): TimerPreset =
        presets.first { it.slot == slot }
}
