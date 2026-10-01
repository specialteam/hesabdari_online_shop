package ir.hesabdari.shop.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tiny SharedPreferences backed settings holder exposed as a [StateFlow]. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun load(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            shopName = prefs.getString("shopName", d.shopName).orEmpty(),
            phone = prefs.getString("phone", d.phone).orEmpty(),
            address = prefs.getString("address", d.address).orEmpty(),
            unit = runCatching { MoneyUnit.valueOf(prefs.getString("unit", d.unit.name).orEmpty()) }
                .getOrDefault(d.unit),
            theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", d.theme.name).orEmpty()) }
                .getOrDefault(d.theme),
            invoiceNote = prefs.getString("invoiceNote", d.invoiceNote).orEmpty(),
            invoiceCounter = prefs.getInt("invoiceCounter", d.invoiceCounter),
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        _state.value = next
        prefs.edit()
            .putString("shopName", next.shopName)
            .putString("phone", next.phone)
            .putString("address", next.address)
            .putString("unit", next.unit.name)
            .putString("theme", next.theme.name)
            .putString("invoiceNote", next.invoiceNote)
            .putInt("invoiceCounter", next.invoiceCounter)
            .apply()
    }

    /** Returns the next invoice number and advances the counter. */
    fun nextInvoiceNumber(): Int {
        val n = _state.value.invoiceCounter + 1
        update { it.copy(invoiceCounter = n) }
        return n
    }
}
