package uz.ozkey.keyboard

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * O'zbek naqshi (girih / "sakkizburchak yulduz") bilan bezatilgan sarlavha foni:
 * feruza → to'q ko'k gradient, ustida yengil naqsh, pastida oltin chiziq. Pastki burchaklari yumaloq.
 */
class HeaderDrawable(
    private val colorA: Int,
    private val colorB: Int,
    private val gold: Int,
    private val cell: Float,
    private val radius: Float,
    private val line: Float
) : Drawable() {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(46, 255, 255, 255)
        strokeWidth = line
    }
    private val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold }
    private val clip = Path()
    private val pattern = Path()

    override fun onBoundsChange(b: android.graphics.Rect) {
        super.onBoundsChange(b)
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        fill.shader = LinearGradient(0f, 0f, w, h, colorA, colorB, Shader.TileMode.CLAMP)
        clip.reset()
        clip.addRoundRect(
            RectF(0f, 0f, w, h),
            floatArrayOf(0f, 0f, 0f, 0f, radius, radius, radius, radius),
            Path.Direction.CW
        )
        pattern.reset()
        val r = cell * 0.34f
        val d = r * 1.41421f
        var y = cell / 2f
        var row = 0
        while (y < h + cell) {
            var x = if (row % 2 == 0) cell / 2f else cell
            while (x < w + cell) {
                // ikkita kvadrat (45° burilgan) = sakkizburchak yulduz
                pattern.addRect(x - r, y - r, x + r, y + r, Path.Direction.CW)
                pattern.moveTo(x, y - d)
                pattern.lineTo(x + d, y)
                pattern.lineTo(x, y + d)
                pattern.lineTo(x - d, y)
                pattern.close()
                pattern.addCircle(x, y, r * 0.32f, Path.Direction.CW)
                x += cell
            }
            y += cell / 2f
            row++
        }
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        canvas.save()
        canvas.clipPath(clip)
        canvas.drawRect(0f, 0f, b.width().toFloat(), b.height().toFloat(), fill)
        canvas.drawPath(pattern, ink)
        // oltin chiziq (pastda)
        canvas.drawRect(0f, b.height() - line * 2.2f, b.width().toFloat(), b.height().toFloat(), goldPaint)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) { fill.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { fill.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
