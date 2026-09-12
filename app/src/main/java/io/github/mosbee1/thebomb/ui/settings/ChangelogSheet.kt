package io.github.mosbee1.thebomb.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.ui.common.CircularIcon
import io.github.mosbee1.thebomb.ui.common.VersionBadge

private data class ChangeEntry(val version: String, val bullets: List<String>)

private val changeLog = listOf(
    ChangeEntry(
        "v1.4",
        listOf(
            "Zenith-style Settings: stacked icon cards, circular icons, chevrons",
            "Rotating support banner (Ko-fi, coffee, star, issues)",
            "Permissions rebuilt as a bottom sheet with a ready pill",
            "Sheet-based editors with segmented controls",
            "In-app changelog viewer",
        ),
    ),
    ChangeEntry(
        "v1.3",
        listOf(
            "Settings grouped into rounded cards",
            "Adaptive centering on tablets and landscape",
            "Media-management row hidden below Android 12",
        ),
    ),
    ChangeEntry("v1.2.1", listOf("Fixed crash on launch under Android 11")),
    ChangeEntry(
        "v1.2",
        listOf(
            "Optional silent auto-delete via Media management",
            "Support links (Ko-fi, Buy Me a Coffee)",
        ),
    ),
    ChangeEntry(
        "v1.1",
        listOf(
            "Floating popup redesign",
            "Popup and notification no longer show together",
            "Permissions dashboard introduced",
        ),
    ),
    ChangeEntry(
        "v1.0",
        listOf("Initial release: popup triage, fuses, nightly blast, stats, search"),
    ),
)

/** In-app changelog timeline, matching the reference app's version sheets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogSheet(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularIcon(Icons.Rounded.History, size = 28)
            Spacer(Modifier.size(10.dp))
            Text(stringResource(R.string.about_changelog_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(16.dp))
            changeLog.forEach { entry ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        VersionBadge(entry.version)
                        Spacer(Modifier.size(8.dp))
                        entry.bullets.forEach { bullet ->
                            Row(Modifier.padding(vertical = 2.dp)) {
                                Box(
                                    Modifier
                                        .padding(top = 7.dp)
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    bullet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            TextButton(onClick = {
                runCatching { uriHandler.openUri("https://github.com/MosBee1/TheBomb/commits/main") }
            }) {
                Text(stringResource(R.string.about_full_changelog))
            }
        }
    }
}
