package ir.hesabdari.shop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabdari.shop.MainViewModel
import ir.hesabdari.shop.domain.Period
import ir.hesabdari.shop.domain.Stat
import ir.hesabdari.shop.report.ReportBuilders
import ir.hesabdari.shop.ui.components.AppCard
import ir.hesabdari.shop.ui.components.EmptyState
import ir.hesabdari.shop.ui.components.ExportAction
import ir.hesabdari.shop.ui.components.InfoChip
import ir.hesabdari.shop.ui.components.KeyValueRow
import ir.hesabdari.shop.ui.components.SectionTitle
import ir.hesabdari.shop.ui.theme.semantic
import ir.hesabdari.shop.util.Fa
import ir.hesabdari.shop.util.Jalali
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(vm: MainViewModel) {
    val deals by vm.filtered.collectAsStateWithLifecycle()
    val stats by vm.filteredStats.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val tags by vm.allTags.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val unit = settings.unit.label
    var showFilter by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("بیلان و گزارش", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showFilter = true }) {
                        BadgedBox(badge = { if (filter.activeCount > 0) Badge { Text(Fa.digits("${filter.activeCount}")) } }) {
                            Icon(Icons.Rounded.FilterList, "فیلتر")
                        }
                    }
                    ExportAction { ReportBuilders.balanceDoc(deals, filter, unit) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (deals.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    Icons.Rounded.BarChart, "دادهای برای گزارش نیست",
                    if (filter.isDefault) "پس از ثبت معامله، بیلان اینجا نمایش داده می‌شود." else "با فیلتر فعلی معامله‌ای وجود ندارد.",
                    action = { if (!filter.isDefault) TextButton(onClick = { vm.clearFilter() }) { Text("پاک کردن فیلترها") } },
                )
            }
        } else {
            Column(
                Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Filter summary
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "فیلتر: " + ReportBuilders.describeFilter(filter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (!filter.isDefault) TextButton(onClick = { vm.clearFilter() }) { Text("پاک کردن") }
                    }
                }
                HeroCard(stats, Period.ALL, unit, label = Fa.digits("${stats.count}") + " معامله")

                SectionTitle("بیلان کلی")
                AppCard(Modifier.fillMaxWidth()) {
                    ReportBuilders.balanceRows(stats, unit).forEach { KeyValueRow(it.label, it.value, tone = it.tone, bold = it.bold) }
                }

                ResultCard(stats)
                if (stats.brokerageCount > 0) BrokerageCard(stats, unit)

                val groups = Stat.breakdown(deals)
                if (groups.size > 1) {
                    SectionTitle("تفکیک نوع و برچسب")
                    AppCard(Modifier.fillMaxWidth()) {
                        groups.forEachIndexed { i, (name, s) ->
                            if (i > 0) Box(Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant))
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        Fa.digits("${s.count}") + " معامله • فروش " + Fa.money(s.totalSell, unit),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        Fa.signedMoney(s.netProfit, unit),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (s.netProfit < 0) MaterialTheme.semantic.loss else MaterialTheme.semantic.gain,
                                    )
                                    InfoChip(Fa.percent(s.profitPercent))
                                }
                            }
                        }
                    }
                }

                val months = Stat.monthlyProfit(deals) { e -> Jalali.fromEpochDay(e).let { it.year to it.month } }
                if (months.isNotEmpty()) {
                    SectionTitle("سود و زیان ماهانه")
                    AppCard(Modifier.fillMaxWidth()) {
                        MonthlyChart(months.takeLast(12))
                        Spacer(Modifier.height(8.dp))
                        months.reversed().forEach { (y, m, p) ->
                            KeyValueRow(
                                Jalali.monthNames[m - 1] + " " + Fa.digits("$y"),
                                Fa.signedMoney(p, unit),
                                tone = if (p < 0) ir.hesabdari.shop.report.Tone.LOSS else if (p > 0) ir.hesabdari.shop.report.Tone.GAIN else ir.hesabdari.shop.report.Tone.NEUTRAL,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showFilter) {
        FilterSheet(filter, tags, { t -> vm.updateFilter(t) }, { vm.clearFilter() }, { showFilter = false })
    }
}

/** Bars of profit (up) and loss (down) per Jalali month. */
@Composable
private fun MonthlyChart(months: List<Triple<Int, Int, Long>>) {
    val maxPos = months.maxOf { it.third }.coerceAtLeast(0L)
    val minNeg = months.minOf { it.third }.coerceAtMost(0L)
    val total = (maxPos + abs(minNeg)).coerceAtLeast(1L).toFloat()
    val chartH = 150f
    val posH = chartH * (maxPos / total)
    val negH = chartH * (abs(minNeg) / total)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(posH.dp.coerceAtLeast(4.dp)), verticalAlignment = Alignment.Bottom) {
            months.forEach { (_, _, p) ->
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp), contentAlignment = Alignment.BottomCenter) {
                    if (p > 0) {
                        Box(
                            Modifier.fillMaxWidth().height((posH * (p.toFloat() / maxPos.toFloat())).dp.coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(MaterialTheme.semantic.gain),
                        )
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
        if (negH > 0f) {
            Row(Modifier.fillMaxWidth().height(negH.dp), verticalAlignment = Alignment.Top) {
                months.forEach { (_, _, p) ->
                    Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp), contentAlignment = Alignment.TopCenter) {
                        if (p < 0) {
                            Box(
                                Modifier.fillMaxWidth().height((negH * (abs(p) / abs(minNeg).toFloat())).dp.coerceAtLeast(3.dp))
                                    .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                                    .background(MaterialTheme.semantic.loss),
                            )
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            months.forEach { (_, m, _) ->
                Text(
                    Jalali.monthNames[m - 1].take(4),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
