package io.github.mosbee1.thebomb.consent

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.notifications.Notifications
import io.github.mosbee1.thebomb.work.BackgroundWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Transparent relay that performs scoped-storage deletion with the mandatory
 * user-consent flow, from a real foreground activity context:
 *  - API 30+: one batched MediaStore.createDeleteRequest (single dialog).
 *  - API 29: per-file delete, catching RecoverableSecurityException.
 * On approval: rows are marked deleted. On explicit cancel: the affected
 * timers are cleared so the sweep never re-prompts in a loop.
 */
class DeletionConsentActivity : ComponentActivity() {

    private val container by lazy { (application as TheBombApp).container }

    private var targetUris: List<Uri> = emptyList()
    private var api29Index = 0
    private var approvedApi29 = mutableListOf<String>()

    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onAllApproved()
        } else {
            onUserCancelled()
        }
    }

    private val api29Launcher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            approvedApi29.add(targetUris[api29Index].toString())
            api29Index += 1
            proceedApi29()
        } else {
            onUserCancelled()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val raw = intent.getStringArrayExtra(Notifications.EXTRA_DELETE_URIS)
        targetUris = raw?.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() } ?: emptyList()
        if (targetUris.isEmpty()) {
            finish()
            return
        }
        lifecycleScope.launch {
            startDeletion()
        }
    }

    private suspend fun startDeletion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startBatchRequest()
        } else {
            api29Index = 0
            proceedApi29()
        }
    }

    private suspend fun startBatchRequest() {
        val outcome = withContext(Dispatchers.IO) {
            runCatching {
                MediaStore.createDeleteRequest(contentResolver, targetUris)
            }
        }
        outcome.fold(
            onSuccess = { pendingIntent ->
                consentLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                )
            },
            onFailure = {
                // One file may have vanished: fall back to per-file requests
                // so the rest still work.
                startPerFileRequests()
            },
        )
    }

    private suspend fun startPerFileRequests() {
        val remaining = withContext(Dispatchers.IO) {
            targetUris.filter { uri -> fileExists(uri) }
        }
        if (remaining.isEmpty()) {
            container.screenshotRepository.onDeletionApproved(targetUris.map { it.toString() })
            Notifications.cancelConsentNotifications(applicationContext)
            finish()
            return
        }
        targetUris = remaining
        api29Index = 0
        approvedApi29 = mutableListOf()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val next = withContext(Dispatchers.IO) {
                runCatching { MediaStore.createDeleteRequest(contentResolver, listOf(targetUris[0])) }
            }
            next.fold(
                onSuccess = { pendingIntent ->
                    consentLauncher.launch(
                        IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                    )
                },
                onFailure = {
                    Notifications.notifyDeletionFailed(applicationContext, targetUris.size)
                    finish()
                },
            )
        } else {
            proceedApi29()
        }
    }

    /** API 29 sequential delete-with-consent loop. */
    private fun proceedApi29() {
        lifecycleScope.launch {
            while (api29Index < targetUris.size) {
                val uri = targetUris[api29Index]
                val outcome = withContext(Dispatchers.IO) {
                    try {
                        contentResolver.delete(uri, null, null)
                        null
                    } catch (recoverable: android.app.RecoverableSecurityException) {
                        recoverable.userAction.actionIntent.intentSender
                    } catch (failure: Throwable) {
                        // File already gone or provider refused: count as resolved.
                        null
                    }
                }
                if (outcome != null) {
                    api29Launcher.launch(IntentSenderRequest.Builder(outcome).build())
                    return@launch
                }
                approvedApi29.add(uri.toString())
                api29Index += 1
            }
            onAllApproved()
        }
    }

    private suspend fun fileExists(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                null,
            )?.use { it.count > 0 } ?: false
        } catch (t: Throwable) {
            false
        }
    }

    private fun onAllApproved() {
        val all = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            targetUris.map { it.toString() }
        } else {
            approvedApi29.toList()
        }
        lifecycleScope.launch {
            container.screenshotRepository.onDeletionApproved(all)
            Notifications.cancelConsentNotifications(applicationContext)
            finish()
        }
    }

    private fun onUserCancelled() {
        // Explicit cancel: clear timers on the unapproved files so the sweep
        // stops re-requesting consent. Nothing is deleted.
        lifecycleScope.launch {
            val unapproved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                targetUris.map { it.toString() }
            } else {
                targetUris.dropWhile { approvedApi29.contains(it.toString()) }.map { it.toString() }
            }
            for (uri in unapproved) {
                container.screenshotRepository.cancelDeleteTimer(uri)
                BackgroundWork.cancelTimerSweep(applicationContext, uri)
            }
            Notifications.cancelConsentNotifications(applicationContext)
            finish()
        }
    }
}
