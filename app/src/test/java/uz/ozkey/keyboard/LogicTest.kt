package uz.ozkey.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Toza mantiq (Android'siz): maxfiy matn aniqlash, so'z tahlili, qo'shni tugmalar, xavfsiz avto-tuzatish. */
class LogicTest {

    // ---- ClipStore.looksSensitive ----
    @Test fun otpLikeNumbersAreSensitive() {
        assertTrue(ClipStore.looksSensitive("1234"))
        assertTrue(ClipStore.looksSensitive("123456"))
    }

    @Test fun validCardNumberIsSensitiveInvalidIsNot() {
        assertTrue(ClipStore.looksSensitive("4111 1111 1111 1111"))
        assertFalse(ClipStore.looksSensitive("4111 1111 1111 1112"))
    }

    @Test fun ordinaryTextIsNotSensitive() {
        assertFalse(ClipStore.looksSensitive("hello"))
        assertFalse(ClipStore.looksSensitive("123"))
        assertFalse(ClipStore.looksSensitive("123456789"))
        assertFalse(ClipStore.looksSensitive("+998901234567"))
    }

    // ---- WordModel.parse ----
    @Test fun parseSplitsPrefixAndPreviousWord() {
        val c = WordModel.parse("salom dunyo")
        assertEquals("dunyo", c.prefix)
        assertEquals("salom", c.prev)
        assertFalse(c.sentenceStart)
    }

    @Test fun parseDetectsSentenceStart() {
        assertTrue(WordModel.parse("").sentenceStart)
        val c = WordModel.parse("Salom. ")
        assertEquals("", c.prefix)
        assertNull(c.prev)
        assertTrue(c.sentenceStart)
    }

    // ---- WordModel.buildNeighbors ----
    @Test fun neighborsFollowKeyboardGeometry() {
        val rows = listOf("qwe".map { it.toString() }, "asd".map { it.toString() })
        val nb = WordModel.buildNeighbors(rows)
        assertEquals(setOf('w', 'a'), nb['q'])
        assertEquals(setOf('s', 'q', 'w'), nb['a'])
    }

    // ---- WordModel.autoFix (o'rnatilgan lug'at resetForTest bilan beriladi; maqsad faqat shu lug'atdan) ----
    @Test fun unknownWordIsNeverChangedWhenNoDictionaryMatch() {
        WordModel.resetForTest(listOf("bugun", "kitob"))
        assertNull(WordModel.autoFix("qwxzp", null, emptyMap()))
    }

    @Test fun adjacentLetterSwapToKnownWordIsFixed() {
        WordModel.resetForTest(listOf("bugun", "kitob"))
        assertEquals("bugun", WordModel.autoFix("bugnu", null, emptyMap()))
    }

    @Test fun keptWordIsNotCorrectedAgain() {
        WordModel.resetForTest(listOf("bugun", "kitob"))
        assertEquals("kitob", WordModel.autoFix("kitbo", null, emptyMap()))
        WordModel.keep("kitbo")   // foydalanuvchi ⌫ bilan tuzatishni bekor qildi
        assertNull(WordModel.autoFix("kitbo", null, emptyMap()))
    }

    @Test fun shortAndNonLetterWordsAreNotCorrected() {
        WordModel.resetForTest(listOf("bugun", "kitob", "kuni"))
        assertNull(WordModel.autoFix("abc", null, emptyMap()))
        assertNull(WordModel.autoFix("ab12", null, emptyMap()))
        assertNull(WordModel.autoFix("kniu", null, emptyMap()))   // 4 harf: juda xavfli, tegilmaydi
    }
}
