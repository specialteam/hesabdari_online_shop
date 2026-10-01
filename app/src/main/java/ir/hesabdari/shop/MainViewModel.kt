package ir.hesabdari.shop

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.hesabdari.shop.data.AppSettings
import ir.hesabdari.shop.data.Deal
import ir.hesabdari.shop.data.ImportMode
import ir.hesabdari.shop.domain.DealFilter
import ir.hesabdari.shop.domain.Filters
import ir.hesabdari.shop.domain.Period
import ir.hesabdari.shop.domain.Stat
import ir.hesabdari.shop.domain.Stats
import ir.hesabdari.shop.util.Jalali
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = app as App
    private val repo = graph.repository
    private val backup = graph.backup
    private val settingsStore = graph.settings

    val settings: StateFlow<AppSettings> = settingsStore.state

    /** Null until the first database emission, so screens can tell "loading" from "empty". */
    val allDeals: StateFlow<List<Deal>?> = repo.deals
        .map<List<Deal>, List<Deal>?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val dealsOrEmpty: StateFlow<List<Deal>> = allDeals.map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- deals list / report filter -------------------------------------------------------------
    val filter = MutableStateFlow(DealFilter())

    val filtered: StateFlow<List<Deal>> = combine(dealsOrEmpty, filter) { d, f -> Filters.apply(d, f) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredStats: StateFlow<Stats> = filtered.map { Stat.compute(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Stats())

    val allTags: StateFlow<List<String>> = dealsOrEmpty
        .map { list -> list.flatMap { it.tags }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Recently used buyer/seller names for quick pick in the form. */
    val partyNames: StateFlow<List<String>> = dealsOrEmpty
        .map { list ->
            list.flatMap { listOf(it.sellerName, it.buyerName) }.filter { it.isNotBlank() }
                .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val titles: StateFlow<List<String>> = dealsOrEmpty
        .map { list -> list.map { it.title }.distinct().take(40) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateFilter(transform: (DealFilter) -> DealFilter) = filter.update(transform)
    fun clearFilter() { filter.value = DealFilter(sort = filter.value.sort) }

    // ---- home period ----------------------------------------------------------------------------
    val homePeriod = MutableStateFlow(Period.ALL)

    val homeDeals: StateFlow<List<Deal>> = combine(dealsOrEmpty, homePeriod) { d, p ->
        Filters.inRange(d, p, Jalali.todayEpochDay())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val homeStats: StateFlow<Stats> = homeDeals.map { Stat.compute(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Stats())

    // ---- actions --------------------------------------------------------------------------------
    fun observeDeal(id: Long) = repo.observe(id)

    suspend fun getDeal(id: Long): Deal? = repo.get(id)

    suspend fun getDeals(ids: List<Long>): List<Deal> = repo.getAll(ids)

    fun saveDeal(deal: Deal, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.save(deal)) }
    }

    fun deleteDeals(ids: List<Long>) {
        viewModelScope.launch { repo.delete(ids) }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsStore.update(transform)

    fun nextInvoiceNumber(): Int = settingsStore.nextInvoiceNumber()

    suspend fun exportBackup(uri: Uri): Int = backup.export(uri, repo.getAll(allDeals.value.orEmpty().map { it.id }))

    suspend fun importBackup(uri: Uri, mode: ImportMode): Int = backup.import(uri, mode)
}
