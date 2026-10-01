package ir.hesabdari.shop.util

import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** A date in the Jalali (Shamsi / Persian solar) calendar. */
data class JalaliDate(val year: Int, val month: Int, val day: Int) : Comparable<JalaliDate> {
    val epochDay: Long get() = Jalali.toEpochDay(year, month, day)
    val monthName: String get() = Jalali.monthNames[month - 1]
    val weekdayName: String get() = Jalali.weekdayName(epochDay)

    /** ۱۴۰۴/۰۷/۰۹ */
    fun short(): String = Fa.digits(String.format(Locale.US, "%04d/%02d/%02d", year, month, day))

    /** ۹ مهر ۱۴۰۴ */
    fun long(): String = Fa.digits("$day $monthName $year")

    /** پنجشنبه ۹ مهر ۱۴۰۴ */
    fun full(): String = "$weekdayName ${long()}"

    override fun compareTo(other: JalaliDate): Int =
        compareValuesBy(this, other, { it.year }, { it.month }, { it.day })
}

/**
 * Jalali <-> Gregorian conversion (port of the well known `jalaali-js` algorithm),
 * expressed in terms of epoch days (days since 1970-01-01) which is how dates are stored.
 */
object Jalali {
    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    /** Weekdays starting with Saturday, as in the Iranian calendar. */
    val weekdayNames = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
    val weekdayShort = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    private class Cal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int, withoutLeap: Boolean = false): Cal {
        val bl = breaks.size
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        require(jy >= jp && jy < breaks[bl - 1]) { "Invalid Jalali year $jy" }
        for (i in 1 until bl) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + (jump % 33) / 4
            jp = jm
        }
        var n = jy - jp
        leapJ += (n / 33) * 8 + ((n % 33) + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG
        if (withoutLeap) return Cal(0, gy, march)
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = ((n + 1) % 33 - 1) % 4
        if (leap == -1) leap = 4
        return Cal(leap, gy, march)
    }

    fun isLeap(jy: Int): Boolean = jalCal(jy).leap == 0

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeap(jy)) 30 else 29
    }

    fun toEpochDay(jy: Int, jm: Int, jd: Int): Long {
        val r = jalCal(jy, true)
        return LocalDate.of(r.gy, 3, r.march).toEpochDay() + (jm - 1) * 31 - (jm / 7) * (jm - 7) + jd - 1
    }

    fun fromEpochDay(epochDay: Long): JalaliDate {
        val gy = LocalDate.ofEpochDay(epochDay).year
        var jy = gy - 621
        val r = jalCal(jy)
        val firstOfFarvardin = LocalDate.of(gy, 3, r.march).toEpochDay()
        var k = (epochDay - firstOfFarvardin).toInt()
        if (k >= 0) {
            if (k <= 185) return JalaliDate(jy, 1 + k / 31, k % 31 + 1)
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        return JalaliDate(jy, 7 + k / 30, k % 30 + 1)
    }

    fun todayEpochDay(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    fun today(): JalaliDate = fromEpochDay(todayEpochDay())

    /** 0 = Saturday … 6 = Friday */
    fun weekdayIndex(epochDay: Long): Int {
        val dow = LocalDate.ofEpochDay(epochDay).dayOfWeek.value // Mon=1 … Sun=7
        return (dow + 1) % 7
    }

    fun weekdayName(epochDay: Long): String = weekdayNames[weekdayIndex(epochDay)]

    fun gregorian(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).toString()

    /** First day (epoch) of the week (Saturday) containing [epochDay]. */
    fun startOfWeek(epochDay: Long): Long = epochDay - weekdayIndex(epochDay)

    fun startOfMonth(epochDay: Long): Long {
        val d = fromEpochDay(epochDay)
        return toEpochDay(d.year, d.month, 1)
    }

    fun startOfYear(epochDay: Long): Long = toEpochDay(fromEpochDay(epochDay).year, 1, 1)

    fun endOfYear(epochDay: Long): Long {
        val y = fromEpochDay(epochDay).year
        return toEpochDay(y, 12, monthLength(y, 12))
    }

    fun endOfMonth(epochDay: Long): Long {
        val d = fromEpochDay(epochDay)
        return toEpochDay(d.year, d.month, monthLength(d.year, d.month))
    }
}
