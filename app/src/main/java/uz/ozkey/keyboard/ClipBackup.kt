package uz.ozkey.keyboard

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Zaxira fayli (.ozkb). Shifrlash har doim AES-256-GCM; farq — kalit olish usulida.
 *
 * v1 (eski, faqat O'QILADI):  "OZKB1" + tuz(16) + iv(12) + shifrmatn+tag
 *                             kalit = PBKDF2-HMAC-SHA1, 120 000 iteratsiya, parol AAD'siz
 * v2 (yangi, shunday YOZILADI): "OZKB2" + iteratsiya(4, big-endian) + tuz(16) + iv(12) + shifrmatn+tag
 *                             kalit = PBKDF2-HMAC-SHA256, [ITER_V2] iteratsiya
 *                             sarlavha (magic + iteratsiya + tuz) GCM AAD sifatida autentifikatsiya qilinadi.
 *
 * Parol va kalit hech qayerda saqlanmaydi va loglanmaydi (bu faylda log/print chaqiruvlari yo'q).
 *
 * Xotira haqida (halol cheklov): parol UI'dan o'zgarmas [String] bo'lib keladi — uni tozalab bo'lmaydi.
 * Bu yerda faqat O'ZIMIZ yaratgan nusxalar (CharArray/ByteArray, olingan kalit baytlari, ochilgan matn baytlari)
 * ishlatilgach nolga to'ldiriladi. JVM/Android GC nusxa ko'chirishi, Cipher/Mac ichki holati va SecretKeySpec
 * ichidagi nusxa bizning nazoratimizda emas, shuning uchun bu «imkon qadar» himoya, kafolat emas.
 */
object ClipBackup {
    private val MAGIC_V1 = "OZKB1".toByteArray(Charsets.US_ASCII)
    private val MAGIC_V2 = "OZKB2".toByteArray(Charsets.US_ASCII)

    const val VERSION_V1 = 1
    const val VERSION_V2 = 2

    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_LEN = 16
    private const val ITER_LEN = 4

    /** Eski formatning iteratsiyasi (o'zgarmaydi — eski fayllar shu bilan yaratilgan). */
    const val ITER_V1 = 120_000

    /**
     * Yangi zaxiralar uchun. OWASP Password Storage Cheat Sheet: PBKDF2-HMAC-SHA256 uchun 600 000.
     * Qiymat faylning o'ziga yoziladi, shuning uchun kelajakda oshirilsa ham eski v2 fayllar ochiladi.
     * Real qurilmada tezligi O'LCHANMAGAN — docs/BACKUP_DEVICE_TEST.md ga qarang.
     */
    const val ITER_V2 = 600_000

    /**
     * Buzilgan/zararli fayl sarlavhasi telefonni uzoq band qilmasligi uchun chegaralar.
     * MAX = joriy qiymatning ~3.3 baravari (kelajakda oshirish uchun zaxira), lekin cheksiz emas.
     */
    const val MIN_ITER_V2 = 100_000
    const val MAX_ITER_V2 = 2_000_000

    private const val KEY_BITS = 256

    /** Fayl formati versiyasi: 1, 2 yoki ÖZKEY zaxirasi bo'lmasa null. */
    fun detectVersion(data: ByteArray): Int? = when {
        startsWith(data, MAGIC_V2) -> VERSION_V2
        startsWith(data, MAGIC_V1) -> VERSION_V1
        else -> null
    }

    /** Bo'sh parol rad etiladi (IllegalArgumentException); chaqiruvchi (BuferTab) try/catch ichida. */
    fun encrypt(json: String, pw: String): ByteArray {
        require(pw.isNotEmpty()) { "empty password" }
        return encryptV2(json, pw, ITER_V2)
    }

    /** Parol noto'g'ri, fayl buzilgan yoki ÖZKEY zaxirasi emas bo'lsa — null. Exception tashlamaydi. */
    fun decrypt(data: ByteArray, pw: String): String? = try {
        if (pw.isEmpty()) {
            null
        } else {
            when (detectVersion(data)) {
                VERSION_V2 -> decryptV2(data, pw)
                VERSION_V1 -> decryptV1(data, pw)
                else -> null
            }
        }
    } catch (e: Exception) {
        null
    }

    // ---- v2 ----

    internal fun encryptV2(json: String, pw: String, iterations: Int): ByteArray {
        require(iterations in MIN_ITER_V2..MAX_ITER_V2) { "iterations" }
        val rnd = SecureRandom()
        val salt = ByteArray(SALT_LEN).also { rnd.nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { rnd.nextBytes(it) }
        val header = MAGIC_V2 + intToBytes(iterations) + salt
        val plain = json.toByteArray(Charsets.UTF_8)
        try {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, deriveSha256(normalizePw(pw), salt, iterations), GCMParameterSpec(TAG_LEN * 8, iv))
            c.updateAAD(header)
            return header + iv + c.doFinal(plain)
        } finally {
            wipe(plain)
        }
    }

    private fun decryptV2(data: ByteArray, pw: String): String? {
        val headerLen = MAGIC_V2.size + ITER_LEN + SALT_LEN
        if (data.size < headerLen + IV_LEN + TAG_LEN) return null
        // Iteratsiya chegarasi KDF'dan OLDIN tekshiriladi (zararli sarlavha uzoq hisoblatmasin)
        val iter = bytesToInt(data, MAGIC_V2.size)
        if (iter < MIN_ITER_V2 || iter > MAX_ITER_V2) return null
        val header = data.copyOfRange(0, headerLen)
        val salt = data.copyOfRange(MAGIC_V2.size + ITER_LEN, headerLen)
        val iv = data.copyOfRange(headerLen, headerLen + IV_LEN)
        val ct = data.copyOfRange(headerLen + IV_LEN, data.size)

        // Avval NFC (yangi zaxiralar shunday yaratiladi), bo'lmasa — kiritilgan ko'rinishi (v7 kabi
        // normallashtirmagan yozilgan fayllar uchun orqaga moslik). ASCII parolda bu bitta urinish.
        val nfc = normalizePw(pw)
        val candidates = if (nfc == pw) listOf(pw) else listOf(nfc, pw)
        for (p in candidates) {
            val out = tryDecryptV2(p, salt, iter, iv, header, ct)
            if (out != null) return out
        }
        return null
    }

    private fun tryDecryptV2(pw: String, salt: ByteArray, iter: Int, iv: ByteArray, header: ByteArray, ct: ByteArray): String? {
        return try {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, deriveSha256(pw, salt, iter), GCMParameterSpec(TAG_LEN * 8, iv))
            c.updateAAD(header)
            val plain = c.doFinal(ct)
            try {
                String(plain, Charsets.UTF_8)
            } finally {
                wipe(plain)
            }
        } catch (e: GeneralSecurityException) {
            null // noto'g'ri parol (AEADBadTagException) yoki buzilgan fayl
        }
    }

    // ---- v1 (faqat o'qish) ----

    private fun decryptV1(data: ByteArray, pw: String): String? {
        val h = MAGIC_V1.size
        if (data.size < h + SALT_LEN + IV_LEN + TAG_LEN) return null
        val salt = data.copyOfRange(h, h + SALT_LEN)
        val iv = data.copyOfRange(h + SALT_LEN, h + SALT_LEN + IV_LEN)
        val ct = data.copyOfRange(h + SALT_LEN + IV_LEN, data.size)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, deriveSha1(pw, salt, ITER_V1), GCMParameterSpec(TAG_LEN * 8, iv))
        val plain = c.doFinal(ct)
        try {
            return String(plain, Charsets.UTF_8)
        } finally {
            wipe(plain)
        }
    }

    // ---- kalit olish ----

    /** v1 uchun: v6 dagi bilan AYNAN bir xil (PBKDF2WithHmacSHA1, parol toCharArray) — eski fayllar ochilishi uchun o'zgartirilmagan. */
    private fun deriveSha1(pw: String, salt: ByteArray, iter: Int): SecretKeySpec {
        val chars = pw.toCharArray()
        val spec = PBEKeySpec(chars, salt, iter, KEY_BITS)
        try {
            val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
            try {
                return SecretKeySpec(raw, "AES") // SecretKeySpec o'z nusxasini oladi
            } finally {
                wipe(raw)
            }
        } finally {
            spec.clearPassword()
            wipe(chars)
        }
    }

    /**
     * PBKDF2WithHmacSHA256 Android'da faqat API 26+ da bor (minSdk = 23), va ba'zi provayderlar
     * ASCII bo'lmagan parolni (o', ʻ, kirill) turlicha baytga aylantirishi mumkin. Shuning uchun:
     * parol toza ASCII bo'lsa — tizimning tez (native/provayder) varianti; aks holda yoki variant yo'q bo'lsa —
     * bir xil natija beradigan, parol har doim UTF-8 bo'lgan qo'lda yozilgan HMAC-SHA256 varianti.
     * Shunda zaxira bir qurilmada yaratilib, boshqasida (boshqa Android versiyasida) ham ochiladi.
     */
    internal fun deriveSha256(pw: String, salt: ByteArray, iter: Int): SecretKeySpec {
        val system: ByteArray? = if (isAscii(pw)) systemSha256OrNull(pw, salt, iter) else null
        val raw: ByteArray = system ?: run {
            val pwBytes = pw.toByteArray(Charsets.UTF_8)
            try {
                pbkdf2HmacSha256(pwBytes, salt, iter, KEY_BITS / 8)
            } finally {
                wipe(pwBytes)
            }
        }
        try {
            return SecretKeySpec(raw, "AES") // SecretKeySpec o'z nusxasini oladi
        } finally {
            wipe(raw)
        }
    }

    /** Tizim PBKDF2WithHmacSHA256 (API 26+). Yo'q bo'lsa null — chaqiruvchi qo'lda variantga o'tadi. */
    private fun systemSha256OrNull(pw: String, salt: ByteArray, iter: Int): ByteArray? {
        val chars = pw.toCharArray()
        try {
            val spec = PBEKeySpec(chars, salt, iter, KEY_BITS)
            try {
                return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            } catch (e: GeneralSecurityException) {
                return null // NoSuchAlgorithm (API 23–25) yoki provayder rad etdi
            } finally {
                spec.clearPassword()
            }
        } finally {
            wipe(chars)
        }
    }

    /**
     * PBKDF2-HMAC-SHA256 (RFC 8018 §5.2). Faqat API 23–25 va ASCII bo'lmagan parollar uchun yo'l;
     * testlarda RFC 7914 vektori va tizim variantiga solishtiriladi.
     * Chaqiruvchining [password] massivi O'ZGARTIRILMAYDI (uni chaqiruvchi tozalaydi).
     */
    internal fun pbkdf2HmacSha256(password: ByteArray, salt: ByteArray, iter: Int, outLen: Int): ByteArray {
        require(password.isNotEmpty()) { "empty password" } // SecretKeySpec bo'sh kalitni qabul qilmaydi
        require(iter >= 1 && outLen >= 1) { "params" }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password, "HmacSHA256"))
        val hLen = mac.macLength
        val blocks = (outLen + hLen - 1) / hLen
        val out = ByteArray(outLen)
        for (b in 1..blocks) {
            mac.update(salt)
            mac.update(intToBytes(b))
            var u = mac.doFinal()
            val t = u.copyOf()
            for (i in 1 until iter) {
                val next = mac.doFinal(u)
                wipe(u)
                u = next
                for (j in t.indices) t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
            }
            val off = (b - 1) * hLen
            System.arraycopy(t, 0, out, off, minOf(hLen, outLen - off))
            wipe(u)
            wipe(t)
        }
        return out
    }

    // ---- yordamchilar ----

    /** v2 uchun parol NFC ga keltiriladi (turli klaviatura/qurilmalarda bir xil ko'rinadigan belgi har xil kodlanishi mumkin). */
    private fun normalizePw(pw: String): String =
        if (isAscii(pw)) pw else Normalizer.normalize(pw, Normalizer.Form.NFC)

    private fun isAscii(s: String): Boolean {
        for (ch in s) if (ch.code >= 0x80) return false
        return true
    }

    private fun wipe(a: ByteArray) = java.util.Arrays.fill(a, 0.toByte())

    private fun wipe(a: CharArray) = java.util.Arrays.fill(a, '\u0000')

    private fun startsWith(data: ByteArray, magic: ByteArray): Boolean {
        if (data.size < magic.size) return false
        for (i in magic.indices) if (data[i] != magic[i]) return false
        return true
    }

    private fun intToBytes(v: Int): ByteArray =
        byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())

    private fun bytesToInt(d: ByteArray, off: Int): Int =
        ((d[off].toInt() and 0xFF) shl 24) or ((d[off + 1].toInt() and 0xFF) shl 16) or
            ((d[off + 2].toInt() and 0xFF) shl 8) or (d[off + 3].toInt() and 0xFF)
}
