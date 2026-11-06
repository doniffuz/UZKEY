package uz.ozkey.keyboard

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
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

    // So'z takliflari / avto-tuzatish / Smart Touch (hammasi qurilmada, serverga hech narsa ketmaydi)
    private var predictOn = true
    private var autoCorrectOn = true
    private var smartTouchOn = false
    private class Corr(val original: String, val corrected: String, val boundary: String)
    private var corr: Corr? = null
    private var neighbors: Map<Char, Set<Char>> = emptyMap()
    private var sugg: List<String> = emptyList()
    private val sgViews = mutableListOf<TextView>()
    private val sgKeys = listOf("sg:0", "sg:1", "sg:2")
    private val suggestRunnable = Runnable { updateSuggestions() }

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
    private var clipTab = ClipStore.TAB_HISTORY   // Tarix | Sevimlilar | Mahkamlangan
    private var clipDelAsk = -1L                  // o'chirish tasdig'i kutilayotgan matn (id)
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

    // Harf tugmalari: shift bosilganda butun klaviatura qayta qurilmaydi, faqat yozuvlar yangilanadi (tez!)
    private val letterViews = mutableListOf<Pair<TextView, String>>()
    private var shiftView: TextView? = null
    private var shiftBg: GradientDrawable? = null

    // Yangi nusxalangan matn taklifi (tepa panelda 2 daqiqa turadi)
    private var recentClip: String? = null
    private var recentClipAt = 0L
    private var recentUsed = true
    private val wsRegex = Regex("\\s+")

    // Tepa panel: 📋 bufer | O'z | Ўз | (yangi nusxa taklifi) | asboblar | yopish
    private val topBar = listOf("CLIP", "LANGLAT", "LANGCYR", "cchip", "TOOLS", "HIDE")
    private val tileKeys = listOf(
        "t:TR", "t:FIELD", "t:PHR", "t:CONV", "t:UNDO",
        "t:REDO", "t:COPY", "t:THEME", "t:NIGHT", "t:SET"
    ).filter { Translator.available || (it != "t:TR" && it != "t:FIELD") }
    private val specials = setOf(
        "⇧", "⌫", "↵", "?123", "ABC", "🌐", "😊", "space",
        "CLIP", "LANGLAT", "LANGCYR", "TOOLS", "HIDE",
        "ttgt", "tboth", "tx", "tok", "cclear", "cchip"
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
        k.startsWith("emo:") || k.startsWith("ph:") || k.startsWith("cp:") || k.startsWith("cpin:") || k.startsWith("cdel:") ||
            k.startsWith("cfav:") || k.startsWith("cno:") || k.startsWith("cyes:")

    private fun isSpecialKey(k: String) =
        k in specials || k.startsWith("sg:") || k.startsWith("cat:") || k.startsWith("t:") || k.startsWith("cpin:") || k.startsWith("cdel:") ||
            k.startsWith("cfav:") || k.startsWith("ctab:") || k.startsWith("cask:") || k.startsWith("cno:") || k.startsWith("cyes:")

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
        if (t.id == Themes.AUTO_ID) {
            val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            t = Themes.byId(if (night) "dark" else "light")
        }
        theme = t
        scale = prefs().getInt("scale", 100).coerceIn(80, 140)
        fontScale = prefs().getInt("fscale", 100).coerceIn(85, 130)
        haptic = prefs().getBoolean("haptic", true)
        oneHand = prefs().getBoolean("onehand", false)
        target = prefs().getString("target", "uz") ?: "uz"
        val code = prefs().getString("lang", "UZ")
        lang = Languages.active(this).firstOrNull { it.code == code } ?: Languages.UZ
        predictOn = prefs().getBoolean("predict", true)
        autoCorrectOn = prefs().getBoolean("autocorrect", true)
        smartTouchOn = prefs().getBoolean("smarttouch", false)
        if (lang.code == "UZ") {
            WordModel.init(this)
            neighbors = WordModel.buildNeighbors(lang.rows)
        }
    }

    private fun sgBarOn() = predictOn && lang.code == "UZ"

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
        keyboard.setPadding(0, 0, extra, navBottom)   // chetda "o'lik zona" yo'q: tugmalar ekran chetigacha bosiladi
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

    /** Ekran balandligiga qarab klaviatura nisbatini avtomatik moslaydi (kichik telefon / katta telefon / planshet / yotiq holat). */
    private fun screenFactor(): Float {
        val dm = resources.displayMetrics
        val hDp = dm.heightPixels / dm.density
        val wDp = dm.widthPixels / dm.density
        return if (wDp > hDp) (hDp / 800f).coerceIn(0.55f, 0.9f)
        else (hDp / 780f).coerceIn(0.86f, 1.12f)
    }

    private fun rowHeightDp(row: List<String>): Int {
        val base = when {
            row === topBar -> 48
            row === sgKeys -> 34
            layer == Layer.LETTERS && row === digits -> 42
            layer == Layer.LETTERS && row === lang.extra -> 50
            else -> 56
        }
        return (base * screenFactor() * scale / 100f).toInt()
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
        letterViews.clear()
        sgViews.clear()
        shiftView = null
        shiftBg = null
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
                else {
                    keyboard.addView(buildRow(topBar, rowHeightDp(topBar)))
                    if (sgBarOn()) keyboard.addView(buildRow(sgKeys, rowHeightDp(sgKeys)))
                }
                lettersRows().forEach { keyboard.addView(buildRow(it, rowHeightDp(it))) }
            }
        }
        if (layer == Layer.LETTERS) { refreshCaps(); scheduleSuggest() }
        startGlow()
    }

    private fun weightOf(key: String) = when {
        key == "space" -> if (layer == Layer.LETTERS || layer == Layer.SYMBOLS) 3.2f else 4f
        key == "?123" || key == "ABC" -> 1.5f
        key == "⇧" || key == "⌫" || key == "↵" || key == "🌐" || key == "😊" -> 1.35f
        key == "sp" -> 1.5f
        layer == Layer.LETTERS && (key == "Ğ" || key == "Ö" || key == "Ş" || key == "Ç") -> 1.25f
        key == "cchip" -> 2.8f
        key == "ttgt" -> 3f
        key == "tboth" -> 2.2f
        key == "tx" -> 0.9f
        key == "ctab:0" -> 1.4f
        key == "ctab:1" -> 2f
        key == "ctab:2" -> 2.4f
        key == "cclear" -> 0.9f
        key.startsWith("cask:") -> 4f
        key.startsWith("cno:") || key.startsWith("cyes:") -> 2f
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
        row.forEachIndexed { i, key ->
            rowLayout.addView(makeKey(key, row.getOrNull(i - 1), row.getOrNull(i + 1)), LinearLayout.LayoutParams(0, dp(heightDp), weightOf(key)))
        }
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
        val h = (92 * scale / 100f).toInt()
        tileKeys.chunked(5).forEach { keyboard.addView(buildRow(it, h)) }
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
        clipItems = ClipStore.list(this, clipTab, limit = 80)
        val h = (46 * scale / 100f).toInt()
        val tabs = if (clipTab == ClipStore.TAB_HISTORY) listOf("ctab:0", "ctab:1", "ctab:2", "cclear")
        else listOf("ctab:0", "ctab:1", "ctab:2")
        keyboard.addView(buildRow(tabs, (40 * scale / 100f).toInt()))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (clipItems.isEmpty()) {
            list.addView(TextView(this).apply {
                text = when (clipTab) {
                    ClipStore.TAB_FAV -> "Sevimli matn yo'q.\nTarixdagi matnning ☆ tugmasini bosing — u hech qachon avtomatik o'chmaydi."
                    ClipStore.TAB_PIN -> "Mahkamlangan matn yo'q.\n📍 ni bosing — matn doim ro'yxat tepasida turadi."
                    else -> if (prefs().getBoolean("cliphist", true))
                        "Hali hech narsa nusxalanmagan.\nBiror joydan matnni nusxalang — shu yerda saqlanadi va keyin bosib qo'yasiz."
                    else "Bufer tarixi o'chirilgan. Ilova → Sozlama bo'limidan yoqing."
                }
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(theme.text)
                alpha = 0.7f
                setPadding(dp(20), dp(24), dp(20), dp(24))
            })
        } else {
            clipItems.forEachIndexed { i, item ->
                if (item.id == clipDelAsk) list.addView(buildRow(listOf("cask:$i", "cno:$i", "cyes:$i"), h))
                else list.addView(buildRow(listOf("cp:$i", "cfav:$i", "cpin:$i", "cdel:$i"), h))
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
            // OTP kod / karta raqami ko'rinishidagi matn tarixga tushmaydi (Sozlamadan o'chirish mumkin)
            if (prefs().getBoolean("clipsafe", true) && ClipStore.looksSensitive(t)) return
            ClipStore.add(this, t)
            val tt = t.trim()
            if (tt.isNotEmpty() && tt.length <= ClipStore.MAX_LEN && tt != recentClip) {
                recentClip = tt
                recentClipAt = SystemClock.uptimeMillis()
                recentUsed = false
                // klaviatura ko'rinib turgan bo'lsa — taklif chipini darhol chiqaramiz
                if (::keyboard.isInitialized && keyboard.isShown && layer == Layer.LETTERS && !trMode) render()
            }
        } catch (e: Exception) { /* buferga ruxsat yo'q — jim o'tamiz */ }
    }

    private fun chipVisible(): Boolean {
        val c = recentClip ?: return false
        return c.isNotEmpty() && !recentUsed && !trMode && !isRawField() &&
            SystemClock.uptimeMillis() - recentClipAt < 120_000
    }

    private fun chipLabel(): String {
        val one = (recentClip ?: "").replace(wsRegex, " ")
        return "📋 " + if (one.length > 24) one.substring(0, 24) + "…" else one
    }

    private fun openClipboard() {
        captureClip()
        clipDelAsk = -1L
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
            liveFailed && liveText.isEmpty() -> { v.text = "Internet yo'q yoki tarjima xizmati javob bermadi. Trafik tejash rejimini tekshiring"; v.alpha = 0.7f }
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
        "t:FIELD" -> "📝\nMaydon\n→ " + targetName(target)
        "t:PHR" -> "💬\nIboralar"
        "t:CONV" -> "🔄\nLotin⇄\nKirill"
        "t:UNDO" -> "↶\nBekor\nqilish"
        "t:REDO" -> "↷\nQayta\nqilish"
        "t:COPY" -> "📄\nOddiy\nnusxa"
        "t:THEME" -> "🎨\nMavzu"
        "t:NIGHT" -> "☀️🌙\nKun/Tun"
        else -> "⚙️\nSozlamalar"
    }

    /** Bufer kartochkasi: 1-qator — matn boshi, 2-qator (kichik, xira) — vaqt, kategoriya, uzunlik. */
    private fun clipSpan(i: Int): CharSequence {
        val item = clipItems.getOrNull(i) ?: return ""
        val one = item.preview.replace("\n", " ⏎ ").replace(wsRegex, " ").trim()
        val head = if (one.length > 48) one.substring(0, 48) + "…" else one
        var meta = ClipStore.ago(item.ts)
        if (item.cat.isNotEmpty()) meta += " · " + ClipStore.catLabel(item.cat)
        if (item.cut) meta += " · ${item.len} belgi"
        val sb = SpannableStringBuilder(head).append('\n')
        val from = sb.length
        sb.append(meta)
        sb.setSpan(RelativeSizeSpan(0.74f), from, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val c = theme.text
        sb.setSpan(ForegroundColorSpan(Color.argb(150, Color.red(c), Color.green(c), Color.blue(c))),
            from, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
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
        key == "cchip" -> chipLabel()
        key == "ctab:0" -> "Tarix"
        key == "ctab:1" -> "⭐ Sevimlilar"
        key == "ctab:2" -> "📌 Mahkamlangan"
        key == "cclear" -> "🗑"
        key == "⇧" -> if (capsLock) "⇪" else "⇧"
        key.startsWith("sg:") -> sugg.getOrElse(key.substring(3).toInt()) { "" }
        key.startsWith("t:") -> tileLabel(key)
        key.startsWith("cat:") -> EmojiData.categories[key.substring(4).toInt()].first
        key.startsWith("emo:") -> key.substring(4)
        key.startsWith("ph:") -> phrases().getOrElse(key.substring(3).toInt()) { "" }
        key.startsWith("cp:") -> ""     // kartochka matni makeKey ichida (clipSpan)
        key.startsWith("cfav:") -> if (clipItems.getOrNull(key.substring(5).toInt())?.fav == true) "★" else "☆"
        key.startsWith("cpin:") -> if (clipItems.getOrNull(key.substring(5).toInt())?.pinned == true) "📌" else "📍"
        key.startsWith("cdel:") -> "✕"
        key.startsWith("cask:") -> "Bu matnni o'chirmoqchimisiz?"
        key.startsWith("cno:") -> "Bekor qilish"
        key.startsWith("cyes:") -> "O'chirish"
        key == "pad" -> ""
        else -> if (key.length <= 2 && key.any { it.isLetter() } && caps) key.uppercase() else key
    }

    private fun makeKey(key: String, left: String? = null, right: String? = null): View {
        if (key == "pad" || key == "sp") return View(this)
        if (key == "cchip" && !chipVisible()) return View(this)
        val special = isSpecialKey(key)
        val scrollish = isScrollKey(key) || key.startsWith("cat:")
        // Bo'shliq: tasodifiy bosilmasin deb faqat barmoq tugma ICHIDA ko'tarilganda ishlaydi
        val deferred = key == "space"
        val langKey = key == "LANGLAT" || key == "LANGCYR"
        val activeLang = (key == "LANGLAT" && lang.code == "UZ") || (key == "LANGCYR" && lang.code == "UZC")
        val isLetterKey = key.length <= 2 && key.any { it.isLetter() }
        val clipText = key.startsWith("cp:")
        val tabKey = key.startsWith("ctab:")
        val activeTab = tabKey && key.substring(5).toIntOrNull() == clipTab
        val askKey = key.startsWith("cask:") || key.startsWith("cno:") || key.startsWith("cyes:")
        val bg = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(if (special && !activeTab) theme.special else theme.key)
            if (theme.stroke != 0) setStroke(dp(2), theme.stroke)
        }
        val label = object : TextView(this@OZKeyInputMethod) {
            override fun hasOverlappingRendering() = false
        }.apply {
            text = if (clipText) clipSpan(key.substring(3).toInt()) else labelFor(key)
            textSize = when {
                key.startsWith("sg:") -> 17f
                key.startsWith("emo:") || key.startsWith("cat:") -> 24f
                key.startsWith("t:") -> 12f
                key.startsWith("ph:") -> 14f
                clipText -> 14f
                key.startsWith("cpin:") || key.startsWith("cdel:") || key.startsWith("cfav:") -> 18f
                tabKey || askKey -> 13f
                key == "CLIP" -> 19f
                langKey -> 18f
                key == "TOOLS" || key == "HIDE" -> 19f
                key == "ttgt" || key == "tboth" || key == "cchip" -> 13f
                key == "cclear" -> 18f
                key == "tok" || key == "tx" -> 20f
                key == "⇧" || key == "⌫" -> 22f
                key == "space" || special -> 14f
                isLetterKey -> 26f * fontScale / 100f * (0.5f + 0.5f * screenFactor())   // harflar: katta
                key.length == 1 && key[0].isDigit() -> 19f * fontScale / 100f
                else -> 20f * fontScale / 100f
            }
            gravity = if (clipText) Gravity.CENTER_VERTICAL or Gravity.START else Gravity.CENTER
            if (clipText) setPadding(dp(12), 0, dp(8), 0)
            includeFontPadding = false
            if (key.startsWith("t:") || key.startsWith("ph:")) { maxLines = 3 }
            if (clipText) { maxLines = 2; ellipsize = TextUtils.TruncateAt.END }
            if (key == "ttgt" || key == "tboth" || key == "cchip" || tabKey || askKey || key.startsWith("sg:")) { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
            val dim = (langKey && !activeLang) || (tabKey && !activeTab)
            setTextColor(if (dim) Color.argb(130, Color.red(theme.text), Color.green(theme.text), Color.blue(theme.text)) else theme.text)
            typeface = Typeface.create(if (activeLang || activeTab) "sans-serif-black" else "sans-serif-medium", Typeface.NORMAL)
            background = bg
        }
        // Butun katak (oraliqlar ham) bosiladi — "o'lik zona" yo'q. Bo'shliq tugmasi bundan mustasno:
        // uning atrofida kichik bo'sh joy bor, shuning uchun chetga tegib ketsangiz probel yozilmaydi.
        val cell = FrameLayout(this).apply {
            if (deferred) setPadding(dp(6), dp(5), dp(6), dp(5)) else setPadding(dp(1), dp(2), dp(1), dp(2))
            addView(label, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        if (key.startsWith("cask:")) return cell     // faqat savol yozuvi — bosilmaydi
        if (key == "⇧") { shiftView = label; shiftBg = bg }
        if (key.startsWith("sg:")) sgViews.add(label)
        val smartOk = isLetterKey && !special && layer == Layer.LETTERS && !trMode
        if (isLetterKey && !special && layer == Layer.LETTERS) letterViews.add(Pair(label, key))
        // Aylantiriladigan ro'yxatlarda va bo'shliqda tasodifiy bosilmasin: ACTION_UP da ishlaydi.
        var downX = 0f
        var downY = 0f
        var moved = false
        val touchView: View = if (deferred) label else cell
        touchView.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; moved = false
                    // Avval harf yoziladi, keyin tebranish/rang (reaksiya tezroq)
                    if (scrollish || deferred) { /* ACTION_UP da */ }
                    else if (key == "⌫") { doBackspace(); handler.postDelayed(repeatDelete, 400) }
                    else handleKey(if (smartOk) smartPick(key, left, right, e.x, v.width) else key)
                    label.alpha = 0.5f
                    if (haptic && !deferred) keyboard.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(e.rawY - downY) > dp(10) || Math.abs(e.rawX - downX) > dp(10)) moved = true
                }
                MotionEvent.ACTION_UP -> {
                    label.alpha = 1f
                    handler.removeCallbacks(repeatDelete)
                    if (scrollish && !moved) handleKey(key)
                    else if (deferred && !moved && e.x >= 0f && e.y >= 0f && e.x <= v.width && e.y <= v.height) {
                        if (haptic) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        handleKey(key)
                    }
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

    /** Shift / avto-katta harf: butun klaviaturani qayta qurmasdan faqat yozuvlarni almashtiradi. */
    private fun refreshCaps() {
        drawnCaps = caps
        letterViews.forEach { (v, k) -> v.text = if (caps) k.uppercase() else k }
        shiftView?.text = if (capsLock) "⇪" else "⇧"
        shiftBg?.setColor(
            if (caps || capsLock) Color.argb(80, Color.red(theme.text), Color.green(theme.text), Color.blue(theme.text))
            else theme.special
        )
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
        val cr = corr
        if (cr != null) {              // avto-tuzatishni bekor qilish: asl so'z qaytadi va qayta tuzatilmaydi
            corr = null
            ic.deleteSurroundingText(cr.corrected.length + cr.boundary.length, 0)
            ic.commitText(cr.original, 1)
            WordModel.keep(cr.original)
            scheduleSuggest()
            return
        }
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        scheduleSuggest()
    }

    /** Boshlang'ich katta harf (gap boshi) holatini sozlaydi. */
    private fun syncCaps() {
        if (capsLock || trMode) return
        val ic = currentInputConnection
        val want = ic != null && !isRawField() &&
            ic.getCursorCapsMode(currentInputEditorInfo?.inputType ?: 0) != 0
        caps = want
        if (caps != drawnCaps) refreshCaps()
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
            "t:PHR" -> { layer = Layer.PHRASES; render() }
            "t:CONV" -> { render(); if (ic != null) doConvert(ic) }
            "t:COPY" -> { render(); if (ic != null) doCopyStandard(ic) }
            "t:UNDO" -> { render(); sendUndoRedo(false) }
            "t:REDO" -> { render(); sendUndoRedo(true) }
            "t:THEME" -> {
                val all = Themes.cycle(this)
                val cur = prefs().getString("theme", Themes.DEFAULT_ID)
                val next = all[(all.indexOfFirst { it.id == cur } + 1) % all.size]
                prefs().edit().putString("theme", next.id).apply()
                loadPrefs(); render()
                toast("Mavzu: " + next.name)
            }
            "t:NIGHT" -> {
                prefs().edit().putString("theme", if (theme.id == "dark") "light" else "dark").apply()
                loadPrefs(); render()
            }
            else -> {
                render()
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    private fun handleKey(key: String) {
        val ic = currentInputConnection ?: return
        pendingUndo = null
        corr = null
        when {
            key == "⇧" -> {
                val now = SystemClock.uptimeMillis()
                if (capsLock) { capsLock = false; caps = false }
                else if (caps && now - lastShiftTap < 400) capsLock = true
                else caps = !caps
                lastShiftTap = now
                refreshCaps()
            }
            key == "↵" -> {
                if (trMode) { translateBuffer(false); return }
                boundaryPrep(ic)
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
            key == "space" -> {
                if (trMode) commit(" ")
                else {
                    val cr = boundaryPrep(ic)
                    commit(" ")
                    if (cr != null) corr = Corr(cr.first, cr.second, " ")
                    syncCaps()
                    scheduleSuggest()
                }
            }
            key.startsWith("sg:") -> pickSuggestion(key.substring(3).toInt())
            key == "🌐" -> {
                val all = Languages.active(this)
                val i = all.indexOfFirst { it.code == lang.code }
                setLang(all[(i + 1) % all.size].code)
            }
            key == "LANGLAT" -> setLang("UZ")
            key == "LANGCYR" -> setLang("UZC")
            key == "CLIP" -> openClipboard()
            key == "cchip" -> {
                recentClip?.let { commit(it) }
                recentUsed = true
                render()
            }
            key == "?123" -> { layer = Layer.SYMBOLS; render() }
            key == "ABC" -> { layer = Layer.LETTERS; render(); syncCaps() }
            key == "😊" -> { layer = Layer.EMOJI; render() }
            key == "TOOLS" -> { layer = Layer.TOOLS; render() }
            key == "HIDE" -> requestHideSelf(0)
            key == "ttgt" -> { cycleTarget(); render(); scheduleLive() }
            key == "tboth" -> translateBuffer(true)
            key == "tx" -> { trMode = false; trBuf.clear(); resetLive(); render(); syncCaps() }
            key == "tok" -> translateBuffer(false)
            key.startsWith("ctab:") -> { clipTab = key.substring(5).toInt(); clipDelAsk = -1L; render() }
            key == "cclear" -> { ClipStore.clearHistory(this); render() }   // Sevimli/Mahkamlanganlar qoladi
            key.startsWith("cp:") -> {
                clipItems.getOrNull(key.substring(3).toInt())?.let { item ->
                    ClipStore.text(this, item.id)?.let { commit(it) }
                }
                layer = Layer.LETTERS; render()
            }
            key.startsWith("cfav:") -> {
                clipItems.getOrNull(key.substring(5).toInt())?.let { ClipStore.toggleFav(this, it.id) }
                render()
            }
            key.startsWith("cpin:") -> {
                clipItems.getOrNull(key.substring(5).toInt())?.let { ClipStore.togglePin(this, it.id) }
                render()
            }
            key.startsWith("cdel:") -> {
                clipItems.getOrNull(key.substring(5).toInt())?.let { item ->
                    // Sevimli / Mahkamlangan matn tasdiqsiz o'chmaydi; oddiy tarix — darhol
                    if (item.saved) clipDelAsk = item.id else ClipStore.remove(this, item.id)
                }
                render()
            }
            key.startsWith("cno:") -> { clipDelAsk = -1L; render() }
            key.startsWith("cyes:") -> {
                clipItems.getOrNull(key.substring(5).toInt())?.let { ClipStore.remove(this, it.id) }
                clipDelAsk = -1L
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
                    val punct = value.length == 1 && value[0] in ".,!?;:"
                    val cr = if (punct) boundaryPrep(ic) else null
                    ic.commitText(value, 1)
                    if (cr != null) corr = Corr(cr.first, cr.second, value)
                    // sh, ch, g', o' juftliklarini faqat kerak bo'lganda tekshiramiz (tezlik)
                    if (lang.uzDigraph && (value.equals("h", true) || value in apostrophes) && !isRawField()) convertDigraph()
                }
                if (!capsLock) caps = false
                // Harfdan keyin gap boshi bo'lishi mumkin emas — tizimdan so'rab o'tirmaymiz
                if (!value.last().isLetterOrDigit()) syncCaps()
                if (caps != drawnCaps) refreshCaps()
                if (!trMode) scheduleSuggest()
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
        corr = null
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
        handler.removeCallbacks(suggestRunnable)
        WordModel.save()
    }

    override fun onDestroy() {
        animator?.cancel()
        handler.removeCallbacks(liveRunnable)
        handler.removeCallbacks(suggestRunnable)
        WordModel.save()
        if (clipListenerOn) {
            try {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).removePrimaryClipChangedListener(clipListener)
            } catch (e: Exception) { /* e'tiborsiz */ }
        }
        super.onDestroy()
    }

    // ---------- so'z takliflari, avto-tuzatish, Smart Touch, Undo/Redo ----------

    private fun applyCase(orig: String, w: String): String = when {
        orig.length > 1 && orig.all { !it.isLetter() || it.isUpperCase() } -> w.uppercase()
        orig.isNotEmpty() && orig[0].isUpperCase() -> w.replaceFirstChar { it.uppercase() }
        else -> w
    }

    /** So'z tugaganda (probel / tinish belgisi / Enter): so'zni o'rganadi va ISHONCHLI xatoni tuzatadi. */
    private fun boundaryPrep(ic: InputConnection): Pair<String, String>? {
        if (!predictOn && !autoCorrectOn) return null
        if (lang.code != "UZ" || isRawField()) return null
        val before = ic.getTextBeforeCursor(80, 0)?.toString() ?: return null
        val cx = WordModel.parse(before)
        val w = cx.prefix
        if (w.length < 2) return null
        if (autoCorrectOn && w.none { it == '\'' }) {
            val capOk = !w[0].isUpperCase() || cx.sentenceStart
            val mixed = w.drop(1).any { it.isUpperCase() } && !w.all { !it.isLetter() || it.isUpperCase() }
            if (capOk && !mixed) {
                val fix = WordModel.autoFix(w.lowercase(), cx.prev, neighbors)
                if (fix != null) {
                    val out = applyCase(w, fix)
                    ic.deleteSurroundingText(w.length, 0)
                    ic.commitText(out, 1)
                    WordModel.learn(out, cx.prev)
                    return Pair(w, out)
                }
            }
        }
        if (predictOn) WordModel.learn(w, cx.prev)
        return null
    }

    private fun scheduleSuggest() {
        if (!sgBarOn() || layer != Layer.LETTERS || trMode) return
        handler.removeCallbacks(suggestRunnable)
        handler.postDelayed(suggestRunnable, 30)
    }

    private fun updateSuggestions() {
        if (sgViews.isEmpty()) return
        var list: List<String> = emptyList()
        val ic = currentInputConnection
        if (ic != null && sgBarOn() && layer == Layer.LETTERS && !trMode && !isRawField()) {
            val before = ic.getTextBeforeCursor(80, 0)?.toString().orEmpty()
            val cx = WordModel.parse(before)
            list = if (cx.prefix.isEmpty()) {
                WordModel.next(cx.prev).map { if (cx.sentenceStart || caps) it.replaceFirstChar { c -> c.uppercase() } else it }
            } else {
                WordModel.complete(cx.prefix, cx.prev).map { applyCase(cx.prefix, it) }
            }
        }
        sugg = list
        sgViews.forEachIndexed { i, v -> v.text = sugg.getOrElse(i) { "" } }
    }

    private fun pickSuggestion(i: Int) {
        val s = sugg.getOrNull(i)?.takeIf { it.isNotEmpty() } ?: return
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val cx = WordModel.parse(before)
        ic.beginBatchEdit()
        if (cx.prefix.isNotEmpty()) ic.deleteSurroundingText(cx.prefix.length, 0)
        ic.commitText("$s ", 1)
        ic.endBatchEdit()
        WordModel.learn(s, cx.prev)
        if (!capsLock) caps = false
        syncCaps()
        if (caps != drawnCaps) refreshCaps()
        scheduleSuggest()
    }

    /** Barmoq tugma CHETIGA tushsa: faqat yozilgan harf lug'atda hech qanday so'zga olib bormasa-yu,
     *  qo'shni harf olib borsagina qo'shni tanlanadi. Aniq bosilgan tugmaga xalaqit bermaydi. */
    private fun smartPick(key: String, left: String?, right: String?, x: Float, w: Int): String {
        if (!smartTouchOn || lang.code != "UZ" || w <= 0 || key.length != 1) return key
        val r = x / w
        val cand = (if (r < 0.18f) left else if (r > 0.82f) right else null) ?: return key
        if (cand.length != 1 || !cand[0].isLetter()) return key
        val ic = currentInputConnection ?: return key
        if (isRawField()) return key
        val cx = WordModel.parse(ic.getTextBeforeCursor(40, 0)?.toString().orEmpty())
        if (cx.prefix.length < 2) return key
        val p = cx.prefix.lowercase()
        return if (!WordModel.isPrefix(p + key.lowercase()) && WordModel.isPrefix(p + cand.lowercase())) cand else key
    }

    /** Ctrl+Z / Ctrl+Shift+Z. Aksar matn maydonlari (Android 8+) qo'llab-quvvatlaydi. */
    private fun sendUndoRedo(redo: Boolean) {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        var meta = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (redo) meta = meta or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, meta))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, meta))
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        scheduleSuggest()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
