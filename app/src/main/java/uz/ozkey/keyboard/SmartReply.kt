package uz.ozkey.keyboard

/**
 * Smart Reply — qisqa javoblar (qurilmada, qoidalar asosida).
 * MUHIM CHEKLOV: Android klaviaturaga boshqa ilovadagi xabarni ko'rsatmaydi. Shuning uchun xabar
 * matni sifatida foydalanuvchi NUSXALAGAN (copy) so'nggi matn olinadi. Maydon bo'sh bo'lganda
 * takliflar paneliga javoblar chiqadi.
 */
object SmartReply {
    private class Rule(val keys: List<String>, val replies: List<String>, val exact: Boolean = false)

    private const val APOS = "'’‘ʻʼ`´＇"

    private val rules = listOf(
        Rule(listOf("assalomu alaykum", "assalom alaykum", "assalomu aleykum"),
            listOf("Va alaykum assalom!", "Va alaykum assalom, yaxshiman rahmat!")),
        Rule(listOf("xayrli tong", "xayrli kun", "xayrli kech", "xayrli tun"),
            listOf("Sizga ham xayrli bo'lsin!", "Rahmat, sizga ham!")),
        Rule(listOf("qalesiz", "qalesan", "qalaysiz", "qalaysan", "yaxshimisiz", "yaxshimisan", "ishlar qalay", "ahvollar qalay", "holingiz qalay"),
            listOf("Yaxshi, rahmat!", "Yaxshiman, o'zingiz yaxshimisiz?", "Hammasi yaxshi, rahmat!")),
        Rule(listOf("kelasizmi", "kelasanmi", "bugun kelasizmi", "bugun kelasanmi"),
            listOf("Ha, albatta.", "Kechirasiz, bugun kela olmayman.", "Hali aniq emas, keyinroq yozaman.")),
        Rule(listOf("bandmisiz", "bandmisan", "vaqtingiz bormi"),
            listOf("Hozir bandman, keyin yozaman.", "Yo'q, bo'shman.", "Ha, gapiring.")),
        Rule(listOf("qayerdasiz", "qayerdasan"),
            listOf("Hozircha shu yerdaman.", "Yo'ldaman.", "Uydaman.")),
        Rule(listOf("nima qilyapsiz", "nima qilyapsan", "nima qilasiz", "nima gap", "nima yangilik"),
            listOf("Hozir ishlarim bilan bandman.", "Hech narsa, o'zingiz-chi?", "Dam olyapman.")),
        Rule(listOf("telefon qiling", "raqamingizni bering", "nomeringizni bering"),
            listOf("Hozir qo'ng'iroq qilaman.", "Keyinroq qo'ng'iroq qilaman.")),
        Rule(listOf("bayramingiz muborak", "tugilgan kun muborak", "tabriklayman", "tabriklaymiz", "muborak bolsin"),
            listOf("Rahmat, sizga ham!", "Katta rahmat!", "Sizni ham tabriklayman!")),
        Rule(listOf("kechirasiz", "kechirasan", "uzr"),
            listOf("Hechqisi yo'q.", "Mayli, tashvishlanmang.")),
        Rule(listOf("rahmat", "raxmat", "katta rahmat"),
            listOf("Arzimaydi!", "Marhamat!", "Doim tayyorman!")),
        Rule(listOf("xayr", "hayr", "korishguncha", "salomat boling"),
            listOf("Xayr, ko'rishguncha!", "Sog' bo'ling!")),
        Rule(listOf("salom"),
            listOf("Salom! Qalaysiz?", "Salom!", "Salom, yaxshimisiz?"), exact = true),
        Rule(listOf("ha", "xa", "ha albatta"), listOf("Mayli 👍", "Yaxshi, kelishdik.", "Rahmat!"), exact = true),
        Rule(listOf("yoq", "yoq rahmat"), listOf("Tushunarli.", "Mayli, hechqisi yo'q."), exact = true),
        Rule(listOf("keyinroq", "keyin", "birozdan keyin"), listOf("Mayli, keyin yozishamiz.", "Yaxshi, kutaman."), exact = true),
        Rule(listOf("salom"), listOf("Salom!", "Salom, yaxshimisiz?"))
    )

    /** ÖZ alifbosi ham, standart lotin ham bir xil ko'rinishga keltiriladi (apostrofsiz, kichik harf). */
    private fun norm(s: String): String {
        val std = Alphabet.toStandard(s.takeLast(300).lowercase())
        val sb = StringBuilder(" ")
        var lastSpace = true
        for (c in std) {
            if (c in APOS) continue
            if (c.isLetter()) { sb.append(c); lastSpace = false }
            else if (!lastSpace) { sb.append(' '); lastSpace = true }
        }
        if (!lastSpace) sb.append(' ')
        return sb.toString()
    }

    /** Xabarga 3 tagacha qisqa javob (ÖZ alifbosida). Mos qoida bo'lmasa — bo'sh ro'yxat. */
    fun suggest(message: String, n: Int = 3): List<String> {
        if (message.isBlank()) return emptyList()
        val nm = norm(message)
        for (r in rules) {
            if (r.keys.any { if (r.exact) nm == " $it " else nm.contains(" $it ") }) return r.replies.take(n).map { Alphabet.toNew(it) }
        }
        return emptyList()
    }
}
