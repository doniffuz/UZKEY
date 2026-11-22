package uz.ozkey.keyboard

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Zaxira shifrlash: v1 (SHA1, eski) o'qiladi, v2 (SHA256) yoziladi. Toza JVM, Android'siz. */
class ClipBackupTest {

    private val json = "{\"v\": 1, \"items\": [{\"text\": \"Salom, dunyo! o\u2018zbek\", \"cat\": \"Ish\"}]}"

    // Python (hashlib + cryptography) bilan, ClipBackup kodisiz yaratilgan mustaqil fixture'lar.
    // v1: parol "eski-parol-123", PBKDF2-HMAC-SHA1 x120000 (v6 formati)
    private val v1Fixture =
        "T1pLQjEBAgMEBQYHCAkKCwwNDg8QZWZnaGlqa2xtbm9wunYhMt6c48LUcWdr+IWs9mN6/QhNKyNt9DuR5NX771T4qEjA4G2LGG8/" +
            "OvLs7aK4IX4PuSqWw3g0OXoDIyTC5nJN4mltSzkEJvBIr8kbQGzm0ckKWA=="
    // v2: parol "yangi-parol-456", PBKDF2-HMAC-SHA256 x600000, sarlavha AAD
    private val v2Fixture =
        "T1pLQjIACSfAFRYXGBkaGxwdHh8gISIjJDM0NTY3ODk6Ozw9PkEjIMbyprQIfOUwXgIU7OyFLG246csi18J/Ii5TChCvxP9iRxy91Xhbo0xxeI5QPkZvdM7J+/MC0JY6HLZ2/m/7VrMH4WoaIU26DoPeStn2F/FYM6k="

    // v2, Python bilan: parol NFC("caf\u00e9-o\u2018zbek-\u043f\u0430\u0440\u043e\u043b\u044c"), PBKDF2-HMAC-SHA256 x100000 (parol UTF-8)
    private val v2UnicodeFixture =
        "T1pLQjIAAYagUFFSU1RVVldYWVpbXF1eX3h5ent8fX5/gIGCg1hacTiXp6NGsrJ5w4gVSlbOm4VflWKNt4eu9qIRoSrK9rV4TeEBvX8MexfFxEieMQ0nr770sDTllgHZ06BMnPdp2eMCPnd8YSMeY/RtLE/4UIZ6jBw="
    private val unicodePwNfc = "caf\u00e9-o\u2018zbek-\u043f\u0430\u0440\u043e\u043b\u044c"
    private val unicodePwNfd = "cafe\u0301-o\u2018zbek-\u043f\u0430\u0440\u043e\u043b\u044c"

    private fun b64(s: String): ByteArray = Base64.getDecoder().decode(s)

    // ---- 1. Eski zaxirani ochish ----

    @Test fun legacyV1FixtureStillOpens() {
        val data = b64(v1Fixture)
        assertEquals(ClipBackup.VERSION_V1, ClipBackup.detectVersion(data))
        assertEquals(json, ClipBackup.decrypt(data, "eski-parol-123"))
    }

    @Test fun legacyV1BuiltWithOldAlgorithmOpens() {
        // v6 kodining aynan o'zidagi algoritm: "OZKB1" + tuz + iv + AES-GCM, PBKDF2WithHmacSHA1 x120000
        val salt = ByteArray(16) { (it + 7).toByte() }
        val iv = ByteArray(12) { (it + 70).toByte() }
        val spec = PBEKeySpec("parol-123".toCharArray(), salt, 120_000, 256)
        val key = SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded, "AES")
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val data = "OZKB1".toByteArray(Charsets.US_ASCII) + salt + iv + c.doFinal(json.toByteArray(Charsets.UTF_8))
        assertEquals(json, ClipBackup.decrypt(data, "parol-123"))
        assertNull(ClipBackup.decrypt(data, "parol-124"))
    }

    // ---- 2. Yangi zaxirani yaratish va qayta ochish ----

    @Test fun newBackupIsV2AndRoundTrips() {
        val data = ClipBackup.encrypt(json, "yangi-parol-456")
        assertEquals(ClipBackup.VERSION_V2, ClipBackup.detectVersion(data))
        assertEquals("OZKB2", String(data.copyOfRange(0, 5), Charsets.US_ASCII))
        // iteratsiya soni sarlavhada (big-endian) saqlanadi
        val iter = ((data[5].toInt() and 0xFF) shl 24) or ((data[6].toInt() and 0xFF) shl 16) or
            ((data[7].toInt() and 0xFF) shl 8) or (data[8].toInt() and 0xFF)
        assertEquals(ClipBackup.ITER_V2, iter)
        assertTrue(ClipBackup.ITER_V2 >= 600_000)
        assertEquals(json, ClipBackup.decrypt(data, "yangi-parol-456"))
    }

    @Test fun v2FixtureFromIndependentImplementationOpens() {
        assertEquals(json, ClipBackup.decrypt(b64(v2Fixture), "yangi-parol-456"))
    }

    @Test fun twoBackupsOfSameDataDiffer() {
        val a = ClipBackup.encrypt(json, "bir-parol")
        val b = ClipBackup.encrypt(json, "bir-parol")
        assertFalse(a.contentEquals(b)) // tuz va iv tasodifiy
    }

    @Test fun nonAsciiPasswordRoundTrips() {
        val pw = "parol\u2018o\u2018zbek-\u0448\u0438\u0444\u0440"
        val data = ClipBackup.encryptV2(json, pw, ClipBackup.MIN_ITER_V2)
        assertEquals(json, ClipBackup.decrypt(data, pw))
        assertNull(ClipBackup.decrypt(data, "parol'o'zbek-shifr"))
    }

    @Test fun manualPbkdf2MatchesRfcVectorAndSystemImplementation() {
        // RFC 7914 §11: PBKDF2-HMAC-SHA256("passwd", "salt", c=1, dkLen=64) boshlanishi
        val rfc = ClipBackup.pbkdf2HmacSha256("passwd".toByteArray(), "salt".toByteArray(), 1, 64)
        assertEquals("55ac046e56e3089fec1691c22544b605", rfc.copyOf(16).joinToString("") { "%02x".format(it) })

        val salt = ByteArray(16) { it.toByte() }
        val sys = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec("sinov-parol".toCharArray(), salt, 5_000, 256)).encoded
        assertArrayEquals(sys, ClipBackup.pbkdf2HmacSha256("sinov-parol".toByteArray(), salt, 5_000, 32))
    }

    // ---- 3. Noto'g'ri parol ----

    @Test fun wrongPasswordReturnsNullForBothVersions() {
        assertNull(ClipBackup.decrypt(b64(v1Fixture), "noto'g'ri"))
        assertNull(ClipBackup.decrypt(b64(v2Fixture), "noto'g'ri"))
        assertNull(ClipBackup.decrypt(ClipBackup.encrypt(json, "to'g'ri-parol"), "to'g'ri-parol "))
        assertNull(ClipBackup.decrypt(b64(v2Fixture), ""))
    }

    // ---- 4. Buzilgan fayl ----

    @Test fun corruptedFilesReturnNullAndNeverThrow() {
        val v2 = b64(v2Fixture)
        val v1 = b64(v1Fixture)
        val pw2 = "yangi-parol-456"
        val pw1 = "eski-parol-123"

        fun flipped(src: ByteArray, i: Int) = src.copyOf().also { it[i] = (it[i].toInt() xor 0x01).toByte() }

        // shifrmatn / tag buzilgan
        assertNull(ClipBackup.decrypt(flipped(v2, v2.size - 1), pw2))
        assertNull(ClipBackup.decrypt(flipped(v2, 40), pw2))
        assertNull(ClipBackup.decrypt(flipped(v1, v1.size - 1), pw1))
        // sarlavha (iteratsiya, tuz) AAD orqali himoyalangan
        assertNull(ClipBackup.decrypt(flipped(v2, 8), pw2))
        assertNull(ClipBackup.decrypt(flipped(v2, 12), pw2))
        // kesilgan fayllar
        for (n in listOf(0, 1, 4, 5, 8, 9, 20, 32, 40, 44, 48, v2.size - 1)) {
            assertNull("v2 n=$n", ClipBackup.decrypt(v2.copyOf(n), pw2))
        }
        for (n in listOf(0, 3, 5, 20, 33, 44, v1.size - 1)) {
            assertNull("v1 n=$n", ClipBackup.decrypt(v1.copyOf(n), pw1))
        }
        // ÖZKEY zaxirasi emas / axlat
        assertNull(ClipBackup.decrypt(ByteArray(0), pw2))
        assertNull(ClipBackup.decrypt("hello world, this is not a backup file at all".toByteArray(), pw2))
        assertNull(ClipBackup.decrypt(ByteArray(200) { (it * 31).toByte() }, pw2))
        assertNull(ClipBackup.detectVersion(ByteArray(10)))
    }

    @Test fun absurdIterationCountInHeaderIsRejectedQuickly() {
        val v2 = b64(v2Fixture)
        val huge = v2.copyOf().also { it[5] = 0x7F; it[6] = 0xFF.toByte(); it[7] = 0xFF.toByte(); it[8] = 0xFF.toByte() }
        val zero = v2.copyOf().also { it[5] = 0; it[6] = 0; it[7] = 0; it[8] = 0 }
        val t0 = System.nanoTime()
        assertNull(ClipBackup.decrypt(huge, "yangi-parol-456"))
        assertNull(ClipBackup.decrypt(zero, "yangi-parol-456"))
        assertTrue("sarlavha tekshiruvi KDF'dan oldin bo'lishi kerak", (System.nanoTime() - t0) / 1_000_000 < 2_000)
    }

    // ---- Kalit/parol loglanmasin ----

    @Test fun backupSourceHasNoLoggingOrPrinting() {
        val f = findUp("app/src/main/java/uz/ozkey/keyboard/ClipBackup.kt")
        assertNotNull("ClipBackup.kt topilmadi", f)
        val code = f!!.readLines().filterNot { it.trimStart().startsWith("*") || it.trimStart().startsWith("//") || it.trimStart().startsWith("/*") }
            .joinToString("\n")
        for (bad in listOf("android.util.Log", "Log.", "println(", "print(", "printStackTrace", "Timber")) {
            assertFalse("ClipBackup.kt ichida '$bad' bo'lmasligi kerak", code.contains(bad))
        }
    }

    // ---- Eksport natijasi plaintext JSON'ni oshkor qilmasin ----

    @Test fun ciphertextDoesNotContainPlaintext() {
        val data = ClipBackup.encrypt(json, "yashirin-parol")
        assertFalse(String(data, Charsets.ISO_8859_1).contains("Salom"))
        assertNotEquals(json.length, data.size)
    }


    // ---- Unicode parollar ----

    @Test fun unicodeFixtureFromPythonOpensWithNfcAndNfdPassword() {
        val data = b64(v2UnicodeFixture)
        assertEquals(json, ClipBackup.decrypt(data, unicodePwNfc))
        // boshqa klaviatura bir xil belgini NFD ko'rinishida yuborsa ham ochilishi kerak
        assertEquals(json, ClipBackup.decrypt(data, unicodePwNfd))
        assertNull(ClipBackup.decrypt(data, "cafe-o\u2018zbek-\u043f\u0430\u0440\u043e\u043b\u044c"))
    }

    @Test fun nfdPasswordBackupOpensWithNfcPasswordAndViceVersa() {
        val data = ClipBackup.encryptV2(json, unicodePwNfd, ClipBackup.MIN_ITER_V2)
        assertEquals(json, ClipBackup.decrypt(data, unicodePwNfc))
        assertEquals(json, ClipBackup.decrypt(data, unicodePwNfd))
    }

    @Test fun v7StyleBackupWithUnnormalizedPasswordStillOpens() {
        // v7 parolni normallashtirmasdan yozgan: kalit = PBKDF2-SHA256(UTF-8 xom parol). Fallback buni ochishi kerak.
        val salt = ByteArray(16) { (it + 3).toByte() }
        val iv = ByteArray(12) { (it + 90).toByte() }
        val iter = ClipBackup.MIN_ITER_V2
        val header = "OZKB2".toByteArray(Charsets.US_ASCII) +
            byteArrayOf((iter ushr 24).toByte(), (iter ushr 16).toByte(), (iter ushr 8).toByte(), iter.toByte()) + salt
        val raw = ClipBackup.pbkdf2HmacSha256(unicodePwNfd.toByteArray(Charsets.UTF_8), salt, iter, 32)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(raw, "AES"), GCMParameterSpec(128, iv))
        c.updateAAD(header)
        val data = header + iv + c.doFinal(json.toByteArray(Charsets.UTF_8))
        assertEquals(json, ClipBackup.decrypt(data, unicodePwNfd))
    }

    // ---- AES-GCM AAD ----

    @Test fun headerIsAuthenticatedAsAad() {
        val data = b64(v2Fixture)
        val key = ClipBackup.deriveSha256("yangi-parol-456", data.copyOfRange(9, 25), ClipBackup.ITER_V2)
        val iv = data.copyOfRange(25, 37)
        val ct = data.copyOfRange(37, data.size)
        // AAD'siz ochishga urinish MUVAFFAQIYATSIZ bo'lishi kerak (tag sarlavhaga bog'langan)
        val noAad = Cipher.getInstance("AES/GCM/NoPadding")
        noAad.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        var failed = false
        try { noAad.doFinal(ct) } catch (e: java.security.GeneralSecurityException) { failed = true }
        assertTrue("AAD'siz ochilmasligi kerak", failed)
        // to'g'ri AAD bilan ochiladi
        val withAad = Cipher.getInstance("AES/GCM/NoPadding")
        withAad.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        withAad.updateAAD(data.copyOfRange(0, 25))
        assertEquals(json, String(withAad.doFinal(ct), Charsets.UTF_8))
    }

    // ---- Iteratsiya chegaralari ----

    @Test fun iterationBoundsAreEnforcedBeforeKdf() {
        val v2 = b64(v2Fixture)
        fun withIter(n: Int) = v2.copyOf().also {
            it[5] = (n ushr 24).toByte(); it[6] = (n ushr 16).toByte(); it[7] = (n ushr 8).toByte(); it[8] = n.toByte()
        }
        val t0 = System.nanoTime()
        for (n in listOf(Int.MIN_VALUE, -1, 0, 1, ClipBackup.MIN_ITER_V2 - 1, ClipBackup.MAX_ITER_V2 + 1, Int.MAX_VALUE)) {
            assertNull("iter=$n", ClipBackup.decrypt(withIter(n), "yangi-parol-456"))
        }
        assertTrue("chegara tekshiruvi KDF'dan oldin bo'lishi kerak", (System.nanoTime() - t0) / 1_000_000 < 2_000)
        assertTrue(ClipBackup.MAX_ITER_V2 <= 2_000_000)
    }

    @Test fun encryptRejectsEmptyPasswordAndOutOfRangeIterations() {
        var threw = 0
        try { ClipBackup.encrypt(json, "") } catch (e: IllegalArgumentException) { threw++ }
        try { ClipBackup.encryptV2(json, "abcdef", ClipBackup.MIN_ITER_V2 - 1) } catch (e: IllegalArgumentException) { threw++ }
        try { ClipBackup.encryptV2(json, "abcdef", ClipBackup.MAX_ITER_V2 + 1) } catch (e: IllegalArgumentException) { threw++ }
        assertEquals(3, threw)
    }

    @Test fun manualPbkdf2DoesNotModifyCallerPasswordArray() {
        val pw = "sinov-parol".toByteArray()
        val copy = pw.copyOf()
        ClipBackup.pbkdf2HmacSha256(pw, ByteArray(16), 10, 32)
        assertArrayEquals(copy, pw)
    }

    private fun findUp(rel: String): File? {
        var d: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (d != null) {
            val f = File(d, rel)
            if (f.isFile) return f
            // user.dir = app/ bo'lsa, "app/" prefiksi ortiqcha
            val g = File(d, rel.removePrefix("app/"))
            if (rel.startsWith("app/") && g.isFile) return g
            d = d.parentFile
        }
        return null
    }
}
