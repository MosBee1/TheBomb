package io.github.mosbee1.thebomb.ui.common

import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.data.local.ScreenshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** One swipe direction's quick action, rendered as the dismiss background. */
data class SwipeAction(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onAction: () -> Unit,
)

/**
 * A single screenshot row: thumbnail (loadThumbnail, never a full decode),
 * name, capture date, and the live countdown chip when a fuse is armed —
 * or an "approval needed" chip when the fuse elapsed and the system consent
 * dialog is outstanding. Entry/exit motion is handled by the LazyColumn.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotCard(
    entity: ScreenshotEntity,
    modifier: Modifier = Modifier,
    swipeRight: SwipeAction? = null,
    swipeLeft: SwipeAction? = null,
    onLongPress: () -> Unit = {},
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    swipeRight?.onAction?.invoke()
                    swipeRight != null
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    swipeLeft?.onAction?.invoke()
                    swipeLeft != null
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )

    val row: @Composable () -> Unit = { CardRow(entity, modifier, onLongPress) }

    if (swipeRight == null && swipeLeft == null) {
        row()
    } else {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val action = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> swipeRight
                    SwipeToDismissBoxValue.EndToStart -> swipeLeft
                    SwipeToDismissBoxValue.Settled -> null
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(action?.color ?: Color.Transparent)
                        .padding(horizontal = 24.dp),
                ) {
                    if (action != null) {
                        Row(
                            Modifier.align(
                                if (direction == SwipeToDismissBoxValue.EndToStart) {
                                    Alignment.CenterEnd
                                } else {
                                    Alignment.CenterStart
                                },
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(action.icon, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(action.label, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            },
            content = { row() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardRow(entity: ScreenshotEntity, modifier: Modifier, onLongPress: () -> Unit) {
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(initialValue = null, entity.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.loadThumbnail(
                    Uri.parse(entity.uri),
                    Size(256, 256),
                    null,
                )
            }.getOrNull()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = entity.fileName,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entity.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    captureDateLabel(entity.createdAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (entity.kept) {
                Chip(
                    text = stringResource(R.string.kept_chip),
                    icon = Icons.Rounded.PushPin,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                )
            } else if (entity.deleteAtMillis != null) {
                val now = System.currentTimeMillis()
                if (entity.deleteAtMillis!! <= now) {
                    Chip(
                        text = stringResource(R.string.timer_approval_needed),
                        icon = Icons.Rounded.Delete,
                        color = MaterialTheme.colorScheme.errorContainer,
                    )
                } else {
                    Chip(
                        text = rememberCountdownText(entity.deleteAtMillis!!),
                        icon = Icons.Rounded.Timer,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, icon: ImageVector, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun captureDateLabel(createdAtMillis: Long): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    return Instant.ofEpochMilli(createdAtMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
