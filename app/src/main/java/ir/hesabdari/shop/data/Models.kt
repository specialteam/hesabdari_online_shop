package ir.hesabdari.shop.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single deal: a normal trade (buy then sell) or a brokerage deal where the goods go directly from
 * the seller to the buyer and we only hold the money and keep our margin.
 *
 * Prices are per unit; totals are `quantity * price`.
 */
@Entity(tableName = "deals")
data class Deal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val quantity: Double = 1.0,
    val unit: String = "عدد",
    val buyPrice: Long = 0,
    /** Null while the goods are not sold yet (open deal). */
    val sellPrice: Long? = null,
    /** Extra costs (shipping, fees…) as a total amount. */
    val extraCost: Long = 0,
    val dateEpochDay: Long,
    val isBrokerage: Boolean = false,
    val sellerName: String = "",
    val buyerName: String = "",
    val tags: List<String> = emptyList(),
    val note: String = "",
    /** Brokerage: money was received from the buyer. */
    val buyerPaid: Boolean = false,
    /** Brokerage: the goods reached the buyer. */
    val delivered: Boolean = false,
    /** Brokerage: we took our margin and settled with the seller. */
    val sellerSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class MoneyUnit(val label: String) { TOMAN("تومان"), RIAL("ریال") }

enum class ThemeMode(val label: String) { SYSTEM("پیش‌فرض سیستم"), LIGHT("روشن"), DARK("تیره") }

data class AppSettings(
    val shopName: String = "",
    val phone: String = "",
    val address: String = "",
    val unit: MoneyUnit = MoneyUnit.TOMAN,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val invoiceNote: String = "این فاکتور غیررسمی بوده و صرفاً جهت اطلاع است.",
    val invoiceCounter: Int = 1000,
)
