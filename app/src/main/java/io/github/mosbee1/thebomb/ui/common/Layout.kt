package io.github.mosbee1.thebomb.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MAX_CONTENT_WIDTH: Dp = 640.dp

/** Horizontal padding that centers content on wide screens; 16dp on phones. */
@Composable
fun rememberAdaptiveHorizontalPadding(): Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    return remember(screenWidth) {
        if (screenWidth > MAX_CONTENT_WIDTH) (screenWidth - MAX_CONTENT_WIDTH) / 2 else 16.dp
    }
}

/** Standard contentPadding for the scrolling lists (Home, Kept). */
@Composable
fun rememberListContentPadding(bottom: Dp = 96.dp): PaddingValues {
    val h = rememberAdaptiveHorizontalPadding()
    return remember(h, bottom) { PaddingValues(start = h, end = h, bottom = bottom) }
}

/** Grouped settings section: label above, one rounded card for all rows. */
@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Column(content = content)
        }
    }
}

/** Inset divider aligned with row text (after the leading icon slot). */
@Composable
fun SettingsRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}
