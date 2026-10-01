package ir.hesabdari.shop.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupDeal(
    val id: Long = 0,
    val title: String = "",
    val quantity: Double = 1.0,
    val unit: String = "عدد",
    val buyPrice: Long = 0,
    val sellPrice: Long? = null,
    val extraCost: Long = 0,
    val dateEpochDay: Long = 0,
    val isBrokerage: Boolean = false,
    val sellerName: String = "",
    val buyerName: String = "",
    val tags: List<String> = emptyList(),
    val note: String = "",
    val buyerPaid: Boolean = false,
    val delivered: Boolean = false,
    val sellerSettled: Boolean = false,
    val createdAt: Long = 0,
)

@Serializable
data class BackupSettings(
    val shopName: String = "",
    val phone: String = "",
    val address: String = "",
    val unit: String = MoneyUnit.TOMAN.name,
    val theme: String = ThemeMode.SYSTEM.name,
    val invoiceNote: String = "",
    val invoiceCounter: Int = 1000,
)

@Serializable
data class BackupFile(
    val app: String = APP_ID,
    val version: Int = 1,
    val exportedAt: Long = 0,
    val settings: BackupSettings = BackupSettings(),
    val deals: List<BackupDeal> = emptyList(),
) {
    companion object {
        const val APP_ID = "hesabdar-shop"
    }
}

enum class ImportMode { REPLACE, MERGE }

class InvalidBackupException(message: String) : Exception(message)

class BackupManager(
    private val context: Context,
    private val repository: DealRepository,
    private val settings: SettingsStore,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    suspend fun export(uri: Uri, deals: List<Deal>): Int = withContext(Dispatchers.IO) {
        val s = settings.state.value
        val file = BackupFile(
            exportedAt = System.currentTimeMillis(),
            settings = BackupSettings(
                s.shopName, s.phone, s.address, s.unit.name, s.theme.name, s.invoiceNote, s.invoiceCounter,
            ),
            deals = deals.map { it.toBackup() },
        )
        val text = json.encodeToString(BackupFile.serializer(), file)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            ?: throw IllegalStateException("cannot open output")
        deals.size
    }

    suspend fun import(uri: Uri, mode: ImportMode): Int = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw InvalidBackupException("فایل قابل خواندن نیست")
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw InvalidBackupException("ساختار فایل پشتیبان معتبر نیست")
        }
        if (file.app != BackupFile.APP_ID) throw InvalidBackupException("این فایل پشتیبان این برنامه نیست")
        val deals = file.deals.map { it.toDeal() }
        when (mode) {
            ImportMode.REPLACE -> {
                repository.replaceAll(deals)
                settings.update {
                    val b = file.settings
                    it.copy(
                        shopName = b.shopName,
                        phone = b.phone,
                        address = b.address,
                        unit = runCatching { MoneyUnit.valueOf(b.unit) }.getOrDefault(it.unit),
                        theme = runCatching { ThemeMode.valueOf(b.theme) }.getOrDefault(it.theme),
                        invoiceNote = b.invoiceNote.ifBlank { it.invoiceNote },
                        invoiceCounter = maxOf(b.invoiceCounter, it.invoiceCounter),
                    )
                }
            }
            ImportMode.MERGE -> repository.addAll(deals)
        }
        deals.size
    }
}

private fun Deal.toBackup() = BackupDeal(
    id, title, quantity, unit, buyPrice, sellPrice, extraCost, dateEpochDay, isBrokerage, sellerName, buyerName,
    tags, note, buyerPaid, delivered, sellerSettled, createdAt,
)

private fun BackupDeal.toDeal() = Deal(
    id, title, quantity, unit, buyPrice, sellPrice, extraCost, dateEpochDay, isBrokerage, sellerName, buyerName,
    tags, note, buyerPaid, delivered, sellerSettled, createdAt,
)
