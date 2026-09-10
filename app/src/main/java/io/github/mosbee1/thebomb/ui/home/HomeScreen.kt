package io.github.mosbee1.thebomb.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.data.local.ScreenshotEntity
import io.github.mosbee1.thebomb.data.model.AppSettings
import io.github.mosbee1.thebomb.ui.MainViewModel
import io.github.mosbee1.thebomb.ui.common.EmptyState
import io.github.mosbee1.thebomb.ui.common.HomePermissionCards
import io.github.mosbee1.thebomb.ui.common.SectionHeader
import io.github.mosbee1.thebomb.ui.common.ScreenshotCard
import io.github.mosbee1.thebomb.ui.common.SwipeAction
import io.github.mosbee1.thebomb.ui.common.Triage
import io.github.mosbee1.thebomb.ui.common.TriageSheet
import io.github.mosbee1.thebomb.ui.common.rememberCountdownText
import io.github.mosbee1.thebomb.ui.common.rememberNowTicker
import io.github.mosbee1.thebomb.util.CleanupSchedule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
private const val EXIT_ANIM_MILLIS = 300L

private data class RecencyGroup(val label: String, val items: List<ScreenshotEntity>)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(mainViewModel: MainViewModel) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel()

    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val pending by homeViewModel.pending.collectAsStateWithLifecycle()
    val archived by homeViewModel.archived.collectAsStateWithLifecycle()
    val filters by homeViewModel.filters.collectAsState()

    // Re-check grants whenever the user returns from a system settings page
    // so the warning cards never show stale state.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mainViewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var removing by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    fun animateThen(uri: String, action: suspend () -> Unit) {
        if (uri in removing) return
        removing = removing + uri
        scope.launch {
            delay(EXIT_ANIM_MILLIS)
            action()
            removing = removing - uri
        }
    }

    var sheetEntity by remember { mutableStateOf<ScreenshotEntity?>(null) }
    var showRangePicker by remember { mutableStateOf(false) }
    val groups = rememberRecencyGroups(pending)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "search") {
            Column {
                OutlinedTextField(
                    value = filters.query,
                    onValueChange = homeViewModel::setQuery,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filters.isActive) {
                            IconButton(onClick = homeViewModel::clearAllFilters) {
                                Icon(Icons.Rounded.Close, contentDescription = null)
                            }
                        }
                    },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { showRangePicker = true },
                        label = { Text(dateRangeLabel(filters.range)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                        },
                    )
                    if (filters.range != null) {
                        AssistChip(
                            onClick = homeViewModel::clearDateRange,
                            label = { Text(stringResource(R.string.filter_clear)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Close, contentDescription = null)
                            },
                        )
                    }
                }
            }
        }

        item(key = "permission_cards") {
            HomePermissionCards(mainViewModel)
        }

        item(key = "pending_header") {
            SectionHeader(
                stringResource(R.string.section_pending) +
                    if (pending.isNotEmpty()) " (${pending.size})" else "",
            )
        }

        if (pending.isEmpty()) {
            item(key = "pending_empty") {
                if (filters.isActive) {
                    EmptyState(
                        emoji = "🔍",
                        title = stringResource(R.string.filter_no_results_title),
                        body = stringResource(R.string.filter_no_results_body),
                    )
                } else {
                    CelebrationState()
                }
            }
        } else {
            groups.forEach { group ->
                item(key = "group_" + group.label) {
                    Text(
                        group.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                items(group.items, key = { it.uri }) { entity ->
                    Box(Modifier.animateItemPlacement()) {
                        AnimatedRow(removing = entity.uri in removing) {
                            PendingRow(
                                entity = entity,
                                onKeep = {
                                    animateThen(entity.uri) { Triage.keep(context, entity.uri) }
                                },
                                onArchive = {
                                    animateThen(entity.uri) { Triage.archive(context, entity.uri) }
                                },
                                onLongPress = { sheetEntity = entity },
                            )
                        }
                    }
                }
            }
        }

        item(key = "archived_header") {
            SectionHeader(
                stringResource(R.string.section_archived) +
                    if (archived.isNotEmpty()) " (${archived.size})" else "",
            )
        }

        item(key = "cleanup_banner") {
            CleanupBanner(settings)
        }

        if (archived.isEmpty()) {
            item(key = "archived_empty") {
                EmptyState(
                    emoji = "🗂️",
                    title = stringResource(R.string.empty_archived_title),
                    body = stringResource(R.string.empty_archived_body),
                )
            }
        } else {
            items(archived, key = { "arch_" + it.uri }) { entity ->
                Box(Modifier.animateItemPlacement()) {
                    AnimatedRow(removing = entity.uri in removing) {
                        ArchivedRow(
                            entity = entity,
                            onKeep = {
                                animateThen(entity.uri) { Triage.keep(context, entity.uri) }
                            },
                            onRestore = {
                                animateThen(entity.uri) { Triage.unarchive(context, entity.uri) }
                            },
                            onLongPress = { sheetEntity = entity },
                        )
                    }
                }
            }
        }
    }

    sheetEntity?.let { entity ->
        TriageSheet(
            entity = entity,
            presets = settings.presets,
            onDismiss = { sheetEntity = null },
            onKeep = {
                animateThen(entity.uri) { Triage.keep(context, entity.uri) }
                sheetEntity = null
            },
            onArchive = if (!entity.archived) {
                {
                    animateThen(entity.uri) { Triage.archive(context, entity.uri) }
                    sheetEntity = null
                }
            } else {
                null
            },
            onRestore = if (entity.archived) {
                {
                    animateThen(entity.uri) { Triage.unarchive(context, entity.uri) }
                    sheetEntity = null
                }
            } else {
                null
            },
            onDeleteNow = {
                Triage.deleteNow(context, listOf(entity.uri))
                sheetEntity = null
            },
            onSetTimer = { millis ->
                scope.launch { Triage.setTimer(context, entity.uri, millis) }
                sheetEntity = null
            },
            onCancelTimer = if (entity.deleteAtMillis != null) {
                {
                    scope.launch { Triage.cancelTimer(context, entity.uri) }
                    sheetEntity = null
                }
            } else {
                null
            },
        )
    }

    if (showRangePicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedEndDateMillis != null,
                    onClick = {
                        val start = pickerState.selectedStartDateMillis
                        val end = pickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            homeViewModel.setDateRange(start, end + DAY_MILLIS)
                        }
                        showRangePicker = false
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DateRangePicker(
                state = pickerState,
                showModeToggle = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun PendingRow(
    entity: ScreenshotEntity,
    onKeep: () -> Unit,
    onArchive: () -> Unit,
    onLongPress: () -> Unit,
) {
    ScreenshotCard(
        entity = entity,
        swipeRight = SwipeAction(
            label = stringResource(R.string.action_keep),
            icon = Icons.Rounded.PushPin,
            color = MaterialTheme.colorScheme.secondaryContainer,
            onAction = onKeep,
        ),
        swipeLeft = SwipeAction(
            label = stringResource(R.string.action_archive),
            icon = Icons.Rounded.Archive,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            onAction = onArchive,
        ),
        onLongPress = onLongPress,
    )
}

@Composable
private fun ArchivedRow(
    entity: ScreenshotEntity,
    onKeep: () -> Unit,
    onRestore: () -> Unit,
    onLongPress: () -> Unit,
) {
    ScreenshotCard(
        entity = entity,
        swipeRight = SwipeAction(
            label = stringResource(R.string.action_keep),
            icon = Icons.Rounded.PushPin,
            color = MaterialTheme.colorScheme.secondaryContainer,
            onAction = onKeep,
        ),
        swipeLeft = SwipeAction(
            label = stringResource(R.string.action_restore),
            icon = Icons.Rounded.Unarchive,
            color = MaterialTheme.colorScheme.surfaceVariant,
            onAction = onRestore,
        ),
        onLongPress = onLongPress,
    )
}

/** Enter on first composition + exit when the row is being acted on. */
@Composable
private fun AnimatedRow(removing: Boolean, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = !removing
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(200)) + expandVertically(),
        exit = fadeOut(tween(EXIT_ANIM_MILLIS.toInt())) +
            shrinkVertically(tween(EXIT_ANIM_MILLIS.toInt())),
    ) {
        content()
    }
}

/** The pending-hit-zero celebration: springy scale-in plus clear copy. */
@Composable
private fun CelebrationState() {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.2f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 380f),
        label = "celebrationScale",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "💣",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier
                .padding(top = 32.dp)
                .scale(scale),
        )
        EmptyState(
            emoji = "",
            title = stringResource(R.string.celebrate_title),
            body = stringResource(R.string.celebrate_body),
        )
    }
}

@Composable
private fun CleanupBanner(settings: AppSettings) {
    val now = rememberNowTicker()
    val nextAt = remember(settings.cleanupTimeMinutes, now) {
        CleanupSchedule.nextOccurrenceAfter(now, settings.cleanupTimeMinutes)
    }
    val countdown = rememberCountdownText(nextAt)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Schedule, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(
                    R.string.banner_next_cleanup,
                    formatTimeOfDay(settings.cleanupTimeMinutes),
                    countdown,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun rememberRecencyGroups(items: List<ScreenshotEntity>): List<RecencyGroup> {
    val todayLabel = stringResource(R.string.group_today)
    val yesterdayLabel = stringResource(R.string.group_yesterday)
    return remember(items, todayLabel, yesterdayLabel) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val byDate = LinkedHashMap<LocalDate, MutableList<ScreenshotEntity>>()
        for (item in items) {
            val date = Instant.ofEpochMilli(item.createdAtMillis).atZone(zone).toLocalDate()
            byDate.getOrPut(date) { mutableListOf() }.add(item)
        }
        byDate.entries.map { (date, list) ->
            val label = when (date) {
                today -> todayLabel
                today.minusDays(1) -> yesterdayLabel
                else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            }
            RecencyGroup(label, list)
        }
    }
}

@Composable
private fun dateRangeLabel(range: Pair<Long, Long>?): String {
    if (range == null) return stringResource(R.string.filter_dates)
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
    val zone = ZoneId.systemDefault()
    val from = Instant.ofEpochMilli(range.first).atZone(zone).toLocalDate().format(formatter)
    val to = Instant.ofEpochMilli(range.second).atZone(zone).toLocalDate().format(formatter)
    return "$from – $to"
}

@Composable
private fun formatTimeOfDay(minutes: Int): String {
    return remember(minutes) {
        val time = LocalTime.of(minutes / 60, minutes % 60)
        time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }
}
