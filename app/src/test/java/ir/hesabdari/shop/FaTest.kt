package ir.hesabdari.shop

import ir.hesabdari.shop.util.Fa
import ir.hesabdari.shop.util.PersianWords
import org.junit.Assert.assertEquals
import org.junit.Test

class FaTest {
    @Test
    fun digitConversion() {
        assertEquals("۱۲۳۴۵", Fa.digits("12345"))
        assertEquals("12345", Fa.latinDigits("۱۲۳۴۵"))
        assertEquals("12345", Fa.latinDigits("١٢٣٤٥"))
        assertEquals("1250000", Fa.digitsOnly("۱,۲۵۰٬۰۰۰ تومان"))
    }

    @Test
    fun money() {
        assertEquals("۱٬۲۵۰٬۰۰۰ تومان", Fa.money(1_250_000, "تومان"))
        assertEquals("۰ تومان", Fa.money(0, "تومان"))
        assertEquals("‎-۱٬۰۰۰ تومان", Fa.money(-1000, "تومان"))
        assertEquals("‎+۱٬۰۰۰ تومان", Fa.signedMoney(1000, "تومان"))
        assertEquals("۹۹۹", Fa.number(999))
    }

    @Test
    fun percentAndQuantity() {
        assertEquals("۱۲٫۵٪", Fa.percent(12.5))
        assertEquals("۱۰٪", Fa.percent(10.0))
        assertEquals("‎-۳٫۲٪", Fa.percent(-3.2))
        assertEquals("۰٪", Fa.percent(0.0))
        assertEquals("۲", Fa.quantity(2.0))
        assertEquals("۲٫۵", Fa.quantity(2.5))
    }

    @Test
    fun decimalParsing() {
        assertEquals(2.5, Fa.parseDecimal("۲٫۵")!!, 0.0)
        assertEquals(2.5, Fa.parseDecimal("2.5")!!, 0.0)
        assertEquals(2.5, Fa.parseDecimal("2,5")!!, 0.0)
        assertEquals(null, Fa.parseDecimal("abc"))
    }

    @Test
    fun words() {
        assertEquals("صفر", PersianWords.toWords(0))
        assertEquals("هزار", PersianWords.toWords(1000))
        assertEquals("یک میلیون و دویست و پنجاه هزار", PersianWords.toWords(1_250_000))
        assertEquals("بیست و یک", PersianWords.toWords(21))
        assertEquals("یکصد و پنج", PersianWords.toWords(105))
        assertEquals("دو میلیارد و سیصد میلیون", PersianWords.toWords(2_300_000_000))
    }

    @Test
    fun normalizeText() {
        assertEquals("کیف", Fa.normalizeText("كيف"))
    }
}
