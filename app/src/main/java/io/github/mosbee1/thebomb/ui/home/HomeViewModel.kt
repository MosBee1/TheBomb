package io.github.mosbee1.thebomb.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.data.local.ScreenshotEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Home screen state: the filename query + optional date-range filter, and
 * the pending/archived lists derived through them. Filters are combined
 * with the Room flows via flatMapLatest so typing re-queries live with no
 * manual refresh, and clearing the filter snaps back to the full lists.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    data class Filters(
        val query: String = "",
        /** Inclusive epoch-millis pair (from, toInclusive); null = unbounded. */
        val range: Pair<Long, Long>? = null,
    ) {
        val isActive: Boolean get() = query.isNotBlank() || range != null
    }

    private val repo = (application as TheBombApp).container.screenshotRepository

    private val _filters = MutableStateFlow(Filters())
    val filters: StateFlow<Filters> = _filters.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun pendingFlow() = _filters.flatMapLatest { f ->
        if (!f.isActive) {
            repo.observePending()
        } else {
            repo.observePendingFiltered(
                f.query.trim(),
                f.range?.first,
                f.range?.second,
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun archivedFlow() = _filters.flatMapLatest { f ->
        if (!f.isActive) {
            repo.observeArchived()
        } else {
            repo.observeArchivedFiltered(
                f.query.trim(),
                f.range?.first,
                f.range?.second,
            )
        }
    }

    val pending: StateFlow<List<ScreenshotEntity>> = pendingFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archived: StateFlow<List<ScreenshotEntity>> = archivedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        _filters.update { it.copy(query = value) }
    }

    /** [toMillisExclusive] is converted to an inclusive end-of-day bound. */
    fun setDateRange(fromMillis: Long, toMillisExclusive: Long) {
        _filters.update { it.copy(range = fromMillis to (toMillisExclusive - 1)) }
    }

    fun clearDateRange() {
        _filters.update { it.copy(range = null) }
    }

    fun clearAllFilters() {
        _filters.value = Filters()
    }
}
