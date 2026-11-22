package uz.ozkey.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LongPressTest {
    @Test fun variantsForUzbekLetters() {
        assertEquals(listOf("ğ", "gʻ"), LongPress.variants("g"))
        assertEquals(listOf("ö", "oʻ"), LongPress.variants("o"))
        assertEquals(listOf("ş", "sh"), LongPress.variants("s"))
        assertEquals(listOf("ç", "ch"), LongPress.variants("c"))
        assertEquals(listOf("’"), LongPress.variants(","))
        assertNull(LongPress.variants("a"))
    }

    @Test fun casing() {
        assertEquals("sh", LongPress.cased("sh", caps = false, capsLock = false))
        assertEquals("Sh", LongPress.cased("sh", caps = true, capsLock = false))
        assertEquals("SH", LongPress.cased("sh", caps = true, capsLock = true))
        assertEquals("Ğ", LongPress.cased("ğ", caps = true, capsLock = false))
        assertEquals("Gʻ", LongPress.cased("gʻ", caps = true, capsLock = false))
        assertEquals("’", LongPress.cased("’", caps = true, capsLock = true))
    }

    @Test fun indexAtClampsToPopup() {
        assertEquals(0, LongPress.indexAt(-50f, 100, 50, 2))
        assertEquals(0, LongPress.indexAt(120f, 100, 50, 2))
        assertEquals(1, LongPress.indexAt(160f, 100, 50, 2))
        assertEquals(1, LongPress.indexAt(900f, 100, 50, 2))
    }
}
