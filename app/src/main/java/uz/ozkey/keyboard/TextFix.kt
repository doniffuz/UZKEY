package uz.ozkey.keyboard

/**
 * Matnni tuzatish (Text Fix) — QURILMADA, internetsiz, qoidalarga asoslangan.
 * Nima qiladi: ortiqcha bo'shliqlar, tinish belgisidan oldin/keyin bo'shliq, salomdan keyin vergul,
 * savol gaplarga "?", gap boshida katta harf, oxirida nuqta, va lug'atdagi ANIQ (noaniq emas) xatolar.
 * Nima QILMAYDI: so'zlarni qayta yozmaydi, ma'noni o'zgartirmaydi. Natija har doim foydalanuvchiga
 * ko'rsatiladi va faqat u tasdiqlasa qo'llanadi.
 */
object TextFix {
    private val wordRe = Regex("^([\\p{L}'’‘ʻʼ]+)([^\\p{L}]*)$")
    private val spaceBefore = Regex("\\s+([,;:!?.])")
    private val noSpaceAfter = Regex("([,;!?])(?=\\p{L})")
    private val multiSpace = Regex("[ \\t]{2,}")
    private val repeatQ = Regex("\\?{2,}")          // ?? -> ?
    private val repeatE = Regex("!{2,}")             // !! -> !
    private val repeatD = Regex("\\.{2,}")          // .. yoki .... -> ... (ko'p nuqta)
    private val repeatC = Regex(",{2,}")

    private val questionWords = setOf(
        "nima", "nega", "nimaga", "qanday", "qanaqa", "qachon", "qayer", "qayerda", "qayerga",
        "qaysi", "kim", "kimlar", "necha", "qancha", "qani"
    )
    private val questionEnds = listOf("misiz", "misan", "musiz", "mikan", "mizmi", "masmi", "izmi", "anmi")

    private fun isQuestionEnd(w: String): Boolean {
        if (w.length < 5) return false
        if (questionEnds.any { w.endsWith(it) }) return true
        return w.endsWith("mi") && WordModel.isKnown(w.dropLast(2))
    }

    fun fix(text: String, uz: Boolean): String =
        text.split('\n').joinToString("\n") { fixLine(it, uz) }

    private fun fixLine(raw: String, uz: Boolean): String {
        var t = raw.trim()
        if (t.isEmpty()) return ""
        t = multiSpace.replace(t, " ")
        t = repeatQ.replace(t, "?")
        t = repeatE.replace(t, "!")
        t = repeatD.replace(t, "...")
        t = repeatC.replace(t, ",")
        t = spaceBefore.replace(t, "\$1")
        t = noSpaceAfter.replace(t, "\$1 ")
        val toks = t.split(' ').filter { it.isNotEmpty() }.toMutableList()
        if (toks.isEmpty()) return ""

        if (uz) {
            for (i in toks.indices) {
                val m = wordRe.find(toks[i]) ?: continue
                var core = m.groupValues[1]
                var tail = m.groupValues[2]
                val low = core.lowercase()
                if (core.length >= 4 && core == low && core.none { it == '\'' }) {
                    val prev = if (i > 0) wordRe.find(toks[i - 1])?.groupValues?.get(1)?.lowercase() else null
                    val fx = WordModel.autoFix(low, prev, emptyMap())
                    if (fx != null) core = fx
                }
                if (tail.isEmpty() && i < toks.lastIndex && isQuestionEnd(core.lowercase())) tail = "?"
                toks[i] = core + tail
            }
            // Salomlashuvdan keyin vergul: "salom yaxshimisiz" -> "Salom, yaxshimisiz"
            val first = toks[0].lowercase()
            val gi = when {
                first == "salom" -> 0
                first == "assalomu" && toks.size > 1 && toks[1].lowercase().trimEnd(',', '.', '!') == "alaykum" -> 1
                else -> -1
            }
            if (gi >= 0 && gi < toks.lastIndex && toks[gi].last().isLetter()) toks[gi] = toks[gi] + ","
        }

        // Gap boshida katta harf
        var start = true
        for (i in toks.indices) {
            var tk = toks[i]
            if (start && tk[0].isLetter() && !tk.startsWith("http") && !tk.startsWith("www") && !tk.contains('@')) {
                tk = tk.replaceFirstChar { it.uppercase() }
            }
            toks[i] = tk
            start = tk.last() in ".!?…"
        }

        // Oxirgi gap tinish belgisisiz tugasa
        val last = toks.last()
        if (last.last().isLetter() && !last.contains('@') && !last.startsWith("http") && !last.startsWith("www")) {
            var from = toks.size - 1
            while (from > 0 && toks[from - 1].last() !in ".!?…") from--
            val sentence = toks.subList(from, toks.size).map { w -> w.lowercase().trim { c -> !c.isLetter() } }
            val isQ = uz && (sentence.any { it in questionWords } || isQuestionEnd(sentence.last()))
            if (isQ) toks[toks.lastIndex] = last + "?"
            else if (toks.size >= 3) toks[toks.lastIndex] = last + "."
        }
        return toks.joinToString(" ")
    }
}
