package uz.ozkey.keyboard

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.LinearLayout

class OZKeyInputMethod : InputMethodService() {

    private enum class Layer { LETTERS, SYMBOLS, EMOJI }

    private var layer = Layer.LETTERS
    private var caps = false
    private var language = "UZ"
    private var neon = true

    private lateinit var keyboard: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val keyViews = mutableListOf<Pair<TextView, GradientDrawable>>()
    private var animator: ValueAnimator? = null
    private var frame = 0

    private val specials = setOf("⇧", "⌫", "↵", "?123", "ABC", "🌐", "😊", "space")
    private val apostrophes = setOf("'", "’", "‘", "ʻ", "ʼ", "＇")

    private val repeatDelete = object : Runnable {
        override fun run() {
            doBackspace()
            handler.postDelayed(this, 55)
        }
    }

    private val emojiRows = listOf(
        listOf("😀", "😂", "🥰", "😍", "😎", "🤔", "😢", "😡"),
        listOf("👍", "👏", "🙏", "💪", "🔥", "❤️", "💔", "🎉"),
        listOf("😊", "😁", "😉", "😘", "🤝", "👌", "✌️", "🙌"),
        listOf("🌹", "⭐", "☀️", "🌙", "🎁", "💯", "✅", "❌")
    )

    private fun currentRows(): List<List<String>> = when (layer) {
        Layer.SYMBOLS -> listOf(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            listOf("@", "#", "\$", "%", "&", "-", "+", "(", ")", "/"),
            listOf("*", "\"", "'", ":", ";", "!", "?", "~", "⌫"),
            listOf("ABC", ",", "space", ".", "↵")
        )
        Layer.EMOJI -> emojiRows + listOf(listOf("ABC", "space", "⌫"))
        Layer.LETTERS -> when (language) {
            "RU" -> listOf(
                digits,
                listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х"),
                listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"),
                listOf("⇧", "я", "ч", "с", "м", "и", "т", "ь", "б", "ю", "⌫"),
                listOf("ё", "ъ", "-", "!", "?", "🌐"),
                listOf("?123", "😊", ",", "space", ".", "↵")
            )
            "EN" -> latin(listOf("'", "-", "!", "?", "🌐"))
            else -> latin(listOf("Ğ", "Ö", "Ş", "Ç", "🌐"))
        }
    }

    private val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

    private fun latin(special: List<String>) = listOf(
        digits,
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"),
        special,
        listOf("?123", "😊", ",", "space", ".", "↵")
    )

    override fun onCreate() {
        super.onCreate()
        neon = getSharedPreferences("ozkey", MODE_PRIVATE).getBoolean("neon", true)
    }

    override fun onCreateInputView(): View {
        keyboard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        render()
        return keyboard
    }

    private fun render() {
        keyboard.removeAllViews()
        keyViews.clear()
        keyboard.setBackgroundColor(if (neon) Color.rgb(8, 10, 22) else Color.rgb(225, 230, 240))
        currentRows().forEachIndexed { rowIndex, row ->
            val rowHeight = if (layer == Layer.LETTERS && rowIndex == 0) 40 else 48
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.forEach { key ->
                val weight = when (key) {
                    "space" -> if (layer == Layer.EMOJI) 4f else 3.2f
                    "?123", "ABC" -> 1.5f
                    "⇧", "⌫", "↵", "🌐", "😊" -> 1.35f
                    else -> 1f
                }
                rowLayout.addView(makeKey(key), LinearLayout.LayoutParams(0, dp(rowHeight), weight))
            }
            keyboard.addView(rowLayout, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
        startGlow()
    }

    private fun toggleTheme() {
        neon = !neon
        getSharedPreferences("ozkey", MODE_PRIVATE).edit().putBoolean("neon", neon).apply()
        render()
    }

    private fun makeKey(key: String): View {
        val special = key in specials
        val bg = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            if (neon) {
                setColor(if (special) Color.rgb(28, 30, 56) else Color.rgb(16, 18, 36))
                setStroke(dp(2), Color.rgb(0, 229, 255))
            } else {
                setColor(if (special) Color.rgb(200, 208, 226) else Color.WHITE)
            }
        }
        val label = TextView(this).apply {
            text = when (key) {
                "space" -> when (language) { "EN" -> "English"; "RU" -> "Русский"; else -> "O'ZKEY" }
                else -> displayKey(key)
            }
            textSize = when {
                key == "space" || key in specials -> 14f
                layer == Layer.EMOJI -> 24f
                else -> 20f
            }
            gravity = Gravity.CENTER
            includeFontPadding = false
            setTextColor(if (neon) Color.rgb(0, 229, 255) else Color.rgb(25, 34, 52))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = bg
        }
        // The whole cell (including the gaps) is touchable, so there are no dead zones.
        val cell = FrameLayout(this).apply {
            setPadding(dp(2), dp(2), dp(2), dp(2))
            addView(label, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        var longFired = false
        val longPress = Runnable { longFired = true; toggleTheme() }
        cell.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    label.alpha = 0.5f
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    when (key) {
                        "⌫" -> { doBackspace(); handler.postDelayed(repeatDelete, 400) }
                        "space" -> { longFired = false; handler.postDelayed(longPress, 500) }
                        else -> handleKey(key)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    label.alpha = 1f
                    handler.removeCallbacks(repeatDelete)
                    if (key == "space") {
                        handler.removeCallbacks(longPress)
                        if (!longFired) handleKey("space")
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    label.alpha = 1f
                    handler.removeCallbacks(repeatDelete)
                    handler.removeCallbacks(longPress)
                }
            }
            true
        }
        keyViews.add(Pair(label, bg))
        return cell
    }

    private fun hue(h: Float): Int =
        Color.HSVToColor(floatArrayOf(((h % 360f) + 360f) % 360f, 0.85f, 1f))

    private fun startGlow() {
        animator?.cancel()
        animator = null
        if (!neon || !::keyboard.isInitialized) return
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 8000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { a ->
                frame++
                if (frame % 3 != 0) return@addUpdateListener
                val base = a.animatedValue as Float
                keyViews.forEachIndexed { i, pair ->
                    val c = hue(base + i * 9f)
                    pair.second.setStroke(dp(2), c)
                    pair.first.setTextColor(c)
                }
            }
            start()
        }
    }

    private fun displayKey(key: String): String {
        if (key.length != 1) return key
        return if (caps) key.uppercase() else key
    }

    private fun doBackspace() {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }

    private fun handleKey(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "⇧" -> { caps = !caps; render() }
            "↵" -> {
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                if (action != null && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(action)
                } else {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
            }
            "space" -> ic.commitText(" ", 1)
            "🌐" -> {
                language = when (language) { "UZ" -> "EN"; "EN" -> "RU"; else -> "UZ" }
                caps = false
                layer = Layer.LETTERS
                render()
            }
            "?123" -> { layer = Layer.SYMBOLS; render() }
            "ABC" -> { layer = Layer.LETTERS; render() }
            "😊" -> { layer = Layer.EMOJI; render() }
            else -> {
                val value = if (key.length == 1 && caps) key.uppercase() else key
                ic.commitText(value, 1)
                if (language == "UZ") convertDigraph()
                if (caps) { caps = false; render() }
            }
        }
    }

    private fun convertDigraph() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        if (before.length < 2) return
        val first = before[0].lowercaseChar()
        val second = before[1]
        val upper = before[0].isUpperCase()
        val replacement = when {
            first == 's' && second.lowercaseChar() == 'h' -> if (upper) "Ş" else "ş"
            first == 'c' && second.lowercaseChar() == 'h' -> if (upper) "Ç" else "ç"
            first == 'g' && second.toString() in apostrophes -> if (upper) "Ğ" else "ğ"
            first == 'o' && second.toString() in apostrophes -> if (upper) "Ö" else "ö"
            else -> null
        }
        if (replacement != null) {
            ic.deleteSurroundingText(2, 0)
            ic.commitText(replacement, 1)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        startGlow()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        animator?.cancel()
        handler.removeCallbacks(repeatDelete)
    }

    override fun onDestroy() {
        animator?.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
