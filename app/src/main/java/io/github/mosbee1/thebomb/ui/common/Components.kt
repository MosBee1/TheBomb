package io.github.mosbee1.thebomb.ui.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.ui.MainViewModel
import io.github.mosbee1.thebomb.ui.PermissionsState

/**
 * Reusable building blocks shared by every screen: warning cards (the
 * "actionable degradation" surface), empty states, and section headers.
 */

enum class CardTone { INFO, WARNING, ERROR }

@Composable
fun WarningCard(
    title: String,
    body: String,
    icon: ImageVector,
    tone: CardTone,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val container = when (tone) {
        CardTone.INFO -> MaterialTheme.colorScheme.secondaryContainer
        CardTone.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        CardTone.ERROR -> MaterialTheme.colorScheme.errorContainer
    }
    val onContainer = when (tone) {
        CardTone.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
        CardTone.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        CardTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = container,
        contentColor = onContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodySmall)
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel, color = onContainer)
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (emoji.isNotEmpty()) {
            Text(emoji, style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * The degradation banners on Home when a required or recommended permission
 * is missing. Each card explains WHY, offers the exact fix, and never
 * blocks the rest of the UI.
 */
@Composable
fun HomePermissionCards(mainViewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val permissions by mainViewModel.permissions.collectAsState()

    val imagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { mainViewModel.refreshPermissions() }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { mainViewModel.refreshPermissions() }

    fun openAppDetails() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!permissions.images) {
            WarningCard(
                title = stringResource(R.string.card_permission_images_title),
                body = stringResource(R.string.card_permission_images_body),
                icon = Icons.Rounded.Image,
                tone = CardTone.ERROR,
                actionLabel = stringResource(R.string.perm_action_grant),
                onAction = { imageLauncher.launch(imagePermission) },
            )
        }
        if (!permissions.notifications) {
            WarningCard(
                title = stringResource(R.string.card_permission_notifications_title),
                body = stringResource(R.string.card_permission_notifications_body),
                icon = Icons.Rounded.NotificationsOff,
                tone = CardTone.WARNING,
                actionLabel = stringResource(R.string.perm_action_grant),
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
        }
        if (!permissions.fullScreenIntent) {
            WarningCard(
                title = stringResource(R.string.card_permission_fsi_title),
                body = stringResource(R.string.card_permission_fsi_body),
                icon = Icons.Rounded.Fullscreen,
                tone = CardTone.WARNING,
                actionLabel = stringResource(R.string.perm_action_open_settings),
                onAction = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    } catch (t: Throwable) {
                        openAppDetails()
                    }
                },
            )
        }
        if (!permissions.batteryOptimization) {
            WarningCard(
                title = stringResource(R.string.card_permission_battery_title),
                body = stringResource(R.string.card_permission_battery_body),
                icon = Icons.Rounded.BatteryAlert,
                tone = CardTone.INFO,
                actionLabel = stringResource(R.string.perm_action_open_settings),
                onAction = { requestIgnoreBatteryOptimizations(context) },
            )
        }
    }
}

fun requestIgnoreBatteryOptimizations(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${context.packageName}")),
        )
    } catch (t: Throwable) {
        // OEM removed the action; the regular settings page is the fallback.
        try {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (ignored: Throwable) {
            // Nothing else to try; fuses simply run on WorkManager cadence.
        }
    }
}

fun openOverlaySettings(context: Context) {
    try {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    } catch (t: Throwable) {
        try {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        } catch (ignored: Throwable) {
            // Overlay stays unavailable; the app degrades as designed.
        }
    }
}

/** State chip text for a boolean grant (used by onboarding helpers). */
@Composable
fun grantedLabel(granted: Boolean): String =
    stringResource(if (granted) R.string.perm_state_granted else R.string.perm_state_denied)

@Composable
fun rememberPermissionIcon(permissions: PermissionsState): ImageVector = remember(permissions) {
    if (permissions.images) Icons.Rounded.Bolt else Icons.Rounded.WbTwilight
}
