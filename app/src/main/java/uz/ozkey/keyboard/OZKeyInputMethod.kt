package uz.ozkey.keyboard

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class OZKeyInputMethod : InputMethodService() {

    private enum class Layer { LETTERS, SYMBOLS, EMOJI }

    private class Grabbed(val text: String, val hadSelection: Boolean, val before: Int, val after: Int)

    private var layer = Layer.LETTERS
    private var caps = false
    private var capsLock = false
    private var drawnCaps = false
    private var lastShiftTap = 0L
    private var language = "UZ"
    private var neon = true
    private var scale = 100
    private var translating = false
    private var pendingUndo: String? = null

    private lateinit var keyboard: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val keyViews = mutableListOf<Pair<TextView, GradientDrawable>>()
    private var animator: ValueAnimator? = null
    private var frame = 0

    private val toolKeys = listOf("TR", "CONV", "COPY", "SET")
    private val specials = setOf("⇧", "⌫", "↵", "?123", "ABC", "🌐", "😊", "space") + toolKeys
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

    private val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private val bottomRow = listOf("?123", "🌐", "😊", ",", "space", ".", "↵")

    private fun currentRows(): List<List<String>> = when (layer) {
        Layer.SYMBOLS -> listOf(
            digits,
            listOf("@", "#", "\$", "%", "&", "-", "+", "(", ")", "/"),
            listOf("*", "\"", "'", ":", ";", "!", "?", "~", "⌫"),
            listOf("_", "=", "<", ">", "[", "]", "{", "}", "\\", "|"),
            listOf("ABC", "🌐", ",", "space", ".", "↵")
        )
        Layer.EMOJI -> emojiRows + listOf(listOf("ABC", "space", "⌫"))
        Layer.LETTERS -> listOf(toolKeys) + when (language) {
            "RU" -> listOf(
                digits,
                listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х"),
                listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"),
                listOf("⇧", "я", "ч", "с", "м", "и", "т", "ь", "б", "ю", "⌫"),
                listOf("ё", "ъ", "-", "!", "?"),
                bottomRow
            )
            "EN" -> latin(listOf("'", "-", "!", "?", "@"))
            else -> latin(listOf("Ğ", "Ö", "Ş", "Ç", "'"))
        }
    }

    private fun latin(extra: List<String>) = listOf(
        digits,
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"),
        extra,
        bottomRow
    )

    private fun prefs() = getSharedPreferences("ozkey", MODE_PRIVATE)

    private fun loadPrefs() {
        neon = prefs().getBoolean("neon", true)
        scale = prefs().getInt("scale", 100).coerceIn(80, 140)
    }

    override fun onCreate() {
        super.onCreate()
        loadPrefs()
    }

    override fun onCreateInputView(): View {
        keyboard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(6), dp(4), dp(6))
            // Android 15+: pastki navigatsiya paneli klaviaturani yopib qo'ymasin
            if (Build.VERSION.SDK_INT >= 35) {
                setOnApplyWindowInsetsListener { v, insets ->
                    val bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
                    v.setPadding(dp(4), dp(6), dp(4), dp(6) + bottom)
                    insets
                }
            }
        }
        render()
        return keyboard
    }

    private fun rowHeightDp(row: List<String>): Int {
        val base = when {
            row === toolKeys -> 34
            layer == Layer.LETTERS && row === digits -> 40
            else -> 48
        }
        return (base * scale / 100f).toInt()
    }

    private fun render() {
        if (!::keyboard.isInitialized) return
        drawnCaps = caps
        keyboard.removeAllViews()
        keyViews.clear()
        keyboard.setBackgroundColor(if (neon) Color.rgb(8, 10, 22) else Color.rgb(225, 230, 240))
        currentRows().forEach { row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.forEach { key ->
                val weight = when (key) {
                    "space" -> if (layer == Layer.EMOJI) 4f else 3.2f
                    "?123", "ABC" -> 1.5f
                    "⇧", "⌫", "↵", "🌐", "😊" -> 1.35f
                    "SET" -> 0.7f
                    "CONV" -> 1.4f
                    else -> 1f
                }
                rowLayout.addView(makeKey(key), LinearLayout.LayoutParams(0, dp(rowHeightDp(row)), weight))
            }
            keyboard.addView(rowLayout, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
        startGlow()
    }

    private fun labelFor(key: String): String = when (key) {
        "space" -> when (language) { "EN" -> "English"; "RU" -> "Русский"; else -> "ÖZKEY" }
        "TR" -> if (translating) "…" else "→ UZ"
        "CONV" -> "ÖZ⇄std"
        "COPY" -> "Nusxa"
        "SET" -> "⚙"
        "⇧" -> if (capsLock) "⇪" else "⇧"
        else -> if (key.length <= 2 && key.any { it.isLetter() } && caps) key.uppercase() else key
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
            text = labelFor(key)
            textSize = when {
                key in toolKeys -> 12f
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
        // Butun katak (oraliqlar ham) bosiladi — "o'lik zona" yo'q.
        val cell = FrameLayout(this).apply {
            setPadding(dp(2), dp(2), dp(2), dp(2))
            addView(label, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        cell.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    label.alpha = 0.5f
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (key == "⌫") { doBackspace(); handler.postDelayed(repeatDelete, 400) }
                    else handleKey(key)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    label.alpha = 1f
                    handler.removeCallbacks(repeatDelete)
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
                if (frame % 6 != 0) return@addUpdateListener   // batareyani tejash
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

    // ---------- matn bilan ishlash ----------

    /** Parol, email, URL maydonlarida avtomatik almashtirish va tarjima ishlamaydi. */
    private fun isRawField(): Boolean {
        val t = currentInputEditorInfo?.inputType ?: return false
        val v = t and InputType.TYPE_MASK_VARIATION
        return when (t and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> v == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                v == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                v == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                v == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                v == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
                v == InputType.TYPE_TEXT_VARIATION_URI
            InputType.TYPE_CLASS_NUMBER -> v == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }

    private fun grabText(ic: InputConnection): Grabbed? {
        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) return Grabbed(sel.toString(), true, 0, 0)
        val b = ic.getTextBeforeCursor(4000, 0)?.toString().orEmpty()
        val a = ic.getTextAfterCursor(4000, 0)?.toString().orEmpty()
        if (b.isEmpty() && a.isEmpty()) return null
        return Grabbed(b + a, false, b.length, a.length)
    }

    private fun replaceText(ic: InputConnection, g: Grabbed, newText: String) {
        ic.beginBatchEdit()
        if (!g.hadSelection) ic.deleteSurroundingText(g.before, g.after)
        ic.commitText(newText, 1)
        ic.endBatchEdit()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun doTranslate(ic: InputConnection) {
        if (translating) return
        if (isRawField()) { toast("Bu maydonda tarjima o'chirilgan"); return }
        val g = grabText(ic) ?: run { toast("Avval matn yozing yoki belgilang"); return }
        translating = true
        render()
        val key = prefs().getString("apikey", "") ?: ""
        Thread {
            val result = try { Result.success(Translator.toUzbek(g.text, key)) }
            catch (e: Exception) { Result.failure<String>(e) }
            handler.post {
                translating = false
                result.onSuccess { out ->
                    val c = currentInputConnection
                    val now = c?.let { grabText(it) }
                    if (c == null || now == null || now.text != g.text) toast("Matn o'zgargan, qayta urinib ko'ring")
                    else replaceText(c, now, out)
                }.onFailure { toast("Tarjima bo'lmadi: internet yoki API kalitni tekshiring") }
                render()
            }
        }.start()
    }

    /** ÖZ alifbosi bo'lsa -> standart; standart bo'lsa -> ÖZ alifbosi. */
    private fun doConvert(ic: InputConnection) {
        if (isRawField()) return
        val g = grabText(ic) ?: run { toast("Avval matn yozing yoki belgilang"); return }
        val out = if (Alphabet.hasNew(g.text)) Alphabet.toStandard(g.text) else Alphabet.toNew(g.text)
        if (out != g.text) replaceText(ic, g, out)
    }

    /** Standart (oddiy klaviaturada yoziladigan) ko'rinishni buferga nusxalaydi, maydon o'zgarmaydi. */
    private fun doCopyStandard(ic: InputConnection) {
        if (isRawField()) return
        val g = grabText(ic) ?: run { toast("Avval matn yozing yoki belgilang"); return }
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("ozkey", Alphabet.toStandard(g.text)))
        toast("Standart yozuvda nusxalandi")
    }

    // ---------- tugmalar ----------

    private fun doBackspace() {
        val ic = currentInputConnection ?: return
        val undo = pendingUndo
        if (undo != null) {            // ş -> sh kabi avtomatik almashtirishni bekor qilish
            pendingUndo = null
            ic.deleteSurroundingText(1, 0)
            ic.commitText(undo, 1)
            return
        }
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }

    /** Boshlang'ich katta harf (gap boshi) holatini sozlaydi. */
    private fun syncCaps() {
        if (capsLock) return
        val ic = currentInputConnection
        val want = ic != null && !isRawField() &&
            ic.getCursorCapsMode(currentInputEditorInfo?.inputType ?: 0) != 0
        caps = want
        if (caps != drawnCaps) render()
    }

    private fun handleKey(key: String) {
        val ic = currentInputConnection ?: return
        pendingUndo = null
        when (key) {
            "⇧" -> {
                val now = SystemClock.uptimeMillis()
                if (capsLock) { capsLock = false; caps = false }
                else if (caps && now - lastShiftTap < 400) capsLock = true
                else caps = !caps
                lastShiftTap = now
                render()
            }
            "↵" -> {
                val info = currentInputEditorInfo
                val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                val noAction = info != null && (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
                if (!noAction && action != null && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(action)
                } else {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
                syncCaps()
            }
            "space" -> { ic.commitText(" ", 1); syncCaps() }
            "🌐" -> {
                language = when (language) { "UZ" -> "EN"; "EN" -> "RU"; else -> "UZ" }
                caps = false; capsLock = false
                layer = Layer.LETTERS
                render()
                syncCaps()
            }
            "?123" -> { layer = Layer.SYMBOLS; render() }
            "ABC" -> { layer = Layer.LETTERS; render(); syncCaps() }
            "😊" -> { layer = Layer.EMOJI; render() }
            "TR" -> doTranslate(ic)
            "CONV" -> doConvert(ic)
            "COPY" -> doCopyStandard(ic)
            "SET" -> startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            else -> {
                val value = if (caps && key.any { it.isLetter() }) key.uppercase() else key
                ic.commitText(value, 1)
                if (language == "UZ" && !isRawField()) convertDigraph()
                if (!capsLock) caps = false
                syncCaps()
                if (caps != drawnCaps) render()
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
            pendingUndo = before          // ⌫ bosilsa asl "sh" qaytadi
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        loadPrefs()
        pendingUndo = null
        render()
        syncCaps()
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
