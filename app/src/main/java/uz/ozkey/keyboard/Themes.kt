package uz.ozkey.keyboard

import android.graphics.Color

/** stroke == 0 bo'lsa chegara yo'q. rainbow == true bo'lsa tugma chegarasi/harfi rang almashtiradi. */
class KeyTheme(
    val id: String,
    val name: String,
    val bgTop: Int,
    val bgBottom: Int,
    val key: Int,
    val special: Int,
    val text: Int,
    val stroke: Int = 0,
    val rainbow: Boolean = false,
    val premium: Boolean = false
)

object Themes {
    private fun c(r: Int, g: Int, b: Int) = Color.rgb(r, g, b)

    val all = listOf(
        KeyTheme("neon", "Neon RGB", c(8, 10, 22), c(8, 10, 22), c(16, 18, 36), c(28, 30, 56), c(0, 229, 255), c(0, 229, 255), rainbow = true),
        KeyTheme("light", "Yorug'", c(225, 230, 240), c(225, 230, 240), Color.WHITE, c(200, 208, 226), c(25, 34, 52)),
        KeyTheme("dark", "Qora", c(28, 28, 30), c(28, 28, 30), c(58, 58, 62), c(44, 44, 47), Color.WHITE),
        KeyTheme("midnight", "Midnight", c(10, 14, 40), c(30, 10, 60), c(24, 28, 70), c(40, 36, 96), c(180, 200, 255), c(90, 110, 255), premium = true),
        KeyTheme("sakura", "Sakura", c(255, 225, 235), c(255, 200, 220), c(255, 245, 248), c(255, 214, 228), c(150, 40, 90), premium = true),
        KeyTheme("gold", "Oltin", c(20, 16, 8), c(48, 36, 10), c(40, 32, 14), c(60, 46, 16), c(255, 205, 80), c(255, 190, 40), premium = true),
        KeyTheme("ocean", "Okean", c(4, 40, 70), c(0, 90, 120), c(10, 70, 100), c(14, 96, 130), c(200, 245, 255), c(70, 210, 230), premium = true),
        KeyTheme("aurora", "Aurora RGB", c(6, 20, 24), c(24, 6, 40), c(14, 28, 40), c(24, 30, 56), c(120, 255, 200), c(120, 255, 200), rainbow = true, premium = true),
        KeyTheme("mint", "Yalpiz", c(220, 245, 235), c(190, 235, 220), Color.WHITE, c(176, 224, 206), c(10, 80, 60), premium = true)
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id } ?: all[0]
}
