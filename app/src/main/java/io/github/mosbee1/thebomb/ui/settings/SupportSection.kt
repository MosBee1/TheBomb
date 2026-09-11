package io.github.mosbee1.thebomb.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.ui.common.SectionHeader

/**
 * Support links. Opens the browser via the system — the app has no INTERNET
 * permission, so the links hand off to the OS entirely; nothing is fetched
 * or sent by this app.
 */
@Composable
fun SupportSection() {
    val uriHandler = LocalUriHandler.current
    Column {
        SectionHeader(stringResource(R.string.support_section_title))
        SupportRow(
            icon = Icons.Rounded.Favorite,
            label = stringResource(R.string.support_bmc),
            url = "https://buymeacoffee.com/MosBee1",
            uriHandler = uriHandler,
        )
        SupportRow(
            icon = Icons.Rounded.LocalCafe,
            label = stringResource(R.string.support_kofi),
            url = "https://ko-fi.com/mosbee1",
            uriHandler = uriHandler,
        )
        Text(
            stringResource(R.string.support_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SupportRow(
    icon: ImageVector,
    label: String,
    url: String,
    uriHandler: UriHandler,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { runCatching { uriHandler.openUri(url) } }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.OpenInNew, contentDescription = null)
        }
    }
}
