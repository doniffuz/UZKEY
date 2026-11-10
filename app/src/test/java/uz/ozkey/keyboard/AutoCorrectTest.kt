package uz.ozkey.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Avto-tuzatish xavfsizligi: lug'atda yo'q so'z va to'g'ri yozilgan so'z hech qachon o'zgarmaydi. */
class AutoCorrectTest {
    private val none = emptyMap<Char, Set<Char>>()

    @Before fun dict() {
        WordModel.resetForTest(listOf("bugun", "kitob", "salom", "maktab", "bilan", "olma"))
    }

    private fun fix(before: String, after: Char? = null) = AutoCorrect.plan(before, after, none)?.corrected

    @Test fun transpositionToDictionaryWordIsFixed() {
        assertEquals("bugun", fix("bugnu"))
        assertEquals("kitob", fix("men kitbo"))
    }

    @Test fun correctWordIsNeverChanged() {
        assertNull(fix("bugun"))
        assertNull(fix("salom"))
    }

    @Test fun wordNotInDictionaryIsNeverReplaced() {
        assertNull(fix("qwxzp"))
        assertNull(fix("xyzabc"))
        assertNull(fix("telefon"))
    }

    @Test fun transpositionWhoseResultIsNotInDictionaryIsNotFixed() {
        assertNull(fix("kitbao"))      // kitbao -> kitabo emas, lug'atda yo'q
        assertNull(fix("mkatba"))      // 2 ta almashish kerak
    }

    @Test fun userLearnedWordsAreNeverCorrectionTargets() {
        repeat(3) { WordModel.learn("bugnu", null) }   // foydalanuvchi xatoni ko'p yozgan: endi "lug'at"da
        assertNull(fix("bugnu"))                        // o'zi ham tegilmaydi (u o'rgangan so'z)
        WordModel.learn("tozaa", null); WordModel.learn("tozaa", null)
        assertNull(WordModel.autoFix("tozaa", null, none))
    }

    @Test fun ambiguousCandidatesAreNotFixed() {
        WordModel.resetForTest(listOf("sabra", "sabab"))
        assertNull(fix("sabar"))       // "sabar" -> sabra (juftlik almashishi) va sabab (harf almashtirish): noaniq
        WordModel.resetForTest(listOf("sabra"))
        assertEquals("sabra", fix("sabar"))   // nazorat: bitta nomzod bo'lsa tuzatiladi
    }

    @Test fun upperCaseAndMixedCaseWordsAreLeftAlone() {
        assertNull(fix("BuGnu"))
        assertNull(fix("hozir Bugnu"))          // gap o'rtasida bosh harf (ism bo'lishi mumkin)
        assertEquals("Bugun", fix("Bugnu"))     // gap boshida: bosh harf saqlanib tuzatiladi
        assertEquals("BUGUN", fix("BUGNU"))
    }

    @Test fun urlsEmailsAndNumbersAreLeftAlone() {
        assertNull(fix("www.bugnu"))
        assertNull(fix("ali@bugnu"))
        assertNull(fix("#bugnu"))
        assertNull(fix("2bugnu"))
        assertNull(fix("o'bugnu"))
    }

    @Test fun editingInsideAWordIsLeftAlone() {
        assertNull(fix("bugnu", after = 'x'))
        assertNotNull(fix("bugnu", after = ' '))
    }

    @Test fun punctuationBeforeWordDoesNotBlockCorrectionAndIsNotPartOfIt() {
        val p = AutoCorrect.plan("Salom, bugnu", null, none)
        assertEquals("bugnu", p?.original)
        assertEquals("bugun", p?.corrected)
    }

    @Test fun apostropheWordsAreNotChanged() {
        assertNull(fix("o'zbek"))
        assertNull(fix("g'oya"))
    }

    @Test fun suggestionsStillWorkWithoutAutoReplace() {
        // taklif (suggestions) saqlangan: noto'g'ri yozilgan so'z uchun lug'at so'zlari taklif qilinadi
        assertEquals(listOf("kitob"), WordModel.complete("kitbo", null))
        assertEquals(listOf("salom"), WordModel.complete("sal", null))
    }
}
