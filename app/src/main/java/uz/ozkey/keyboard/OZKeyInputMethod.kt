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

    private enum class Layer { LETTERS, SYMBOLS, EMOJI, TOOLS, PHRASES }

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
    private var oneHand = false
    private var emojiCat = 0
    private var scale = 100
    private var translating = false
    private var pendingUndo: String? = null
    private var navBottom = 0

    // Tarjima paneli: yozilgan matn ilovaga emas, shu yerga tushadi
    private var trMode = false
    private val trBuf = StringBuilder()
    private var trView: TextView? = null

    private lateinit var keyboard: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val keyViews = mutableListOf<Pair<TextView, GradientDrawable>>()
    private var animator: ValueAnimator? = null
    private var frame = 0

    private val topBar = listOf("LANGLAT", "LANGCYR", "sp", "😊", "TOOLS", "HIDE")
    private val tileKeys = listOf(
        "t:TR", "t:FIELD", "t:CLIP", "t:PHR",
        "t:CONV", "t:COPY", "t:THEME", "t:NIGHT",
        "t:HAPTIC", "t:ONE", "t:TGT", "t:SET"
    )
    private val specials = setOf(
        "⇧", "⌫", "↵", "?123", "ABC", "🌐", "😊", "space",
        "LANGLAT", "LANGCYR", "TOOLS", "HIDE", "tsrc", "ttgt", "tx", "tok"
    )
    private val apostrophes = setOf("'", "’", "‘", "ʻ", "ʼ", "＇")

    private val repeatDelete = object : Runnable {
        override fun run() {
            doBackspace()
            handler.postDelayed(this, 55)
        }
    }

    private val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private val bottomRow = listOf("?123", "🌐", "😊", ",", "space", ".", "↵")

    private fun isScrollKey(k: String) = k.startsWith("emo:") || k.startsWith("ph:")
    private fun isSpecialKey(k: String) = k in specials || k.startsWith("cat:") || k.startsWith("t:")

    private fun lettersRows(): List<List<String>> {
        val r = lang.rows
        return listOf(
            digits,
            r.getOrElse(0) { emptyList() },
            r.getOrElse(1) { emptyList() },
            listOf("⇧") + r.getOrElse(2) { emptyList() } + listOf("⌫")
        ) + (if (lang.extra.isNotEmpty()) listOf(lang.extra) else emptyList()) + listOf(bottomRow)
    }

    private fun symbolRows(): List<List<String>> = listOf(
        digits,
        listOf("@", "#", "\$", "%", "&", "-", "+", "(", ")", "/"),
        listOf("*", "\"", "'", ":", ";", "!", "?", "~", "⌫"),
        listOf("_", "=", "<", ">", "[", "]", "{", "}", "\\", "|"),
        listOf("ABC", "🌐", ",", "space", ".", "↵")
    )

    private fun prefs() = getSharedPreferences("ozkey", MODE_PRIVATE)

    private fun loadPrefs() {
        theme = Themes.byId(prefs().getString("theme", "neon")).let {
            if (it.premium && !Premium.isPremium(this)) Themes.all[0] else it
        }
        scale = prefs().getInt("scale", 100).coerceIn(80, 140)
        haptic = prefs().getBoolean("haptic", true)
        oneHand = prefs().getBoolean("onehand", false)
        target = prefs().getString("target", "uz") ?: "uz"
        val code = prefs().getString("lang", "UZ")
        lang = Languages.active(this).firstOrNull { it.code == code } ?: Languages.UZ
    }

    override fun onCreate() {
        super.onCreate()
        loadPrefs()
    }

    private fun applyPad() {
        val extra = if (oneHand) (resources.displayMetrics.widthPixels * 0.25f).toInt() else 0
        keyboard.setPadding(dp(4), dp(6), dp(4) + extra, dp(6) + navBottom)
    }

    override fun onCreateInputView(): View {
        keyboard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Android 15+: pastki navigatsiya paneli klaviaturani yopib qo'ymasin
            if (Build.VERSION.SDK_INT >= 35) {
                setOnApplyWindowInsetsListener { _, insets ->
                    navBottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
                    applyPad()
                    insets
                }
            }
        }
        applyPad()
        render()
        return keyboard
    }

    private fun rowHeightDp(row: List<String>): Int {
        val base = when {
            row === topBar -> 34
            layer == Layer.LETTERS && row === digits -> 40
            else -> 48
        }
        return (base * scale / 100f).toInt()
    }

    private fun render() {
        if (!::keyboard.isInitialized) return
        drawnCaps = caps
        trView = null
        keyboard.removeAllViews()
        keyViews.clear()
        keyboard.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(theme.bgTop, theme.bgBottom)
        )
        when (layer) {
            Layer.EMOJI -> renderEmoji()
            Layer.TOOLS -> renderTools()
            Layer.PHRASES -> renderPhrases()
            Layer.SYMBOLS -> symbolRows().forEach { keyboard.addView(buildRow(it, rowHeightDp(it))) }
            Layer.LETTERS -> {
                if (trMode) keyboard.addView(translateHeader())
                else keyboard.addView(buildRow(topBar, rowHeightDp(topBar)))
                lettersRows().forEach { keyboard.addView(buildRow(it, rowHeightDp(it))) }
            }
        }
        startGlow()
    }

    private fun weightOf(key: String) = when {
        key == "space" -> if (layer == Layer.EMOJI || layer == Layer.TOOLS || layer == Layer.PHRASES) 4f else 3.2f
        key == "?123" || key == "ABC" -> 1.5f
        key == "⇧" || key == "⌫" || key == "↵" || key == "🌐" || key == "😊" -> 1.35f
        key == "sp" -> 2.5f
        key == "tsrc" || key == "ttgt" -> 2f
        key == "tx" -> 0.8f
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

    private fun bottomBarRow() = buildRow(listOf("ABC", "space", "⌫"), (46 * scale / 100f).toInt())

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
        keyboard.addView(bottomBarRow())
    }

    private fun renderTools() {
        val h = (66 * scale / 100f).toInt()
        tileKeys.chunked(4).forEach { keyboard.addView(buildRow(it, h)) }
        keyboard.addView(bottomBarRow())
    }

    /** Tez ishlatiladigan iboralar (tanlangan yozuvga moslab). */
    private fun phrases(): List<String> {
        val base = listOf(
            "Assalomu alaykum", "Va alaykum assalom", "Rahmat", "Iltimos", "Xayrli tong", "Xayrli kech",
            "Yaxshimisiz?", "Hozir bandman, keyin yozaman", "Kechirasiz", "Xayr, ko'rishguncha",
            "Bayramingiz muborak bo'lsin!", "Yo'ldaman", "Mayli, bo'ladi", "Telefon qiling", "Raqamingizni bering"
        )
        return when (lang.code) {
            "UZC" -> base.map { Script.latToCyr(it) }
            else -> base.map { Alphabet.toNew(it) }
        }
    }

    private fun renderPhrases() {
        val h = (48 * scale / 100f).toInt()
        val list = phrases()
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.indices.map { "ph:$it" }.chunked(2).forEach { chunk ->
            grid.addView(buildRow(if (chunk.size == 1) chunk + "pad" else chunk, h))
        }
        keyboard.addView(
            ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(grid) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(h * 5))
        )
        keyboard.addView(bottomBarRow())
    }

    private fun targetName(t: String) = when (t) {
        "uz" -> "O'zbekcha"; "uz-cyr" -> "Ўзбекча"; "ru" -> "Русский"; else -> "English"
    }

    private fun bufferText() = if (trBuf.isEmpty()) "Matnni kiriting" else trBuf.toString()

    private fun refreshBuf() {
        trView?.let { it.text = bufferText(); it.alpha = if (trBuf.isEmpty()) 0.55f else 1f }
    }

    private fun translateHeader(): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(buildRow(listOf("tsrc", "ttgt", "tx"), (40 * scale / 100f).toInt()))
        val h = (48 * scale / 100f).toInt()
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val tv = TextView(this).apply {
            text = bufferText()
            alpha = if (trBuf.isEmpty()) 0.55f else 1f
            textSize = 17f
            maxLines = 2
            setTextColor(theme.text)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            background = GradientDrawable().apply { setColor(theme.special); cornerRadius = dp(12).toFloat() }
        }
        trView = tv
        row.addView(tv, LinearLayout.LayoutParams(0, dp(h), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        row.addView(makeKey("tok"), LinearLayout.LayoutParams(dp(68), dp(h)))
        box.addView(row)
        return box
    }

    private fun tileLabel(key: String): String = when (key) {
        "t:TR" -> "🌍\nTarjima"
        "t:FIELD" -> "📝\nMaydonni\ntarjima"
        "t:CLIP" -> "📋\nBufer"
        "t:PHR" -> "💬\nIboralar"
        "t:CONV" -> "🔄\nLotin⇄Kirill"
        "t:COPY" -> "📄\nOddiy yozuvda\nnusxa"
        "t:THEME" -> "🎨\nMavzu"
        "t:NIGHT" -> "🌙\nTungi rejim"
        "t:HAPTIC" -> "📳\nTebranish: " + if (haptic) "ON" else "OFF"
        "t:ONE" -> "👍\nBir qo'l: " + if (oneHand) "ON" else "OFF"
        "t:TGT" -> "🎯\nTarjima tili:\n" + targetName(target)
        else -> "⚙️\nSozlamalar"
    }

    private fun labelFor(key: String): String = when {
        key == "space" -> lang.space
        key == "LANGLAT" -> "O'z"
        key == "LANGCYR" -> "Ўз"
        key == "TOOLS" -> "⚙"
        key == "HIDE" -> "⌄"
        key == "tsrc" -> "Avto-aniqlash"
        key == "ttgt" -> targetName(target) + " ▾"
        key == "tx" -> "✕"
        key == "tok" -> "✓"
        key == "⇧" -> if (capsLock) "⇪" else "⇧"
        key.startsWith("t:") -> tileLabel(key)
        key.startsWith("cat:") -> EmojiData.categories[key.substring(4).toInt()].first
        key.startsWith("emo:") -> key.substring(4)
        key.startsWith("ph:") -> phrases().getOrElse(key.substring(3).toInt()) { "" }
        key == "pad" -> ""
        else -> if (key.length <= 2 && key.any { it.isLetter() } && caps) key.uppercase() else key
    }

    private fun makeKey(key: String): View {
        if (key == "pad" || key == "sp") return View(this)
        val special = isSpecialKey(key)
        val scrollish = isScrollKey(key) || key.startsWith("cat:")
        val langKey = key == "LANGLAT" || key == "LANGCYR"
        val activeLang = (key == "LANGLAT" && lang.code == "UZ") || (key == "LANGCYR" && lang.code == "UZC")
        val bg = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(if (special) theme.special else theme.key)
            if (theme.stroke != 0) setStroke(dp(2), theme.stroke)
        }
        val label = TextView(this).apply {
            text = labelFor(key)
            textSize = when {
                key.startsWith("emo:") || key.startsWith("cat:") -> 24f
                key.startsWith("t:") -> 11.5f
                key.startsWith("ph:") -> 14f
                langKey -> 16f
                key == "TOOLS" || key == "HIDE" -> 18f
                key == "tsrc" || key == "ttgt" -> 14f
                key == "space" || special -> 14f
                else -> 20f
            }
            gravity = Gravity.CENTER
            includeFontPadding = false
            if (key.startsWith("t:") || key.startsWith("ph:")) { maxLines = 3 }
            setTextColor(if (langKey && !activeLang) Color.argb(130, Color.red(theme.text), Color.green(theme.text), Color.blue(theme.text)) else theme.text)
            typeface = Typeface.create(if (activeLang) "sans-serif-black" else "sans-serif-medium", Typeface.NORMAL)
            background = bg
        }
        // Butun katak (oraliqlar ham) bosiladi — "o'lik zona" yo'q.
        val cell = FrameLayout(this).apply {
            setPadding(dp(2), dp(2), dp(2), dp(2))
            addView(label, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        // Aylantiriladigan ro'yxatlarda tasodifiy bosilmasin: tugma ACTION_UP da ishlaydi.
        var downX = 0f
        var downY = 0f
        var moved = false
        cell.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; moved = false
                    label.alpha = 0.5f
                    if (haptic) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (scrollish) { /* ACTION_UP da */ }
                    else if (key == "⌫") { doBackspace(); handler.postDelayed(repeatDelete, 400) }
                    else handleKey(key)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(e.rawY - downY) > dp(10) || Math.abs(e.rawX - downX) > dp(10)) moved = true
                }
                MotionEvent.ACTION_UP -> {
                    label.alpha = 1f
                    handler.removeCallbacks(repeatDelete)
                    if (scrollish && !moved) handleKey(key)
                }
                MotionEvent.ACTION_CANCEL -> {
                    label.alpha = 1f
                    handler.removeCallbacks(repeatDelete)
                }
            }
            true
        }
        if (!scrollish && !langKey && !key.startsWith("t:")) keyViews.add(Pair(label, bg))
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
        if (trMode) { backspaceBuf(); return }
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
        if (capsLock || trMode) return
        val ic = currentInputConnection
        val want = ic != null && !isRawField() &&
            ic.getCursorCapsMode(currentInputEditorInfo?.inputType ?: 0) != 0
        caps = want
        if (caps != drawnCaps) render()
    }

    // ---------- tarjima paneli va asboblar ----------

    private fun commit(text: String) {
        if (trMode) { trBuf.append(text); refreshBuf() }
        else currentInputConnection?.commitText(text, 1)
    }

    private fun backspaceBuf() {
        if (trBuf.isEmpty()) return
        val n = if (trBuf.length >= 2 && Character.isLowSurrogate(trBuf[trBuf.length - 1])) 2 else 1
        trBuf.setLength(trBuf.length - n)
        refreshBuf()
    }

    /** Buferdagi "s"+"h" kabi juftliklarni ÖZ harfiga almashtiradi. */
    private fun convertBufDigraph() {
        if (trBuf.length < 2) return
        val tail = trBuf.substring(trBuf.length - 2)
        val conv = Alphabet.toNew(tail)
        if (conv != tail) { trBuf.setLength(trBuf.length - 2); trBuf.append(conv) }
    }

    private fun translateBuffer() {
        val raw = trBuf.toString().trim()
        if (raw.isEmpty()) { toast("Avval matn yozing"); return }
        if (isRawField()) { toast("Bu maydonda tarjima o'chirilgan"); return }
        if (translating) return
        val src = if (lang.uzDigraph) Alphabet.toStandard(raw) else raw
        translating = true
        toast("Tarjima qilinmoqda…")
        val key = prefs().getString("apikey", "") ?: ""
        Thread {
            val result = try { Result.success(Translator.translate(src, target, key)) }
            catch (e: Exception) { Result.failure<String>(e) }
            handler.post {
                translating = false
                result.onSuccess { out ->
                    trMode = false
                    trBuf.clear()
                    currentInputConnection?.commitText(out, 1)
                    render()
                    syncCaps()
                }.onFailure { toast("Tarjima bo'lmadi: internetni tekshiring") }
            }
        }.start()
    }

    private fun cycleTarget() {
        val t = Translator.targets
        target = t[(t.indexOf(target) + 1) % t.size]
        prefs().edit().putString("target", target).apply()
    }

    private fun setLang(code: String) {
        lang = Languages.active(this).firstOrNull { it.code == code } ?: return
        prefs().edit().putString("lang", lang.code).apply()
        caps = false; capsLock = false
        layer = Layer.LETTERS
        render()
        syncCaps()
    }

    private fun handleTile(key: String) {
        val ic = currentInputConnection
        layer = Layer.LETTERS
        when (key) {
            "t:TR" -> { trMode = true; trBuf.clear(); render() }
            "t:FIELD" -> { render(); if (ic != null) doTranslate(ic) }
            "t:CLIP" -> {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val t = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
                if (t.isNullOrEmpty()) toast("Bufer bo'sh") else commit(t)
                render()
            }
            "t:PHR" -> { layer = Layer.PHRASES; render() }
            "t:CONV" -> { render(); if (ic != null) doConvert(ic) }
            "t:COPY" -> { render(); if (ic != null) doCopyStandard(ic) }
            "t:THEME" -> {
                val free = Themes.all.filter { !it.premium || Premium.isPremium(this) }
                val next = free[(free.indexOfFirst { it.id == theme.id } + 1) % free.size]
                prefs().edit().putString("theme", next.id).apply()
                loadPrefs(); render()
                toast("Mavzu: " + next.name)
            }
            "t:NIGHT" -> {
                prefs().edit().putString("theme", if (theme.id == "dark") "light" else "dark").apply()
                loadPrefs(); render()
            }
            "t:HAPTIC" -> {
                haptic = !haptic
                prefs().edit().putBoolean("haptic", haptic).apply()
                layer = Layer.TOOLS; render()
            }
            "t:ONE" -> {
                oneHand = !oneHand
                prefs().edit().putBoolean("onehand", oneHand).apply()
                applyPad(); render()
            }
            "t:TGT" -> { cycleTarget(); layer = Layer.TOOLS; render() }
            else -> {
                render()
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    private fun handleKey(key: String) {
        val ic = currentInputConnection ?: return
        pendingUndo = null
        when {
            key == "⇧" -> {
                val now = SystemClock.uptimeMillis()
                if (capsLock) { capsLock = false; caps = false }
                else if (caps && now - lastShiftTap < 400) capsLock = true
                else caps = !caps
                lastShiftTap = now
                render()
            }
            key == "↵" -> {
                if (trMode) { translateBuffer(); return }
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
            key == "space" -> { commit(" "); if (!trMode) syncCaps() }
            key == "🌐" -> {
                val all = Languages.active(this)
                val i = all.indexOfFirst { it.code == lang.code }
                setLang(all[(i + 1) % all.size].code)
            }
            key == "LANGLAT" -> setLang("UZ")
            key == "LANGCYR" -> setLang("UZC")
            key == "?123" -> { layer = Layer.SYMBOLS; render() }
            key == "ABC" -> { layer = Layer.LETTERS; render(); syncCaps() }
            key == "😊" -> { layer = Layer.EMOJI; render() }
            key == "TOOLS" -> { layer = Layer.TOOLS; render() }
            key == "HIDE" -> requestHideSelf(0)
            key == "tsrc" -> toast("Til avtomatik aniqlanadi")
            key == "ttgt" -> { cycleTarget(); render() }
            key == "tx" -> { trMode = false; trBuf.clear(); render() }
            key == "tok" -> translateBuffer()
            key.startsWith("t:") -> handleTile(key)
            key.startsWith("cat:") -> { emojiCat = key.substring(4).toInt(); render() }
            key.startsWith("emo:") -> commit(key.substring(4))
            key.startsWith("ph:") -> {
                phrases().getOrNull(key.substring(3).toInt())?.let { commit(it) }
                layer = Layer.LETTERS; render()
            }
            else -> {
                val value = if (caps && key.any { it.isLetter() }) key.uppercase() else key
                if (trMode) {
                    trBuf.append(value)
                    if (lang.uzDigraph) convertBufDigraph()
                    refreshBuf()
                } else {
                    ic.commitText(value, 1)
                    if (lang.uzDigraph && !isRawField()) convertDigraph()
                }
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
        trMode = false; trBuf.clear()
        layer = Layer.LETTERS
        if (::keyboard.isInitialized) applyPad()
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
