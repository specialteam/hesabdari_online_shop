package ir.hesabdari.shop

import ir.hesabdari.shop.data.AppSettings
import ir.hesabdari.shop.data.Deal
import ir.hesabdari.shop.domain.DealFilter
import ir.hesabdari.shop.domain.KindFilter
import ir.hesabdari.shop.report.Block
import ir.hesabdari.shop.report.InvoiceData
import ir.hesabdari.shop.report.ReportBuilders
import ir.hesabdari.shop.report.TextRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportTest {
    private val deal = Deal(
        id = 1, title = "برنج", quantity = 10.0, unit = "کیلو", buyPrice = 90_000, sellPrice = 100_000,
        extraCost = 50_000, dateEpochDay = 20_362, buyerName = "علی", sellerName = "رضا",
    )

    @Test
    fun invoiceTotalsAndWords() {
        val items = ReportBuilders.invoiceItems(listOf(deal, deal.copy(id = 2, sellPrice = null)))
        assertEquals(1, items.size)
        assertEquals(1_000_000L, items[0].total)
        val inv = InvoiceData(1001, 20_362, "علی", items, discount = 50_000)
        assertEquals(950_000L, inv.payable)
        val doc = ReportBuilders.invoiceDoc(inv, AppSettings(shopName = "فروشگاه نمونه"))
        assertEquals("فروشگاه نمونه", doc.title)
        val text = TextRenderer.render(doc)
        assertTrue(text, text.contains("نهصد و پنجاه هزار تومان"))
        assertTrue(text, text.contains("۱۰۰۱"))
        // the invoice must never reveal the purchase price
        assertTrue(!text.contains("۹۰٬۰۰۰"))
    }

    @Test
    fun dealDocShowsProfit() {
        val doc = ReportBuilders.dealDoc(deal, "تومان")
        val text = TextRenderer.render(doc)
        assertTrue(text, text.contains("سود: ۵۰٬۰۰۰ تومان")) // 1,000,000 - 900,000 - 50,000
        assertTrue(doc.blocks.first() is Block.KeyValues)
    }

    @Test
    fun filterDescription() {
        assertEquals("بدون فیلتر (همهٔ معاملات)", ReportBuilders.describeFilter(DealFilter()))
        assertTrue(ReportBuilders.describeFilter(DealFilter(kind = KindFilter.BROKERAGE)).contains("واسطه‌گری"))
    }
}
