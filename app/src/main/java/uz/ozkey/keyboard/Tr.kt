package uz.ozkey.keyboard

import android.content.Context
import java.util.Locale

/**
 * Ilova interfeysi matnlari: O'zbekcha / Русский / English.
 * Til: sozlamadagi tanlov ("ui_lang"), bo'lmasa telefon tili (ru/en), aks holda o'zbekcha.
 * Yangi matn qo'shish: pastdagi `M` ro'yxatiga uch tilni bitta qatorda yozing.
 */
object Tr {
    const val UZ = 0; const val RU = 1; const val EN = 2
    val names = listOf("O‘zbekcha", "Русский", "English")

    fun lang(c: Context): Int {
        val s = c.getSharedPreferences("ozkey", Context.MODE_PRIVATE).getInt("ui_lang", -1)
        if (s in 0..2) return s
        return when (Locale.getDefault().language) { "ru" -> RU; "en" -> EN; else -> UZ }
    }

    fun setLang(c: Context, l: Int) {
        c.getSharedPreferences("ozkey", Context.MODE_PRIVATE).edit().putInt("ui_lang", l).apply()
    }

    fun t(c: Context, key: String): String = M[key]?.getOrNull(lang(c)) ?: M[key]?.get(0) ?: key

    private val M: Map<String, List<String>> = mapOf(
        "tg_missing" to listOf("Telegram topilmadi", "Telegram не найден", "Telegram not found"),
    )
}
