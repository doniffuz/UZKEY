package uz.ozkey.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Yangi alifbo (Ş Ç Ğ Ö) <-> standart lotin. C+H → Ç mantig'i `Alphabet.toNew` bilan bir xil qoidaga ega. */
class AlphabetTest {

    @Test fun shBecomesS_cedilla() {
        assertEquals("şahar", Alphabet.toNew("shahar"))
        assertEquals("Şahar", Alphabet.toNew("Shahar"))
    }

    @Test fun chBecomesC_cedilla_lowerAndUpper() {
        assertEquals("çoy", Alphabet.toNew("choy"))
        assertEquals("Çoy", Alphabet.toNew("Choy"))
        assertEquals("ÇOY", Alphabet.toNew("CHOY"))
    }

    @Test fun apostropheLettersBecomeGAndO() {
        assertEquals("ğozal", Alphabet.toNew("g'ozal"))
        assertEquals("özbek", Alphabet.toNew("o'zbek"))
        assertEquals("özbek", Alphabet.toNew("o\u2018zbek"))
        assertEquals("Ğ", Alphabet.toNew("G'"))
    }

    @Test fun consecutiveDigraphsAreAllConverted() {
        assertEquals("işçi", Alphabet.toNew("ishchi"))
    }

    @Test fun textWithoutDigraphsIsUnchanged() {
        assertEquals("salom dunyo", Alphabet.toNew("salom dunyo"))
        assertEquals("cat", Alphabet.toNew("cat"))
    }

    @Test fun toStandardRestoresDigraphs() {
        assertEquals("shahar", Alphabet.toStandard("şahar"))
        assertEquals("Shahar", Alphabet.toStandard("Şahar"))
        assertEquals("CHOY", Alphabet.toStandard("ÇOY"))
        assertEquals("g" + Alphabet.STD_APOS + "ozal", Alphabet.toStandard("ğozal"))
        assertEquals("o" + Alphabet.STD_APOS + "zbek", Alphabet.toStandard("özbek"))
    }

    @Test fun hasNewDetectsOnlyNewLetters() {
        assertTrue(Alphabet.hasNew("çoy"))
        assertTrue(Alphabet.hasNew("Ğ"))
        assertFalse(Alphabet.hasNew("choy"))
    }

    @Test fun cyrillicToLatinAndBackKeepsNewLetters() {
        assertEquals("ч", Script.latToCyr("ç"))
        assertEquals("Ч", Script.latToCyr("Ç"))
        assertEquals("ш", Script.latToCyr("ş"))
    }

    // IME convertDigraph() aynan shu Alphabet.toNew ni chaqiradi (oxirgi 2 belgi bilan)
    @Test fun cPlusHBecomesC_cedillaForImeTailPair() {
        assertEquals("ç", Alphabet.toNew("ch"))
        assertEquals("Ç", Alphabet.toNew("Ch"))
        assertEquals("Ç", Alphabet.toNew("CH"))
    }

    @Test fun cPlusOtherLetterIsNotConverted() {
        assertEquals("ca", Alphabet.toNew("ca"))
        assertEquals("c'", Alphabet.toNew("c'"))
        assertEquals("hc", Alphabet.toNew("hc"))
    }

    @Test fun cPlusHRoundTripsToStandard() {
        assertEquals("ch", Alphabet.toStandard("ç"))
    }
}
