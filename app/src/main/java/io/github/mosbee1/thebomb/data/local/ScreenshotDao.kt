package io.github.mosbee1.thebomb.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.mosbee1.thebomb.data.model.StatsSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: ScreenshotEntity): Long

    @Query("UPDATE screenshots SET kept = 1, archived = 0, deleteAtMillis = NULL WHERE uri = :uri")
    suspend fun markKept(uri: String)

    @Query("UPDATE screenshots SET archived = 1, kept = 0 WHERE uri = :uri")
    suspend fun markArchived(uri: String)

    @Query("UPDATE screenshots SET archived = 0, kept = 0 WHERE uri = :uri")
    suspend fun markPending(uri: String)

    @Query("UPDATE screenshots SET deleted = 1, deleteAtMillis = NULL WHERE uri IN (:uris)")
    suspend fun markDeleted(uris: List<String>)

    @Query("UPDATE screenshots SET deleteAtMillis = :millis WHERE uri = :uri")
    suspend fun setDeleteAt(uri: String, millis: Long?)

    @Query("SELECT * FROM screenshots WHERE uri = :uri")
    suspend fun getByUri(uri: String): ScreenshotEntity?

    @Query("SELECT * FROM screenshots WHERE uri = :uri")
    fun observeByUri(uri: String): Flow<ScreenshotEntity?>

    @Query(
        """SELECT * FROM screenshots
           WHERE deleted = 0 AND kept = 0 AND archived = 0
           ORDER BY createdAtMillis DESC"""
    )
    fun observePending(): Flow<List<ScreenshotEntity>>

    @Query(
        """SELECT * FROM screenshots
           WHERE deleted = 0 AND kept = 0 AND archived = 1
           ORDER BY createdAtMillis DESC"""
    )
    fun observeArchived(): Flow<List<ScreenshotEntity>>

    @Query(
        """SELECT * FROM screenshots
           WHERE deleted = 0 AND kept = 1
           ORDER BY createdAtMillis DESC"""
    )
    fun observeKept(): Flow<List<ScreenshotEntity>>

    @Query(
        """SELECT * FROM screenshots
           WHERE deleted = 0 AND kept = 0 AND archived = 0
             AND (:query = '' OR fileName LIKE '%' || :query || '%')
             AND (:fromMillis IS NULL OR createdAtMillis >= :fromMillis)
             AND (:toMillis IS NULL OR createdAtMillis <= :toMillis)
           ORDER BY createdAtMillis DESC"""
    )
    fun observePendingFiltered(
        query: String,
        fromMillis: Long?,
        toMillis: Long?,
    ): Flow<List<ScreenshotEntity>>

    @Query(
        """SELECT * FROM screenshots
           WHERE deleted = 0 AND kept = 0 AND archived = 1
             AND (:query = '' OR fileName LIKE '%' || :query || '%')
             AND (:fromMillis IS NULL OR createdAtMillis >= :fromMillis)
             AND (:toMillis IS NULL OR createdAtMillis <= :toMillis)
           ORDER BY createdAtMillis DESC"""
    )
    fun observeArchivedFiltered(
        query: String,
        fromMillis: Long?,
        toMillis: Long?,
    ): Flow<List<ScreenshotEntity>>

    @Query(
        """SELECT * FROM screenshots
           WHERE deleted = 0 AND kept = 1
             AND (:query = '' OR fileName LIKE '%' || :query || '%')
             AND (:fromMillis IS NULL OR createdAtMillis >= :fromMillis)
             AND (:toMillis IS NULL OR createdAtMillis <= :toMillis)
           ORDER BY createdAtMillis DESC"""
    )
    fun observeKeptFiltered(
        query: String,
        fromMillis: Long?,
        toMillis: Long?,
    ): Flow<List<ScreenshotEntity>>

    @Query(
        """SELECT * FROM screenshots
           WHERE deleted = 0 AND kept = 0
             AND deleteAtMillis IS NOT NULL AND deleteAtMillis <= :now"""
    )
    suspend fun dueTimers(now: Long): List<ScreenshotEntity>

    @Query(
        """SELECT * FROM screenshots
           WHERE deleted = 0 AND kept = 0 AND archived = 1
             AND deleteAtMillis IS NULL"""
    )
    suspend fun dailyCleanupCandidates(): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE deleted = 0")
    suspend fun activeRows(): List<ScreenshotEntity>

    @Query(
        """SELECT
             COUNT(*) AS processedCount,
             COALESCE(SUM(CASE WHEN deleted = 1 THEN 1 ELSE 0 END), 0) AS deletedCount,
             COALESCE(SUM(CASE WHEN kept = 1 AND deleted = 0 THEN 1 ELSE 0 END), 0) AS keptCount,
             COALESCE(SUM(CASE WHEN archived = 1 AND kept = 0 AND deleted = 0 THEN 1 ELSE 0 END), 0) AS archivedCount,
             COALESCE(SUM(CASE WHEN archived = 0 AND kept = 0 AND deleted = 0 THEN 1 ELSE 0 END), 0) AS pendingCount,
             COALESCE(SUM(CASE WHEN deleted = 1 THEN sizeBytes ELSE 0 END), 0) AS bytesFreed,
             COALESCE(SUM(CASE WHEN archived = 0 AND kept = 0 AND deleted = 0 THEN sizeBytes ELSE 0 END), 0) AS pendingBytes
           FROM screenshots"""
    )
    fun observeStats(): Flow<StatsSnapshot>
}
