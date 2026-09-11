package io.github.mosbee1.thebomb.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.ui.MainViewModel
import io.github.mosbee1.thebomb.ui.common.SettingsGroup
import io.github.mosbee1.thebomb.ui.common.SettingsRowDivider
import io.github.mosbee1.thebomb.ui.common.requestIgnoreBatteryOptimizations

/**
 * Live permission status table in one grouped card. The Media-management
 * row only exists on Android 12+ (the setting itself was introduced there)
 * - on Android 11 it would show a permanently unfixable "Denied".
 */
@Composable
fun PermissionDashboard(mainViewModel: MainViewModel) {
    val context = LocalContext.current
    val permissions by mainViewModel.permissions.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mainViewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    fun openAppPage() {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
        }
    }

    SettingsGroup(title = stringResource(R.string.settings_section_permissions)) {
        PermissionRow(
            Icons.Rounded.Image,
            stringResource(R.string.perm_row_images),
            permissions.images,
            stringResource(R.string.perm_action_grant),
        ) { imageLauncher.launch(imagePermission) }
        SettingsRowDivider()
        PermissionRow(
            Icons.Rounded.NotificationsActive,
            stringResource(R.string.perm_row_notifications),
            permissions.notifications,
            stringResource(R.string.perm_action_grant),
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        SettingsRowDivider()
        PermissionRow(
            Icons.Rounded.Fullscreen,
            stringResource(R.string.perm_row_fsi),
            permissions.fullScreenIntent,
            stringResource(R.string.perm_action_open_settings),
        ) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }
        }
        SettingsRowDivider()
        PermissionRow(
            Icons.Rounded.Layers,
            stringResource(R.string.perm_row_overlay),
            permissions.overlay,
            stringResource(R.string.perm_action_open_settings),
        ) { openAppPage() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingsRowDivider()
            PermissionRow(
                Icons.Rounded.PhotoLibrary,
                stringResource(R.string.perm_row_media),
                permissions.manageMedia,
                stringResource(R.string.perm_action_open_settings),
            ) { openAppPage() }
        }
        SettingsRowDivider()
        PermissionRow(
            Icons.Rounded.BatteryChargingFull,
            stringResource(R.string.perm_row_battery),
            permissions.batteryOptimization,
            stringResource(R.string.perm_action_open_settings),
        ) { requestIgnoreBatteryOptimizations(context) }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(50),
            color = if (granted) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer,
            contentColor = if (granted) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Text(
                stringResource(if (granted) R.string.perm_state_granted else R.string.perm_state_denied),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        if (!granted) {
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
