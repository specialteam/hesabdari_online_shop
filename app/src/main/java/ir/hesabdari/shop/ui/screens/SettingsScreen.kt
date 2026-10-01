package ir.hesabdari.shop.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabdari.shop.BuildConfig
import ir.hesabdari.shop.MainViewModel
import ir.hesabdari.shop.data.ImportMode
import ir.hesabdari.shop.data.InvalidBackupException
import ir.hesabdari.shop.data.MoneyUnit
import ir.hesabdari.shop.data.ThemeMode
import ir.hesabdari.shop.ui.components.AppCard
import ir.hesabdari.shop.ui.components.SectionTitle
import ir.hesabdari.shop.util.Fa
import ir.hesabdari.shop.util.Jalali
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val n = vm.exportBackup(uri)
                    toast("پشتیبان‌گیری انجام شد (${Fa.digits("$n")} معامله)")
                } catch (e: Exception) {
                    toast("پشتیبان‌گیری ناموفق بود")
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("تنظیمات", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("مشخصات فروشگاه (سربرگ فاکتور)")
            AppCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = settings.shopName, onValueChange = { v -> vm.updateSettings { it.copy(shopName = v) } },
                        label = { Text("نام فروشگاه / کسب‌وکار") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = settings.phone, onValueChange = { v -> vm.updateSettings { it.copy(phone = v) } },
                        label = { Text("تلفن") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = settings.address, onValueChange = { v -> vm.updateSettings { it.copy(address = v) } },
                        label = { Text("نشانی") }, minLines = 2, maxLines = 3,
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = settings.invoiceNote, onValueChange = { v -> vm.updateSettings { it.copy(invoiceNote = v) } },
                        label = { Text("متن پایین فاکتور") }, minLines = 2, maxLines = 3,
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                    )
                }
            }

            SectionTitle("واحد پول")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                MoneyUnit.entries.forEachIndexed { i, u ->
                    SegmentedButton(
                        selected = settings.unit == u,
                        onClick = { vm.updateSettings { it.copy(unit = u) } },
                        shape = SegmentedButtonDefaults.itemShape(i, MoneyUnit.entries.size),
                    ) { Text(u.label) }
                }
            }
            Text(
                "فقط برچسب نمایش مبلغ‌ها عوض می‌شود؛ اعداد ثبت‌شده تغییری نمی‌کنند.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle("ظاهر برنامه")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { m ->
                    FilterChip(selected = settings.theme == m, onClick = { vm.updateSettings { it.copy(theme = m) } }, label = { Text(m.label) })
                }
            }

            SectionTitle("پشتیبان‌گیری و بازیابی")
            AppCard(Modifier.fillMaxWidth()) {
                Text(
                    "از همهٔ معاملات و تنظیمات یک فایل پشتیبان (JSON) بگیرید و هر زمان یا روی دستگاه دیگر بازیابی کنید.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { exportLauncher.launch("hesabdar-backup-" + Jalali.today().short().replace("/", "-").let(Fa::latinDigits) + ".json") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.FileUpload, null)
                        Text("  خروجی گرفتن")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.FileDownload, null)
                        Text("  ورود اطلاعات")
                    }
                }
            }

            SectionTitle("دربارهٔ برنامه")
            AppCard(Modifier.fillMaxWidth()) {
                Text("حسابدار شاپ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "نسخهٔ " + Fa.digits(BuildConfig.VERSION_NAME) + " • ثبت خرید و فروش، واسطه‌گری، سود و زیان و فاکتور غیررسمی",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    pendingImport?.let { uri ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("ورود اطلاعات از فایل پشتیبان") },
            text = {
                Text(
                    "«جایگزینی کامل»، همهٔ اطلاعات فعلی را پاک می‌کند و فقط اطلاعات فایل را نگه می‌دارد.\n" +
                        "«افزودن»، معاملات فایل را به اطلاعات فعلی اضافه می‌کند.",
                )
            },
            confirmButton = {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    TextButton(onClick = { pendingImport = null; runImport(scope, vm, uri, ImportMode.REPLACE, ::toast) }) {
                        Text("جایگزینی کامل", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { pendingImport = null; runImport(scope, vm, uri, ImportMode.MERGE, ::toast) }) { Text("افزودن به اطلاعات فعلی") }
                }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("انصراف") } },
        )
    }
}

private fun runImport(
    scope: kotlinx.coroutines.CoroutineScope,
    vm: MainViewModel,
    uri: Uri,
    mode: ImportMode,
    toast: (String) -> Unit,
) {
    scope.launch {
        try {
            val n = vm.importBackup(uri, mode)
            toast("ورود اطلاعات انجام شد (${Fa.digits("$n")} معامله)")
        } catch (e: InvalidBackupException) {
            toast(e.message ?: "فایل پشتیبان معتبر نیست")
        } catch (e: Exception) {
            toast("ورود اطلاعات ناموفق بود")
        }
    }
}
