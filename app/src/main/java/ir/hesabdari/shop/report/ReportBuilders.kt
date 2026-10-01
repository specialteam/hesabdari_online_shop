package ir.hesabdari.shop.report

import ir.hesabdari.shop.data.AppSettings
import ir.hesabdari.shop.data.Deal
import ir.hesabdari.shop.domain.Calc
import ir.hesabdari.shop.domain.DealFilter
import ir.hesabdari.shop.domain.Outcome
import ir.hesabdari.shop.domain.Period
import ir.hesabdari.shop.domain.Stat
import ir.hesabdari.shop.domain.Stats
import ir.hesabdari.shop.util.Fa
import ir.hesabdari.shop.util.Jalali
import ir.hesabdari.shop.util.PersianWords
import kotlin.math.abs

data class InvoiceItem(val title: String, val quantity: Double, val unit: String, val unitPrice: Long, val total: Long)

data class InvoiceData(
    val number: Int,
    val dateEpochDay: Long,
    val buyer: String,
    val items: List<InvoiceItem>,
    val discount: Long = 0,
    val note: String = "",
) {
    val subtotal: Long get() = items.sumOf { it.total }
    val payable: Long get() = (subtotal - discount).coerceAtLeast(0)
}

object ReportBuilders {
    private fun toneOf(v: Long) = when {
        v > 0 -> Tone.GAIN
        v < 0 -> Tone.LOSS
        else -> Tone.NEUTRAL
    }

    private fun stamp(): String {
        val t = java.time.LocalTime.now()
        return "تاریخ گزارش: ${Jalali.today().full()}  ساعت ${Fa.digits(String.format(java.util.Locale.US, "%02d:%02d", t.hour, t.minute))}"
    }

    fun describeFilter(f: DealFilter): String {
        val parts = mutableListOf<String>()
        if (f.query.isNotBlank()) parts += "جستجو: «${f.query.trim()}»"
        if (f.kind != ir.hesabdari.shop.domain.KindFilter.ALL) parts += f.kind.label
        if (f.result != ir.hesabdari.shop.domain.ResultFilter.ALL) parts += f.result.label
        f.stage?.let { parts += it.label }
        if (f.tags.isNotEmpty()) parts += "برچسب: " + f.tags.joinToString("، ")
        if (f.fromDay != null || f.toDay != null) {
            val from = f.fromDay?.let { "از " + Jalali.fromEpochDay(it).short() }.orEmpty()
            val to = f.toDay?.let { "تا " + Jalali.fromEpochDay(it).short() }.orEmpty()
            parts += "$from $to".trim()
        }
        return if (parts.isEmpty()) "بدون فیلتر (همهٔ معاملات)" else parts.joinToString(" • ")
    }

    private fun shopHeader(s: AppSettings): String = s.shopName.ifBlank { "حسابدار شاپ" }

    fun balanceRows(st: Stats, unit: String): List<KV> = listOf(
        KV("تعداد معاملات", Fa.digits("${st.count}") + " (فروخته‌شده: ${Fa.digits("${st.soldCount}")}، فروش نرفته: ${Fa.digits("${st.openCount}")})"),
        KV("مبلغ کل خرید", Fa.money(st.totalBuy, unit)),
        KV("مبلغ کل فروش", Fa.money(st.totalSell, unit)),
        KV("هزینه‌های جانبی", Fa.money(st.extraCosts, unit)),
        KV("اضافه / کسری قیمت (فروش − خرید)", Fa.signedMoney(st.grossMargin, unit), toneOf(st.grossMargin)),
        KV("سود خالص (پس از کسر هزینه‌ها)", Fa.signedMoney(st.netProfit, unit), toneOf(st.netProfit), true),
        KV("درصد سود کل", Fa.percent(st.profitPercent), toneOf(st.netProfit), true),
        KV("جمع معاملات سودده", Fa.money(st.gain, unit) + " (${Fa.percent(st.gainPercent)})", Tone.GAIN),
        KV("جمع معاملات زیان‌ده", Fa.money(st.loss, unit) + " (${Fa.percent(st.lossPercent)})", if (st.loss > 0) Tone.LOSS else Tone.NEUTRAL),
        KV("سود محقق‌شده", Fa.signedMoney(st.realizedProfit, unit), toneOf(st.realizedProfit)),
        KV("سود در انتظار (تحویل‌نشده)", Fa.signedMoney(st.pendingProfit, unit), Tone.MUTED),
        KV("میانگین سود هر معامله", Fa.signedMoney(st.averageProfit, unit), toneOf(st.averageProfit)),
        KV("معاملات سودده", Fa.digits("${st.profitCount}") + " معامله (${Fa.percent(st.winRate)})", Tone.GAIN),
        KV("معاملات زیان‌ده (احتمال ضرر)", Fa.digits("${st.lossCount}") + " معامله (${Fa.percent(st.lossRate)})", if (st.lossCount > 0) Tone.LOSS else Tone.NEUTRAL),
        KV("بدون سود و زیان", Fa.digits("${st.evenCount}") + " معامله (${Fa.percent(st.evenRate)})"),
        KV("موجودی فروش‌نرفته (به قیمت خرید)", Fa.money(st.stockValue, unit)),
    )

    fun brokerageRows(st: Stats, unit: String): List<KV> = listOf(
        KV("تعداد معاملات واسطه‌گری", Fa.digits("${st.brokerageCount}")),
        KV("طلب از خریداران (هنوز پرداخت نکرده‌اند)", Fa.money(st.receivable, unit)),
        KV("پول امانی نزد ما", Fa.money(st.heldMoney, unit), Tone.NEUTRAL, true),
        KV("قابل پرداخت به فروشندگان", Fa.money(st.owedToSellers, unit)),
        KV("سود قابل برداشت (تحویل‌شده)", Fa.money(st.marginToTake, unit), Tone.GAIN),
    )

    fun dealsTable(deals: List<Deal>, unit: String): Block.Table {
        val headers = listOf("ردیف", "تاریخ", "کالا", "تعداد", "جمع خرید", "جمع فروش", "سود / زیان", "درصد")
        val rows = deals.mapIndexed { i, d ->
            val p = Calc.profit(d)
            listOf(
                Fa.digits("${i + 1}"),
                Jalali.fromEpochDay(d.dateEpochDay).short(),
                d.title + if (d.isBrokerage) " (واسطه‌گری)" else "",
                Fa.quantity(d.quantity) + " " + d.unit,
                Fa.number(Calc.buyTotal(d)),
                Calc.sellTotal(d)?.let { Fa.number(it) } ?: "—",
                p?.let { Fa.number(it) } ?: "—",
                Calc.profitPercent(d)?.let { Fa.percent(it) } ?: "—",
            )
        }
        val st = Stat.compute(deals)
        return Block.Table(
            headers, rows,
            weights = listOf(0.7f, 1.4f, 2.4f, 1.2f, 1.7f, 1.7f, 1.6f, 1.0f),
            footer = listOf("", "", "جمع", "", Fa.number(st.totalBuy), Fa.number(st.totalSell), Fa.number(st.netProfit), Fa.percent(st.profitPercent)),
        )
    }

    fun dealsDoc(deals: List<Deal>, filter: DealFilter, unit: String): ReportDoc {
        val st = Stat.compute(deals)
        return ReportDoc(
            title = "فهرست معاملات",
            subtitle = describeFilter(filter) + "\n" + stamp() + "  •  واحد پول: $unit",
            blocks = listOf(
                Block.Heading("خلاصه"),
                Block.KeyValues(
                    listOf(
                        KV("تعداد معاملات", Fa.digits("${st.count}")),
                        KV("جمع خرید", Fa.money(st.totalBuy, unit)),
                        KV("جمع فروش", Fa.money(st.totalSell, unit)),
                        KV("سود / زیان خالص", Fa.signedMoney(st.netProfit, unit) + " (${Fa.percent(st.profitPercent)})", toneOf(st.netProfit), true),
                    ),
                ),
                Block.Heading("معاملات"),
                dealsTable(deals, unit),
            ),
            fileName = "deals-" + Jalali.today().short().replace("/", "-"),
        )
    }

    fun balanceDoc(deals: List<Deal>, filter: DealFilter, unit: String): ReportDoc {
        val st = Stat.compute(deals)
        val blocks = mutableListOf<Block>()
        blocks += Block.Heading("بیلان کلی")
        blocks += Block.KeyValues(balanceRows(st, unit))
        if (st.brokerageCount > 0) {
            blocks += Block.Heading("وضعیت واسطه‌گری")
            blocks += Block.KeyValues(brokerageRows(st, unit))
        }
        val groups = Stat.breakdown(deals)
        if (groups.size > 1) {
            blocks += Block.Heading("تفکیک بر اساس نوع و برچسب")
            blocks += Block.Table(
                listOf("گروه", "تعداد", "جمع فروش", "سود / زیان", "درصد"),
                groups.map { (name, s) ->
                    listOf(name, Fa.digits("${s.count}"), Fa.number(s.totalSell), Fa.number(s.netProfit), Fa.percent(s.profitPercent))
                },
                weights = listOf(2.2f, 0.9f, 1.8f, 1.8f, 1f),
            )
        }
        val months = Stat.monthlyProfit(deals) { e -> Jalali.fromEpochDay(e).let { it.year to it.month } }
        if (months.isNotEmpty()) {
            blocks += Block.Heading("سود و زیان ماهانه")
            blocks += Block.Table(
                listOf("ماه", "سود / زیان"),
                months.map { (y, m, p) -> listOf("${Jalali.monthNames[m - 1]} ${Fa.digits("$y")}", Fa.number(p)) },
                weights = listOf(2f, 2f),
            )
        }
        return ReportDoc(
            title = "بیلان و گزارش سود و زیان",
            subtitle = describeFilter(filter) + "\n" + stamp() + "  •  واحد پول: $unit",
            blocks = blocks,
            fileName = "balance-" + Jalali.today().short().replace("/", "-"),
        )
    }

    fun homeDoc(deals: List<Deal>, period: Period, unit: String): ReportDoc {
        val st = Stat.compute(deals)
        val blocks = mutableListOf<Block>(
            Block.Heading("بیلان"),
            Block.KeyValues(balanceRows(st, unit)),
        )
        if (st.brokerageCount > 0) {
            blocks += Block.Heading("وضعیت واسطه‌گری")
            blocks += Block.KeyValues(brokerageRows(st, unit))
        }
        blocks += Block.Heading("آخرین معاملات")
        blocks += dealsTable(deals.sortedByDescending { it.dateEpochDay }.take(10), unit)
        return ReportDoc(
            title = "خلاصهٔ وضعیت",
            subtitle = "بازه: ${period.label}\n" + stamp() + "  •  واحد پول: $unit",
            blocks = blocks,
            fileName = "summary-" + Jalali.today().short().replace("/", "-"),
        )
    }

    fun dealDoc(d: Deal, unit: String): ReportDoc {
        val buy = Calc.buyTotal(d)
        val sell = Calc.sellTotal(d)
        val profit = Calc.profit(d)
        val rows = mutableListOf<KV>()
        rows += KV("نوع معامله", if (d.isBrokerage) "واسطه‌گری" else "خرید و فروش")
        rows += KV("تاریخ", Jalali.fromEpochDay(d.dateEpochDay).full())
        rows += KV("تعداد", Fa.quantity(d.quantity) + " " + d.unit)
        rows += KV("قیمت خرید (هر واحد)", Fa.money(d.buyPrice, unit))
        rows += KV("قیمت فروش (هر واحد)", d.sellPrice?.let { Fa.money(it, unit) } ?: "هنوز فروخته نشده")
        rows += KV("جمع خرید", Fa.money(buy, unit))
        rows += KV("جمع فروش", sell?.let { Fa.money(it, unit) } ?: "—")
        if (d.extraCost > 0) rows += KV("هزینه‌های جانبی", Fa.money(d.extraCost, unit))
        if (profit != null) {
            val tone = toneOf(profit)
            rows += KV(if (profit < 0) "زیان" else "سود", Fa.money(abs(profit), unit), tone, true)
            Calc.profitPercent(d)?.let { rows += KV("درصد " + if (profit < 0) "زیان" else "سود", Fa.percent(abs(it)), tone, true) }
            Calc.unitMargin(d)?.let { rows += KV("سود هر واحد", Fa.signedMoney(it, unit), toneOf(it)) }
        }
        if (d.sellerName.isNotBlank()) rows += KV("فروشنده", d.sellerName)
        if (d.buyerName.isNotBlank()) rows += KV("خریدار", d.buyerName)
        if (d.tags.isNotEmpty()) rows += KV("برچسب‌ها", d.tags.joinToString("، "))
        val blocks = mutableListOf<Block>(Block.KeyValues(rows))
        if (d.isBrokerage) {
            blocks += Block.Heading("وضعیت واسطه‌گری")
            fun yn(b: Boolean) = if (b) "بله ✓" else "خیر"
            blocks += Block.KeyValues(
                listOf(
                    KV("مرحله", Calc.stage(d).label, Tone.NEUTRAL, true),
                    KV("پول از خریدار دریافت شد", yn(d.buyerPaid)),
                    KV("کالا به خریدار رسید", yn(d.delivered)),
                    KV("تسویه با فروشنده و برداشت سود", yn(d.sellerSettled)),
                    KV("سهم فروشنده (پس از کسر سود ما)", Fa.money(buy, unit)),
                ),
            )
        }
        if (d.note.isNotBlank()) {
            blocks += Block.Heading("یادداشت")
            blocks += Block.Paragraph(d.note)
        }
        return ReportDoc(
            title = "معامله: ${d.title}",
            subtitle = stamp(),
            blocks = blocks,
            fileName = "deal-${d.id}",
        )
    }

    fun invoiceItems(deals: List<Deal>): List<InvoiceItem> = deals.mapNotNull { d ->
        val price = d.sellPrice ?: return@mapNotNull null
        InvoiceItem(d.title, d.quantity, d.unit, price, Calc.sellTotal(d) ?: 0)
    }

    fun invoiceDoc(inv: InvoiceData, settings: AppSettings): ReportDoc {
        val unit = settings.unit.label
        val sub = listOf(settings.phone, settings.address).filter { it.isNotBlank() }.joinToString("  •  ")
        val blocks = mutableListOf<Block>()
        blocks += Block.Heading("فاکتور فروش (غیررسمی)")
        blocks += Block.KeyValues(
            buildList {
                add(KV("شمارهٔ فاکتور", Fa.digits("${inv.number}")))
                add(KV("تاریخ", Jalali.fromEpochDay(inv.dateEpochDay).full()))
                if (inv.buyer.isNotBlank()) add(KV("خریدار", inv.buyer, Tone.NEUTRAL, true))
            },
        )
        blocks += Block.Spacer
        blocks += Block.Table(
            listOf("ردیف", "شرح کالا", "تعداد", "واحد", "فی ($unit)", "مبلغ ($unit)"),
            inv.items.mapIndexed { i, it ->
                listOf(Fa.digits("${i + 1}"), it.title, Fa.quantity(it.quantity), it.unit, Fa.number(it.unitPrice), Fa.number(it.total))
            },
            weights = listOf(0.7f, 3f, 1f, 1f, 1.8f, 2f),
        )
        blocks += Block.Spacer
        val rows = mutableListOf(KV("جمع کل", Fa.money(inv.subtotal, unit)))
        if (inv.discount > 0) rows += KV("تخفیف", Fa.money(inv.discount, unit), Tone.LOSS)
        rows += KV("مبلغ قابل پرداخت", Fa.money(inv.payable, unit), Tone.NEUTRAL, true)
        blocks += Block.KeyValues(rows)
        blocks += Block.Paragraph("مبلغ به حروف: ${PersianWords.toWords(inv.payable)} $unit")
        if (inv.note.isNotBlank()) blocks += Block.Paragraph(inv.note, small = true)
        blocks += Block.Spacer
        blocks += Block.Paragraph("امضا و مهر فروشنده  ____________________        امضای خریدار  ____________________", small = true, center = true)
        return ReportDoc(
            title = shopHeader(settings),
            subtitle = sub.ifBlank { null },
            blocks = blocks,
            fileName = "invoice-${inv.number}",
        )
    }

    /** Outcome label helper used by UI and text. */
    fun outcomeLabel(d: Deal): String = Calc.outcome(d).let { if (it == Outcome.OPEN) "فروش نرفته" else it.label }
}
