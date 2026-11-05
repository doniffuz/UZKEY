package uz.ozkey.keyboard

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.text.TextUtils
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

    private enum class Layer { LETTERS, SYMBOLS, EMOJI, TOOLS, PHRASES, CLIPBOARD }

    private class Grabbed(val text: String, val hadSelection: Boolean, val before: Int, val after: Int)

    private var layer = Layer.LETTERS
    private var caps = false
    private var capsLock = false
    private var drawnCaps = false
    private var lastShiftTap = 0L
    private var lang: Lang = Languages.UZ
    private var target = "uz"
    private var theme: KeyTheme = Themes.byId(Themes.DEFAULT_ID)
    private var haptic = true
    private var oneHand = false
    private var emojiCat = 0
    private var scale = 100        // klaviatura balandligi, %
    private var fontScale = 100    // harf o'lchami, %
    private var translating = false
    private var pendingUndo: String? = null
    private var navBottom = 0

    // Tarjima paneli: yozilgan matn ilovaga emas, shu yerga tushadi
    private var trMode = false
    private val trBuf = StringBuilder()
    private var trView: TextView? = null
    private var liveView: TextView? = null
    private var liveText = ""      // jonli tarjima natijasi
    private var liveFor = ""       // natija qaysi matn (src) uchun
    private var liveBusy = false
    private var liveFailed = false
    private var liveSeq = 0
    private val liveRunnable = Runnable { runLive() }

    // Bufer tarixi
    private var clipItems: List<ClipStore.Item> = emptyList()
    private var clipListenerOn = false
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { captureClip() }

    // Rasm fon (kesh)
    private var photoBmp: Bitmap? = null
    private var photoStamp = 0L

    private lateinit var keyboard: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val keyViews = mutableListOf<Pair<TextView, GradientDrawable>>()
    private var animator: ValueAnimator? = null
    private var frame = 0

    private val topBar = listOf("CLIP", "LANGLAT", "LANGCYR", "sp", "😊", "TOOLS", "HIDE")
    private val tileKeys = listOf(
        "t:TR", "t:FIELD", "t:CLIP", "t:PHR",
        "t:CONV", "t:COPY", "t:THEME", "t:NIGHT",
        "t:HAPTIC", "t:ONE", "t:TGT", "t:SET"
    )
    private val specials = setOf(
        "⇧", "⌫", "↵", "?123", "ABC", "🌐", "😊", "space",
        "CLIP", "LANGLAT", "LANGCYR", "TOOLS", "HIDE",
        "ttgt", "tboth", "tx", "tok", "ctitle", "cclear"
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

    private fun isScrollKey(k: String) =
        k.startsWith("emo:") || k.startsWith("ph:") || k.startsWith("cp:") || k.startsWith("cpin:") || k.startsWith("cdel:")

    private fun isSpecialKey(k: String) =
        k in specials || k.startsWith("cat:") || k.startsWith("t:") || k.startsWith("cpin:") || k.startsWith("cdel:")

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
        var t = Themes.byId(prefs().getString("theme", Themes.DEFAULT_ID))
        if (t.photo && !PhotoStore.exists(this)) t = Themes.byId(Themes.DEFAULT_ID)
        theme = t
        scale = prefs().getInt("scale", 100).coerceIn(80, 140)
        fontScale = prefs().getInt("fscale", 100).coerceIn(85, 130)
        haptic = prefs().getBoolean("haptic", true)
        oneHand = prefs().getBoolean("onehand", false)
        target = prefs().getString("target", "uz") ?: "uz"
        val code = prefs().getString("lang", "UZ")
        lang = Languages.active(this).firstOrNull { it.code == code } ?: Languages.UZ
    }

    override fun onCreate() {
        super.onCreate()
        loadPrefs()
        try {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).addPrimaryClipChangedListener(clipListener)
            clipListenerOn = true
        } catch (e: Exception) { clipListenerOn = false }
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
            row === topBar -> 38
            layer == Layer.LETTERS && row === digits -> 42
            else -> 50
        }
        return (base * scale / 100f).toInt()
    }

    // ---------- fon ----------

    private fun photoBackground(): Drawable? {
        val f = PhotoStore.file(this)
        if (!f.exists()) return null
        if (photoBmp == null || photoStamp != f.lastModified()) {
            photoBmp = try { BitmapFactory.decodeFile(f.path) } catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
            photoStamp = f.lastModified()
        }
        val b = photoBmp ?: return null
        return PhotoBackground(b, prefs().getInt("photodim", 35))
    }

    private fun gradientBackground() = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(theme.bgTop, theme.bgBottom)
    )

    private fun render() {
        if (!::keyboard.isInitialized) return
        drawnCaps = caps
        trView = null
        liveView = null
        keyboard.removeAllViews()
        keyViews.clear()
        keyboard.background = (if (theme.photo) photoBackground() else null) ?: gradientBackground()
        when (layer) {
            Layer.EMOJI -> renderEmoji()
            Layer.TOOLS -> renderTools()
            Layer.PHRASES -> renderPhrases()
            Layer.CLIPBOARD -> renderClipboard()
            Layer.SYMBOLS -> {
                if (trMode) keyboard.addView(translateHeader())
                symbolRows().forEach { keyboard.addView(buildRow(it, rowHeightDp(it))) }
            }
            Layer.LETTERS -> {
                if (trMode) keyboard.addView(translateHeader())
                else keyboard.addView(buildRow(topBar, rowHeightDp(topBar)))
                lettersRows().forEach { keyboard.addView(buildRow(it, rowHeightDp(it))) }
            }
        }
        startGlow()
    }

    private fun weightOf(key: String) = when {
        key == "space" -> if (layer == Layer.LETTERS || layer == Layer.SYMBOLS) 3.2f else 4f
        key == "?123" || key == "ABC" -> 1.5f
        key == "⇧" || key == "⌫" || key == "↵" || key == "🌐" || key == "😊" -> 1.35f
        key == "sp" -> 1.5f
        key == "ttgt" -> 3f
        key == "tboth" -> 2.2f
        key == "tx" -> 0.9f
        key == "ctitle" -> 3f
        key == "cclear" -> 2f
        key.startsWith("cp:") -> 6f
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
        if (trMode) keyboard.addView(translateHeader())
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
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(h * (if (trMode) 3 else 4)))
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

    // ---------- bufer (clipboard) ----------

    private fun renderClipboard() {
        clipItems = ClipStore.list(this).take(30)
        val h = (46 * scale / 100f).toInt()
        keyboard.addView(buildRow(listOf("ctitle", "cclear"), (40 * scale / 100f).toInt()))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (clipItems.isEmpty()) {
            list.addView(TextView(this).apply {
                text = if (prefs().getBoolean("cliphist", true))
                    "Hali hech narsa nusxalanmagan.\nBiror joydan matnni nusxalang — shu yerda saqlanadi va keyin bosib qo'yasiz."
                else "Bufer tarixi o'chirilgan. Ilova → Sozlama bo'limidan yoqing."
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(theme.text)
                alpha = 0.7f
                setPadding(dp(20), dp(24), dp(20), dp(24))
            })
        } else {
            clipItems.indices.forEach { i ->
                list.addView(buildRow(listOf("cp:$i", "cpin:$i", "cdel:$i"), h))
            }
        }
        keyboard.addView(
            ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(list) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(h * 4))
        )
        keyboard.addView(bottomBarRow())
    }

    private fun captureClip() {
        if (!prefs().getBoolean("cliphist", true)) return
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip ?: return
            if (clip.itemCount == 0) return
            // Android 13+: parol kabi "maxfiy" nusxalar tarixga tushmaydi
            if (Build.VERSION.SDK_INT >= 33 &&
                clip.description.extras?.getBoolean("android.content.extra.IS_SENSITIVE", false) == true) return
            val t = clip.getItemAt(0).coerceToText(this)?.toString() ?: return
            ClipStore.add(this, t)
        } catch (e: Exception) { /* buferga ruxsat yo'q — jim o'tamiz */ }
    }

    private fun openClipboard() {
        captureClip()
        layer = Layer.CLIPBOARD
        render()
    }

    // ---------- tarjima paneli ----------

    private fun targetName(t: String) = when (t) {
        "uz" -> "O'zbekcha"; "uz-cyr" -> "Ўзбекча"; "ru" -> "Русский"; else -> "English"
    }

    private fun bufferText() = if (trBuf.isEmpty()) "Matnni kiriting…" else trBuf.toString()

    /** Tarjimaga yuboriladigan matn (ÖZ harflari oddiy lotinga qaytariladi). */
    private fun srcText(): String {
        val raw = trBuf.toString().trim()
        return if (lang.uzDigraph) Alphabet.toStandard(raw) else raw
    }

    private fun refreshBuf() {
        trView?.let { it.text = bufferText(); it.alpha = if (trBuf.isEmpty()) 0.55f else 1f }
        scheduleLive()
    }

    private fun refreshLive() {
        val v = liveView ?: return
        when {
            trBuf.isEmpty() -> { v.text = "Tarjima shu yerda ko'rinadi"; v.alpha = 0.5f }
            liveFailed && liveText.isEmpty() -> { v.text = "Internet yo'q yoki tarjima xizmati javob bermadi"; v.alpha = 0.7f }
            liveText.isEmpty() -> { v.text = "⏳ Tarjima qilinmoqda…"; v.alpha = 0.6f }
            else -> { v.text = liveText; v.alpha = if (liveBusy) 0.55f else 1f }
        }
    }

    private fun scheduleLive() {
        handler.removeCallbacks(liveRunnable)
        if (!trMode) return
        if (trBuf.toString().isBlank() || isRawField()) {
            liveText = ""; liveFor = ""; liveBusy = false; liveFailed = false
            liveSeq++
            refreshLive()
            return
        }
        liveBusy = true
        refreshLive()
        handler.postDelayed(liveRunnable, 650)
    }

    private fun runLive() {
        if (!trMode) return
        val src = srcText()
        if (src.isEmpty()) return
        val tgt = target
        val seq = ++liveSeq
        Thread {
            val res = try { Translator.translate(src, tgt) } catch (e: Exception) { null }
            handler.post {
                if (seq != liveSeq || !trMode) return@post
                liveBusy = false
                if (res != null) { liveText = res; liveFor = tgt + "\u0000" + src; liveFailed = false }
                else { liveFailed = true }
                refreshLive()
            }
        }.start()
    }

    private fun resetLive() {
        handler.removeCallbacks(liveRunnable)
        liveSeq++
        liveText = ""; liveFor = ""; liveBusy = false; liveFailed = false
    }

    private fun translateHeader(): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(buildRow(listOf("ttgt", "tboth", "tx"), (38 * scale / 100f).toInt()))

        // 1) yozilayotgan matn + ✓
        val h = (46 * scale / 100f).toInt()
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val tv = TextView(this).apply {
            text = bufferText()
            alpha = if (trBuf.isEmpty()) 0.55f else 1f
            textSize = 17f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START     // oxirgi yozilgan qism ko'rinib turadi
            setTextColor(theme.text)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            background = GradientDrawable().apply { setColor(theme.special); cornerRadius = dp(12).toFloat() }
        }
        trView = tv
        row.addView(tv, LinearLayout.LayoutParams(0, dp(h), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        row.addView(makeKey("tok"), LinearLayout.LayoutParams(dp(68), dp(h)))
        box.addView(row)

        // 2) jonli tarjima ko'rinishi
        val live = TextView(this).apply {
            textSize = 15f
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(theme.text)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            background = GradientDrawable().apply {
                setColor(theme.key); cornerRadius = dp(12).toFloat()
                if (theme.stroke != 0) setStroke(dp(1), theme.stroke)
            }
        }
        liveView = live
        refreshLive()
        box.addView(live, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(h)
        ).apply { setMargins(dp(2), dp(2), dp(2), dp(4)) })
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

    private fun clipLabel(i: Int): String {
        val t = clipItems.getOrNull(i)?.text ?: return ""
        val one = t.replace("\n", " ⏎ ").replace(Regex("\\s+"), " ")
        return if (one.length > 90) one.substring(0, 90) + "…" else one
    }

    private fun labelFor(key: String): String = when {
        key == "space" -> lang.space
        key == "CLIP" -> "📋"
        key == "LANGLAT" -> "O'z"
        key == "LANGCYR" -> "Ўз"
        key == "TOOLS" -> "⚙"
        key == "HIDE" -> "⌄"
        key == "ttgt" -> "🌍 Avto → " + targetName(target) + " ▾"
        key == "tboth" -> "Asl + tarjima"
        key == "tx" -> "✕"
        key == "tok" -> "✓"
        key == "ctitle" -> "📋  Bufer"
        key == "cclear" -> "🗑 Tozalash"
        key == "⇧" -> if (capsLock) "⇪" else "⇧"
        key.startsWith("t:") -> tileLabel(key)
        key.startsWith("cat:") -> EmojiData.categories[key.substring(4).toInt()].first
        key.startsWith("emo:") -> key.substring(4)
        key.startsWith("ph:") -> phrases().getOrElse(key.substring(3).toInt()) { "" }
        key.startsWith("cp:") -> clipLabel(key.substring(3).toInt())
        key.startsWith("cpin:") -> if (clipItems.getOrNull(key.substring(5).toInt())?.pinned == true) "★" else "☆"
        key.startsWith("cdel:") -> "✕"
        key == "pad" -> ""
        else -> if (key.length <= 2 && key.any { it.isLetter() } && caps) key.uppercase() else key
    }

    private fun makeKey(key: String): View {
        if (key == "pad" || key == "sp") return View(this)
        val special = isSpecialKey(key)
        val scrollish = isScrollKey(key) || key.startsWith("cat:")
        val langKey = key == "LANGLAT" || key == "LANGCYR"
        val activeLang = (key == "LANGLAT" && lang.code == "UZ") || (key == "LANGCYR" && lang.code == "UZC")
        val isLetterKey = key.length <= 2 && key.any { it.isLetter() }
        val clipText = key.startsWith("cp:")
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
                clipText -> 14f
                key.startsWith("cpin:") || key.startsWith("cdel:") -> 18f
                key == "CLIP" -> 18f
                langKey -> 16f
                key == "TOOLS" || key == "HIDE" -> 18f
                key == "ttgt" || key == "tboth" -> 13f
                key == "ctitle" || key == "cclear" -> 14f
                key == "tok" || key == "tx" -> 20f
                key == "⇧" || key == "⌫" -> 22f
                key == "space" || special -> 14f
                isLetterKey -> 23f * fontScale / 100f          // harflar: katta
                key.length == 1 && key[0].isDigit() -> 18f * fontScale / 100f
                else -> 20f * fontScale / 100f
            }
            gravity = if (clipText) Gravity.CENTER_VERTICAL or Gravity.START else Gravity.CENTER
            if (clipText) setPadding(dp(12), 0, dp(8), 0)
            includeFontPadding = false
            if (key.startsWith("t:") || key.startsWith("ph:")) { maxLines = 3 }
            if (clipText) { maxLines = 2; ellipsize = TextUtils.TruncateAt.END }
            if (key == "ttgt" || key == "tboth") { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
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
        if (key == "ctitle") return cell     // faqat yozuv — bosilmaydi
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

    private fun isMultiLineField(): Boolean =
        ((currentInputEditorInfo?.inputType ?: 0) and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0

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

    /** "Maydonni tarjima": belgilangan matn (yoki butun maydon) tarjima bilan almashtiriladi. */
    private fun doTranslate(ic: InputConnection) {
        if (translating) return
        if (isRawField()) { toast("Bu maydonda tarjima o'chirilgan"); return }
        val g = grabText(ic) ?: run { toast("Avval matn yozing yoki belgilang"); return }
        translating = true
        toast("Tarjima qilinmoqda…")
        Thread {
            val result = try { Result.success(Translator.translate(g.text, target)) }
            catch (e: Exception) { Result.failure<String>(e) }
            handler.post {
                translating = false
                result.onSuccess { out ->
                    val c = currentInputConnection
                    val now = c?.let { grabText(it) }
                    if (c == null || now == null || now.text != g.text) toast("Matn o'zgargan, qayta urinib ko'ring")
                    else replaceText(c, now, out)
                }.onFailure { toast("Tarjima bo'lmadi: internetni tekshiring") }
            }
        }.start()
    }

    /** ÖZ alifbosi bo'lsa -> standart; standart bo'lsa -> ÖZ alifbosi. */
    private fun doConvert(ic: InputConnection) {
        if (isRawField()) return
        val g = grabText(ic) ?: run { toast("Avval matn yozing yoki belgilang"); return }
        val hasCyr = g.text.any { it in 'Ѐ'..'ӿ' }
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

    /** Natijani ilovaga yozadi va tarjima rejimidan chiqadi. */
    private fun finishWith(out: String, original: String, both: Boolean) {
        val text = if (both) original + (if (isMultiLineField()) "\n" else " — ") + out else out
        trMode = false
        trBuf.clear()
        resetLive()
        currentInputConnection?.commitText(text, 1)
        layer = Layer.LETTERS
        render()
        syncCaps()
    }

    /** ✓ (yoki Asl + tarjima). Jonli tarjima tayyor bo'lsa — darhol yozadi, bo'lmasa so'rov yuboradi. */
    private fun translateBuffer(both: Boolean) {
        val raw = trBuf.toString().trim()
        if (raw.isEmpty()) { toast("Avval matn yozing"); return }
        if (isRawField()) { toast("Bu maydonda tarjima o'chirilgan"); return }
        val src = srcText()
        if (liveText.isNotEmpty() && liveFor == target + "\u0000" + src) { finishWith(liveText, raw, both); return }
        if (translating) return
        translating = true
        toast("Tarjima qilinmoqda…")
        val tgt = target
        Thread {
            val result = try { Result.success(Translator.translate(src, tgt)) }
            catch (e: Exception) { Result.failure<String>(e) }
            handler.post {
                translating = false
                result.onSuccess { out -> if (trMode) finishWith(out, raw, both) }
                    .onFailure { toast("Tarjima bo'lmadi: internetni tekshiring") }
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
        if (trMode) scheduleLive()
    }

    private fun handleTile(key: String) {
        val ic = currentInputConnection
        layer = Layer.LETTERS
        when (key) {
            "t:TR" -> { trMode = true; trBuf.clear(); resetLive(); render() }
            "t:FIELD" -> { render(); if (ic != null) doTranslate(ic) }
            "t:CLIP" -> openClipboard()
            "t:PHR" -> { layer = Layer.PHRASES; render() }
            "t:CONV" -> { render(); if (ic != null) doConvert(ic) }
            "t:COPY" -> { render(); if (ic != null) doCopyStandard(ic) }
            "t:THEME" -> {
                val all = Themes.cycle(this)
                val next = all[(all.indexOfFirst { it.id == theme.id } + 1) % all.size]
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
                if (trMode) { translateBuffer(false); return }
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
            key == "CLIP" -> openClipboard()
            key == "?123" -> { layer = Layer.SYMBOLS; render() }
            key == "ABC" -> { layer = Layer.LETTERS; render(); syncCaps() }
            key == "😊" -> { layer = Layer.EMOJI; render() }
            key == "TOOLS" -> { layer = Layer.TOOLS; render() }
            key == "HIDE" -> requestHideSelf(0)
            key == "ttgt" -> { cycleTarget(); render(); scheduleLive() }
            key == "tboth" -> translateBuffer(true)
            key == "tx" -> { trMode = false; trBuf.clear(); resetLive(); render(); syncCaps() }
            key == "tok" -> translateBuffer(false)
            key == "cclear" -> { ClipStore.clearUnpinned(this); render() }
            key.startsWith("cp:") -> {
                clipItems.getOrNull(key.substring(3).toInt())?.let { commit(it.text) }
                layer = Layer.LETTERS; render()
            }
            key.startsWith("cpin:") -> {
                clipItems.getOrNull(key.substring(5).toInt())?.let { ClipStore.togglePin(this, it.text) }
                render()
            }
            key.startsWith("cdel:") -> {
                clipItems.getOrNull(key.substring(5).toInt())?.let { ClipStore.remove(this, it.text) }
                render()
            }
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
        trMode = false; trBuf.clear(); resetLive()
        layer = Layer.LETTERS
        if (::keyboard.isInitialized) applyPad()
        render()
        syncCaps()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        animator?.cancel()
        handler.removeCallbacks(repeatDelete)
        handler.removeCallbacks(liveRunnable)
    }

    override fun onDestroy() {
        animator?.cancel()
        handler.removeCallbacks(liveRunnable)
        if (clipListenerOn) {
            try {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).removePrimaryClipChangedListener(clipListener)
            } catch (e: Exception) { /* e'tiborsiz */ }
        }
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
