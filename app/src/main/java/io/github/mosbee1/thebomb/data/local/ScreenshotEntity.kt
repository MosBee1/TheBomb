package io.github.mosbee1.thebomb.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screenshots")
data class ScreenshotEntity(
    @PrimaryKey val uri: String,
    val mediaStoreId: Long,
    val fileName: String,
    val createdAtMillis: Long,
    val sizeBytes: Long,
    val archived: Boolean = false,
    val kept: Boolean = false,
    val deleted: Boolean = false,
    val deleteAtMillis: Long? = null,
)
