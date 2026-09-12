package io.github.mosbee1.thebomb.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.ui.MainViewModel
import io.github.mosbee1.thebomb.ui.common.CircularIcon
import io.github.mosbee1.thebomb.ui.common.PillButton
import io.github.mosbee1.thebomb.ui.common.requestIgnoreBatteryOptimizations

private data class PermRow(
    val icon: ImageVector,
    val titleRes: Int,
    val subRes: Int,
    val granted: Boolean,
    val fixLabelRes: Int,
    val fix: () -> Unit,
)

/** Zenith-style permissions sheet: shield header, check circles, ready pill. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSheet(mainViewModel: MainViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val permissions by mainViewModel.permissions.collectAsState()
    val sheetState = rememberModalBottomSheetState()

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

    val rows = buildList {
        add(
            PermRow(
                Icons.Rounded.Image, R.string.perm_row_images, R.string.perm_sub_images,
                permissions.images, R.string.perm_action_grant,
            ) { imageLauncher.launch(imagePermission) },
        )
        add(
            PermRow(
                Icons.Rounded.NotificationsActive, R.string.perm_row_notifications,
                R.string.perm_sub_notifications, permissions.notifications,
                R.string.perm_action_grant,
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
        add(
            PermRow(
                Icons.Rounded.Fullscreen, R.string.perm_row_fsi, R.string.perm_sub_fsi,
                permissions.fullScreenIntent, R.string.perm_action_open_settings,
            ) {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }
            },
        )
        add(
            PermRow(
                Icons.Rounded.Layers, R.string.perm_row_overlay, R.string.perm_sub_overlay,
                permissions.overlay, R.string.perm_action_open_settings,
            ) { openAppPage() },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(
                PermRow(
                    Icons.Rounded.PhotoLibrary, R.string.perm_row_media, R.string.perm_sub_media,
                    permissions.manageMedia, R.string.perm_action_open_settings,
                ) { openAppPage() },
            )
        }
        add(
            PermRow(
                Icons.Rounded.BatteryChargingFull, R.string.perm_row_battery,
                R.string.perm_sub_battery, permissions.batteryOptimization,
                R.string.perm_action_open_settings,
            ) { requestIgnoreBatteryOptimizations(context) },
        )
    }
    val missing = rows.count { !it.granted }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularIcon(Icons.Rounded.Security, size = 28)
            Spacer(Modifier.size(10.dp))
            Text(
                stringResource(R.string.perm_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.perm_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            rows.forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularIcon(row.icon)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(row.titleRes), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(row.subRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (row.granted) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = stringResource(R.string.perm_state_granted),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp),
                            )
                        }
                    } else {
                        TextButton(onClick = row.fix) { Text(stringResource(row.fixLabelRes)) }
                    }
                }
            }
            Spacer(Modifier.size(12.dp))
            PillButton(
                text = if (missing == 0) stringResource(R.string.perm_all_ready)
                else stringResource(R.string.perm_missing_fmt, missing),
                enabled = missing == 0,
                onClick = onDismiss,
            )
        }
    }
}
