package io.github.mosbee1.thebomb.ui.onboarding

import android.Manifest
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.ui.MainViewModel
import io.github.mosbee1.thebomb.ui.common.CardTone
import io.github.mosbee1.thebomb.ui.common.WarningCard
import io.github.mosbee1.thebomb.ui.common.openOverlaySettings
import io.github.mosbee1.thebomb.ui.common.requestIgnoreBatteryOptimizations

/**
 * First-run permissions onboarding. Every card explains WHY the permission
 * is needed BEFORE the ask. Nothing here is mandatory except image access
 * for the core loop; every denial degrades gracefully (warning cards on
 * Home + no-op paths), so the flow always has a "Not now" exit.
 */
@Composable
fun OnboardingScreen(mainViewModel: MainViewModel) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            stringResource(R.string.onboarding_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PermissionCard(
            icon = Icons.Rounded.Image,
            title = stringResource(R.string.perm_images_title),
            body = stringResource(R.string.perm_images_body),
            granted = permissions.images,
            required = true,
            actionLabel = stringResource(R.string.perm_action_grant),
            onAction = { imageLauncher.launch(imagePermission) },
        )
        PermissionCard(
            icon = Icons.Rounded.NotificationsActive,
            title = stringResource(R.string.perm_notifications_title),
            body = stringResource(R.string.perm_notifications_body),
            granted = permissions.notifications,
            required = false,
            actionLabel = stringResource(R.string.perm_action_grant),
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
        PermissionCard(
            icon = Icons.Rounded.Fullscreen,
            title = stringResource(R.string.perm_fsi_title),
            body = stringResource(R.string.perm_fsi_body),
            granted = permissions.fullScreenIntent,
            required = false,
            actionLabel = stringResource(R.string.perm_action_open_settings),
            onAction = {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                } catch (t: Throwable) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                }
            },
        )
        PermissionCard(
            icon = Icons.Rounded.BatteryChargingFull,
            title = stringResource(R.string.perm_battery_title),
            body = stringResource(R.string.perm_battery_body),
            granted = permissions.batteryOptimization,
            required = false,
            actionLabel = stringResource(R.string.perm_action_open_settings),
            onAction = { requestIgnoreBatteryOptimizations(context) },
        )
        PermissionCard(
            icon = Icons.Rounded.Layers,
            title = stringResource(R.string.perm_overlay_title),
            body = stringResource(R.string.perm_overlay_body),
            granted = permissions.overlay,
            required = false,
            actionLabel = stringResource(R.string.perm_action_open_settings),
            onAction = { openOverlaySettings(context) },
        )

        if (!permissions.notifications && permissions.fullScreenIntent.not()) {
            // Both popup channels degraded: make the consequence explicit once.
            WarningCard(
                title = stringResource(R.string.onboarding_degraded_title),
                body = stringResource(R.string.onboarding_degraded_body),
                icon = Icons.Rounded.NotificationsActive,
                tone = CardTone.INFO,
                actionLabel = null,
                onAction = null,
            )
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                mainViewModel.completeOnboarding()
                mainViewModel.setJanitorEnabled(true)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_enable_janitor))
        }
        OutlinedButton(
            onClick = { mainViewModel.completeOnboarding() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_not_now))
        }

        val uriHandler = LocalUriHandler.current
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(onClick = {
                uriHandler.openUri("https://github.com/MosBee1/TheBomb/blob/main/PRIVACY.md")
            }) { Text(stringResource(R.string.about_privacy)) }
            TextButton(onClick = {
                uriHandler.openUri("https://github.com/MosBee1/TheBomb/blob/main/LICENSE")
            }) { Text(stringResource(R.string.about_license_title)) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    body: String,
    granted: Boolean,
    required: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (granted) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (granted) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            stringResource(
                                if (granted) R.string.perm_state_granted
                                else R.string.perm_state_denied
                            )
                        )
                    },
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodySmall)
            if (!granted) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onAction) {
                    Text(
                        actionLabel +
                            if (!required) {
                                " (" + stringResource(R.string.onboarding_optional_hint) + ")"
                            } else {
                                ""
                            }
                    )
                }
            }
        }
    }
}
