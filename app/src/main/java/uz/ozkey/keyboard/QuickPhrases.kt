package uz.ozkey.keyboard

import android.content.Context

/**
 * ⚡ Tezkor iboralar: tayyor kategoriyalar + foydalanuvchining o'z iboralari («Mening»).
 * Tayyor matnlar standart lotinda yoziladi; klaviatura ularni tanlangan yozuvga (ÖZ / Кирилл) o'tkazadi.
 * Shablonlardagi [qavs ichidagi] joylar qo'yilgach avtomatik belgilanadi — ustidan yozib o'zgartirasiz.
 * O'z iboralar faqat shu telefonda saqlanadi.
 */
object QuickPhrases {
    val cats = listOf("Umumiy", "Ariza", "Maktab", "Ish", "Telegram", "Savdo", "Tabassum", "Mening")
    val MY: Int = cats.lastIndex

    private const val PREFS = "ozkey_qp"
    private const val SEP = '\u001F'
    private const val MAX_USER = 60
    private const val MAX_LEN = 1000

    fun builtin(cat: Int): List<String> = when (cat) {
        0 -> listOf(
            "Assalomu alaykum", "Va alaykum assalom", "Rahmat", "Iltimos", "Xayrli tong", "Xayrli kech",
            "Yaxshimisiz?", "Hozir bandman, keyin yozaman", "Kechirasiz", "Xayr, ko'rishguncha",
            "Bayramingiz muborak bo'lsin!", "Yo'ldaman", "Mayli, bo'ladi", "Telefon qiling", "Raqamingizni bering"
        )
        1 -> listOf(
            "Hurmatli [rahbar]!\nMenga [boshlanish sanasi] dan [tugash sanasi] gacha mehnat ta'tili berishingizni so'rayman.\n[sana]\n[ism-familiya]",
            "Hurmatli [rahbar]!\nOilaviy sharoitim tufayli [sana] kuni ish joyida bo'la olmayman. Menga bir kunlik ruxsat berishingizni so'rayman.\n[sana]\n[ism-familiya]",
            "Hurmatli [lavozim]!\nMen, [ism-familiya], [murojaat mazmuni] bo'yicha ariza bilan murojaat qilaman. Iltimos, arizamni ko'rib chiqing.\n[sana]\n[ism-familiya]",
            "Hurmatli [rahbar]!\nMenga [sana] dan boshlab o'z xohishimga ko'ra ishdan bo'shashga ruxsat berishingizni so'rayman.\n[sana]\n[ism-familiya]"
        )
        2 -> listOf(
            "Assalomu alaykum, hurmatli [o'qituvchi ismi]! Farzandim [ism] bugun kasallik sababli darsga kela olmaydi. Iltimos, uzr qabul qiling.",
            "Assalomu alaykum, ustoz! Uy vazifasi nima edi?",
            "Ustoz, darsga [soat] da yetib boraman. Kechikkanim uchun uzr.",
            "Assalomu alaykum! [sinf]-sinf ota-onalar yig'ilishi qachon bo'ladi?",
            "Rahmat, ustoz! Tushundim."
        )
        3 -> listOf(
            "Assalomu alaykum! Hisobotni [sana] gacha yuboraman.",
            "Uchrashuvni [vaqt] ga ko'chirsak bo'ladimi?",
            "Hujjatlarni ko'rib chiqdim, izohlarim bor.",
            "Hozir yig'ilishdaman, keyinroq bog'lanaman.",
            "Xabaringiz uchun rahmat, bugun javob beraman.",
            "Murojaat uchun telefon: [telefon]"
        )
        4 -> listOf(
            "Salom! Qalaysiz?",
            "Tushundim 👍",
            "Hozir yozaman, bir daqiqa.",
            "Kanalimizga a'zo bo'ling: @[kanal]",
            "Iltimos, ovozli xabar yuboring.",
            "Rahmat, xabarni oldim ✅"
        )
        5 -> listOf(
            "Assalomu alaykum! Buyurtmangiz qabul qilindi. Tez orada bog'lanamiz.",
            "Narxi: [narx] so'm. Yetkazib berish [shahar] bo'ylab bepul.",
            "Mahsulot mavjud. Buyurtma berasizmi?",
            "To'lov: Click / Payme / naqd. Qaysi biri qulay?",
            "Manzilingizni va telefon raqamingizni yozib yuboring.",
            "Xaridingiz uchun rahmat! Yana kutib qolamiz."
        )
        6 -> listOf(
            "😊", "😂 Juda kulgili!", "❤️ Rahmat, juda yoqdi!", "👍 Zo'r!",
            "🔥 Ajoyib!", "🙏 Katta rahmat!", "😄 Kayfiyatingiz yaxshi bo'lsin!", "🤝 Kelishdik!"
        )
        else -> emptyList()
    }

    @Synchronized
    fun user(c: Context): List<String> = try {
        (c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("u", "") ?: "")
            .split(SEP).filter { it.isNotEmpty() }
    } catch (e: Exception) { emptyList() }

    private fun save(c: Context, list: List<String>) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("u", list.joinToString(SEP.toString())).apply()
    }

    /** 0 — saqlandi, 1 — bo'sh matn, 2 — allaqachon bor, 3 — joy qolmagan. */
    @Synchronized
    fun addUser(c: Context, text: String): Int {
        val t = text.trim().take(MAX_LEN).replace(SEP, ' ')
        if (t.isEmpty()) return 1
        val cur = user(c)
        if (t in cur) return 2
        if (cur.size >= MAX_USER) return 3
        return try { save(c, cur + t); 0 } catch (e: Exception) { 1 }
    }

    @Synchronized
    fun removeUser(c: Context, index: Int) {
        val cur = user(c).toMutableList()
        if (index !in cur.indices) return
        cur.removeAt(index)
        try { save(c, cur) } catch (e: Exception) { }
    }
}
