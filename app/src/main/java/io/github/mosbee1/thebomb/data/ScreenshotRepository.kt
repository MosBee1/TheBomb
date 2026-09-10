package io.github.mosbee1.thebomb.data

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import io.github.mosbee1.thebomb.data.local.ScreenshotDao
import io.github.mosbee1.thebomb.data.local.ScreenshotEntity
import io.github.mosbee1.thebomb.data.model.StatsSnapshot
import io.github.mosbee1.thebomb.util.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ScreenshotRepository(
    private val context: Context,
    private val dao: ScreenshotDao,
    private val timeProvider: TimeProvider,
) {

    fun observePending(): Flow<List<ScreenshotEntity>> = dao.observePending()

    fun observeArchived(): Flow<List<ScreenshotEntity>> = dao.observeArchived()

    fun observeKept(): Flow<List<ScreenshotEntity>> = dao.observeKept()

    fun observePendingFiltered(query: String, from: Long?, to: Long?): Flow<List<ScreenshotEntity>> =
        dao.observePendingFiltered(query, from, to)

    fun observeArchivedFiltered(query: String, from: Long?, to: Long?): Flow<List<ScreenshotEntity>> =
        dao.observeArchivedFiltered(query, from, to)

    fun observeKeptFiltered(query: String, from: Long?, to: Long?): Flow<List<ScreenshotEntity>> =
        dao.observeKeptFiltered(query, from, to)

    fun observeByUri(uri: String): Flow<ScreenshotEntity?> = dao.observeByUri(uri)

    fun observeStats(): Flow<StatsSnapshot> = dao.observeStats()

    /** True only if the row was newly inserted; duplicates never re-notify. */
    suspend fun recordScreenshot(entity: ScreenshotEntity): Boolean =
        dao.insertIfAbsent(entity) != -1L

    /**
     * Marks rows deleted when their files no longer exist in MediaStore
     * (deleted from the gallery app, or a consent dialog was approved).
     * On query failure a row is assumed to still exist so stats never
     * overcount "freed".
     */
    suspend fun reconcileDeletedExternally(): Int = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val missing = mutableListOf<String>()
        for (row in dao.activeRows()) {
            val stillExists = try {
                resolver
                    .query(Uri.parse(row.uri), arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                    ?.use { cursor -> cursor.count > 0 } ?: false
            } catch (t: Throwable) {
                true
            }
            if (!stillExists) missing += row.uri
        }
        if (missing.isNotEmpty()) dao.markDeleted(missing)
        missing.size
    }

    /** Kept items are exempt from all auto-deletion, so any timer is cleared. */
    suspend fun markKept(uri: String) = dao.markKept(uri)

    /** Timer (if any) stays active: an archived+timed item still fires its timer. */
    suspend fun markArchived(uri: String) = dao.markArchived(uri)

    suspend fun markPending(uri: String) = dao.markPending(uri)

    suspend fun setDeleteTimer(uri: String, durationMillis: Long): Long {
        val fireAt = timeProvider.nowMillis() + durationMillis
        dao.setDeleteAt(uri, fireAt)
        return fireAt
    }

    suspend fun cancelDeleteTimer(uri: String) {
        dao.setDeleteAt(uri, null)
    }

    suspend fun dueTimers(nowMillis: Long): List<ScreenshotEntity> = dao.dueTimers(nowMillis)

    suspend fun dailyCleanupCandidates(): List<ScreenshotEntity> = dao.dailyCleanupCandidates()

    /**
     * Deletes files via MediaStore. API 30+: one batched createDeleteRequest
     * (one consent dialog for the whole batch). API 29: per-file delete,
     * recovering from RecoverableSecurityException on the first file that
     * needs consent; files deleted before that point are already recorded.
     */
    suspend fun deleteFromMediaStore(uris: List<String>): DeletionOutcome =
        withContext(Dispatchers.IO) {
            if (uris.isEmpty()) return@withContext DeletionOutcome.DeletedDirectly
            val parsed = uris.map(Uri::parse)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                createBatchDeleteRequest(parsed)
            } else {
                deleteOneByOne(parsed)
            }
        }

    /** Records rows as deleted after an approved consent dialog. */
    suspend fun onDeletionApproved(uris: List<String>) {
        dao.markDeleted(uris)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun createBatchDeleteRequest(uris: List<Uri>): DeletionOutcome = try {
        val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
        DeletionOutcome.ConsentRequired(pendingIntent.intentSender)
    } catch (t: Throwable) {
        DeletionOutcome.Failed(t)
    }

    private suspend fun deleteOneByOne(uris: List<Uri>): DeletionOutcome {
        var consentSender: IntentSender? = null
        for (uri in uris) {
            try {
                context.contentResolver.delete(uri, null, null)
                dao.markDeleted(listOf(uri.toString()))
            } catch (e: RecoverableSecurityException) {
                consentSender = e.userAction.actionIntent.intentSender
                break
            } catch (t: Throwable) {
                return DeletionOutcome.Failed(t)
            }
        }
        return consentSender
            ?.let { DeletionOutcome.ConsentRequired(it) }
            ?: DeletionOutcome.DeletedDirectly
    }
}
