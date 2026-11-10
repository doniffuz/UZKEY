package uz.ozkey.keyboard

/**
 * Avto-tuzatish qarori (Android'siz, testlanadi). Klaviatura faqat shu qarorni bajaradi:
 * [plan] null qaytarsa — foydalanuvchi yozganiga TEGILMAYDI.
 */
object AutoCorrect {
    class Plan(val original: String, val corrected: String)

    /** Tutuq belgilari: so'zning boshi "o'zbek" kabi so'zning o'rtasi bo'lishi mumkin — tegilmaydi. */
    private const val APOS = "'’‘ʻʼ＇`´"
    /** So'z oldidagi bu belgilar havola/email/hashtag/raqam/yo'l ekanini bildiradi — tegilmaydi. */
    private const val GLUE = "@./\\:_-#+&=%~"

    fun applyCase(orig: String, w: String): String = when {
        orig.length > 1 && orig.all { !it.isLetter() || it.isUpperCase() } -> w.uppercase()
        orig.isNotEmpty() && orig[0].isUpperCase() -> w.replaceFirstChar { it.uppercase() }
        else -> w
    }

    /**
     * @param before kursor oldidagi matn (so'z tugagan joyda)
     * @param after kursordan keyingi bitta belgi (so'z o'rtasida tahrir qilinayotganini bilish uchun), bo'lmasa null
     */
    fun plan(before: String, after: Char?, nb: Map<Char, Set<Char>>): Plan? {
        if (after != null && (after.isLetterOrDigit() || after in APOS)) return null   // so'z o'rtasida
        val cx = WordModel.parse(before)
        val w = cx.prefix
        if (w.length < 2 || w.any { it in APOS }) return null
        val i = before.length - w.length
        if (i > 0) {
            val c = before[i - 1]
            if (c in APOS || c in GLUE || c.isDigit()) return null
        }
        val capOk = !w[0].isUpperCase() || cx.sentenceStart
        val mixed = w.drop(1).any { it.isUpperCase() } && !w.all { !it.isLetter() || it.isUpperCase() }
        if (!capOk || mixed) return null
        val fix = WordModel.autoFix(w.lowercase(), cx.prev, nb) ?: return null
        return Plan(w, applyCase(w, fix))
    }
}
