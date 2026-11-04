package uz.ozkey.keyboard

/** Yangi o'zbek alifbosi (Ş Ç Ğ Ö) <-> hozirgi standart lotin (sh ch gʻ oʻ). */
object Alphabet {
    /** Standart yozuvdagi tutuq belgisi. Oddiy klaviaturada yozilsin desangiz "'" ga almashtiring. */
    const val STD_APOS = "ʻ"

    private const val APOS_CHARS = "'’‘ʻʼ`´＇"
    private const val NEW_LETTERS = "ŞşÇçĞğÖö"

    fun hasNew(s: String): Boolean = s.any { it in NEW_LETTERS }

    /** sh→ş, ch→ç, gʻ→ğ, oʻ→ö (katta-kichik harf saqlanadi). */
    fun toNew(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val n = if (i + 1 < s.length) s[i + 1] else '\u0000'
            val lc = c.lowercaseChar()
            val ln = n.lowercaseChar()
            val up = c.isUpperCase()
            val rep: Char? = when {
                lc == 's' && ln == 'h' -> if (up) 'Ş' else 'ş'
                lc == 'c' && ln == 'h' -> if (up) 'Ç' else 'ç'
                lc == 'g' && n in APOS_CHARS -> if (up) 'Ğ' else 'ğ'
                lc == 'o' && n in APOS_CHARS -> if (up) 'Ö' else 'ö'
                else -> null
            }
            if (rep != null) { sb.append(rep); i += 2 } else { sb.append(c); i++ }
        }
        return sb.toString()
    }

    /** ş→sh, ç→ch, ğ→gʻ, ö→oʻ. Butun so'z KATTA harfda bo'lsa SH, CH ... chiqadi. */
    fun toStandard(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (i in s.indices) {
            val c = s[i]
            val base = when (c) {
                'ş', 'Ş' -> "sh"
                'ç', 'Ç' -> "ch"
                'ğ', 'Ğ' -> "g$STD_APOS"
                'ö', 'Ö' -> "o$STD_APOS"
                else -> { sb.append(c); continue }
            }
            if (c.isUpperCase()) {
                val neighbourUpper =
                    (i + 1 < s.length && s[i + 1].isUpperCase()) || (i > 0 && s[i - 1].isUpperCase())
                sb.append(if (neighbourUpper) base.uppercase() else base.replaceFirstChar { it.uppercase() })
            } else sb.append(base)
        }
        return sb.toString()
    }
}
