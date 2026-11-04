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
import android.widget.ScrollView
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
    private var lang: Lang = Languages.UZ
    private var target = "uz"
    private var theme: KeyTheme = Themes.all[0]
    private var haptic = true
    private var emojiCat = 0
    private var scale = 100
    private var translating = false
    private var pendingUndo: String? = null

    private lateinit var keyboard: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val keyViews = mutableListOf<Pair<TextView, GradientDrawable>>()
    private var animator: ValueAnimator? = null
    private var frame = 0

    private val toolKeys = listOf("TR", "TGT", "CONV", "COPY", "SET")
    private val specials = setOf("⇧", "⌫", "↵", "?123", "ABC", "🌐", "😊", "space") + toolKeys
    private val apostrophes = setOf("'", "’", "‘", "ʻ", "ʼ", "＇")

    private val repeatDelete = object : Runnable {
        override fun run() {
            doBackspace()
            handler.postDelayed(this, 55)
        }
    }

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
        Layer.EMOJI -> emptyList()   // renderEmoji() chizadi
        Layer.LETTERS -> {
            val r = lang.rows
            listOf(toolKeys, digits, r.getOrElse(0) { emptyList() }, r.getOrElse(1) { emptyList() },
                listOf("⇧") + r.getOrElse(2) { emptyList() } + listOf("⌫")) +
                (if (lang.extra.isNotEmpty()) listOf(lang.extra) else emptyList()) + listOf(bottomRow)
        }
    }

    private fun prefs() = getSharedPreferences("ozkey", MODE_PRIVATE)

    private fun loadPrefs() {
        theme = Themes.byId(prefs().getString("theme", "neon")).let {
            if (it.premium && !Premium.isPremium(this)) Themes.all[0] else it
        }
        scale = prefs().getInt("scale", 100).coerceIn(80, 140)
        haptic = prefs().getBoolean("haptic", true)
        target = prefs().getString("target", "uz") ?: "uz"
        val code = prefs().getString("lang", "UZ")
        lang = Languages.active(this).firstOrNull { it.code == code } ?: Languages.UZ
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
        keyboard.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(theme.bgTop, theme.bgBottom)
        )
        if (layer == Layer.EMOJI) renderEmoji() else {
            currentRows().forEach { row -> keyboard.addView(buildRow(row, rowHeightDp(row))) }
        }
        startGlow()
    }

    private fun weightOf(key: String) = when {
        key == "space" -> if (layer == Layer.EMOJI) 4f else 3.2f
        key == "?123" || key == "ABC" -> 1.5f
        key == "⇧" || key == "⌫" || key == "↵" || key == "🌐" || key == "😊" -> 1.35f
        key == "SET" -> 0.7f
        key == "CONV" || key == "TR" -> 1.3f
        else -> 1f
    }

    private fun buildRow(row: List<String>, heightDp: Int): LinearLayout {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        row.forEach { key -> rowLayout.addView(makeKey(key), LinearLayout.LayoutParams(0, dp(heightDp), weightOf(key))) }
        return rowLayout
    }

    private fun renderEmoji() {
        val cats = EmojiData.categories
        keyboard.addView(buildRow(cats.indices.map { "cat:$it" }, (40 * scale / 100f).toInt()))
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val h = (46 * scale / 100f).toInt()
        cats[emojiCat].second.chunked(8).forEach { chunk ->
            val padded = chunk.map { "emo:$it" } + List(8 - chunk.size) { "pad" }
            grid.addView(buildRow(padded, h))
        }
        keyboard.addView(
            ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(grid) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(h * 4))
        )
        keyboard.addView(buildRow(listOf("ABC", "space", "⌫"), h))
    }

    private fun labelFor(key: String): String = when {
        key == "space" -> lang.space
        key == "TR" -> if (translating) "…" else "→ " + Translator.label(target)
        key == "TGT" -> "🌍"
        key == "CONV" -> "ÖZ⇄std"
        key == "COPY" -> "Nusxa"
        key == "SET" -> "⚙"
        key == "⇧" -> if (capsLock) "⇪" else "⇧"
        key.startsWith("cat:") -> EmojiData.categories[key.substring(4).toInt()].first
        key.startsWith("emo:") -> key.substring(4)
        key == "pad" -> ""
        else -> if (key.length <= 2 && key.any { it.isLetter() } && caps) key.uppercase() else key
    }

    private fun makeKey(key: String): View {
        if (key == "pad") return View(this)
        val special = key in specials || key.startsWith("cat:")
        val isEmo = key.startsWith("emo:") || key.startsWith("cat:")
        val bg = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(if (special) theme.special else theme.key)
            if (theme.stroke != 0) setStroke(dp(2), theme.stroke)
        }
        val label = TextView(this).apply {
            text = labelFor(key)
            textSize = when {
                isEmo -> 24f
                key in toolKeys -> 12f
                key == "space" || key in specials -> 14f
                else -> 20f
            }
            gravity = Gravity.CENTER
            includeFontPadding = false
            setTextColor(theme.text)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = bg
        }
        // Butun katak (oraliqlar ham) bosiladi — "o'lik zona" yo'q.
        val cell = FrameLayout(this).apply {
            setPadding(dp(2), dp(2), dp(2), dp(2))
            addView(label, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        // Emoji ro'yxati aylantirilganda tasodifiy bosilmasin: tugma ACTION_UP da ishlaydi.
        val onUp = isEmo
        var downX = 0f
        var downY = 0f
        var moved = false
        cell.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; moved = false
                    label.alpha = 0.5f
                    if (haptic) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (onUp) { /* ACTION_UP da */ }
                    else if (key == "⌫") { doBackspace(); handler.postDelayed(repeatDelete, 400) }
                    else handleKey(key)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(e.rawY - downY) > dp(10) || Math.abs(e.rawX - downX) > dp(10)) moved = true
                }
                MotionEvent.ACTION_UP -> {
                    label.alpha = 1f
                    handler.removeCallbacks(repeatDelete)
                    if (onUp && !moved) handleKey(key)
                }
                MotionEvent.ACTION_CANCEL -> {
                    label.alpha = 1f
                    handler.removeCallbacks(repeatDelete)
                }
            }
            true
        }
        if (!isEmo) keyViews.add(Pair(label, bg))
        return cell
    }

    private fun hue(h: Float): Int =
        Color.HSVToColor(floatArrayOf(((h % 360f) + 360f) % 360f, 0.85f, 1f))

    private fun startGlow() {
        animator?.cancel()
        animator = null
        if (!theme.rainbow || !::keyboard.isInitialized) return
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
            val result = try { Result.success(Translator.translate(g.text, target, key)) }
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
        val hasCyr = g.text.any { it in '\u0400'..'\u04FF' }
        val out = when {
            hasCyr -> Alphabet.toNew(Script.cyrToLat(g.text))     // kirill -> ÖZ lotin
            Alphabet.hasNew(g.text) -> Alphabet.toStandard(g.text)
            else -> Alphabet.toNew(g.text)
        }
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
                val all = Languages.active(this)
                val i = all.indexOfFirst { it.code == lang.code }
                lang = all[(i + 1) % all.size]
                prefs().edit().putString("lang", lang.code).apply()
                caps = false; capsLock = false
                layer = Layer.LETTERS
                render()
                syncCaps()
            }
            "?123" -> { layer = Layer.SYMBOLS; render() }
            "ABC" -> { layer = Layer.LETTERS; render(); syncCaps() }
            "😊" -> { layer = Layer.EMOJI; render() }
            "TR" -> doTranslate(ic)
            "TGT" -> {
                val t = Translator.targets
                target = t[(t.indexOf(target) + 1) % t.size]
                prefs().edit().putString("target", target).apply()
                toast("Tarjima tili: " + when (target) { "uz" -> "O'zbekcha (lotin)"; "uz-cyr" -> "Ўзбекча (кирилл)"; "ru" -> "Русский"; else -> "English" })
                render()
            }
            "CONV" -> doConvert(ic)
            "COPY" -> doCopyStandard(ic)
            "SET" -> startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            else -> if (key.startsWith("cat:")) {
                emojiCat = key.substring(4).toInt(); render()
            } else if (key.startsWith("emo:")) {
                ic.commitText(key.substring(4), 1)
            } else {
                val value = if (caps && key.any { it.isLetter() }) key.uppercase() else key
                ic.commitText(value, 1)
                if (lang.uzDigraph && !isRawField()) convertDigraph()
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
