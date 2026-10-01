package ir.hesabdari.shop

import ir.hesabdari.shop.data.Deal
import ir.hesabdari.shop.domain.Calc
import ir.hesabdari.shop.domain.DealFilter
import ir.hesabdari.shop.domain.Filters
import ir.hesabdari.shop.domain.KindFilter
import ir.hesabdari.shop.domain.Outcome
import ir.hesabdari.shop.domain.ResultFilter
import ir.hesabdari.shop.domain.SortOrder
import ir.hesabdari.shop.domain.Stage
import ir.hesabdari.shop.domain.Stat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcTest {
    private fun deal(
        id: Long, buy: Long, sell: Long?, qty: Double = 1.0, extra: Long = 0, day: Long = 100,
        broker: Boolean = false, paid: Boolean = false, delivered: Boolean = false, settled: Boolean = false,
        tags: List<String> = emptyList(), title: String = "کالا $id",
    ) = Deal(
        id = id, title = title, quantity = qty, buyPrice = buy, sellPrice = sell, extraCost = extra,
        dateEpochDay = day, isBrokerage = broker, buyerPaid = paid, delivered = delivered,
        sellerSettled = settled, tags = tags,
    )

    @Test
    fun singleDeal() {
        val d = deal(1, buy = 1000, sell = 1250, qty = 4.0, extra = 100)
        assertEquals(4000L, Calc.buyTotal(d))
        assertEquals(5000L, Calc.sellTotal(d))
        assertEquals(4100L, Calc.cost(d))
        assertEquals(900L, Calc.profit(d))
        assertEquals(900 * 100.0 / 4100, Calc.profitPercent(d)!!, 1e-9)
        assertEquals(Outcome.PROFIT, Calc.outcome(d))
    }

    @Test
    fun openDealHasNoProfit() {
        val d = deal(1, buy = 1000, sell = null)
        assertNull(Calc.profit(d))
        assertEquals(Outcome.OPEN, Calc.outcome(d))
    }

    @Test
    fun brokerageStages() {
        val d = deal(1, buy = 900, sell = 1000, qty = 10.0, broker = true)
        assertEquals(Stage.WAIT_PAYMENT, Calc.stage(d))
        assertEquals(10_000L, Calc.receivable(d))
        val paid = d.copy(buyerPaid = true)
        assertEquals(Stage.WAIT_DELIVERY, Calc.stage(paid))
        assertEquals(10_000L, Calc.heldMoney(paid))
        assertEquals(9_000L, Calc.owedToSeller(paid))
        assertEquals(0L, Calc.marginToTake(paid))
        val delivered = paid.copy(delivered = true)
        assertEquals(Stage.WAIT_SETTLE, Calc.stage(delivered))
        assertEquals(1_000L, Calc.marginToTake(delivered))
        val settled = delivered.copy(sellerSettled = true)
        assertEquals(Stage.SETTLED, Calc.stage(settled))
        assertEquals(0L, Calc.heldMoney(settled))
    }

    @Test
    fun statsAggregation() {
        val deals = listOf(
            deal(1, 1000, 1200),                    // +200
            deal(2, 1000, 900),                     // -100
            deal(3, 500, 500),                      // 0
            deal(4, 700, null),                     // open, stock 700
            deal(5, 800, 1000, broker = true, paid = true, delivered = true), // +200 realized
            deal(6, 800, 900, broker = true, paid = true),                    // +100 pending
        )
        val s = Stat.compute(deals)
        assertEquals(6, s.count)
        assertEquals(5, s.soldCount)
        assertEquals(1, s.openCount)
        assertEquals(400L, s.netProfit)
        assertEquals(300L, s.realizedProfit)
        assertEquals(100L, s.pendingProfit)
        assertEquals(500L, s.gain)
        assertEquals(100L, s.loss)
        assertEquals(3, s.profitCount)
        assertEquals(1, s.lossCount)
        assertEquals(1, s.evenCount)
        assertEquals(700L, s.stockValue)
        assertEquals(60.0, s.winRate, 1e-9)
        assertEquals(20.0, s.lossRate, 1e-9)
        assertEquals(4800L, s.totalBuy)
        assertEquals(4500L, s.totalSell)
        assertEquals(4100L, s.soldCost)
        assertEquals(400 * 100.0 / 4100, s.profitPercent, 1e-9)
        assertEquals(1900L, s.heldMoney)
        assertEquals(1600L, s.owedToSellers)
        assertEquals(200L, s.marginToTake)
        assertEquals(0L, s.receivable)
    }

    @Test
    fun filtersAndSorting() {
        val deals = listOf(
            deal(1, 1000, 1200, day = 10, tags = listOf("عمده")),
            deal(2, 1000, 900, day = 20),
            deal(3, 700, null, day = 30),
            deal(4, 800, 1000, day = 40, broker = true, paid = true),
        )
        assertEquals(listOf(4L, 3L, 2L, 1L), Filters.apply(deals, DealFilter()).map { it.id })
        assertEquals(listOf(1L), Filters.apply(deals, DealFilter(tags = setOf("عمده"))).map { it.id })
        assertEquals(listOf(4L), Filters.apply(deals, DealFilter(kind = KindFilter.BROKERAGE)).map { it.id })
        assertEquals(listOf(2L), Filters.apply(deals, DealFilter(result = ResultFilter.LOSS)).map { it.id })
        assertEquals(listOf(3L), Filters.apply(deals, DealFilter(result = ResultFilter.OPEN)).map { it.id })
        assertEquals(listOf(2L, 3L), Filters.apply(deals, DealFilter(fromDay = 15, toDay = 35, sort = SortOrder.OLDEST)).map { it.id })
        assertEquals(listOf(1L, 4L, 2L, 3L), Filters.apply(deals, DealFilter(sort = SortOrder.PROFIT_DESC)).map { it.id })
        assertEquals(listOf(3L), Filters.apply(deals, DealFilter(query = "کالا ۳")).map { it.id })
    }
}
