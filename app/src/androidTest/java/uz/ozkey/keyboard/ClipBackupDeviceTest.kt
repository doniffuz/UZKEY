package uz.ozkey.keyboard

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * REAL QURILMA/EMULYATORDA ishga tushiriladi (JVM'da emas): 600 000 iteratsiyali PBKDF2-HMAC-SHA256
 * qancha vaqt olishini o'lchaydi va v1/v2 formatlar shu Android versiyasida ishlashini tekshiradi.
 * Faqat VAQT va API darajasi loglanadi (parol/kalit/matn HECH QACHON). Natijani:
 *   adb logcat -s OZKEY-BENCH
 * orqali ko'ring va docs/BACKUP_DEVICE_TEST.md jadvaliga yozing. Qat'iy vaqt chegarasi qo'yilmagan
 * (qurilmaga bog'liq, flaky bo'lmasligi uchun) — qaror qabul qilish uchun jadvalga qarang.
 */
@RunWith(AndroidJUnit4::class)
class ClipBackupDeviceTest {
    private val tag = "OZKEY-BENCH"
    private val json = "{\"v\": 1, \"items\": [{\"text\": \"Salom\", \"cat\": \"Ish\"}]}"

    private fun ms(block: () -> Unit): Long {
        val t = SystemClock.elapsedRealtime(); block(); return SystemClock.elapsedRealtime() - t
    }

    @Test fun v2RoundTripAndTimingAscii() {
        var data = ByteArray(0)
        val enc = ms { data = ClipBackup.encrypt(json, "bench-parol-123") }
        var out: String? = null
        val dec = ms { out = ClipBackup.decrypt(data, "bench-parol-123") }
        Log.i(tag, "API ${Build.VERSION.SDK_INT} ${Build.MODEL}: ASCII parol, ${ClipBackup.ITER_V2} iter: encrypt=${enc}ms decrypt=${dec}ms")
        assertEquals(json, out)
    }

    @Test fun v2RoundTripAndTimingUnicodePassword() {
        // ASCII bo'lmagan parol har doim qo'lda (Mac) yo'ldan o'tadi — odatda sekinroq
        val pw = "bench-o\u2018zbek-\u043f\u0430\u0440\u043e\u043b\u044c"
        var data = ByteArray(0)
        val enc = ms { data = ClipBackup.encrypt(json, pw) }
        var out: String? = null
        val dec = ms { out = ClipBackup.decrypt(data, pw) }
        Log.i(tag, "API ${Build.VERSION.SDK_INT} ${Build.MODEL}: Unicode parol, ${ClipBackup.ITER_V2} iter: encrypt=${enc}ms decrypt=${dec}ms")
        assertEquals(json, out)
    }

    @Test fun manualPbkdf2MatchesSystemOnThisDevice() {
        // API 26+ da tizim varianti bor — qo'lda variant bilan bir xil bo'lishi shart. API 23–25 da tizim varianti yo'q (o'tkazib yuboriladi).
        val salt = ByteArray(16) { it.toByte() }
        val sys = try {
            javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(javax.crypto.spec.PBEKeySpec("sinov-parol".toCharArray(), salt, 10_000, 256)).encoded
        } catch (e: java.security.GeneralSecurityException) { null }
        var manual = ByteArray(0)
        val t = ms { manual = ClipBackup.pbkdf2HmacSha256("sinov-parol".toByteArray(), salt, 10_000, 32) }
        Log.i(tag, "API ${Build.VERSION.SDK_INT}: manual 10k iter=${t}ms, systemAvailable=${sys != null}")
        if (sys != null) org.junit.Assert.assertArrayEquals(sys, manual)
    }

    @Test fun wrongPasswordAndCorruptFileDoNotCrashOnDevice() {
        val data = ClipBackup.encrypt(json, "bench-parol-123")
        assertNull(ClipBackup.decrypt(data, "boshqa-parol"))
        assertNull(ClipBackup.decrypt(data.copyOf(data.size - 1), "bench-parol-123"))
        assertNull(ClipBackup.decrypt(ByteArray(64), "bench-parol-123"))
        assertNotNull(ClipBackup.decrypt(data, "bench-parol-123"))
    }
}
