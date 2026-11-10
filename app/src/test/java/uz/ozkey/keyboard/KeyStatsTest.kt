package uz.ozkey.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyStatsTest {
    /** Xotirada "SharedPreferences": ilova qayta ochilganda ham saqlanadi. */
    private class Mem : KeyStats.Store {
        val data = HashMap<String, Long>()
        override fun add(deltas: Map<String, Long>) {
            deltas.forEach { (k, d) -> data[k] = ((data[k] ?: 0L) + d).coerceAtLeast(0L) }
        }
        fun v(k: String) = data[k] ?: 0L
    }

    /** "salom dunyo" ni IME kabi yozadi: oldingi belgi har safar matndan olinadi. */
    private fun type(st: KeyStats, text: StringBuilder, s: String) {
        for (ch in s) {
            val prev = text.lastOrNull()
            when (ch) {
                ' ' -> st.onSpace(prev)
                '\n' -> st.onEnter(prev)
                else -> st.onType(ch.toString(), prev)
            }
            text.append(ch)
        }
    }

    @Test fun lettersAreCounted() {
        val m = Mem(); val st = KeyStats(m); val t = StringBuilder()
        type(st, t, "salom"); st.flush()
        assertEquals(5, m.v("stat_keys")); assertEquals(5, m.v("stat_letters"))
        assertEquals(0, m.v("stat_words"))   // so'z hali tugamagan
    }

    @Test fun spaceEndsWordOnce() {
        val m = Mem(); val st = KeyStats(m); val t = StringBuilder()
        type(st, t, "salom dunyo "); st.flush()
        assertEquals(2, m.v("stat_words")); assertEquals(2, m.v("stat_spaces"))
        assertEquals(12, m.v("stat_keys"))
    }

    @Test fun repeatedSpacesDoNotCountExtraWords() {
        val m = Mem(); val st = KeyStats(m); val t = StringBuilder()
        type(st, t, "salom    "); st.flush()
        assertEquals(1, m.v("stat_words")); assertEquals(4, m.v("stat_spaces"))
    }

    @Test fun enterEndsWordAndIsCountedOnceAsEnter() {
        val m = Mem(); val st = KeyStats(m); val t = StringBuilder()
        type(st, t, "salom\n"); st.flush()
        assertEquals(1, m.v("stat_words")); assertEquals(1, m.v("stat_enters")); assertEquals(0, m.v("stat_spaces"))
        assertEquals(6, m.v("stat_keys"))
    }

    @Test fun enterOnEmptyLineOrAfterSpaceAddsNoWord() {
        val m = Mem(); val st = KeyStats(m); val t = StringBuilder()
        type(st, t, "\n"); type(st, t, "salom \n"); st.flush()
        assertEquals(1, m.v("stat_words")); assertEquals(2, m.v("stat_enters"))
    }

    @Test fun punctuationEndsWordButFollowingSpaceDoesNotRecountIt() {
        val m = Mem(); val st = KeyStats(m); val t = StringBuilder()
        type(st, t, "salom, dunyo. "); st.flush()
        assertEquals(2, m.v("stat_words"))
        assertEquals(2, m.v("stat_spaces"))
    }

    @Test fun backspaceIsCountedAsBackspaceNotLetter() {
        val m = Mem(); val st = KeyStats(m); val t = StringBuilder()
        type(st, t, "ab"); st.onBackspace(); st.flush()
        assertEquals(1, m.v("stat_backs")); assertEquals(2, m.v("stat_letters")); assertEquals(3, m.v("stat_keys"))
    }

    @Test fun deletingBoundaryAndRetypingDoesNotCountWordTwice() {
        val m = Mem(); val st = KeyStats(m); val t = StringBuilder()
        type(st, t, "salom ")        // 1 so'z
        st.onBackspace(); t.setLength(t.length - 1)   // probel o'chdi: so'z yana ochiq
        type(st, t, " ")             // qayta probel
        st.flush()
        assertEquals(1, m.v("stat_words"))
        assertEquals(1, m.v("stat_backs"))
    }

    @Test fun backspaceAfterLetterNeverReducesWords() {
        val m = Mem(); val st = KeyStats(m); val t = StringBuilder()
        type(st, t, "salom "); type(st, t, "d"); st.onBackspace(); st.flush()
        assertEquals(1, m.v("stat_words"))
    }

    @Test fun countersPersistAcrossReopenAndAccumulate() {
        val m = Mem()
        var st = KeyStats(m); var t = StringBuilder()
        type(st, t, "salom "); st.flush()
        st = KeyStats(m); t = StringBuilder()          // ilova qayta ochildi: yangi KeyStats, o'sha xotira
        type(st, t, "dunyo "); st.flush()
        assertEquals(12, m.v("stat_keys")); assertEquals(2, m.v("stat_words"))
    }

    @Test fun autoFlushHappensEveryNEventsAndFlushDoesNotDoubleCount() {
        val m = Mem(); val st = KeyStats(m, flushEvery = 5); val t = StringBuilder()
        type(st, t, "abcdefghij")      // 10 hodisa -> 2 marta avtomatik yoziladi
        assertEquals(10, m.v("stat_keys"))
        st.flush(); st.flush()         // takroriy flush qo'shimcha sanamaydi
        assertEquals(10, m.v("stat_keys"))
    }

    @Test fun negativeTotalIsNeverStored() {
        val m = Mem(); val st = KeyStats(m, flushEvery = 1); val t = StringBuilder()
        type(st, t, "ab ")             // 1 so'z yozildi va yozib qo'yildi (flushEvery=1)
        m.data["stat_words"] = 0L      // foydalanuvchi hisoblagichni nolga qaytardi
        st.onBackspace()               // so'z -1 bo'lardi
        assertEquals(0, m.v("stat_words"))
    }
}
