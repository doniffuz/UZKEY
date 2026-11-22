package uz.ozkey.keyboard

/**
 * Uzun bosish variantlari (Android'siz, testlanadi). Faqat ÖZ o'zbek klaviaturasi uchun:
 * g → ğ | gʻ, o → ö | oʻ, s → ş | sh, c → ç | ch, vergul → ’ (tutuq).
 * Variantlar oddiy bosishdagi avto-almashtirishsiz (konversiyasiz) kiritiladi.
 */
object LongPress {
    const val DELAY_MS = 350L
    const val APOS = "’"

    private val map: Map<String, List<String>> = mapOf(
        "g" to listOf("ğ", "g" + Alphabet.STD_APOS),
        "o" to listOf("ö", "o" + Alphabet.STD_APOS),
        "s" to listOf("ş", "sh"),
        "c" to listOf("ç", "ch"),
        "," to listOf(APOS)
    )

    fun variants(key: String): List<String>? = map[key]

    /** Shift/Caps Lock holatiga mos yozuv: Caps Lock = hammasi katta, Shift = birinchi harf katta. */
    fun cased(v: String, caps: Boolean, capsLock: Boolean): String = when {
        v == APOS -> v
        capsLock -> v.uppercase()
        caps -> v.replaceFirstChar { it.uppercase() }
        else -> v
    }

    /** Barmoq x koordinatasiga mos variant indeksi (chegaradan chiqsa — eng yaqin variant). */
    fun indexAt(x: Float, left: Int, itemW: Int, n: Int): Int {
        if (n <= 0 || itemW <= 0) return 0
        return Math.floor(((x - left) / itemW).toDouble()).toInt().coerceIn(0, n - 1)
    }
}
