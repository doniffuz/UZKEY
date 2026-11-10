package uz.ozkey.keyboard

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch

/**
 * ÖZKEY — sozlash ilovasi. Premium dizayn tizimi: deep navy fon, electric blue/cyan aksent, yumaloq kartalar,
 * yagona ikonka uslubi (res/drawable/ic_*.xml). Premium qorong'i dizayn — majburiy standart.
 * Dasturchi: Donyor.
 */
class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var pickerShown = false
    private var tab = 0                  // pastki navigatsiya: 0 Asosiy, 1 Statistika, 2 Haqida
    private var screen: String? = null   // ichki ekran: null | "settings" | "themes" | "bufer" | "lang"
    private var onboardPage = 0
    private var topInset = 0
    private var bottomInset = 0
    private var stepsHolder: LinearLayout? = null

    private lateinit var outer: FrameLayout   // ildiz + splash qatlami
    private lateinit var root: FrameLayout
    private val bufer by lazy { BuferTab(this) }

    // ---- ÖZKEY premium ranglari (dizayn tokenlari) ----
    internal val cBg = Color.parseColor("#07122B")          // asosiy fon (deep navy)
    internal val cBgTop = Color.parseColor("#0E2256")
    internal val cSurface = Color.parseColor("#0F1E43")     // karta
    internal val cField = Color.parseColor("#162A58")       // kiritish maydoni / ikkilamchi sirt
    internal val cAccent = Color.parseColor("#2F80FF")      // electric blue
    internal val cAccentDeep = Color.parseColor("#1B4FD6")
    internal val cAccentSoft = Color.parseColor("#7DB0FF")
    internal val cCyan = Color.parseColor("#38D0FF")
    internal val cInk = Color.parseColor("#F2F6FF")         // oq matn
    internal val cMuted = Color.parseColor("#8EA2C8")
    internal val cLine = Color.parseColor("#233864")
    internal val cOk = Color.parseColor("#2ED47A")

    private val PRIVACY_URL = "https://doniffuz.github.io/UZKEY/privacy.html"
    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    private val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

    private fun prefs() = getSharedPreferences("ozkey", Context.MODE_PRIVATE)
    internal fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun imm() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    private fun isEnabled() = imm().enabledInputMethodList.any { it.packageName == packageName }
    private fun isSelected(): Boolean {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return false
        return id.startsWith("$packageName/")
    }

    private fun showUserWords() {
        WordModel.init(this)
        val list = WordModel.userWords().take(200)
        if (list.isEmpty()) { toast("Shaxsiy lug'at hali bo'sh"); return }
        val items = list.map { "${it.first}  ·  ${it.second}" }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("So'zni o'chirish uchun bosing")
            .setItems(items) { _, i ->
                list.getOrNull(i)?.let { WordModel.removeUserWord(it.first); toast("O'chirildi: " + it.first) }
            }
            .setNegativeButton("Yopish", null)
            .show()
    }


    internal fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    // ---------- kichik yordamchilar ----------

    internal fun tv(t: String, size: Float = 15f, color: Int = cInk, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    internal fun gap(h: Int = 12) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }

    internal fun rounded(color: Int, r: Int = 16) = GradientDrawable().apply { setColor(color); cornerRadius = dp(r).toFloat() }

    private fun glow(v: View, elev: Int) {
        v.elevation = dp(elev).toFloat()
        if (Build.VERSION.SDK_INT >= 28) { v.outlineSpotShadowColor = cAccent; v.outlineAmbientShadowColor = cAccent }
    }

    internal fun primaryBtn(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label; textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(cAccent, cAccentDeep))
            .apply { cornerRadius = dp(28).toFloat() }
        glow(this, 4)
        setPadding(dp(20), dp(15), dp(20), dp(15))
        setOnClickListener { onClick() }
    }

    internal fun outlineBtn(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label; textSize = 14f; setTextColor(cAccentSoft); gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        background = rounded(Color.argb(38, 47, 128, 255), 28).apply { setStroke(dp(1), Color.argb(90, 47, 128, 255)) }
        setPadding(dp(16), dp(12), dp(16), dp(12))
        setOnClickListener { onClick() }
    }

    internal fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(cSurface, 24).apply { setStroke(dp(1), cLine) }
        setPadding(dp(18), dp(16), dp(18), dp(18))
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(14) }
    }

    internal fun section(t: String) = tv(t.uppercase(), 12f, cCyan, true).apply {
        letterSpacing = 0.12f
        setPadding(dp(4), dp(10), 0, dp(8))
    }

    internal fun field(hintText: String) = EditText(this).apply {
        hint = hintText; setHintTextColor(cMuted); setTextColor(cInk); textSize = 15f
        background = rounded(cField, 16).apply { setStroke(dp(1), cLine) }
        setPadding(dp(16), dp(13), dp(16), dp(13))
    }

    private fun logo(size: Int) = ImageView(this).apply {
        setImageResource(R.drawable.logo_app)
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
    }

    internal fun icon(res: Int, sizeDp: Int = 22, tint: Int = cAccentSoft) = ImageView(this).apply {
        setImageResource(res); setColorFilter(tint)
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    }

    private fun badge(res: Int): View = FrameLayout(this).apply {
        background = rounded(Color.argb(44, 47, 128, 255), 13)
        addView(ImageView(this@MainActivity).apply { setImageResource(res); setColorFilter(cAccentSoft) },
            FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
    }

    private class Row(val view: LinearLayout, val sub: TextView)

    /** Yagona qator uslubi: ikonka · sarlavha · izoh · (switch / o'q). Matn hech qachon kesilib chiqib ketmaydi. */
    private fun rowBase(iconRes: Int, title: String, subtitle: String, trailing: View): Row {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(cSurface, 18).apply { setStroke(dp(1), cLine) }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(10) }
        }
        row.addView(badge(iconRes))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, dp(10), 0) }
        col.addView(tv(title, 15.5f, cInk, true).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END })
        val sub = tv(subtitle, 12.5f, cMuted).apply {
            maxLines = 3; ellipsize = TextUtils.TruncateAt.END; setPadding(0, dp(2), 0, 0)
            if (subtitle.isEmpty()) visibility = View.GONE
        }
        col.addView(sub)
        row.addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(trailing)
        return Row(row, sub)
    }

    private fun chevron() = icon(R.drawable.ic_chevron, 20, cMuted)

    private fun navRow(iconRes: Int, title: String, subtitle: String, onClick: () -> Unit): View =
        rowBase(iconRes, title, subtitle, chevron()).view.apply { setOnClickListener { onClick() } }

    private fun styleSwitch(sw: Switch) {
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        sw.thumbTintList = ColorStateList(states, intArrayOf(Color.WHITE, Color.parseColor("#9FB0D0")))
        sw.trackTintList = ColorStateList(states, intArrayOf(cAccent, Color.parseColor("#2A3E6E")))
    }

    private fun switchRow(iconRes: Int, title: String, key: String, default: Boolean,
                          subtitle: String? = null, on: String = "Yoqilgan", off: String = "O'chirilgan"): View {
        val sw = Switch(this).apply { styleSwitch(this); isChecked = prefs().getBoolean(key, default) }
        val r = rowBase(iconRes, title, subtitle ?: (if (sw.isChecked) on else off), sw)
        sw.setOnCheckedChangeListener { _, v ->
            prefs().edit().putBoolean(key, v).apply()
            if (subtitle == null) r.sub.text = if (v) on else off
        }
        r.view.setOnClickListener { sw.toggle() }
        return r.view
    }

    private fun circleBtn(iconRes: Int, desc: String, onClick: () -> Unit): View = FrameLayout(this).apply {
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(cSurface); setStroke(dp(1), cLine) }
        addView(ImageView(this@MainActivity).apply { setImageResource(iconRes); setColorFilter(cInk) },
            FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        contentDescription = desc
        layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
        setOnClickListener { onClick() }
    }

    private fun statusPill(label: String, ok: Boolean): View = TextView(this).apply {
        text = "●  $label"; textSize = 12.5f
        val c = if (ok) cOk else Color.parseColor("#FFB020")
        setTextColor(c)
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        background = rounded(Color.argb(34, Color.red(c), Color.green(c), Color.blue(c)), 14)
        setPadding(dp(12), dp(6), dp(12), dp(6))
        layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
    }

    private fun scrollOf(page: View): View = ScrollView(this).apply {
        isVerticalScrollBarEnabled = false
        // keng ekran / yotiq holatda kontent o'rtada, 640dp dan kengaymaydi
        val maxW = minOf(resources.displayMetrics.widthPixels, dp(640))
        addView(FrameLayout(this@MainActivity).apply {
            addView(page, FrameLayout.LayoutParams(maxW, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL))
        })
    }

    private fun topBar(): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(20), topInset + dp(14), dp(16), dp(10))
        addView(logo(36))
        addView(tv("ÖZKEY", 22f, cInk, true).apply { letterSpacing = 0.06f; setPadding(dp(12), 0, 0, 0) },
            LinearLayout.LayoutParams(0, WRAP, 1f))
        addView(circleBtn(R.drawable.ic_gear, "Sozlamalar") { screen = "settings"; showMain() })
    }

    private fun subBar(title: String): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), topInset + dp(12), dp(16), dp(8))
        addView(circleBtn(R.drawable.ic_back, "Orqaga") { screen = null; showMain() })
        addView(tv(title, 20f, cInk, true).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; setPadding(dp(14), 0, 0, 0) },
            LinearLayout.LayoutParams(0, WRAP, 1f))
    }

    /** Katta (kirish) sarlavha: milliy naqsh (juda xira) + elektr-ko'k gradient. */
    private fun bigHeader(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), topInset + dp(36), dp(20), dp(30))
            background = HeaderDrawable(Color.parseColor("#143A8C"), cBg, cCyan, dp(40).toFloat(), dp(36).toFloat(), dp(1).toFloat())
        }
        box.addView(logo(110))
        box.addView(gap(12))
        box.addView(tv("ÖZKEY", 38f, Color.WHITE, true).apply { letterSpacing = 0.05f })
        box.addView(tv("Zamonaviy O‘zbek klaviaturasi", 15f, Color.argb(220, 255, 255, 255)))
        box.addView(tv("Dasturchi: Donyor", 14f, cCyan, true).apply { setPadding(0, dp(4), 0, 0) })
        return box
    }

    // ---------- hayot sikli ----------

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = cBg
            window.navigationBarColor = cBg
        }
        savedInstanceState?.let { tab = it.getInt("tab", 0); screen = it.getString("screen") }
        root = FrameLayout(this).apply { setBackgroundColor(cBg) }
        outer = FrameLayout(this).apply { setBackgroundColor(cBg); addView(root) }
        setContentView(outer)
        // Android 15+: ilova tizim panellari ostigacha cho'ziladi — chekkalarni o'zimiz hisobga olamiz
        root.setOnApplyWindowInsetsListener { _, insets ->
            if (Build.VERSION.SDK_INT >= 35) {
                val sys = insets.getInsets(WindowInsets.Type.systemBars())
                val ime = insets.getInsets(WindowInsets.Type.ime()).bottom
                // Klaviatura ochiq bo'lsa ilova uning tepasiga ko'tariladi (qayta qurilmaydi — yozilgan matn yo'qolmasin)
                root.setPadding(0, 0, 0, maxOf(0, ime - sys.bottom))
                if (sys.top != topInset || sys.bottom != bottomInset) {
                    topInset = sys.top; bottomInset = sys.bottom
                    rebuild()
                }
            }
            insets
        }
        rebuild()
        if (savedInstanceState == null) showSplash()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", tab)
        outState.putString("screen", screen)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (!prefs().getBoolean("onboarded", false)) { super.onBackPressed(); return }
        if (screen != null) { screen = null; showMain() }
        else if (tab != 0) { tab = 0; showMain() }
        else super.onBackPressed()
    }

    /** Premium splash: markazda logo, ostida sarlavha va izoh. Qisqa (≈1 s), faqat sovuq ishga tushishda. */
    private fun showSplash() {
        val v = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(cBgTop, cBg))
            isClickable = true
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        col.addView(logo(132))
        col.addView(tv("ÖZKEY", 34f, cInk, true).apply { letterSpacing = 0.08f; setPadding(0, dp(14), 0, 0) })
        col.addView(tv("Zamonaviy O‘zbek klaviaturasi", 15f, cMuted).apply { gravity = Gravity.CENTER; setPadding(dp(24), dp(6), dp(24), 0) })
        v.addView(col, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        col.alpha = 0f; col.scaleX = 0.92f; col.scaleY = 0.92f
        col.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(380).start()
        outer.addView(v, FrameLayout.LayoutParams(MATCH, MATCH))
        handler.postDelayed({
            v.animate().alpha(0f).setDuration(260).withEndAction { outer.removeView(v) }.start()
        }, 900)
    }

    private fun rebuild() {
        if (!prefs().getBoolean("onboarded", false)) showOnboarding()
        else showMain()
    }

    override fun onResume() {
        super.onResume()
        stepsHolder?.let { fillSteps(it, auto = true, home = screen == null && onboarded()) }
    }

    private fun onboarded() = prefs().getBoolean("onboarded", false)

    // ---------- ulash holati ----------

    private fun stepRow(n: Int, label: String, done: Boolean): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, dp(5))
        }
        row.addView(TextView(this).apply {
            text = if (done) "✓" else n.toString()
            textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if (done) cOk else cAccent) }
        }, LinearLayout.LayoutParams(dp(28), dp(28)))
        row.addView(tv(label, 15f, if (done) cMuted else cInk).apply { setPadding(dp(12), 0, 0, 0) })
        return row
    }


    private fun fillSteps(h: LinearLayout, auto: Boolean, home: Boolean = false) {
        h.removeAllViews()
        val en = isEnabled()
        val sel = en && isSelected()
        h.addView(statusPill(if (sel) "Faol — ÖZKEY tanlangan" else if (en) "Yoqilgan — endi tanlang" else "Hali yoqilmagan", sel))
        h.addView(gap(10))
        if (!sel) {
            h.addView(stepRow(1, "ÖZKEY klaviaturasiga ruxsat bering", en))
            h.addView(stepRow(2, "ÖZKEY ni tanlang", sel))
            h.addView(gap(10))
        }
        when {
            !en -> h.addView(primaryBtn("Sozlamalarni ochish") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) })
            !sel -> h.addView(primaryBtn("Klaviaturani tanlash") { imm().showInputMethodPicker() })
            home -> h.addView(primaryBtn("Mavzuni sozlash") { screen = "themes"; showMain() })
            else -> h.addView(tv("✅ Hammasi tayyor! Istalgan ilovada yozishni boshlang.", 14f, cOk, true))
        }
        if (!en || sel) pickerShown = false
        if (en && !sel && auto && !pickerShown) {
            pickerShown = true
            handler.postDelayed({ if (!isFinishing) imm().showInputMethodPicker() }, 500)
        }
    }

    // ---------- KIRISH EKRANI ----------

    private fun showOnboarding() {
        root.removeAllViews()
        stepsHolder = null
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(bigHeader())
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(22), dp(24), dp(8))
        }
        if (onboardPage == 0) {
            body.addView(tv("Xush kelibsiz!", 24f, cInk, true))
            body.addView(gap(6))
            body.addView(tv("O'zbek tilida tez, qulay va chiroyli yozing.", 16f, cMuted))
            body.addView(gap(8))
            listOf(
                "🔤" to "Lotin ⇄ Kirill — bitta tugma bilan",
                "⚡" to "So'z takliflari va xavfsiz avto-tuzatish",
                "📋" to "Bufer tarixi — nusxalanganlar saqlanadi",
                "🎨" to "32 mavzu va galereyadan o'z rasmingiz"
            ).forEach { (e, t) ->
                body.addView(LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    background = rounded(cSurface, 18).apply { setStroke(dp(1), cLine) }
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    addView(tv(e, 22f).apply { setPadding(0, 0, dp(14), 0) })
                    addView(tv(t, 15f, cInk))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(8) }
                })
            }
        } else {
            body.addView(tv("ÖZKEY ni yoqish", 24f, cInk, true))
            body.addView(gap(8))
            val holder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            body.addView(holder)
            stepsHolder = holder
            fillSteps(holder, auto = false, home = false)
            body.addView(gap(12))
            body.addView(tv("Android «klaviatura ma'lumot yig'ishi mumkin» deb ogohlantiradi — bu tizimning standart xabari. " +
                "ÖZKEY yozganingizni serverga yubormaydi (faqat siz o'zingiz yoqadigan ixtiyoriy tarjimadan tashqari).", 12f, cMuted))
        }
        col.addView(scrollOf(body), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        // nuqtalar
        col.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            for (i in 0..1) addView(View(this@MainActivity).apply {
                background = rounded(if (i == onboardPage) cAccent else cLine, 8)
                layoutParams = LinearLayout.LayoutParams(dp(if (i == onboardPage) 22 else 8), dp(8)).apply {
                    leftMargin = dp(4); rightMargin = dp(4)
                }
            })
        })
        col.addView(gap(14))
        col.addView(primaryBtn(if (onboardPage == 0) "Davom etish" else "Boshlash") {
            if (onboardPage == 0) { onboardPage = 1; showOnboarding() }
            else { prefs().edit().putBoolean("onboarded", true).apply(); showMain() }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(24); rightMargin = dp(24) }
        })
        col.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(20) + bottomInset) })
        root.addView(col)
    }


    // ---------- ASOSIY EKRAN ----------

    internal fun pageBox() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(24))
    }

    private fun showMain() {
        root.removeAllViews()
        stepsHolder = null
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val sc = screen
        val title = when (sc) { "settings" -> "Sozlamalar"; "themes" -> "Mavzu tanlash"; "bufer" -> "Clipboard"; "lang" -> "Til / Layout"; else -> "" }
        col.addView(if (sc == null) topBar() else subBar(title))
        val page = when (sc) {
            "settings" -> settingsTab()
            "themes" -> themesTab()
            "bufer" -> bufer.build()
            "lang" -> langTab()
            else -> when (tab) { 0 -> homeTab(); 1 -> statsTab(); else -> aboutTab() }
        }
        col.addView(scrollOf(page), LinearLayout.LayoutParams(MATCH, 0, 1f))
        if (sc == null) col.addView(navBar())
        else col.addView(View(this), LinearLayout.LayoutParams(1, bottomInset))
        root.addView(col)
    }

    private fun navBar(): View {
        val bar = LinearLayout(this).apply {
            background = rounded(cSurface, 30).apply { setStroke(dp(1), cLine) }
            setPadding(dp(6), dp(6), dp(6), dp(6))
            glow(this, 10)
        }
        val tabs = listOf(Triple(R.drawable.ic_home, "Asosiy", 0), Triple(R.drawable.ic_chart, "Statistika", 1), Triple(R.drawable.ic_info, "Haqida", 2))
        tabs.forEach { (ic, name, i) ->
            val on = i == tab
            bar.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(8))
                background = if (on) rounded(Color.argb(46, 47, 128, 255), 24).apply { setStroke(dp(1), Color.argb(120, 56, 208, 255)) } else null
                addView(icon(ic, 22, if (on) cCyan else cMuted))
                addView(tv(name, 11.5f, if (on) cInk else cMuted, on).apply {
                    maxLines = 1; ellipsize = TextUtils.TruncateAt.END; setPadding(0, dp(2), 0, 0)
                })
                setOnClickListener { tab = i; showMain() }
            }, LinearLayout.LayoutParams(0, WRAP, 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        return FrameLayout(this).apply {
            setPadding(dp(16), dp(6), dp(16), dp(10) + bottomInset)
            addView(bar)
        }
    }

    // ---------- ASOSIY (Home) ----------

    private fun currentLangName(): String {
        val code = prefs().getString("lang", "UZ")
        return (Languages.builtins + Languages.installed(this)).firstOrNull { it.code == code }?.name ?: Languages.UZ.name
    }

    private fun heroCard(): View {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#143A8C"), Color.parseColor("#0B1B45"))).apply {
                cornerRadius = dp(26).toFloat(); setStroke(dp(1), Color.argb(130, 56, 208, 255))
            }
            setPadding(dp(18), dp(18), dp(18), dp(18))
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(14) }
            glow(this, 8)
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(logo(64))
        val t = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0) }
        t.addView(tv("ÖZKEY", 26f, cInk, true).apply { letterSpacing = 0.05f })
        t.addView(tv("Premium O'zbek klaviaturasi", 13.5f, Color.argb(225, 220, 232, 255)))
        t.addView(tv("v" + BuildConfig.VERSION_NAME, 11.5f, cCyan, true).apply { setPadding(0, dp(2), 0, 0) })
        top.addView(t, LinearLayout.LayoutParams(0, WRAP, 1f))
        c.addView(top)
        c.addView(gap(14))
        val holder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        c.addView(holder)
        stepsHolder = holder
        fillSteps(holder, auto = true, home = true)
        return c
    }

    private fun homeTab(): View {
        val p = pageBox()
        p.addView(heroCard())

        p.addView(navRow(R.drawable.ic_globe, "Til / Layout", currentLangName()) { screen = "lang"; showMain() })
        p.addView(navRow(R.drawable.ic_moon, "Mavzu", Themes.byId(prefs().getString("theme", Themes.DEFAULT_ID)).name) { screen = "themes"; showMain() })
        p.addView(navRow(R.drawable.ic_clipboard, "Clipboard", "Saqlangan matnlar") { screen = "bufer"; showMain() })
        p.addView(switchRow(R.drawable.ic_sparkle, "Avto tuzatish", "autocorrect", true))
        p.addView(switchRow(R.drawable.ic_bulb, "Takliflar", "predict", true))
        p.addView(switchRow(R.drawable.ic_volume, "Tovush", "sound", true))
        p.addView(switchRow(R.drawable.ic_vibrate, "Vibratsiya", "haptic", true))
        p.addView(gap(6))

        val c2 = card()
        c2.addView(tv("Sinab ko'ring", 17f, cInk, true))
        c2.addView(gap(8))
        c2.addView(field("Shu yerga yozib ko'ring…"))
        p.addView(c2)

        // Konvertor
        val c3 = card()
        c3.addView(tv("Lotin ⇄ Kirill konvertor", 17f, cInk, true))
        c3.addView(gap(8))
        val input = field("Matnni kiriting yoki joylang")
        val out = tv("", 16f, cCyan, true).apply { setTextIsSelectable(true); setPadding(dp(2), dp(10), dp(2), dp(10)) }
        c3.addView(input)
        c3.addView(gap(8))
        fun convBtn(label: String, f: (String) -> String) = outlineBtn(label) { out.text = f(input.text.toString()) }.apply {
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply { rightMargin = dp(6) }
        }
        c3.addView(LinearLayout(this).apply {
            addView(convBtn("→ Lotin") { Alphabet.toStandard(Script.cyrToLat(it)) })
            addView(convBtn("→ ÖZ") { Alphabet.toNew(Script.cyrToLat(it)) })
            addView(convBtn("→ Кирилл") { Script.latToCyr(it) })
        })
        c3.addView(out)
        c3.addView(outlineBtn("Nusxa olish") {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("ozkey", out.text.toString())); toast("Nusxalandi")
        })
        p.addView(c3)
        return p
    }

    // ---------- STATISTIKA ----------

    private fun statTile(iconRes: Int, value: String, label: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(cSurface, 20).apply { setStroke(dp(1), cLine) }
        setPadding(dp(16), dp(16), dp(16), dp(16))
        layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply { setMargins(dp(5), dp(5), dp(5), dp(5)) }
        addView(icon(iconRes, 22, cCyan))
        addView(tv(value, 26f, cInk, true).apply { maxLines = 1; setPadding(0, dp(10), 0, 0) })
        addView(tv(label, 12.5f, cMuted).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END })
    }

    private fun statsTab(): View {
        val p = pageBox()
        WordModel.init(this)
        val keys = prefs().getLong("stat_keys", 0L)
        val words = prefs().getLong("stat_words", 0L)
        val clips = ClipStore.stats(this).first
        val learned = WordModel.userWords().size
        p.addView(tv("Statistika", 24f, cInk, true))
        p.addView(tv("Faqat shu telefonda hisoblanadi, hech qayerga yuborilmaydi.", 12.5f, cMuted).apply { setPadding(0, dp(2), 0, dp(10)) })
        p.addView(LinearLayout(this).apply {
            addView(statTile(R.drawable.ic_keyboard, keys.toString(), "Bosilgan tugmalar"))
            addView(statTile(R.drawable.ic_sparkle, words.toString(), "Yozilgan so'zlar"))
        })
        p.addView(LinearLayout(this).apply {
            addView(statTile(R.drawable.ic_clipboard, clips.toString(), "Clipboard matnlari"))
            addView(statTile(R.drawable.ic_bulb, learned.toString(), "O'rganilgan so'zlar"))
        })
        p.addView(gap(10))
        p.addView(outlineBtn("Hisoblagichni nolga qaytarish") {
            prefs().edit().apply { KeyStats.KEYS.forEach { remove(it) } }.apply(); showMain()
        })
        return p
    }

    // ---------- HAQIDA ----------

    private fun aboutTab(): View {
        val p = pageBox()
        p.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(8), 0, dp(16))
            addView(logo(96))
            addView(tv("ÖZKEY", 28f, cInk, true).apply { letterSpacing = 0.06f; setPadding(0, dp(10), 0, 0) })
            addView(tv("Zamonaviy O‘zbek klaviaturasi", 14f, cMuted))
            addView(tv("v" + BuildConfig.VERSION_NAME, 12f, cCyan, true).apply { setPadding(0, dp(4), 0, 0) })
        })
        p.addView(developerCard())
        p.addView(supportCard())
        p.addView(privacyCard())
        p.addView(tv("ÖZKEY ${BuildConfig.VERSION_NAME} · Donyor", 12f, cMuted).apply { gravity = Gravity.CENTER })
        return p
    }

    private fun privacyCard(): View {
        val c3 = card()
        c3.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(icon(R.drawable.ic_shield, 22, cCyan))
            addView(tv("Maxfiylik", 17f, cInk, true).apply { setPadding(dp(10), 0, 0, 0) })
        })
        c3.addView(gap(6))
        val online = if (Translator.available)
            "Internet faqat ixtiyoriy tarjima serveri ulangani uchun va siz tarjima rejimida yozsangiz yoki «Maydonni tarjima»ni bossangiz ishlatiladi: bunda faqat shu matn dasturchining tarjima serveriga yuboriladi (server uni Anthropic Claude tarjima xizmatiga uzatadi). "
        else
            "Tarjima serveri ulanmagan: hozir ilova internetga chiqmaydi va hech narsa yuborilmaydi. "
        c3.addView(tv("Klaviatura internetsiz ishlaydi: yozganingiz, o'rganilgan so'zlar va bufer faqat telefoningizda qoladi. " + online +
            "Parol, email va havola maydonlarida avto-almashtirish va o'rganish o'chirilgan.", 13f, cMuted))
        c3.addView(gap(10))
        c3.addView(outlineBtn("🔒 Privacy Policy") { openUrl(PRIVACY_URL) })
        return c3
    }

    private fun shareInstalledApk() {
        try {
            val uri = Uri.parse("content://$packageName.apkshare/app.apk")
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "ÖZKEY — O‘zbek klaviaturasi. Tez, qulay va reklamasiz.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "ÖZKEY APKni ulashish"))
        } catch (e: Exception) {
            toast("APKni ulashib bo'lmadi")
        }
    }

    /** Play (AAB) o'rnatishida ilova bo'laklarga (split APK) bo'linadi: bitta base.apk alohida o'rnatilmaydi, shuning uchun u ulashilmaydi. */
    private fun apkShareable(): Boolean = applicationInfo.splitSourceDirs.isNullOrEmpty()

    private fun shareStoreLink() {
        try {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "ÖZKEY — O‘zbek klaviaturasi: ${BuildConfig.STORE_URL}")
            }
            startActivity(Intent.createChooser(send, "ÖZKEY ni ulashish"))
        } catch (e: Exception) {
            toast("Ulashib bo'lmadi")
        }
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (e: Exception) { toast("Ochib bo'lmadi") }
    }

    /** Daromad va o'sish: baho, ulashish, ixtiyoriy qo'llab-quvvatlash (INTERNET ruxsati kerak emas). */
    private fun supportCard(): View {
        val c = card()
        c.addView(tv("❤️ ÖZKEY sizga yoqdimi?", 17f, cInk, true))
        c.addView(tv("Ilova bepul va reklamasiz. Do'stlaringizga ulashing yoki rivojiga hissa qo'shing.", 13f, cMuted)
            .apply { setPadding(0, dp(4), 0, dp(10)) })
        if (apkShareable()) {
            c.addView(primaryBtn("📦 APKni do'stlarga ulashish") {
                shareInstalledApk()
            })
        } else if (BuildConfig.STORE_URL.isNotEmpty()) {
            c.addView(primaryBtn("📤 Do'stlarga ulashish") { shareStoreLink() })
        }
        if (BuildConfig.STORE_URL.isNotEmpty()) {
            c.addView(gap(8))
            c.addView(outlineBtn("⭐ Baho berish") { openUrl(BuildConfig.STORE_URL) })
        }
        if (BuildConfig.SUPPORT_URL.isNotEmpty()) {
            c.addView(gap(8))
            c.addView(outlineBtn("☕ Dasturchini qo'llab-quvvatlash") { openUrl(BuildConfig.SUPPORT_URL) })
        }
        return c
    }

    private fun openTelegram() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Doniku_Admin")))
        } catch (e: Exception) { toast("Telegram topilmadi: @Doniku_Admin") }
    }

    /** Ilova haqida: dasturchi Donyor va aloqa. */
    private fun developerCard(): View {
        val c = card()
        c.addView(tv("ÖZKEY Keyboard 🇺🇿 · v${BuildConfig.VERSION_NAME}", 20f, cInk, true))
        c.addView(tv("O‘zbekcha yozuv — yangi daraja.", 14f, cAccent, true).apply { setPadding(0, dp(2), 0, dp(8)) })
        c.addView(tv("ÖZKEY — o‘zbek tilida qulay, zamonaviy va tez yozish uchun yaratilgan Android klaviatura. " +
            "Ilova yangi o‘zbek alifbosini qo‘llab-quvvatlaydi va yozishni yanada qulay qilishga qaratilgan.", 13f, cMuted))
        c.addView(gap(12))

        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this).apply {
            text = "D"; textSize = 22f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(cCyan, cAccent))
                .apply { shape = GradientDrawable.OVAL }
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0) }
        col.addView(tv("👨‍💻 DASTURCHI", 11f, cCyan, true).apply { letterSpacing = 0.12f })
        col.addView(tv("Donyor", 20f, cInk, true))
        row.addView(col)
        c.addView(row)
        c.addView(gap(8))
        c.addView(tv("ÖZKEY — foydalanuvchilar uchun foydali va qulay bo‘ladigan mahsulotlar yaratish maqsadida ishlab chiqilmoqda. " +
            "Sizda taklif, xato yoki yangi g‘oya bo‘lsa, bizga yozishingiz mumkin.", 13f, cMuted))
        c.addView(gap(10))
        c.addView(primaryBtn("📩 Telegram: @Doniku_Admin") { openTelegram() })
        c.addView(gap(6))
        c.addView(outlineBtn("📢 @OZKEY_Official") { openUrl("https://t.me/OZKEY_Official") })
        c.addView(gap(4))
        c.addView(tv("Sizning fikringiz ÖZKEY'ni yanada yaxshilashga yordam beradi.", 12f, cMuted).apply { setPadding(0, dp(6), 0, 0) })
        c.addView(gap(10))
        c.addView(tv("🚀 Kelajakdagi maqsad", 14f, cInk, true))
        c.addView(tv("ÖZKEY'ni yanada qulay, tezkor va foydali qilish hamda o‘zbek tilidagi raqamli mahsulotlarni ko‘paytirish.", 13f, cMuted))
        c.addView(gap(8))
        c.addView(tv("ÖZKEY — O‘zbekcha yozuv, yangi avlod!", 13f, cAccent, true))
        return c
    }


    // ---------- MAVZU ----------

    private fun miniKey(t: KeyTheme, w: Float, accent: Boolean = false): View = View(this).apply {
        background = GradientDrawable().apply {
            setColor(if (accent) t.accent else t.key); cornerRadius = dp(4).toFloat()
            if (t.stroke != 0) setStroke(dp(1), t.stroke)
        }
        layoutParams = LinearLayout.LayoutParams(0, dp(15), w).apply { setMargins(dp(1), dp(2), dp(1), dp(2)) }
    }

    private fun miniKeyboard(t: KeyTheme): View {
        val kb = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(t.bgTop, t.bgBottom)).apply { cornerRadius = dp(12).toFloat() }
            setPadding(dp(5), dp(6), dp(5), dp(6))
        }
        listOf(10, 9, 7).forEach { n ->
            kb.addView(LinearLayout(this).apply { gravity = Gravity.CENTER; repeat(n) { addView(miniKey(t, 1f)) } })
        }
        kb.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(miniKey(t, 1.4f)); addView(miniKey(t, 4f)); addView(miniKey(t, 1.4f, accent = true))
        })
        return kb
    }

    private fun bigPreview(t0: KeyTheme, accentId: String, selected: Boolean): View {
        val t = Themes.withAccent(t0, accentId)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(cSurface, 20).apply { setStroke(dp(if (selected) 2 else 1), if (selected) t.accent else cLine) }
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply { setMargins(dp(4), 0, dp(4), dp(6)) }
            if (selected) glow(this, 6)
        }
        box.addView(miniKeyboard(t))
        box.addView(tv(t0.name + if (selected) "  ✓" else "", 13.5f, if (selected) cInk else cMuted, selected).apply {
            gravity = Gravity.CENTER; maxLines = 1; setPadding(0, dp(8), 0, dp(2))
        })
        box.setOnClickListener {
            prefs().edit().putString("theme", t0.id).apply()
            toast("«${t0.name}» tanlandi")
            showMain()
        }
        return box
    }

    private fun accentRow(accentId: String): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER
        Themes.accents.forEach { (id, name, col) ->
            val sel = id == accentId
            addView(FrameLayout(this@MainActivity).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(Color.TRANSPARENT)
                    if (sel) setStroke(dp(2), col)
                }
                setPadding(dp(4), dp(4), dp(4), dp(4))
                addView(View(this@MainActivity).apply {
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(col) }
                }, FrameLayout.LayoutParams(MATCH, MATCH))
                contentDescription = name
                setOnClickListener {
                    prefs().edit().putString("accent", id).apply()
                    toast("Rang uslubi: $name")
                    showMain()
                }
            }, LinearLayout.LayoutParams(dp(40), dp(40)).apply { setMargins(dp(4), 0, dp(4), 0) })
        }
    }

    private fun themesTab(): View {
        val p = pageBox()
        val current = prefs().getString("theme", Themes.DEFAULT_ID) ?: Themes.DEFAULT_ID
        val accentId = prefs().getString("accent", Themes.DEFAULT_ACCENT) ?: Themes.DEFAULT_ACCENT

        val tryCard = card()
        tryCard.addView(tv("Mavzuni shu yerda sinang", 15f, cInk, true))
        tryCard.addView(gap(8))
        tryCard.addView(field("Bosing — klaviatura ochiladi"))
        p.addView(tryCard)

        p.addView(LinearLayout(this).apply {
            addView(bigPreview(Themes.byId("ozdark"), accentId, current == "ozdark"))
            addView(bigPreview(Themes.byId("ozlight"), accentId, current == "ozlight"))
        })

        p.addView(section("Rang uslubi"))
        p.addView(accentRow(accentId))
        p.addView(tv("Rang uslubi Qora va Oq temaga qo'llanadi (Enter, Shift va belgilar).", 12f, cMuted).apply { setPadding(dp(4), dp(10), 0, dp(6)) })

        p.addView(gap(6))
        p.addView(photoCard())

        Themes.groups.filter { it != "Premium" }.forEach { g ->
            p.addView(section(g))
            Themes.byGroup(g).chunked(3).forEach { triple ->
                val row = LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(MATCH, WRAP) }
                triple.forEach { t -> row.addView(themeCard(t, t.id == current)) }
                repeat(3 - triple.size) { row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f)) }
                p.addView(row)
            }
        }
        p.addView(gap(8))
        p.addView(tv("Tanlangan mavzu klaviaturada darhol o'zgaradi.", 12f, cMuted))
        return p
    }

    private fun themeCard(t: KeyTheme, selected: Boolean): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(cSurface, 18).apply { setStroke(dp(if (selected) 2 else 1), if (selected) cAccent else cLine) }
            setPadding(dp(6), dp(6), dp(6), dp(6))
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) }
        }
        val preview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(t.bgTop, t.bgBottom))
                .apply { cornerRadius = dp(9).toFloat() }
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        repeat(3) { r ->
            preview.addView(LinearLayout(this).apply {
                repeat(6 - r) { i ->
                    addView(View(this@MainActivity).apply {
                        background = GradientDrawable().apply {
                            setColor(t.key); cornerRadius = dp(3).toFloat()
                            if (t.stroke != 0) setStroke(dp(1), if (t.rainbow) Color.HSVToColor(floatArrayOf((i * 55f + r * 30f) % 360f, .85f, 1f)) else t.stroke)
                        }
                        layoutParams = LinearLayout.LayoutParams(0, dp(13), 1f).apply { setMargins(dp(1), dp(2), dp(1), dp(2)) }
                    })
                }
            })
        }
        box.addView(preview)
        box.addView(tv(t.name + if (selected) " ✓" else "", 12f, if (selected) cCyan else cInk, selected).apply {
            gravity = Gravity.CENTER; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(6), 0, dp(2))
        })
        box.setOnClickListener {
            prefs().edit().putString("theme", t.id).apply()
            toast("«${t.name}» tanlandi")
            showMain()
        }
        return box
    }

    // ----- galereyadan rasm -----

    private val reqPick = 4211

    private fun photoCard(): View {
        val c = card()
        val has = PhotoStore.exists(this)
        val selected = prefs().getString("theme", Themes.DEFAULT_ID) == Themes.PHOTO_ID && has
        c.addView(tv("🖼  O'z rasmingiz" + if (selected) "  ✓" else "", 17f, cInk, true))
        c.addView(tv("Galereyadan istalgan rasmni klaviatura foni qiling.", 13f, cMuted))
        c.addView(gap(10))
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(cField, 12).apply { setStroke(dp(1), cLine) }
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(12).toFloat())
                }
            }
            clipToOutline = true
        }
        if (has) {
            try {
                val o = BitmapFactory.Options().apply { inSampleSize = 4 }
                thumb.setImageBitmap(BitmapFactory.decodeFile(PhotoStore.file(this).path, o))
            } catch (e: Exception) { /* ko'rinmasa ham mayli */ }
        }
        row.addView(thumb, LinearLayout.LayoutParams(dp(110), dp(70)))
        val btns = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
        btns.addView(primaryBtn(if (has) "Boshqa rasm" else "Galereyadan tanlash") { pickPhoto() })
        if (has) {
            btns.addView(gap(6))
            btns.addView(outlineBtn(if (selected) "Olib tashlash" else "Foydalanish") {
                if (selected) {
                    PhotoStore.delete(this)
                    prefs().edit().putString("theme", Themes.DEFAULT_ID).apply()
                } else prefs().edit().putString("theme", Themes.PHOTO_ID).apply()
                showMain()
            })
        }
        row.addView(btns, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        c.addView(row)
        if (has) {
            c.addView(gap(12))
            val dim = prefs().getInt("photodim", 35)
            val label = tv("Rasm xiraligi: $dim%", 13f, cMuted)
            c.addView(label)
            c.addView(seek(0, 80, dim) { v ->
                label.text = "Rasm xiraligi: $v%"
                prefs().edit().putInt("photodim", v).apply()
            })
            c.addView(tv("Xiralikni oshirsangiz harflar yaxshiroq o'qiladi.", 11f, cMuted))
        }
        return c
    }

    private fun pickPhoto() {
        val i = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(Intent.createChooser(i, "Rasm tanlang"), reqPick)
        } catch (e: Exception) { toast("Galereya topilmadi") }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (bufer.onResult(requestCode, resultCode, data)) return
        if (requestCode == reqPick && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            toast("Rasm tayyorlanmoqda…")
            Thread {
                val ok = try { savePhoto(uri) } catch (e: Exception) { false } catch (e: OutOfMemoryError) { false }
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    if (ok) {
                        prefs().edit().putString("theme", Themes.PHOTO_ID).apply()
                        toast("Rasm klaviaturaga qo'yildi")
                    } else toast("Rasmni o'qib bo'lmadi")
                    showMain()
                }
            }.start()
        }
    }

    /** Rasmni kichraytirib (eng uzun tomoni 1280 px) ilovaning ichki xotirasiga yozadi. */
    private fun savePhoto(uri: Uri): Boolean {
        val maxSide = 1280
        val bmp: Bitmap = decodeScaled(uri, maxSide) ?: return false
        FileOutputStream(PhotoStore.file(this)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return true
    }

    private fun decodeScaled(uri: Uri, maxSide: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= 28) {
            val src = ImageDecoder.createSource(contentResolver, uri)
            return ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val m = maxOf(w, h)
                if (m > maxSide) {
                    val s = maxSide.toFloat() / m
                    decoder.setTargetSize((w * s).toInt().coerceAtLeast(1), (h * s).toInt().coerceAtLeast(1))
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val m = maxOf(raw.width, raw.height)
        if (m <= maxSide) return raw
        val s = maxSide.toFloat() / m
        return Bitmap.createScaledBitmap(raw, (raw.width * s).toInt().coerceAtLeast(1), (raw.height * s).toInt().coerceAtLeast(1), true)
    }


    // ---------- TIL / LAYOUT ----------

    private fun radioDot(sel: Boolean): View = FrameLayout(this).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(Color.TRANSPARENT)
            setStroke(dp(2), if (sel) cAccent else cLine)
        }
        if (sel) addView(View(this@MainActivity).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(cAccent) }
        }, FrameLayout.LayoutParams(dp(12), dp(12), Gravity.CENTER))
        layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
    }

    private fun langTab(): View {
        val p = pageBox()
        val cur = prefs().getString("lang", "UZ")
        (Languages.builtins + Languages.installed(this)).forEach { l ->
            val sub = when (l.code) { "UZ" -> "Yangi alifbo: Ğ Ö Ş Ç"; "UZC" -> "Kirill alifbosi"; else -> "" }
            p.addView(rowBase(R.drawable.ic_globe, l.name, sub, radioDot(l.code == cur)).view.apply {
                setOnClickListener {
                    prefs().edit().putString("lang", l.code).apply()
                    toast(l.name)
                    showMain()
                }
            })
        }
        p.addView(tv("Klaviaturadagi 🌐 tugmasi tillarni almashtiradi.", 12f, cMuted).apply { setPadding(dp(4), dp(4), 0, dp(10)) })
        p.addView(extraLangsCard())
        return p
    }

    private fun extraLangsCard(): View {
        val c = card()
        val inst = Languages.installed(this).map { it.code }.toSet()
        val extra = Languages.bundled(this)
        if (extra.isEmpty()) return View(this)
        c.addView(tv("Qo'shimcha tillar", 15f, cInk, true))
        extra.forEach { l ->
            val has = l.code in inst
            c.addView(LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(2))
                addView(tv(l.name, 15f, cInk), LinearLayout.LayoutParams(0, WRAP, 1f))
                addView(outlineBtn(if (has) "O'chirish" else "Yuklash") {
                    if (has) Languages.remove(this@MainActivity, l.code)
                    else Languages.installBundled(this@MainActivity, l.code)
                    showMain()
                })
            })
        }
        return c
    }

    // ---------- SOZLAMALAR ----------

    private fun seek(lo: Int, hi: Int, value: Int, onChange: (Int) -> Unit): SeekBar {
        val bar = SeekBar(this)
        bar.max = hi - lo
        bar.progress = (value - lo).coerceIn(0, hi - lo)
        bar.progressTintList = ColorStateList.valueOf(cAccent)
        bar.thumbTintList = ColorStateList.valueOf(cAccent)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, fromUser: Boolean) { onChange(lo + v) }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        return bar
    }


    private fun settingsTab(): View {
        val p = pageBox()

        val c1 = card()
        c1.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(icon(R.drawable.ic_keyboard, 22, cCyan))
            addView(tv("Klaviatura o'lchami", 17f, cInk, true).apply { setPadding(dp(10), 0, 0, 0) })
        })
        c1.addView(gap(10))
        val scale = prefs().getInt("scale", 100).coerceIn(80, 140)
        val sizeLabel = tv("Balandligi: $scale%", 14f, cMuted)
        c1.addView(sizeLabel)
        c1.addView(seek(80, 140, scale) { v -> sizeLabel.text = "Balandligi: $v%"; prefs().edit().putInt("scale", v).apply() })
        c1.addView(gap(10))
        val fs = prefs().getInt("fscale", 100).coerceIn(85, 130)
        val fontLabel = tv("Harflar o'lchami: $fs%", 14f, cMuted)
        c1.addView(fontLabel)
        c1.addView(seek(85, 130, fs) { v -> fontLabel.text = "Harflar o'lchami: $v%"; prefs().edit().putInt("fscale", v).apply() })
        p.addView(c1)

        p.addView(section("Yozish"))
        p.addView(switchRow(R.drawable.ic_vibrate, "Vibratsiya", "haptic", true, "Tugma bosilganda tebranish"))
        p.addView(switchRow(R.drawable.ic_volume, "Tovush", "sound", true, "Tugma bosilganda tovush (telefon sozlamasiga bo'ysunadi)"))
        p.addView(switchRow(R.drawable.ic_keyboard, "Bir qo'lda yozish rejimi", "onehand", false))
        p.addView(switchRow(R.drawable.ic_bulb, "So'z takliflari", "predict", true, "3 ta taklif, o'zingiz yozganlarni o'rganadi"))
        p.addView(switchRow(R.drawable.ic_sparkle, "Avto-tuzatish", "autocorrect", true, "Faqat ishonchli xatolar, ⌫ bekor qiladi"))
        p.addView(switchRow(R.drawable.ic_keyboard, "Smart Touch", "smarttouch", false, "Tugma chetiga bosilganda aniqlash"))
        p.addView(switchRow(R.drawable.ic_sparkle, "Smart Reply", "smartreply", true, "Nusxalangan xabarga qisqa javob taklifi"))
        p.addView(tv("Takliflar va o'rganilgan so'zlar faqat shu telefonda saqlanadi, serverga yuborilmaydi. Parol, email va havola maydonlarida o'rganilmaydi.", 12f, cMuted)
            .apply { setPadding(dp(4), dp(2), dp(4), dp(10)) })
        p.addView(outlineBtn("📖 Shaxsiy lug'atni ko'rish / so'z o'chirish") { showUserWords() })
        p.addView(gap(8))
        p.addView(outlineBtn("O'rganilgan so'zlarni o'chirish") { WordModel.clearUser(this); toast("O'rganilgan so'zlar o'chirildi") })
        p.addView(gap(10))

        p.addView(section("Clipboard"))
        p.addView(switchRow(R.drawable.ic_clipboard, "Bufer tarixini saqlash", "cliphist", true, "Nusxalangan matnlar klaviaturadagi 📋 tugmasida saqlanadi"))
        p.addView(switchRow(R.drawable.ic_shield, "Parol/OTP/karta raqamini saqlamaslik", "clipsafe", true))
        p.addView(outlineBtn("Tarixni tozalash") { ClipStore.clearHistory(this); toast("Tarix tozalandi (Sevimlilar qoldi)") })
        p.addView(gap(10))

        p.addView(section("Ko'rinish va til"))
        p.addView(navRow(R.drawable.ic_moon, "Mavzu", "Qora / Oq tema, rang uslubi") { screen = "themes"; showMain() })
        p.addView(navRow(R.drawable.ic_globe, "Til / Layout", currentLangName()) { screen = "lang"; showMain() })
        p.addView(navRow(R.drawable.ic_clipboard, "Clipboard", "Saqlangan matnlar") { screen = "bufer"; showMain() })

        val c2b = card()
        c2b.addView(tv("⚡ Iboralar · ✍️ Matnni tuzatish · 🌍 Tarjima", 16f, cInk, true))
        c2b.addView(gap(4))
        c2b.addView(tv("⚡ Iboralar: klaviaturadagi ⚙ → «Iboralar». «Mening» bo'limiga o'z matningizni saqlaysiz.\n" +
            "✍️ Matnni tuzatish: ⚙ → «Matnni tuzatish». Avval natija ko'rsatiladi, siz tasdiqlaysiz. Internet kerak emas.\n" +
            (if (Translator.available) "🌍 Tarjima yoqilgan: matn sizning serveringizga yuboriladi."
            else "🌍 Tarjima hozir O'CHIQ: server ulanmagan (ilova internetga chiqmaydi)."), 13f, cMuted))
        p.addView(gap(10))
        p.addView(c2b)
        p.addView(privacyCard())
        return p
    }
}
