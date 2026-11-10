package uz.ozkey.keyboard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** rows[0..2] — harf qatorlari (3-qator ⇧ va ⌫ orasiga qo'yiladi), extra — pastki qo'shimcha qator. */
class Lang(
    val code: String,
    val name: String,
    val space: String,
    val rows: List<List<String>>,
    val extra: List<String>,
    val uzDigraph: Boolean = false,
    val builtin: Boolean = true
)

object Languages {
    private fun l(s: String) = s.map { it.toString() }

    val UZ = Lang("UZ", "O'zbekcha (Lotin, ÖZ)", "ÖZKEY",
        listOf(l("qwertyuiop"), l("asdfghjkl"), l("zxcvbnm")),
        listOf("ğ", "ö", "ş", "ç", "'"), uzDigraph = true)   // oddiy holat: kichik; ⇧: Ğ Ö Ş Ç

    val UZC = Lang("UZC", "Ўзбекча (Кирилл)", "Ўзбекча",
        listOf(l("йцукенгшўзх"), l("фқвапролджэ"), l("ячсмитьбю")),
        listOf("ғ", "ҳ", "ъ", "-", "!", "?"))

    val RU = Lang("RU", "Русский", "Русский",
        listOf(l("йцукенгшщзх"), l("фывапролджэ"), l("ячсмитьбю")),
        listOf("ё", "ъ", "-", "!", "?"))

    val EN = Lang("EN", "English", "English",
        listOf(l("qwertyuiop"), l("asdfghjkl"), l("zxcvbnm")),
        listOf("'", "-", "!", "?", "@"))

    val builtins = listOf(UZ, UZC, RU, EN)

    fun fromJson(j: JSONObject, builtin: Boolean = false): Lang {
        fun arr(a: JSONArray) = (0 until a.length()).map { a.getString(it) }
        val rowsJ = j.getJSONArray("rows")
        return Lang(
            code = j.getString("code").uppercase(),
            name = j.getString("name"),
            space = j.optString("space", j.getString("name")),
            rows = (0 until rowsJ.length()).map { arr(rowsJ.getJSONArray(it)) },
            extra = arr(j.optJSONArray("extra") ?: JSONArray()),
            builtin = builtin
        )
    }

    private fun dir(c: Context) = File(c.filesDir, "langs").also { it.mkdirs() }

    fun installed(c: Context): List<Lang> =
        (dir(c).listFiles { f -> f.extension == "json" } ?: emptyArray()).mapNotNull {
            try { fromJson(JSONObject(it.readText())) } catch (e: Exception) { null }
        }.sortedBy { it.code }

    /** Foydalanuvchi yoqqan tillar tartibida (UZ, UZC, RU, EN, keyin yuklab olinganlar). */
    fun active(c: Context): List<Lang> = builtins + installed(c)

    /** APK ichidagi tayyor paketlar (internetsiz o'rnatiladi). */
    fun bundled(c: Context): List<Lang> =
        (c.assets.list("langpacks") ?: emptyArray()).mapNotNull {
            try { fromJson(JSONObject(c.assets.open("langpacks/$it").bufferedReader().readText())) } catch (e: Exception) { null }
        }.sortedBy { it.name }

    fun installBundled(c: Context, code: String): Boolean = try {
        val text = c.assets.open("langpacks/${code.lowercase()}.json").bufferedReader().readText()
        fromJson(JSONObject(text))                       // tekshiruv
        File(dir(c), "${code.lowercase()}.json").writeText(text)
        true
    } catch (e: Exception) { false }

    fun remove(c: Context, code: String) { File(dir(c), "${code.lowercase()}.json").delete() }

    /** Server ro'yxati: {"packs":[{"code":"ar","name":"العربية","url":"https://.../ar.json"}]} — fon oqimida chaqiring. */
    fun fetchRemoteIndex(): List<Triple<String, String, String>> {
        val u = BuildConfig.PACKS_URL
        if (!u.startsWith("https://")) return emptyList()
        val conn = URL(u).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8000; conn.readTimeout = 8000
            conn.instanceFollowRedirects = false
            if (conn.responseCode != 200) return emptyList()
            val a = JSONObject(Net.readCapped(conn.inputStream, 100_000)).getJSONArray("packs")
            return (0 until minOf(a.length(), 50)).map {
                val o = a.getJSONObject(it); Triple(o.getString("code"), o.getString("name").take(40), o.getString("url"))
            }.filter { it.third.startsWith("https://") && Regex("[A-Za-z0-9_-]{1,16}").matches(it.first) }
        } finally { conn.disconnect() }
    }

    fun installRemote(c: Context, code: String, url: String): Boolean {
        if (!url.startsWith("https://")) return false
        if (!Regex("[A-Za-z0-9_-]{1,16}").matches(code)) return false   // yo'l almashtirishdan himoya
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 8000; conn.readTimeout = 8000
            conn.instanceFollowRedirects = false
            if (conn.responseCode != 200) return false
            val text = Net.readCapped(conn.inputStream, 100_000)
            fromJson(JSONObject(text))
            File(dir(c), "${code.lowercase()}.json").writeText(text)
            true
        } catch (e: Exception) { false } finally { conn.disconnect() }
    }
}

/** Emoji bo'limlari. Hozir tizim shrifti (Noto) ishlatiladi — Apple emoji rasmlari mualliflik huquqi bilan himoyalangan. */
object EmojiData {
    private fun e(s: String) = s.split(" ").filter { it.isNotBlank() }

    val categories: List<Pair<String, List<String>>> = listOf(
        "😀" to e("😀 😃 😄 😁 😆 😅 😂 🤣 🥲 ☺️ 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🥸 🤩 🥳 😏 😒 😞 😔 😟 😕 🙁 ☹️ 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🫡 🤭 🤫 🤥 😶 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕 🤑 🤠 😈 👿 💀 ☠️ 💩 🤡 👻 👽 🤖"),
        "👍" to e("👍 👎 👌 🤌 🤏 ✌️ 🤞 🫰 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ ✋ 🤚 🖐️ 🖖 👋 🤝 🙏 ✍️ 💪 👏 🙌 🫶 👐 🤲 🫱 🫲 👀 👂 👃 👄 🧠 🦷 👅"),
        "❤️" to e("❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❤️‍🔥 💕 💞 💓 💗 💖 💘 💝 💟 ♥️ 😻 💌 💋 🌹 🥀 🌷 🌸 💐 🎁 🎉 🎊 ✨ ⭐ 🌟 💫 🔥 💯 ✅ ❌ ⚡ 💥"),
        "🐶" to e("🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🙈 🙉 🙊 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🦋 🐌 🐞 🐢 🐍 🐙 🐬 🐳 🐟 🌵 🌲 🌳 🌴 🍀 🌻 🌼 🌙 ☀️ ⛅ ☁️ 🌧️ ❄️ 🌈"),
        "🍔" to e("🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🥑 🥕 🌽 🥔 🍞 🧀 🥚 🍳 🥞 🥓 🍗 🍖 🌭 🍔 🍟 🍕 🌮 🌯 🥙 🍝 🍜 🍲 🍣 🍚 🍰 🎂 🍫 🍬 🍭 🍪 🍩 ☕ 🍵 🥤 🍺 🥂"),
        "⚽" to e("⚽ 🏀 🏈 ⚾ 🎾 🏐 🏉 🎱 🏓 🥊 🏆 🥇 🥈 🥉 🎮 🎯 🎲 🎵 🎶 🎤 🎧 📱 💻 ⌚ 📷 💡 📚 ✏️ 📌 🔑 🔒 💰 💳 🚗 🚕 🚌 ✈️ 🚀 🏠 🕌 ⏰ 📞 🔔")
    )
}
