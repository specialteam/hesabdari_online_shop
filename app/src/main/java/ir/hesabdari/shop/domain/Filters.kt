package ir.hesabdari.shop.domain

import ir.hesabdari.shop.data.Deal
import ir.hesabdari.shop.util.Fa
import ir.hesabdari.shop.util.Jalali

enum class KindFilter(val label: String) { ALL("همه"), TRADE("خرید و فروش"), BROKERAGE("واسطه‌گری") }

enum class ResultFilter(val label: String) {
    ALL("همه"), PROFIT("سودده"), LOSS("زیان‌ده"), EVEN("بدون سود و زیان"), OPEN("فروش نرفته")
}

enum class SortOrder(val label: String) {
    NEWEST("جدیدترین"),
    OLDEST("قدیمی‌ترین"),
    PROFIT_DESC("بیشترین سود"),
    PROFIT_ASC("کمترین سود / بیشترین زیان"),
    AMOUNT_DESC("بیشترین مبلغ"),
}

enum class Period(val label: String) {
    TODAY("امروز"), WEEK("این هفته"), MONTH("این ماه"), YEAR("امسال"), ALL("همه");

    /** Inclusive epoch-day range, or null bounds when open. */
    fun range(today: Long): Pair<Long?, Long?> = when (this) {
        TODAY -> today to today
        WEEK -> Jalali.startOfWeek(today) to Jalali.startOfWeek(today) + 6
        MONTH -> Jalali.startOfMonth(today) to Jalali.endOfMonth(today)
        YEAR -> Jalali.startOfYear(today) to Jalali.endOfYear(today)
        ALL -> null to null
    }
}

data class DealFilter(
    val query: String = "",
    val kind: KindFilter = KindFilter.ALL,
    val result: ResultFilter = ResultFilter.ALL,
    /** Only for brokerage deals; null = any stage. */
    val stage: Stage? = null,
    val tags: Set<String> = emptySet(),
    val fromDay: Long? = null,
    val toDay: Long? = null,
    val sort: SortOrder = SortOrder.NEWEST,
) {
    /** Number of active filters (the sort order and the search box are not counted). */
    val activeCount: Int
        get() = listOf(
            kind != KindFilter.ALL,
            result != ResultFilter.ALL,
            stage != null,
            tags.isNotEmpty(),
            fromDay != null || toDay != null,
        ).count { it }

    val isDefault: Boolean get() = this == DealFilter()
}

object Filters {
    fun apply(deals: List<Deal>, f: DealFilter): List<Deal> {
        val q = Fa.normalizeText(f.query.trim())
        val filtered = deals.filter { d ->
            matchesQuery(d, q) &&
                when (f.kind) {
                    KindFilter.ALL -> true
                    KindFilter.TRADE -> !d.isBrokerage
                    KindFilter.BROKERAGE -> d.isBrokerage
                } &&
                when (f.result) {
                    ResultFilter.ALL -> true
                    ResultFilter.PROFIT -> Calc.outcome(d) == Outcome.PROFIT
                    ResultFilter.LOSS -> Calc.outcome(d) == Outcome.LOSS
                    ResultFilter.EVEN -> Calc.outcome(d) == Outcome.EVEN
                    ResultFilter.OPEN -> Calc.outcome(d) == Outcome.OPEN
                } &&
                (f.stage == null || (d.isBrokerage && Calc.stage(d) == f.stage)) &&
                (f.tags.isEmpty() || d.tags.any { it in f.tags }) &&
                (f.fromDay == null || d.dateEpochDay >= f.fromDay) &&
                (f.toDay == null || d.dateEpochDay <= f.toDay)
        }
        return when (f.sort) {
            SortOrder.NEWEST -> filtered.sortedWith(compareByDescending<Deal> { it.dateEpochDay }.thenByDescending { it.id })
            SortOrder.OLDEST -> filtered.sortedWith(compareBy<Deal> { it.dateEpochDay }.thenBy { it.id })
            SortOrder.PROFIT_DESC -> filtered.sortedByDescending { Calc.profit(it) ?: Long.MIN_VALUE }
            SortOrder.PROFIT_ASC -> filtered.sortedBy { Calc.profit(it) ?: Long.MAX_VALUE }
            SortOrder.AMOUNT_DESC -> filtered.sortedByDescending { Calc.sellTotal(it) ?: Calc.buyTotal(it) }
        }
    }

    private fun matchesQuery(d: Deal, q: String): Boolean {
        if (q.isEmpty()) return true
        val hay = listOf(d.title, d.sellerName, d.buyerName, d.note, d.tags.joinToString(" "), if (d.isBrokerage) "واسطه گری واسطه‌گری" else "")
            .joinToString(" ")
        return Fa.normalizeText(hay).contains(q)
    }

    fun inRange(deals: List<Deal>, period: Period, today: Long): List<Deal> {
        val (from, to) = period.range(today)
        return deals.filter { (from == null || it.dateEpochDay >= from) && (to == null || it.dateEpochDay <= to) }
    }
}
