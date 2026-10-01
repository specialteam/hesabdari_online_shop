package ir.hesabdari.shop.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Persian-flavoured formatting helpers (digits, money, percent, text normalisation). */
object Fa {
    private const val PERSIAN_ZERO = '۰'
    private const val ARABIC_ZERO = '٠'
    private const val LRM = "‎"
    const val THOUSANDS = "٬"
    const val DECIMAL = "٫"

    /** Latin digits -> Persian digits. */
    fun digits(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(if (c in '0'..'9') PERSIAN_ZERO + (c - '0') else c)
        return sb.toString()
    }

    /** Persian / Arabic-Indic digits -> Latin digits; also unifies Arabic ي ك to Persian ی ک for search. */
    fun latinDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            sb.append(
                when (c) {
                    in '۰'..'۹' -> '0' + (c - PERSIAN_ZERO)
                    in '٠'..'٩' -> '0' + (c - ARABIC_ZERO)
                    else -> c
                },
            )
        }
        return sb.toString()
    }

    fun normalizeText(s: String): String =
        latinDigits(s).replace('ي', 'ی').replace('ك', 'ک').replace("‌", " ").lowercase()

    /** Keeps only the digits of a user typed number (accepts Persian digits and separators). */
    fun digitsOnly(s: String): String = latinDigits(s).filter { it in '0'..'9' }

    /** Parses a decimal number typed by the user (Persian digits, "٫" or "." or "," as the decimal mark). */
    fun parseDecimal(s: String): Double? {
        val t = latinDigits(s).replace('٫', '.').replace(',', '.').replace("٬", "").trim()
        return t.toDoubleOrNull()
    }

    private fun group(n: Long): String {
        val raw = abs(n).toString()
        val sb = StringBuilder()
        for ((i, ch) in raw.withIndex()) {
            if (i > 0 && (raw.length - i) % 3 == 0) sb.append(THOUSANDS)
            sb.append(ch)
        }
        return digits(sb.toString())
    }

    /** ۱٬۲۵۰٬۰۰۰ (negative numbers get a leading minus that stays on the left of the digits). */
    fun number(n: Long): String = if (n < 0) "$LRM-${group(n)}" else group(n)

    fun money(n: Long, unit: String): String = "${number(n)} $unit"

    /** Signed money: "+" for positive values (used for profit/diff). */
    fun signedMoney(n: Long, unit: String): String =
        if (n > 0) "$LRM+${group(n)} $unit" else money(n, unit)

    fun quantity(q: Double): String {
        if (q == q.roundToLong().toDouble()) return digits(q.roundToLong().toString())
        return digits(String.format(Locale.US, "%.3f", q).trimEnd('0').trimEnd('.')).replace(".", DECIMAL)
    }

    fun percent(p: Double, fractionDigits: Int = 1): String {
        var body = String.format(Locale.US, "%.${fractionDigits}f", abs(p))
        if (body.contains('.')) body = body.trimEnd('0').trimEnd('.')
        val negative = p < 0 && body.any { it in '1'..'9' }
        return (if (negative) "$LRM-" else "") + digits(body).replace(".", DECIMAL) + "٪"
    }

    /** Plain grouped digits typed in an input (Latin digits kept, so the field stays editable). */
    fun groupedLatin(digits: String): String {
        val sb = StringBuilder()
        for ((i, ch) in digits.withIndex()) {
            if (i > 0 && (digits.length - i) % 3 == 0) sb.append(',')
            sb.append(ch)
        }
        return sb.toString()
    }
}

/** Amount in Persian words ("یک میلیون و دویست هزار"). */
object PersianWords {
    private val ones = listOf(
        "", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه", "ده", "یازده", "دوازده",
        "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده",
    )
    private val tens = listOf("", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    private val hundreds = listOf(
        "", "یکصد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد",
    )
    private val scales = listOf("", "هزار", "میلیون", "میلیارد", "تریلیون", "کوادریلیون")

    private fun below1000(n: Int): String {
        val parts = mutableListOf<String>()
        val h = n / 100
        val r = n % 100
        if (h > 0) parts += hundreds[h]
        if (r > 0) {
            if (r < 20) {
                parts += ones[r]
            } else {
                val t = tens[r / 10]
                val o = r % 10
                parts += if (o == 0) t else "$t و ${ones[o]}"
            }
        }
        return parts.joinToString(" و ")
    }

    fun toWords(n: Long): String {
        if (n == 0L) return "صفر"
        if (n < 0) return "منفی " + toWords(-n)
        val groups = mutableListOf<Int>()
        var rest = n
        while (rest > 0) {
            groups += (rest % 1000).toInt()
            rest /= 1000
        }
        val parts = mutableListOf<String>()
        for (i in groups.indices.reversed()) {
            val g = groups[i]
            if (g == 0) continue
            parts += when {
                i == 0 -> below1000(g)
                i == 1 && g == 1 -> scales[1]
                else -> below1000(g) + " " + scales[i]
            }
        }
        return parts.joinToString(" و ")
    }
}
