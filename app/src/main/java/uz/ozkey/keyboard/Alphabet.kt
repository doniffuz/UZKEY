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

/** Lotin <-> Kirill (o'zbek). Kirish: standart lotin yoki ÖZ alifbosi (Ş Ç Ğ Ö) bo'lishi mumkin. */
object Script {
    private const val APOS = "'’‘ʻʼ`´＇"

    private val latCyr = mapOf(
        'a' to "а", 'b' to "б", 'd' to "д", 'f' to "ф", 'g' to "г", 'h' to "ҳ", 'i' to "и",
        'j' to "ж", 'k' to "к", 'l' to "л", 'm' to "м", 'n' to "н", 'o' to "о", 'p' to "п",
        'q' to "қ", 'r' to "р", 's' to "с", 't' to "т", 'u' to "у", 'v' to "в", 'x' to "х",
        'y' to "й", 'z' to "з", 'c' to "ц", 'w' to "в", 'e' to "е"
    )

    private fun isLat(c: Char) = c.lowercaseChar() in 'a'..'z' || c in "ŞşÇçĞğÖö"

    fun latToCyr(input: String): String {
        val s = input
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val lc = c.lowercaseChar()
            val n = if (i + 1 < s.length) s[i + 1] else '\u0000'
            val ln = n.lowercaseChar()
            val up = c.isUpperCase()
            fun put(x: String, len: Int) {
                sb.append(if (up) x.uppercase() else x); i += len
            }
            when {
                c == 'Ş' || c == 'ş' -> put("ш", 1)
                c == 'Ç' || c == 'ç' -> put("ч", 1)
                c == 'Ğ' || c == 'ğ' -> put("ғ", 1)
                c == 'Ö' || c == 'ö' -> put("ў", 1)
                lc == 's' && ln == 'h' -> put("ш", 2)
                lc == 'c' && ln == 'h' -> put("ч", 2)
                lc == 'y' && ln == 'o' -> put("ё", 2)
                lc == 'y' && ln == 'u' -> put("ю", 2)
                lc == 'y' && ln == 'a' -> put("я", 2)
                lc == 'y' && ln == 'e' -> put("е", 2)
                lc == 't' && ln == 's' -> put("ц", 2)
                lc == 'o' && n in APOS -> put("ў", 2)
                lc == 'g' && n in APOS -> put("ғ", 2)
                lc == 'e' && (i == 0 || !isLat(s[i - 1]) ) -> put("э", 1)
                c in APOS -> { sb.append("ъ"); i++ }
                else -> {
                    val m = latCyr[lc]
                    if (m != null) put(m, 1) else { sb.append(c); i++ }
                }
            }
        }
        return sb.toString()
    }

    private val cyrLat = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "yo",
        'ж' to "j", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m",
        'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
        'ф' to "f", 'х' to "x", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "shch",
        'ъ' to "ʼ", 'ы' to "i", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya",
        'ў' to "oʻ", 'қ' to "q", 'ғ' to "gʻ", 'ҳ' to "h"
    )
    private val cyrVowels = "аеёиоуэюяўы"

    /** Natija standart lotinda (oʻ, gʻ, sh, ch). ÖZ alifbosi uchun keyin Alphabet.toNew() chaqiring. */
    fun cyrToLat(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (i in s.indices) {
            val c = s[i]
            val lc = c.lowercaseChar()
            var base = cyrLat[lc]
            if (base == null) { sb.append(c); continue }
            if (lc == 'е') {
                val prev = if (i > 0) s[i - 1].lowercaseChar() else ' '
                if (!prev.isLetter() || prev in cyrVowels) base = "ye"
            }
            if (c.isUpperCase() && base.isNotEmpty()) {
                val nb = (i + 1 < s.length && s[i + 1].isUpperCase()) || (i > 0 && s[i - 1].isUpperCase())
                sb.append(if (nb && base.length > 1) base.uppercase() else base.replaceFirstChar { it.uppercase() })
            } else sb.append(base)
        }
        return sb.toString()
    }
}
