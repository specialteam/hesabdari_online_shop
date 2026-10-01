package ir.hesabdari.shop

import ir.hesabdari.shop.util.Jalali
import ir.hesabdari.shop.util.JalaliDate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliTest {
    private fun epoch(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test
    fun knownConversions() {
        assertEquals(JalaliDate(1404, 1, 1), Jalali.fromEpochDay(epoch(2025, 3, 21)))
        assertEquals(JalaliDate(1403, 12, 30), Jalali.fromEpochDay(epoch(2025, 3, 20)))
        assertEquals(JalaliDate(1403, 1, 1), Jalali.fromEpochDay(epoch(2024, 3, 20)))
        assertEquals(JalaliDate(1402, 1, 1), Jalali.fromEpochDay(epoch(2023, 3, 21)))
        assertEquals(JalaliDate(1404, 7, 9), Jalali.fromEpochDay(epoch(2025, 10, 1)))
        assertEquals(JalaliDate(1399, 12, 30), Jalali.fromEpochDay(epoch(2021, 3, 20)))
        assertEquals(epoch(2025, 10, 1), JalaliDate(1404, 7, 9).epochDay)
    }

    @Test
    fun roundTripOverManyYears() {
        var day = epoch(1990, 1, 1)
        val end = epoch(2070, 1, 1)
        while (day < end) {
            val j = Jalali.fromEpochDay(day)
            assertTrue("month ${j}", j.month in 1..12)
            assertTrue("day ${j}", j.day in 1..Jalali.monthLength(j.year, j.month))
            assertEquals(day, Jalali.toEpochDay(j.year, j.month, j.day))
            day += 1
        }
    }

    @Test
    fun leapYears() {
        assertTrue(Jalali.isLeap(1403))
        assertTrue(Jalali.isLeap(1399))
        assertFalse(Jalali.isLeap(1404))
        assertEquals(30, Jalali.monthLength(1403, 12))
        assertEquals(29, Jalali.monthLength(1404, 12))
        assertEquals(31, Jalali.monthLength(1404, 6))
        assertEquals(30, Jalali.monthLength(1404, 7))
    }

    @Test
    fun weekdays() {
        // 2025-10-01 was a Wednesday -> چهارشنبه
        assertEquals("چهارشنبه", Jalali.weekdayName(epoch(2025, 10, 1)))
        // 2025-03-22 was a Saturday
        assertEquals(0, Jalali.weekdayIndex(epoch(2025, 3, 22)))
        assertEquals(6, Jalali.weekdayIndex(epoch(2025, 3, 21)))
    }

    @Test
    fun formatting() {
        assertEquals("۱۴۰۴/۰۷/۰۹", JalaliDate(1404, 7, 9).short())
        assertEquals("۹ مهر ۱۴۰۴", JalaliDate(1404, 7, 9).long())
    }

    @Test
    fun periodsBounds() {
        val d = epoch(2025, 10, 1) // 1404/07/09
        assertEquals(JalaliDate(1404, 7, 1), Jalali.fromEpochDay(Jalali.startOfMonth(d)))
        assertEquals(JalaliDate(1404, 7, 30), Jalali.fromEpochDay(Jalali.endOfMonth(d)))
        assertEquals(JalaliDate(1404, 1, 1), Jalali.fromEpochDay(Jalali.startOfYear(d)))
        assertEquals(JalaliDate(1404, 12, 29), Jalali.fromEpochDay(Jalali.endOfYear(d)))
        // week starts on Saturday: 1404/07/06 is Saturday
        assertEquals(JalaliDate(1404, 7, 5), Jalali.fromEpochDay(Jalali.startOfWeek(d)))
    }
}
