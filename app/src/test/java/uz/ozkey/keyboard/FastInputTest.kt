package uz.ozkey.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tez yozish tartibi: IME ning touch qatlami aynan shu sinflarni ishlatadi. */
class FastInputTest {

    /** IME ning touch oqimini soddalashtirib takrorlaydi: matn va hodisalar tartibi. */
    private class Sim {
        val gate = PendingSpace()
        val text = StringBuilder()
        val stats = HashMap<String, Long>()
        val ks = KeyStats(object : KeyStats.Store {
            override fun add(deltas: Map<String, Long>) { deltas.forEach { (k, d) -> stats[k] = (stats[k] ?: 0L) + d } }
        })

        private fun typeSpace() { ks.onSpace(text.lastOrNull()); text.append(' ') }
        fun letterDown(c: Char) {
            if (gate.otherDown()) typeSpace()
            ks.onType(c.toString(), null); text.append(c)
        }
        fun backspaceDown() {
            if (gate.otherDown()) typeSpace()
            ks.onBackspace(); if (text.isNotEmpty()) text.setLength(text.length - 1)
        }
        fun spaceDown() = gate.spaceDown()
        fun spaceUp(inside: Boolean = true, moved: Boolean = false) { if (gate.spaceUp(inside, moved)) typeSpace() }
    }

    @Test fun normalTapSpaceIsTypedOnceOnRelease() {
        val s = Sim()
        "ab".forEach { s.letterDown(it) }
        s.spaceDown(); s.spaceUp()
        assertEquals("ab ", s.text.toString())
    }

    @Test fun rolloverLetterBeforeSpaceReleaseKeepsOrder() {
        val s = Sim()
        "salom".forEach { s.letterDown(it) }
        s.spaceDown()           // probel bosildi
        s.letterDown('d')       // probel ko'tarilmasdan harf bosildi
        s.spaceUp()             // endi ko'tarildi: ikkinchi marta yozilmasligi kerak
        "unyo".forEach { s.letterDown(it) }
        assertEquals("salom dunyo", s.text.toString())
    }

    @Test fun spaceIsNeverTypedTwice() {
        val s = Sim()
        s.letterDown('a'); s.spaceDown(); s.letterDown('b'); s.letterDown('c'); s.spaceUp()
        assertEquals("a bc", s.text.toString())
    }

    @Test fun backspaceAfterPressedSpaceDeletesTheSpaceNotTheLetter() {
        val s = Sim()
        s.letterDown('a'); s.spaceDown(); s.backspaceDown(); s.spaceUp()
        assertEquals("a", s.text.toString())
    }

    @Test fun slidingOffSpaceOrCancelDoesNotType() {
        val s = Sim()
        s.letterDown('a')
        s.spaceDown(); s.spaceUp(inside = false)
        s.spaceDown(); s.spaceUp(moved = true)
        s.spaceDown(); s.gate.reset(); s.spaceUp()
        assertEquals("a", s.text.toString())
    }

    @Test fun hundredRapidKeysWithRolloverLoseNothing() {
        val s = Sim()
        val expected = StringBuilder()
        repeat(100) { i ->
            val c = 'a' + (i % 26)
            s.letterDown(c); expected.append(c)
            if (i % 5 == 4) { s.spaceDown(); expected.append(' '); if (i % 2 == 0) s.letterDown('x').also { expected.append('x') }; s.spaceUp() }
        }
        s.ks.flush()
        assertEquals(expected.toString(), s.text.toString())
        assertEquals(s.stats["stat_keys"], (s.stats["stat_letters"] ?: 0) + (s.stats["stat_spaces"] ?: 0))
    }

    @Test fun fiveHundredMixedEventsKeepExactOrderAndCounts() {
        val s = Sim()
        val expected = StringBuilder()
        var letters = 0; var spaces = 0; var backs = 0
        val rnd = java.util.Random(42)
        repeat(500) {
            when (rnd.nextInt(10)) {
                in 0..6 -> { val c = 'a' + rnd.nextInt(26); s.letterDown(c); expected.append(c); letters++ }
                7 -> { s.spaceDown(); s.spaceUp(); expected.append(' '); spaces++ }
                8 -> { s.spaceDown(); val c = 'q'; s.letterDown(c); s.spaceUp(); expected.append(' ').append(c); spaces++; letters++ }
                else -> { s.backspaceDown(); if (expected.isNotEmpty()) expected.setLength(expected.length - 1); backs++ }
            }
        }
        s.ks.flush()
        assertEquals(expected.toString(), s.text.toString())
        assertEquals(letters.toLong(), s.stats["stat_letters"])
        assertEquals(spaces.toLong(), s.stats["stat_spaces"])
        assertEquals(backs.toLong(), s.stats["stat_backs"])
        assertEquals((letters + spaces + backs).toLong(), s.stats["stat_keys"])
    }

    @Test fun repeatDeleteAcceleratesAndNeverStalls() {
        assertEquals(55L, RepeatDelay.next(0))
        assertEquals(55L, RepeatDelay.next(9))
        assertEquals(35L, RepeatDelay.next(10))
        assertTrue(RepeatDelay.INITIAL_MS > RepeatDelay.next(0))
        assertFalse((0..1000).any { RepeatDelay.next(it) <= 0L })
    }
}
