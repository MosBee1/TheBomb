package io.github.mosbee1.thebomb.data.model

data class StatsSnapshot(
    val processedCount: Long = 0,
    val deletedCount: Long = 0,
    val keptCount: Long = 0,
    val archivedCount: Long = 0,
    val pendingCount: Long = 0,
    val bytesFreed: Long = 0,
    val pendingBytes: Long = 0,
)
