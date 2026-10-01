package ir.hesabdari.shop.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabdari.shop.MainViewModel
import ir.hesabdari.shop.data.Deal
import ir.hesabdari.shop.domain.Calc
import ir.hesabdari.shop.report.Tone
import ir.hesabdari.shop.ui.components.AppCard
import ir.hesabdari.shop.ui.components.DateField
import ir.hesabdari.shop.ui.components.DecimalField
import ir.hesabdari.shop.ui.components.KeyValueRow
import ir.hesabdari.shop.ui.components.LocalMoneyUnit
import ir.hesabdari.shop.ui.components.MoneyField
import ir.hesabdari.shop.ui.components.SectionTitle
import ir.hesabdari.shop.ui.components.toneOf
import ir.hesabdari.shop.util.Fa
import ir.hesabdari.shop.util.Jalali
import kotlin.math.abs

private val unitSuggestions = listOf("عدد", "کیلو", "گرم", "تن", "متر", "جفت", "بسته", "کارتن", "دستگاه", "لیتر")

private val stringListSaver = listSaver<List<String>, String>(save = { it }, restore = { it })

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DealFormScreen(vm: MainViewModel, dealId: Long, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val unitLabel = LocalMoneyUnit.current
    val titles by vm.titles.collectAsStateWithLifecycle()
    val parties by vm.partyNames.collectAsStateWithLifecycle()
    val knownTags by vm.allTags.collectAsStateWithLifecycle()

    var brokerage by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var qty by rememberSaveable { mutableStateOf("1") }
    var unit by rememberSaveable { mutableStateOf("عدد") }
    var buy by rememberSaveable { mutableStateOf("") }
    var sell by rememberSaveable { mutableStateOf("") }
    var extra by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableLongStateOf(Jalali.todayEpochDay()) }
    var seller by rememberSaveable { mutableStateOf("") }
    var buyer by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable(stateSaver = stringListSaver) { mutableStateOf(emptyList<String>()) }
    var tagInput by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var buyerPaid by rememberSaveable { mutableStateOf(false) }
    var delivered by rememberSaveable { mutableStateOf(false) }
    var settled by rememberSaveable { mutableStateOf(false) }
    var createdAt by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var loaded by rememberSaveable { mutableStateOf(dealId == 0L) }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(dealId) {
        if (!loaded) {
            vm.getDeal(dealId)?.let { d ->
                brokerage = d.isBrokerage
                title = d.title
                qty = Fa.latinDigits(Fa.quantity(d.quantity)).replace('٫', '.')
                unit = d.unit
                buy = d.buyPrice.toString()
                sell = d.sellPrice?.toString().orEmpty()
                extra = if (d.extraCost > 0) d.extraCost.toString() else ""
                date = d.dateEpochDay
                seller = d.sellerName
                buyer = d.buyerName
                tags = d.tags
                note = d.note
                buyerPaid = d.buyerPaid
                delivered = d.delivered
                settled = d.sellerSettled
                createdAt = d.createdAt
            }
            loaded = true
        }
    }

    val qtyValue = Fa.parseDecimal(qty) ?: 0.0
    val titleError = showErrors && title.isBlank()
    val qtyError = showErrors && qtyValue <= 0.0
    val buyError = showErrors && (buy.toLongOrNull() ?: 0L) <= 0L
    val sellError = showErrors && brokerage && (sell.toLongOrNull() ?: 0L) <= 0L

    fun build(): Deal = Deal(
        id = dealId,
        title = title.trim(),
        quantity = qtyValue,
        unit = unit.trim().ifBlank { "عدد" },
        buyPrice = buy.toLongOrNull() ?: 0L,
        sellPrice = sell.toLongOrNull()?.takeIf { it > 0 },
        extraCost = extra.toLongOrNull() ?: 0L,
        dateEpochDay = date,
        isBrokerage = brokerage,
        sellerName = seller.trim(),
        buyerName = buyer.trim(),
        tags = tags,
        note = note.trim(),
        buyerPaid = brokerage && buyerPaid,
        delivered = brokerage && delivered,
        sellerSettled = brokerage && settled,
        createdAt = createdAt,
    )

    fun addTag() {
        val t = tagInput.trim().trimStart('#').trim()
        if (t.isNotEmpty() && t !in tags) tags = tags + t
        tagInput = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (dealId == 0L) "ثبت معامله جدید" else "ویرایش معامله", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت") } },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = {
                        showErrors = true
                        val valid = title.isNotBlank() && qtyValue > 0 && (buy.toLongOrNull() ?: 0L) > 0 &&
                            (!brokerage || (sell.toLongOrNull() ?: 0L) > 0)
                        if (valid) vm.saveDeal(build()) { id -> onSaved(id) }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding().height(52.dp),
                ) {
                    Icon(Icons.Rounded.Check, null)
                    Text("  ذخیرهٔ معامله", style = MaterialTheme.typography.titleSmall)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !brokerage,
                    onClick = { brokerage = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("خرید و فروش") }
                SegmentedButton(
                    selected = brokerage,
                    onClick = { brokerage = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("واسطه‌گری") }
            }
            if (brokerage) {
                Text(
                    "کالا مستقیم از فروشنده به خریدار می‌رسد. ما فقط پول را می‌گیریم و پس از تحویل، سود خود را برمی‌داریم و باقی را به فروشنده می‌دهیم.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("نام کالا") },
                isError = titleError,
                supportingText = { if (titleError) Text("نام کالا را وارد کنید") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            val titleHints = titles.filter { it != title && (title.isBlank() || Fa.normalizeText(it).contains(Fa.normalizeText(title))) }.take(6)
            if (titleHints.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    titleHints.forEach { h -> AssistChip(onClick = { title = h }, label = { Text(h) }) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DecimalField(qty, { qty = it }, "تعداد / مقدار", Modifier.weight(1f), isError = qtyError)
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it.take(12) },
                    label = { Text("واحد") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                )
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                unitSuggestions.forEach { u -> FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) }) }
            }

            MoneyField(
                buy, { buy = it }, "قیمت خرید (هر واحد)", Modifier.fillMaxWidth(),
                isError = buyError, supportingText = if (buyError) "قیمت خرید را وارد کنید" else null,
            )
            MoneyField(
                sell, { sell = it }, if (brokerage) "قیمت فروش (هر واحد)" else "قیمت فروش (هر واحد) — اختیاری",
                Modifier.fillMaxWidth(),
                isError = sellError,
                supportingText = when {
                    sellError -> "در واسطه‌گری قیمت فروش لازم است"
                    !brokerage && sell.isBlank() -> "خالی بگذارید تا معامله «فروش نرفته» ثبت شود"
                    else -> null
                },
            )
            MoneyField(extra, { extra = it }, "هزینه‌های جانبی (حمل، کارمزد…) — اختیاری", Modifier.fillMaxWidth())

            PreviewCard(build(), unitLabel)

            DateField(date, { date = it }, "تاریخ معامله")

            SectionTitle("طرف‌های معامله")
            OutlinedTextField(
                value = seller, onValueChange = { seller = it }, label = { Text("فروشنده (تأمین‌کننده)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
            )
            PartyHints(parties, seller) { seller = it }
            OutlinedTextField(
                value = buyer, onValueChange = { buyer = it }, label = { Text("خریدار (مشتری)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
            )
            PartyHints(parties, buyer) { buyer = it }

            SectionTitle("برچسب‌ها")
            if (tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { t ->
                        InputChip(
                            selected = true,
                            onClick = { tags = tags - t },
                            label = { Text("#$t") },
                            trailingIcon = { Icon(Icons.Rounded.Close, "حذف", Modifier.padding(0.dp)) },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("برچسب جدید") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Text),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { addTag() }),
                )
                IconButton(onClick = { addTag() }) { Icon(Icons.Rounded.Add, "افزودن برچسب") }
            }
            val tagHints = knownTags.filter { it !in tags }.take(8)
            if (tagHints.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tagHints.forEach { t -> AssistChip(onClick = { tags = tags + t }, label = { Text("#$t") }) }
                }
            }

            if (brokerage) {
                SectionTitle("وضعیت واسطه‌گری")
                AppCard(Modifier.fillMaxWidth()) {
                    SwitchRow("پول از خریدار دریافت شد", buyerPaid) { buyerPaid = it }
                    SwitchRow("کالا به دست خریدار رسید", delivered) { delivered = it }
                    SwitchRow("سود برداشته و با فروشنده تسویه شد", settled) { settled = it }
                }
            }

            OutlinedTextField(
                value = note, onValueChange = { note = it }, label = { Text("یادداشت") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5, shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PartyHints(parties: List<String>, current: String, onPick: (String) -> Unit) {
    val hints = parties.filter { it != current && (current.isBlank() || Fa.normalizeText(it).contains(Fa.normalizeText(current))) }.take(6)
    if (hints.isEmpty()) return
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        hints.forEach { h -> AssistChip(onClick = { onPick(h) }, label = { Text(h) }) }
    }
}

/** Live profit / loss summary while the form is being filled. */
@Composable
private fun PreviewCard(d: Deal, unit: String) {
    val buyTotal = Calc.buyTotal(d)
    val sellTotal = Calc.sellTotal(d)
    val profit = Calc.profit(d)
    AppCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
        Text("پیش‌نمایش حساب", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.height(4.dp))
        KeyValueRow("جمع خرید", Fa.money(buyTotal, unit))
        KeyValueRow("جمع فروش", sellTotal?.let { Fa.money(it, unit) } ?: "—")
        if (d.extraCost > 0) KeyValueRow("هزینه‌های جانبی", Fa.money(d.extraCost, unit))
        if (profit != null) {
            val tone = toneOf(profit)
            KeyValueRow(if (profit < 0) "زیان" else "سود", Fa.money(abs(profit), unit), tone, bold = true)
            Calc.profitPercent(d)?.let { KeyValueRow(if (profit < 0) "درصد زیان" else "درصد سود", Fa.percent(abs(it)), tone, bold = true) }
            if (d.isBrokerage) KeyValueRow("سهم فروشنده (پس از کسر سود ما)", Fa.money(buyTotal, unit), Tone.NEUTRAL)
        } else {
            Text("با ثبت قیمت فروش، سود و زیان نمایش داده می‌شود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
    }
}
