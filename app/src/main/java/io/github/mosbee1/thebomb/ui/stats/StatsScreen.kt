package io.github.mosbee1.thebomb.ui.stats

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.data.model.StatsSnapshot
import io.github.mosbee1.thebomb.util.ByteSizeFormatter
import io.github.mosbee1.thebomb.ui.common.rememberAdaptiveHorizontalPadding
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as TheBombApp).container.screenshotRepository

    val stats = repo.observeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsSnapshot())
}

/**
 * Stats: animated big numbers (processed / detonated / defused / storage
 * cleared) plus a stacked distribution bar of the current armed/archived/
 * defused mix with a legend. All numbers animate whenever the flows change.
 */
@Composable
fun StatsScreen() {
    val statsViewModel: StatsViewModel = viewModel()
    val stats by statsViewModel.stats.collectAsStateWithLifecycle()
    val hPad = rememberAdaptiveHorizontalPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = hPad, end = hPad, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.stats_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.stats_storage_freed),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    ByteSizeFormatter.format(stats.bytesFreed),
                    style = MaterialTheme.typography.displayMedium,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CounterCard(
                icon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null) },
                label = stringResource(R.string.stats_processed),
                value = stats.processedCount,
                modifier = Modifier.weight(1f),
            )
            CounterCard(
                icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                label = stringResource(R.string.stats_deleted),
                value = stats.deletedCount,
                modifier = Modifier.weight(1f),
            )
            CounterCard(
                icon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                label = stringResource(R.string.stats_kept),
                value = stats.keptCount,
                modifier = Modifier.weight(1f),
            )
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.stats_breakdown),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(16.dp))
                DistributionBar(
                    pending = stats.pendingCount,
                    archived = stats.archivedCount,
                    kept = stats.keptCount,
                )
                Spacer(Modifier.height(16.dp))
                LegendRow(
                    color = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.legend_pending),
                    count = stats.pendingCount,
                )
                LegendRow(
                    color = MaterialTheme.colorScheme.tertiary,
                    label = stringResource(R.string.legend_archived),
                    count = stats.archivedCount,
                )
                LegendRow(
                    color = MaterialTheme.colorScheme.secondary,
                    label = stringResource(R.string.legend_kept),
                    count = stats.keptCount,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(
                        R.string.stats_pending_now,
                        ByteSizeFormatter.format(stats.pendingBytes),
                        stats.pendingCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (stats.processedCount == 0L) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("💣", style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.stats_empty_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.stats_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CounterCard(
    icon: @Composable () -> Unit,
    label: String,
    value: Long,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            icon()
            Spacer(Modifier.height(8.dp))
            AnimatedCount(value = value)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun AnimatedCount(value: Long) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "statCount",
    )
    Text(
        NumberFormat.getIntegerInstance().format(animated.toLong()),
        style = MaterialTheme.typography.headlineMedium,
    )
}

@Composable
private fun DistributionBar(pending: Long, archived: Long, kept: Long) {
    val total = pending + archived + kept
    if (total == 0L) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface),
        )
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp)),
    ) {
        if (pending > 0) {
            Box(
                Modifier
                    .weight(pending.toFloat())
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        if (archived > 0) {
            Box(
                Modifier
                    .weight(archived.toFloat())
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.tertiary),
            )
        }
        if (kept > 0) {
            Box(
                Modifier
                    .weight(kept.toFloat())
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.secondary),
            )
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, count: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$label — ${NumberFormat.getIntegerInstance().format(count)}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
