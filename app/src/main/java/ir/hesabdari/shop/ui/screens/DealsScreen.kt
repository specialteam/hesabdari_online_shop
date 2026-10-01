package ir.hesabdari.shop.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabdari.shop.MainViewModel
import ir.hesabdari.shop.domain.KindFilter
import ir.hesabdari.shop.domain.ResultFilter
import ir.hesabdari.shop.report.ReportBuilders
import ir.hesabdari.shop.ui.components.AppCard
import ir.hesabdari.shop.ui.components.ConfirmDialog
import ir.hesabdari.shop.ui.components.DealCard
import ir.hesabdari.shop.ui.components.EmptyState
import ir.hesabdari.shop.ui.components.ExportAction
import ir.hesabdari.shop.ui.components.toneColor
import ir.hesabdari.shop.ui.components.toneOf
import ir.hesabdari.shop.util.Fa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealsScreen(
    vm: MainViewModel,
    onAdd: () -> Unit,
    onOpenDeal: (Long) -> Unit,
    onInvoice: (List<Long>) -> Unit,
) {
    val deals by vm.filtered.collectAsStateWithLifecycle()
    val stats by vm.filteredStats.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val tags by vm.allTags.collectAsStateWithLifecycle()
    val all by vm.allDeals.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val unit = settings.unit.label

    var showFilter by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Long>() }
    val selecting = selected.isNotEmpty()

    Scaffold(
        topBar = {
            if (selecting) {
                CenterAlignedTopAppBar(
                    title = { Text(Fa.digits("${selected.size}") + " مورد انتخاب شد") },
                    navigationIcon = { IconButton(onClick = { selected.clear() }) { Icon(Icons.Rounded.Close, "لغو انتخاب") } },
                    actions = {
                        IconButton(onClick = { selected.clear(); selected.addAll(deals.map { it.id }) }) { Icon(Icons.Rounded.SelectAll, "انتخاب همه") }
                        IconButton(onClick = { onInvoice(selected.toList()) }) { Icon(Icons.Rounded.Receipt, "صدور فاکتور") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "حذف") }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text("معاملات", fontWeight = FontWeight.Bold) },
                    actions = { ExportAction { ReportBuilders.dealsDoc(deals, filter, unit) } },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        },
        floatingActionButton = {
            if (!selecting) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text("ثبت معامله") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = filter.query,
                        onValueChange = { q -> vm.updateFilter { it.copy(query = q) } },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("جستجوی کالا، خریدار، فروشنده، برچسب…") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = {
                            if (filter.query.isNotEmpty()) {
                                IconButton(onClick = { vm.updateFilter { it.copy(query = "") } }) { Icon(Icons.Rounded.Close, "پاک کردن") }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showFilter = true }) {
                        BadgedBox(badge = { if (filter.activeCount > 0) Badge { Text(Fa.digits("${filter.activeCount}")) } }) {
                            Icon(Icons.Rounded.FilterList, "فیلتر")
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filter.isDefault || filter.activeCount == 0,
                        onClick = { vm.clearFilter() },
                        label = { Text("همه") },
                    )
                    KindFilter.entries.filter { it != KindFilter.ALL }.forEach { k ->
                        FilterChip(
                            selected = filter.kind == k,
                            onClick = { vm.updateFilter { it.copy(kind = if (it.kind == k) KindFilter.ALL else k) } },
                            label = { Text(k.label) },
                        )
                    }
                    listOf(ResultFilter.PROFIT, ResultFilter.LOSS, ResultFilter.OPEN).forEach { r ->
                        FilterChip(
                            selected = filter.result == r,
                            onClick = { vm.updateFilter { it.copy(result = if (it.result == r) ResultFilter.ALL else r) } },
                            label = { Text(r.label) },
                        )
                    }
                }
            }
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Fa.digits("${stats.count}") + " معامله",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            (if (stats.netProfit < 0) "زیان: " else "سود: ") + Fa.money(kotlin.math.abs(stats.netProfit), unit) +
                                "  (" + Fa.percent(kotlin.math.abs(stats.profitPercent)) + ")",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = toneColor(toneOf(stats.netProfit)),
                        )
                    }
                }
            }
            if (deals.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (all != null && all!!.isEmpty()) {
                            EmptyState(Icons.Rounded.Receipt, "معامله‌ای ثبت نشده", "با دکمهٔ «ثبت معامله» اولین معامله را اضافه کنید.")
                        } else {
                            EmptyState(
                                Icons.Rounded.SearchOff, "نتیجه‌ای پیدا نشد", "فیلترها یا عبارت جستجو را تغییر دهید.",
                                action = { TextButton(onClick = { vm.clearFilter(); vm.updateFilter { it.copy(query = "") } }) { Text("پاک کردن فیلترها") } },
                            )
                        }
                    }
                }
            }
            items(deals, key = { it.id }) { d ->
                DealCard(
                    d,
                    selected = d.id in selected,
                    selectionMode = selecting,
                    onClick = {
                        if (selecting) {
                            if (d.id in selected) selected.remove(d.id) else selected.add(d.id)
                        } else {
                            onOpenDeal(d.id)
                        }
                    },
                    onLongClick = { if (d.id !in selected) selected.add(d.id) },
                )
            }
        }
    }

    if (showFilter) {
        FilterSheet(
            filter = filter,
            allTags = tags,
            onChange = { t -> vm.updateFilter(t) },
            onClear = { vm.clearFilter() },
            onDismiss = { showFilter = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "حذف معاملات",
            text = "${Fa.digits("${selected.size}")} معامله برای همیشه حذف می‌شود. ادامه می‌دهید؟",
            confirmLabel = "حذف",
            destructive = true,
            onConfirm = {
                vm.deleteDeals(selected.toList())
                selected.clear()
                confirmDelete = false
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
