package uz.ozkey.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import java.io.File

/**
 * stroke == 0 bo'lsa chegara yo'q. rainbow == true bo'lsa tugma chegarasi/harfi rang almashtiradi.
 * photo == true bo'lsa fon — foydalanuvchi galereyadan tanlagan rasm (PhotoStore).
 */
class KeyTheme(
    val id: String,
    val name: String,
    val group: String,
    val bgTop: Int,
    val bgBottom: Int,
    val key: Int,
    val special: Int,
    val text: Int,
    val stroke: Int = 0,
    val rainbow: Boolean = false,
    val photo: Boolean = false
)

object Themes {
    const val DEFAULT_ID = "samarkand"
    const val PHOTO_ID = "photo"

    val groups = listOf("O'zbekona", "Oddiy", "Rangli", "Neon")

    private fun c(r: Int, g: Int, b: Int) = Color.rgb(r, g, b)
    private fun a(al: Int, r: Int, g: Int, b: Int) = Color.argb(al, r, g, b)

    val all: List<KeyTheme> = listOf(
        // ---- O'zbekona ----
        KeyTheme("samarkand", "Samarqand", "O'zbekona", c(5, 62, 84), c(0, 112, 128), c(10, 98, 116), c(6, 76, 94), c(228, 250, 250), c(217, 169, 62)),
        KeyTheme("registon", "Registon", "O'zbekona", c(10, 28, 88), c(22, 72, 152), c(24, 60, 132), c(16, 42, 104), c(255, 238, 190), c(232, 192, 76)),
        KeyTheme("buxoro", "Buxoro", "O'zbekona", c(104, 38, 20), c(160, 72, 32), c(142, 62, 32), c(112, 44, 22), c(255, 236, 208), c(234, 196, 96)),
        KeyTheme("atlas", "Atlas", "O'zbekona", c(62, 10, 86), c(206, 32, 92), c(98, 20, 112), c(128, 24, 110), c(255, 232, 130), c(255, 196, 64)),
        KeyTheme("paxta", "Paxta", "O'zbekona", c(251, 247, 237), c(240, 232, 214), Color.WHITE, c(232, 223, 202), c(14, 98, 108), c(217, 169, 62)),
        KeyTheme("ipak", "Ipak yo'li", "O'zbekona", c(78, 48, 18), c(150, 108, 38), c(168, 126, 48), c(124, 88, 32), c(255, 244, 214), c(255, 222, 130)),
        KeyTheme("anor", "Anor", "O'zbekona", c(84, 8, 28), c(142, 20, 46), c(122, 20, 44), c(150, 34, 60), c(255, 226, 232), c(255, 150, 170)),
        KeyTheme("feruza", "Feruza", "O'zbekona", c(176, 236, 232), c(120, 214, 212), c(240, 253, 252), c(150, 224, 220), c(8, 84, 92), c(14, 140, 150)),

        // ---- Oddiy ----
        KeyTheme("light", "Yorug'", "Oddiy", c(225, 230, 240), c(225, 230, 240), Color.WHITE, c(200, 208, 226), c(25, 34, 52)),
        KeyTheme("snow", "Oq", "Oddiy", c(246, 246, 248), c(246, 246, 248), Color.WHITE, c(230, 230, 236), c(20, 20, 24), c(222, 222, 228)),
        KeyTheme("dark", "Qora", "Oddiy", c(28, 28, 30), c(28, 28, 30), c(58, 58, 62), c(44, 44, 47), Color.WHITE),
        KeyTheme("amoled", "Sof qora", "Oddiy", c(0, 0, 0), c(0, 0, 0), c(24, 24, 26), c(38, 38, 42), Color.WHITE),
        KeyTheme("graphite", "Grafit", "Oddiy", c(36, 40, 48), c(48, 54, 66), c(66, 72, 86), c(52, 58, 70), c(232, 236, 246)),
        KeyTheme("sand", "Qum", "Oddiy", c(238, 230, 214), c(228, 216, 194), c(252, 248, 240), c(222, 210, 188), c(72, 54, 32)),

        // ---- Rangli ----
        KeyTheme("ocean", "Okean", "Rangli", c(4, 40, 70), c(0, 90, 120), c(10, 70, 100), c(14, 96, 130), c(200, 245, 255), c(70, 210, 230)),
        KeyTheme("mint", "Yalpiz", "Rangli", c(220, 245, 235), c(190, 235, 220), Color.WHITE, c(176, 224, 206), c(10, 80, 60)),
        KeyTheme("sakura", "Sakura", "Rangli", c(255, 225, 235), c(255, 200, 220), c(255, 245, 248), c(255, 214, 228), c(150, 40, 90)),
        KeyTheme("midnight", "Tungi osmon", "Rangli", c(10, 14, 40), c(30, 10, 60), c(24, 28, 70), c(40, 36, 96), c(180, 200, 255), c(90, 110, 255)),
        KeyTheme("gold", "Oltin", "Rangli", c(20, 16, 8), c(48, 36, 10), c(40, 32, 14), c(60, 46, 16), c(255, 205, 80), c(255, 190, 40)),
        KeyTheme("sunset", "Shafaq", "Rangli", c(255, 150, 90), c(214, 56, 124), a(70, 255, 255, 255), a(60, 0, 0, 0), Color.WHITE),
        KeyTheme("forest", "O'rmon", "Rangli", c(10, 40, 28), c(20, 72, 50), c(26, 66, 48), c(18, 50, 36), c(206, 255, 224)),
        KeyTheme("sky", "Osmon", "Rangli", c(150, 200, 255), c(196, 228, 255), Color.WHITE, c(170, 205, 245), c(20, 60, 110)),
        KeyTheme("coral", "Marjon", "Rangli", c(255, 202, 192), c(255, 172, 162), c(255, 241, 238), c(255, 210, 202), c(132, 32, 32)),
        KeyTheme("lemon", "Limon", "Rangli", c(255, 246, 176), c(255, 230, 120), c(255, 252, 228), c(250, 228, 130), c(92, 72, 0)),
        KeyTheme("lavender", "Lavanda", "Rangli", c(232, 222, 250), c(208, 194, 245), c(248, 244, 255), c(204, 190, 240), c(60, 30, 110)),
        KeyTheme("violet", "Binafsha", "Rangli", c(40, 10, 70), c(92, 22, 124), c(68, 28, 102), c(86, 38, 126), c(242, 218, 255)),

        // ---- Neon ----
        KeyTheme("neon", "Neon RGB", "Neon", c(8, 10, 22), c(8, 10, 22), c(16, 18, 36), c(28, 30, 56), c(0, 229, 255), c(0, 229, 255), rainbow = true),
        KeyTheme("aurora", "Aurora RGB", "Neon", c(6, 20, 24), c(24, 6, 40), c(14, 28, 40), c(24, 30, 56), c(120, 255, 200), c(120, 255, 200), rainbow = true),
        KeyTheme("neonpink", "Neon pushti", "Neon", c(14, 6, 20), c(14, 6, 20), c(26, 10, 36), c(38, 14, 52), c(255, 96, 204), c(255, 60, 190)),
        KeyTheme("neongreen", "Neon yashil", "Neon", c(4, 14, 8), c(4, 14, 8), c(8, 26, 14), c(12, 38, 22), c(70, 255, 136), c(40, 255, 110)),
        KeyTheme("neonorange", "Neon sariq", "Neon", c(16, 10, 2), c(16, 10, 2), c(32, 20, 6), c(46, 30, 8), c(255, 176, 48), c(255, 150, 20)),
        KeyTheme("neonblue", "Neon ko'k", "Neon", c(4, 8, 24), c(4, 8, 24), c(8, 16, 44), c(12, 24, 64), c(96, 150, 255), c(70, 120, 255))
    )

    /** Galereyadan tanlangan rasm uchun mavzu: tugmalar yarim shaffof. */
    val photo = KeyTheme(
        PHOTO_ID, "Mening rasmim", "Rasm", c(20, 20, 24), c(20, 20, 24),
        a(78, 255, 255, 255), a(110, 0, 0, 0), Color.WHITE, photo = true
    )

    fun byGroup(g: String) = all.filter { it.group == g }

    fun byId(id: String?): KeyTheme = when (id) {
        PHOTO_ID -> photo
        else -> all.firstOrNull { it.id == id } ?: all.first { it.id == DEFAULT_ID }
    }

    /** Klaviaturadagi "Mavzu" tugmasi aylanib o'tadigan ro'yxat. */
    fun cycle(ctx: Context): List<KeyTheme> =
        if (PhotoStore.exists(ctx)) all + photo else all
}

/** Foydalanuvchi tanlagan rasm shu yerda saqlanadi (ilovaning ichki xotirasi, hech qayerga yuborilmaydi). */
object PhotoStore {
    fun file(c: Context) = File(c.filesDir, "keyboard_bg.jpg")
    fun exists(c: Context) = file(c).let { it.exists() && it.length() > 0 }
    fun delete(c: Context) { file(c).delete() }
}

/** Rasmni klaviatura o'lchamiga "center-crop" qilib chizadi va ustidan qora qatlam (xiralik) soladi. */
class PhotoBackground(private val bmp: Bitmap, dimPercent: Int) : Drawable() {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val shade = Paint().apply { color = Color.argb((dimPercent.coerceIn(0, 90) * 255) / 100, 0, 0, 0) }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val s = maxOf(b.width().toFloat() / bmp.width, b.height().toFloat() / bmp.height)
        val dx = b.left + (b.width() - bmp.width * s) / 2f
        val dy = b.top + (b.height() - bmp.height * s) / 2f
        canvas.save()
        canvas.clipRect(b)
        canvas.translate(dx, dy)
        canvas.scale(s, s)
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        canvas.restore()
        canvas.drawRect(b, shade)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
