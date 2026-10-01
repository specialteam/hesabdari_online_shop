package ir.hesabdari.shop.domain

import ir.hesabdari.shop.data.Deal
import kotlin.math.roundToLong

enum class Outcome(val label: String) {
    PROFIT("سود"), LOSS("زیان"), EVEN("بدون سود و زیان"), OPEN("فروش نرفته")
}

/** Where a brokerage deal currently is. */
enum class Stage(val label: String, val hint: String) {
    WAIT_PAYMENT("منتظر پول خریدار", "هنوز پولی از خریدار نگرفته‌ایم"),
    WAIT_DELIVERY("پول رسیده، منتظر تحویل", "پول نزد ماست؛ کالا هنوز به خریدار نرسیده"),
    WAIT_SETTLE("تحویل شد، آماده تسویه", "سود خود را برداشته و باقی را به فروشنده بدهید"),
    SETTLED("تسویه شد", "حساب فروشنده تسویه و سود برداشته شد"),
}

object Calc {
    fun buyTotal(d: Deal): Long = (d.quantity * d.buyPrice).roundToLong()

    fun sellTotal(d: Deal): Long? = d.sellPrice?.let { (d.quantity * it).roundToLong() }

    /** Everything we spend on the deal: purchase + extra costs. */
    fun cost(d: Deal): Long = buyTotal(d) + d.extraCost

    fun profit(d: Deal): Long? = sellTotal(d)?.let { it - cost(d) }

    /** Profit relative to cost, in percent. */
    fun profitPercent(d: Deal): Double? {
        val p = profit(d) ?: return null
        val c = cost(d)
        return if (c > 0) p * 100.0 / c else null
    }

    /** Sell price minus buy price, per unit. */
    fun unitMargin(d: Deal): Long? = d.sellPrice?.let { it - d.buyPrice }

    fun outcome(d: Deal): Outcome {
        val p = profit(d) ?: return Outcome.OPEN
        return when {
            p > 0 -> Outcome.PROFIT
            p < 0 -> Outcome.LOSS
            else -> Outcome.EVEN
        }
    }

    /** The profit is in our pocket: normal deal that is sold, or brokerage that reached the buyer. */
    fun isRealized(d: Deal): Boolean = d.sellPrice != null && (!d.isBrokerage || d.delivered)

    fun stage(d: Deal): Stage = when {
        d.sellerSettled -> Stage.SETTLED
        !d.buyerPaid -> Stage.WAIT_PAYMENT
        !d.delivered -> Stage.WAIT_DELIVERY
        else -> Stage.WAIT_SETTLE
    }

    /** Money from the buyer that is currently held by us. */
    fun heldMoney(d: Deal): Long =
        if (d.isBrokerage && d.buyerPaid && !d.sellerSettled) sellTotal(d) ?: 0 else 0

    /** What we still have to pay the seller (after taking our margin). */
    fun owedToSeller(d: Deal): Long =
        if (d.isBrokerage && d.buyerPaid && !d.sellerSettled) buyTotal(d) else 0

    /** What the buyer still has to pay us. */
    fun receivable(d: Deal): Long =
        if (d.isBrokerage && !d.buyerPaid) sellTotal(d) ?: 0 else 0

    /** Our margin that can be taken right now (delivered, paid, not settled yet). */
    fun marginToTake(d: Deal): Long =
        if (d.isBrokerage && d.buyerPaid && d.delivered && !d.sellerSettled) {
            (sellTotal(d) ?: 0) - buyTotal(d)
        } else {
            0
        }
}

/** Aggregated numbers (the "balance sheet" / بیلان). */
data class Stats(
    val count: Int = 0,
    val soldCount: Int = 0,
    val openCount: Int = 0,
    /** Total purchase amount of every deal. */
    val totalBuy: Long = 0,
    /** Total sales of sold deals. */
    val totalSell: Long = 0,
    /** Purchase + extra costs of sold deals. */
    val soldCost: Long = 0,
    val extraCosts: Long = 0,
    /** Net profit of sold deals (sales - cost). */
    val netProfit: Long = 0,
    val realizedProfit: Long = 0,
    val pendingProfit: Long = 0,
    /** Sum of positive profits. */
    val gain: Long = 0,
    /** Sum of losses as a positive number. */
    val loss: Long = 0,
    val profitCount: Int = 0,
    val lossCount: Int = 0,
    val evenCount: Int = 0,
    /** Cost of goods that are not sold yet. */
    val stockValue: Long = 0,
    val heldMoney: Long = 0,
    val owedToSellers: Long = 0,
    val receivable: Long = 0,
    val marginToTake: Long = 0,
    val brokerageCount: Int = 0,
) {
    /** Sales minus purchase of sold deals, before extra costs: how much was gained or lost on prices. */
    val grossMargin: Long get() = totalSell - (soldCost - extraCosts)

    /** Net profit relative to cost of sold deals. */
    val profitPercent: Double get() = if (soldCost > 0) netProfit * 100.0 / soldCost else 0.0

    /** Share of sold deals that made profit. */
    val winRate: Double get() = if (soldCount > 0) profitCount * 100.0 / soldCount else 0.0

    /** Share of sold deals that lost money - the estimated chance that a deal ends in a loss. */
    val lossRate: Double get() = if (soldCount > 0) lossCount * 100.0 / soldCount else 0.0

    val evenRate: Double get() = if (soldCount > 0) evenCount * 100.0 / soldCount else 0.0

    /** Average profit per sold deal. */
    val averageProfit: Long get() = if (soldCount > 0) netProfit / soldCount else 0

    /** Loss amount relative to the cost of everything that was sold. */
    val lossPercent: Double get() = if (soldCost > 0) loss * 100.0 / soldCost else 0.0

    /** Gains relative to the cost of everything that was sold. */
    val gainPercent: Double get() = if (soldCost > 0) gain * 100.0 / soldCost else 0.0

    /** Brokerage: the money that is ours in the end = what we hold minus what we owe sellers. */
    val ourShareOfHeld: Long get() = heldMoney - owedToSellers
}

object Stat {
    fun compute(deals: List<Deal>): Stats {
        var s = Stats()
        for (d in deals) {
            val buy = Calc.buyTotal(d)
            val sell = Calc.sellTotal(d)
            s = s.copy(
                count = s.count + 1,
                totalBuy = s.totalBuy + buy,
                brokerageCount = s.brokerageCount + if (d.isBrokerage) 1 else 0,
                heldMoney = s.heldMoney + Calc.heldMoney(d),
                owedToSellers = s.owedToSellers + Calc.owedToSeller(d),
                receivable = s.receivable + Calc.receivable(d),
                marginToTake = s.marginToTake + Calc.marginToTake(d),
            )
            if (sell == null) {
                s = s.copy(openCount = s.openCount + 1, stockValue = s.stockValue + Calc.cost(d))
                continue
            }
            val profit = sell - Calc.cost(d)
            s = s.copy(
                soldCount = s.soldCount + 1,
                totalSell = s.totalSell + sell,
                soldCost = s.soldCost + Calc.cost(d),
                extraCosts = s.extraCosts + d.extraCost,
                netProfit = s.netProfit + profit,
                realizedProfit = s.realizedProfit + if (Calc.isRealized(d)) profit else 0,
                pendingProfit = s.pendingProfit + if (Calc.isRealized(d)) 0 else profit,
                gain = s.gain + if (profit > 0) profit else 0,
                loss = s.loss + if (profit < 0) -profit else 0,
                profitCount = s.profitCount + if (profit > 0) 1 else 0,
                lossCount = s.lossCount + if (profit < 0) 1 else 0,
                evenCount = s.evenCount + if (profit == 0L) 1 else 0,
            )
        }
        return s
    }

    /** Groups for the report: normal deals, brokerage, and one per custom tag. */
    fun breakdown(deals: List<Deal>): List<Pair<String, Stats>> {
        val out = mutableListOf<Pair<String, Stats>>()
        val trade = deals.filter { !it.isBrokerage }
        val broker = deals.filter { it.isBrokerage }
        if (trade.isNotEmpty()) out += "خرید و فروش" to compute(trade)
        if (broker.isNotEmpty()) out += "واسطه‌گری" to compute(broker)
        val tags = deals.flatMap { it.tags }.distinct().sorted()
        for (t in tags) out += "#$t" to compute(deals.filter { t in it.tags })
        return out
    }

    /** Profit per Jalali month, oldest first: (year, month) -> profit. */
    fun monthlyProfit(deals: List<Deal>, monthOf: (Long) -> Pair<Int, Int>): List<Triple<Int, Int, Long>> {
        val map = sortedMapOf<Int, Long>()
        for (d in deals) {
            val p = Calc.profit(d) ?: continue
            val (y, m) = monthOf(d.dateEpochDay)
            val key = y * 100 + m
            map[key] = (map[key] ?: 0L) + p
        }
        return map.map { (k, v) -> Triple(k / 100, k % 100, v) }
    }
}
