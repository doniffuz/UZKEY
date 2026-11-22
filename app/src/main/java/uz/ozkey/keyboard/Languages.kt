package uz.ozkey.keyboard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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
        listOf("ğ", "ö", "ş", "ç", "'"), uzDigraph = true)   // ÖZ 30-tugmali layout: apostrof pastki panelda qoladi

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
