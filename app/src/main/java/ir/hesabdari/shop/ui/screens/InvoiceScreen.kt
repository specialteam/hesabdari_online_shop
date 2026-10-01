package ir.hesabdari.shop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabdari.shop.MainViewModel
import ir.hesabdari.shop.data.Deal
import ir.hesabdari.shop.report.InvoiceData
import ir.hesabdari.shop.report.ReportBuilders
import ir.hesabdari.shop.ui.components.AppCard
import ir.hesabdari.shop.ui.components.DateField
import ir.hesabdari.shop.ui.components.DocView
import ir.hesabdari.shop.ui.components.ExportAction
import ir.hesabdari.shop.ui.components.MoneyField
import ir.hesabdari.shop.util.Fa
import ir.hesabdari.shop.util.Jalali

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(vm: MainViewModel, ids: List<Long>, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val deals by produceState(initialValue = emptyList<Deal>(), ids) { value = vm.getDeals(ids).sortedBy { it.dateEpochDay } }

    var buyer by rememberSaveable { mutableStateOf<String?>(null) }
    var date by rememberSaveable { mutableLongStateOf(Jalali.todayEpochDay()) }
    var discount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf<String?>(null) }
    var committedNumber by rememberSaveable { mutableStateOf<Int?>(null) }

    val items = ReportBuilders.invoiceItems(deals)
    val skipped = deals.size - items.size
    val buyerValue = buyer ?: deals.map { it.buyerName }.filter { it.isNotBlank() }.groupingBy { it }.eachCount()
        .maxByOrNull { it.value }?.key.orEmpty()
    val noteValue = note ?: settings.invoiceNote

    fun build(number: Int) = ReportBuilders.invoiceDoc(
        InvoiceData(
            number = number,
            dateEpochDay = date,
            buyer = buyerValue.trim(),
            items = items,
            discount = discount.toLongOrNull() ?: 0L,
            note = noteValue.trim(),
        ),
        settings,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("صدور فاکتور غیررسمی", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت") } },
                actions = {
                    // The invoice number is only consumed when the invoice is actually shared / printed.
                    ExportAction {
                        val n = committedNumber ?: vm.nextInvoiceNumber().also { committedNumber = it }
                        build(n)
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = buyerValue, onValueChange = { buyer = it }, label = { Text("نام خریدار") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                    )
                    DateField(date, { date = it }, "تاریخ فاکتور")
                    MoneyField(discount, { discount = it }, "تخفیف (اختیاری)", Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = noteValue, onValueChange = { note = it }, label = { Text("توضیحات پایین فاکتور") },
                        minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                    )
                }
            }
            if (skipped > 0) {
                Text(
                    Fa.digits("$skipped") + " معامله چون قیمت فروش ندارد در فاکتور نیامده است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text("پیش‌نمایش (برای ارسال یا چاپ از دکمهٔ بالا استفاده کنید)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppCard(Modifier.fillMaxWidth()) {
                DocView(build(committedNumber ?: (settings.invoiceCounter + 1)))
            }
        }
    }
}
