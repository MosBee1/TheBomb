package io.github.mosbee1.thebomb.popup

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.data.local.ScreenshotEntity
import io.github.mosbee1.thebomb.data.model.AppSettings
import io.github.mosbee1.thebomb.notifications.Notifications
import io.github.mosbee1.thebomb.ui.common.TimerPickerPanel
import io.github.mosbee1.thebomb.ui.common.TimerSelection
import io.github.mosbee1.thebomb.ui.common.Triage
import io.github.mosbee1.thebomb.ui.common.rememberDurationLabel
import io.github.mosbee1.thebomb.ui.theme.TheBombTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The post-screenshot pop-up: a translucent, dialog-styled activity with a
 * live thumbnail (loadThumbnail - never a full decode), Defuse / Archive /
 * Detonate Now, and "Set the fuse" expanding into the full timer picker.
 *
 * Launched by the notification's full-screen intent (locked/off screen) or
 * directly by the watcher service when the optional overlay grant is held
 * (unlocked). noHistory + excludeFromRecents + no window animation.
 */
class PopupActivity : ComponentActivity() {

    private val uriState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.45f)
        setFinishOnTouchOutside(true)
        uriState.value = intent?.getStringExtra(Notifications.EXTRA_SCREENSHOT_URI)
        if (uriState.value == null) {
            finish()
            return
        }
        setContent {
            TheBombTheme {
                val uri = uriState.value
                if (uri != null) {
                    PopupRoot(uri = uri, onFinish = { finish() })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        uriState.value = intent.getStringExtra(Notifications.EXTRA_SCREENSHOT_URI)
    }
}

private enum class PopupStage { Loading, Ready, Gone, TimerSet }

/** Wraps the row with a "has the DB answered yet" flag so a not-yet-loaded
 *  row is never mistaken for an untracked one. */
private data class PopupEntityState(
    val loaded: Boolean = false,
    val entity: ScreenshotEntity? = null,
)

@Composable
fun PopupRoot(uri: String, onFinish: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TheBombApp
    val container = app.container
    val scope = rememberCoroutineScope()

    val entityState by remember(uri) {
        container.screenshotRepository.observeByUri(uri)
            .map { PopupEntityState(loaded = true, entity = it) }
    }.collectAsStateWithLifecycle(initialValue = PopupEntityState())
    val entity = entityState.entity

    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())

    var stage by remember { mutableStateOf(PopupStage.Loading) }
    var timerPanelOpen by remember { mutableStateOf(false) }
    var pendingSelection by remember { mutableStateOf<TimerSelection?>(null) }

    // Keyed on `loaded` (fires once): later entity updates (e.g. a newly
    // armed fuse) must NOT reset the TimerSet confirmation stage.
    LaunchedEffect(entityState.loaded) {
        if (entityState.loaded) {
            stage = if (entity == null) PopupStage.Gone else PopupStage.Ready
        }
    }

    LaunchedEffect(stage) {
        when (stage) {
            PopupStage.Gone -> { delay(900); onFinish() }
            PopupStage.TimerSet -> { delay(1400); onFinish() }
            else -> Unit
        }
    }

    val thumbnail by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.loadThumbnail(
                    Uri.parse(uri),
                    Size(512, 512),
                    null,
                )
            }.getOrNull()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail!!.asImageBitmap(),
                            contentDescription = entity?.fileName,
                            modifier = Modifier.size(72.dp),
                        )
                    } else {
                        Icon(Icons.Rounded.Image, contentDescription = null)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.notif_screenshot_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    entity?.let { e ->
                        Text(
                            e.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when (stage) {
                PopupStage.Gone -> {
                    Text(
                        stringResource(R.string.popup_untracked),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                PopupStage.TimerSet -> {
                    val sel = pendingSelection
                    val label = when (sel) {
                        is TimerSelection.Preset -> {
                            val p = settings.presets.firstOrNull { it.slot == sel.slot }
                            if (p != null) rememberDurationLabel(p.amount, p.unit) else ""
                        }
                        is TimerSelection.Custom -> rememberDurationLabel(sel.amount, sel.unit)
                        null -> ""
                    }
                    Text(
                        stringResource(R.string.popup_timer_set_toast, label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = {
                            scope.launch {
                                entity?.let { e -> Triage.keep(context, e.uri) }
                                dismiss(context, entity)
                                onFinish()
                            }
                        }) {
                            Icon(Icons.Rounded.PushPin, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_keep))
                        }
                        FilledTonalButton(onClick = {
                            scope.launch {
                                entity?.let { e -> Triage.archive(context, e.uri) }
                                dismiss(context, entity)
                                onFinish()
                            }
                        }) {
                            Icon(Icons.Rounded.Archive, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_archive))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                entity?.let { e ->
                                    Triage.deleteNow(context, listOf(e.uri))
                                }
                                onFinish()
                            }
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_delete_now))
                        }
                        FilledTonalButton(onClick = { timerPanelOpen = !timerPanelOpen }) {
                            Icon(Icons.Rounded.Timer, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.popup_set_timer))
                        }
                    }

                    AnimatedVisibility(
                        visible = timerPanelOpen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        TimerPickerPanel(
                            presets = settings.presets,
                            initialSelection = null,
                            onConfirm = { selection ->
                                val millis = selection.durationMillis(settings.presets)
                                if (millis != null && entity != null) {
                                    scope.launch {
                                        Triage.setTimer(context, entity.uri, millis)
                                        pendingSelection = selection
                                        stage = PopupStage.TimerSet
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun dismiss(context: android.content.Context, entity: ScreenshotEntity?) {
    entity?.let {
        Notifications.cancelScreenshotNotification(context, it.mediaStoreId)
    }
}
