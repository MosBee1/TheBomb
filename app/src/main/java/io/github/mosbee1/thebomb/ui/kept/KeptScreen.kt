package io.github.mosbee1.thebomb.ui.kept

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.data.local.ScreenshotEntity
import io.github.mosbee1.thebomb.ui.MainViewModel
import io.github.mosbee1.thebomb.ui.common.EmptyState
import io.github.mosbee1.thebomb.ui.common.SectionHeader
import io.github.mosbee1.thebomb.ui.common.ScreenshotCard
import io.github.mosbee1.thebomb.ui.common.Triage
import io.github.mosbee1.thebomb.ui.common.TriageSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeptViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as TheBombApp).container.screenshotRepository

    val kept = repo.observeKept()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

private const val EXIT_ANIM_MILLIS = 300L

/**
 * The Defused tab: screenshots explicitly pinned, permanently exempt from
 * every auto-deletion path. Long-press opens the action sheet; the Archive
 * slot is relabeled "Rearm" and returns the row to the triage list.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun KeptScreen(mainViewModel: MainViewModel) {
    val context = LocalContext.current
    val keptViewModel: KeptViewModel = viewModel()
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val kept by keptViewModel.kept.collectAsStateWithLifecycle()

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "kept_header") {
            SectionHeader(
                stringResource(R.string.tab_kept) +
                    if (kept.isNotEmpty()) " (${kept.size})" else "",
            )
        }
        if (kept.isEmpty()) {
            item(key = "kept_empty") {
                EmptyState(
                    emoji = "📌",
                    title = stringResource(R.string.kept_empty_title),
                    body = stringResource(R.string.kept_empty_body),
                )
            }
        } else {
            items(kept, key = { it.uri }) { entity ->
                Box(Modifier.animateItemPlacement()) {
                    val state = remember { MutableTransitionState(false) }
                    state.targetState = entity.uri !in removing
                    AnimatedVisibility(
                        visibleState = state,
                        enter = fadeIn(tween(200)) + expandVertically(),
                        exit = fadeOut(tween(EXIT_ANIM_MILLIS.toInt())) +
                            shrinkVertically(tween(EXIT_ANIM_MILLIS.toInt())),
                    ) {
                        ScreenshotCard(
                            entity = entity,
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
            onKeep = { sheetEntity = null },
            onArchive = {
                // Relabeled "Rearm" below: markPending returns the row to the
                // pending triage list with an exit animation.
                animateThen(entity.uri) { Triage.unarchive(context, entity.uri) }
                sheetEntity = null
            },
            archiveLabelOverride = stringResource(R.string.action_unkeep),
            onRestore = null,
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
}
