package io.github.mosbee1.thebomb.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.data.local.ScreenshotEntity
import io.github.mosbee1.thebomb.data.model.TimerPreset

/**
 * The long-press action sheet shared by Home and Kept. Options appear
 * according to the row's current state. [archiveLabelOverride] lets the
 * Kept tab relabel the Archive slot as "Rearm" (unkeep) without duplicating
 * the sheet. "Set the fuse" expands the full timer picker in place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageSheet(
    entity: ScreenshotEntity,
    presets: List<TimerPreset>,
    onDismiss: () -> Unit,
    onKeep: () -> Unit,
    onArchive: (() -> Unit)?,
    archiveLabelOverride: String? = null,
    onRestore: (() -> Unit)?,
    onDeleteNow: () -> Unit,
    onSetTimer: (durationMillis: Long) -> Unit,
    onCancelTimer: (() -> Unit)?,
) {
    val sheetState = rememberModalBottomSheetState()
    var timerOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                entity.fileName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Spacer(Modifier.padding(bottom = 8.dp))

            SheetRow(
                icon = Icons.Rounded.PushPin,
                label = stringResource(R.string.action_keep),
                onClick = onKeep,
            )
            if (onArchive != null) {
                SheetRow(
                    icon = Icons.Rounded.Archive,
                    label = archiveLabelOverride ?: stringResource(R.string.action_archive),
                    onClick = onArchive,
                )
            }
            if (onRestore != null) {
                SheetRow(
                    icon = Icons.Rounded.Unarchive,
                    label = stringResource(R.string.action_restore),
                    onClick = onRestore,
                )
            }
            SheetRow(
                icon = Icons.Rounded.Timer,
                label = stringResource(R.string.popup_set_timer),
                onClick = { timerOpen = !timerOpen },
            )
            if (onCancelTimer != null && entity.deleteAtMillis != null) {
                val remaining = rememberCountdownText(entity.deleteAtMillis)
                SheetRow(
                    icon = Icons.Rounded.TimerOff,
                    label = stringResource(R.string.timer_cancel) + " (" + remaining + ")",
                    onClick = onCancelTimer,
                )
            }
            SheetRow(
                icon = Icons.Rounded.Delete,
                label = stringResource(R.string.action_delete_now),
                onClick = onDeleteNow,
            )

            AnimatedVisibility(
                visible = timerOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                TimerPickerPanel(
                    presets = presets,
                    initialSelection = null,
                    onConfirm = { selection ->
                        val millis = selection.durationMillis(presets)
                        if (millis != null) onSetTimer(millis)
                    },
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun SheetRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
