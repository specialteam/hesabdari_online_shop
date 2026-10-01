package ir.hesabdari.shop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabdari.shop.MainViewModel
import ir.hesabdari.shop.data.Deal
import ir.hesabdari.shop.domain.Calc
import ir.hesabdari.shop.report.ReportBuilders
import ir.hesabdari.shop.report.Tone
import ir.hesabdari.shop.ui.components.AppCard
import ir.hesabdari.shop.ui.components.ConfirmDialog
import ir.hesabdari.shop.ui.components.EmptyState
import ir.hesabdari.shop.ui.components.ExportAction
import ir.hesabdari.shop.ui.components.InfoChip
import ir.hesabdari.shop.ui.components.KeyValueRow
import ir.hesabdari.shop.ui.components.SectionTitle
import ir.hesabdari.shop.ui.components.ToneChip
import ir.hesabdari.shop.ui.components.stageTone
import ir.hesabdari.shop.ui.components.toneColor
import ir.hesabdari.shop.ui.components.toneOf
import ir.hesabdari.shop.ui.theme.semantic
import ir.hesabdari.shop.util.Fa
import ir.hesabdari.shop.util.Jalali
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DealDetailScreen(
    vm: MainViewModel,
    dealId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onInvoice: (List<Long>) -> Unit,
) {
    val deal by vm.observeDeal(dealId).collectAsStateWithLifecycle(initialValue = null)
    val settings by vm.settings.collectAsStateWithLifecycle()
    val unit = settings.unit.label
    var confirmDelete by remember { mutableStateOf(false) }
    val d = deal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جزئیات معامله", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت") } },
                actions = {
                    if (d != null) {
                        ExportAction { ReportBuilders.dealDoc(d, unit) }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "حذف") }
                    }
                },
            )
        },
        bottomBar = {
            if (d != null) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (d.sellPrice != null) {
                            OutlinedButton(onClick = { onInvoice(listOf(d.id)) }, modifier = Modifier.weight(1f).height(48.dp)) {
                                Icon(Icons.Rounded.Receipt, null)
                                Text("  فاکتور")
                            }
                        }
                        Button(onClick = { onEdit(d.id) }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Rounded.Edit, null)
                            Text("  ویرایش")
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (d == null) {
            Box(Modifier.padding(padding).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Rounded.Receipt, "معامله پیدا نشد", "ممکن است حذف شده باشد.")
            }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderCard(d, unit)
            NumbersCard(d, unit)
            if (d.isBrokerage) BrokerageFlow(d, unit) { vm.saveDeal(it) }
            if (d.sellerName.isNotBlank() || d.buyerName.isNotBlank()) {
                AppCard(Modifier.fillMaxWidth()) {
                    if (d.sellerName.isNotBlank()) KeyValueRow("فروشنده", d.sellerName)
                    if (d.buyerName.isNotBlank()) KeyValueRow("خریدار", d.buyerName)
                }
            }
            if (d.note.isNotBlank()) {
                SectionTitle("یادداشت")
                AppCard(Modifier.fillMaxWidth()) { Text(d.note, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }

    if (confirmDelete && d != null) {
        ConfirmDialog(
            title = "حذف معامله",
            text = "«${d.title}» برای همیشه حذف می‌شود. ادامه می‌دهید؟",
            confirmLabel = "حذف",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                vm.deleteDeals(listOf(d.id))
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderCard(d: Deal, unit: String) {
    val profit = Calc.profit(d)
    val percent = Calc.profitPercent(d)
    val tone = profit?.let { toneOf(it) } ?: Tone.MUTED
    AppCard(Modifier.fillMaxWidth()) {
        Text(d.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            Jalali.fromEpochDay(d.dateEpochDay).full() + "  •  " + Fa.quantity(d.quantity) + " " + d.unit,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (d.isBrokerage || d.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (d.isBrokerage) ToneChip("واسطه‌گری", Tone.NEUTRAL, icon = Icons.Rounded.SwapHoriz)
                d.tags.forEach { InfoChip("#$it") }
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = when (tone) {
                Tone.GAIN -> MaterialTheme.semantic.gainContainer
                Tone.LOSS -> MaterialTheme.semantic.lossContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                if (profit == null) {
                    Text("هنوز فروخته نشده", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("با ویرایش معامله و ثبت قیمت فروش، سود و زیان محاسبه می‌شود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(if (profit < 0) "زیان این معامله" else if (profit == 0L) "بدون سود و زیان" else "سود این معامله", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Fa.money(abs(profit), unit), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = toneColor(tone))
                    if (percent != null) {
                        Text(
                            Fa.percent(abs(percent)) + (if (profit < 0) " زیان" else " سود") + " نسبت به هزینه",
                            style = MaterialTheme.typography.bodyMedium,
                            color = toneColor(tone),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumbersCard(d: Deal, unit: String) {
    AppCard(Modifier.fillMaxWidth()) {
        KeyValueRow("قیمت خرید (هر واحد)", Fa.money(d.buyPrice, unit))
        KeyValueRow("قیمت فروش (هر واحد)", d.sellPrice?.let { Fa.money(it, unit) } ?: "—")
        Calc.unitMargin(d)?.let { KeyValueRow("سود هر واحد", Fa.signedMoney(it, unit), toneOf(it)) }
        KeyValueRow("جمع خرید", Fa.money(Calc.buyTotal(d), unit))
        KeyValueRow("جمع فروش", Calc.sellTotal(d)?.let { Fa.money(it, unit) } ?: "—")
        if (d.extraCost > 0) KeyValueRow("هزینه‌های جانبی", Fa.money(d.extraCost, unit))
        Calc.profit(d)?.let { KeyValueRow("سود / زیان خالص", Fa.signedMoney(it, unit), toneOf(it), bold = true) }
    }
}

@Composable
private fun BrokerageFlow(d: Deal, unit: String, onSave: (Deal) -> Unit) {
    val stage = Calc.stage(d)
    val sell = Calc.sellTotal(d) ?: 0
    val buy = Calc.buyTotal(d)
    SectionTitle("روند واسطه‌گری")
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToneChip(stage.label, stageTone(stage))
        }
        Text(stage.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
        StepRow(1, "دریافت پول از خریدار", Fa.money(sell, unit), d.buyerPaid) { onSave(d.copy(buyerPaid = it)) }
        StepRow(2, "تحویل کالا به خریدار", "مستقیم از فروشنده", d.delivered) { onSave(d.copy(delivered = it)) }
        StepRow(
            3, "برداشت سود و تسویه با فروشنده",
            "پرداخت ${Fa.money(buy, unit)} به فروشنده",
            d.sellerSettled,
        ) { onSave(d.copy(sellerSettled = it)) }
        Spacer(Modifier.height(6.dp))
        KeyValueRow("سهم فروشنده", Fa.money(buy, unit))
        KeyValueRow("سود ما از این معامله", Fa.money(sell - buy, unit), toneOf(sell - buy), bold = true)
        if (Calc.heldMoney(d) > 0) KeyValueRow("پول امانی نزد ما", Fa.money(Calc.heldMoney(d), unit))
    }
}

@Composable
private fun StepRow(index: Int, title: String, subtitle: String, done: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).background(if (done) MaterialTheme.semantic.gain else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.surface)
            } else {
                Text(Fa.digits("$index"), style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = done, onCheckedChange = onChange)
    }
}
