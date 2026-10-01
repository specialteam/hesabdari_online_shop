package ir.hesabdari.shop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabdari.shop.MainViewModel
import ir.hesabdari.shop.domain.Period
import ir.hesabdari.shop.domain.Stats
import ir.hesabdari.shop.report.ReportBuilders
import ir.hesabdari.shop.report.Tone
import ir.hesabdari.shop.ui.components.AppCard
import ir.hesabdari.shop.ui.components.DealCard
import ir.hesabdari.shop.ui.components.EmptyState
import ir.hesabdari.shop.ui.components.ExportAction
import ir.hesabdari.shop.ui.components.KeyValueRow
import ir.hesabdari.shop.ui.components.LocalMoneyUnit
import ir.hesabdari.shop.ui.components.SectionTitle
import ir.hesabdari.shop.ui.components.StatTile
import ir.hesabdari.shop.ui.components.toneOf
import ir.hesabdari.shop.ui.theme.semantic
import ir.hesabdari.shop.util.Fa
import ir.hesabdari.shop.util.Jalali

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onAdd: () -> Unit,
    onOpenDeal: (Long) -> Unit,
    onSeeAll: () -> Unit,
    onOpenReport: () -> Unit,
) {
    val stats by vm.homeStats.collectAsStateWithLifecycle()
    val deals by vm.homeDeals.collectAsStateWithLifecycle()
    val all by vm.allDeals.collectAsStateWithLifecycle()
    val period by vm.homePeriod.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val unit = settings.unit.label

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(settings.shopName.ifBlank { "حسابدار شاپ" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(Jalali.today().full(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = { ExportAction { ReportBuilders.homeDoc(deals, period, unit) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("ثبت معامله") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (all != null && all!!.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.Receipt,
                    title = "هنوز معامله‌ای ثبت نشده",
                    text = "قیمت خرید و فروش کالاها را ثبت کنید تا سود، زیان و بیلان را خودکار ببینید.",
                    action = {
                        androidx.compose.material3.Button(onClick = onAdd) { Text("ثبت اولین معامله") }
                    },
                )
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Period.entries.forEach { p ->
                        FilterChip(
                            selected = p == period,
                            onClick = { vm.homePeriod.value = p },
                            label = { Text(p.label) },
                        )
                    }
                }
            }
            item { HeroCard(stats, period, unit) }
            item {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("مبلغ کل خرید", Fa.money(stats.totalBuy, unit), Modifier.weight(1f).fillMaxHeight())
                    StatTile("مبلغ کل فروش", Fa.money(stats.totalSell, unit), Modifier.weight(1f).fillMaxHeight())
                }
            }
            item {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        "اضافه / کسری قیمت", Fa.signedMoney(stats.grossMargin, unit), Modifier.weight(1f).fillMaxHeight(),
                        tone = toneOf(stats.grossMargin), hint = "فروش − خرید، پیش از هزینه‌ها",
                    )
                    StatTile(
                        "موجودی فروش‌نرفته", Fa.money(stats.stockValue, unit), Modifier.weight(1f).fillMaxHeight(),
                        hint = Fa.digits("${stats.openCount}") + " معامله باز",
                    )
                }
            }
            item { ResultCard(stats) }
            if (stats.brokerageCount > 0) item { BrokerageCard(stats, unit) }
            item {
                SectionTitle("آخرین معاملات") {
                    TextButton(onClick = onSeeAll) { Text("مشاهده همه") }
                }
            }
            items(deals.take(5), key = { it.id }) { d ->
                DealCard(d, onClick = { onOpenDeal(d.id) })
            }
            if (deals.isEmpty()) {
                item {
                    Text(
                        "در این بازه معامله‌ای ثبت نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            item {
                TextButton(onClick = onOpenReport, modifier = Modifier.fillMaxWidth()) { Text("مشاهدهٔ بیلان کامل و نمودارها") }
            }
        }
    }
}

@Composable
internal fun HeroCard(stats: Stats, period: Period, unit: String, label: String? = null) {
    val loss = stats.netProfit < 0
    val gradient = if (loss) {
        listOf(Color(0xFF8E2B2B), Color(0xFFC0504D))
    } else {
        listOf(Color(0xFF064E55), Color(0xFF0A8A93))
    }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(gradient)).padding(20.dp),
    ) {
        Column {
            Text(
                (if (loss) "زیان خالص" else "سود خالص") + " • " + (label ?: period.label),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                Fa.money(kotlin.math.abs(stats.netProfit), unit),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        Fa.percent(stats.profitPercent) + (if (loss) " زیان" else " سود") + " از سرمایهٔ فروخته‌شده",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row {
                HeroMini("سود محقق‌شده", Fa.money(stats.realizedProfit, unit), Modifier.weight(1f))
                HeroMini("سود در انتظار", Fa.money(stats.pendingProfit, unit), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroMini(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

/** Share of profitable / break-even / losing deals as a stacked bar. */
@Composable
fun ResultCard(stats: Stats, modifier: Modifier = Modifier) {
    AppCard(modifier.fillMaxWidth()) {
        Text("نتیجهٔ معاملات", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        if (stats.soldCount == 0) {
            Text("هنوز معامله‌ای با قیمت فروش ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@AppCard
        }
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))) {
            if (stats.profitCount > 0) Box(Modifier.weight(stats.profitCount.toFloat()).height(14.dp).background(MaterialTheme.semantic.gain))
            if (stats.evenCount > 0) Box(Modifier.weight(stats.evenCount.toFloat()).height(14.dp).background(MaterialTheme.colorScheme.outline))
            if (stats.lossCount > 0) Box(Modifier.weight(stats.lossCount.toFloat()).height(14.dp).background(MaterialTheme.semantic.loss))
        }
        Spacer(Modifier.height(12.dp))
        Row {
            Legend("سودده", stats.profitCount, stats.winRate, MaterialTheme.semantic.gain, Modifier.weight(1f))
            Legend("بدون تغییر", stats.evenCount, stats.evenRate, MaterialTheme.colorScheme.outline, Modifier.weight(1f))
            Legend("زیان‌ده", stats.lossCount, stats.lossRate, MaterialTheme.semantic.loss, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "احتمال ضرر بر اساس سابقه: ${Fa.percent(stats.lossRate)} از معاملات فروخته‌شده با زیان بسته شده‌اند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Legend(label: String, count: Int, percent: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(10.dp).height(10.dp).clip(RoundedCornerShape(50)).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(Fa.percent(percent), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(Fa.digits("$count") + " معامله", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BrokerageCard(stats: Stats, unit: String, modifier: Modifier = Modifier) {
    AppCard(modifier.fillMaxWidth()) {
        Text("وضعیت واسطه‌گری", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "پول از خریدار می‌آید، کالا مستقیم به او می‌رسد؛ پس از تحویل سود ما برداشته و باقی به فروشنده پرداخت می‌شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        KeyValueRow("پول امانی نزد ما", Fa.money(stats.heldMoney, unit), bold = true)
        KeyValueRow("قابل پرداخت به فروشندگان", Fa.money(stats.owedToSellers, unit))
        KeyValueRow("سود قابل برداشت (تحویل‌شده)", Fa.money(stats.marginToTake, unit), tone = Tone.GAIN)
        KeyValueRow("طلب از خریداران", Fa.money(stats.receivable, unit), tone = if (stats.receivable > 0) Tone.LOSS else Tone.NEUTRAL)
    }
}
