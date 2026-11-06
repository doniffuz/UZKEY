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
import android.widget.TextView
import android.widget.Toast
import java.io.FileOutputStream

/**
 * ÖZKEY — sozlash ilovasi. Dizayn: o'zbek naqshlari (feruza, lojuvard ko'k, oltin) + zamonaviy yumaloq kartalar.
 * Dasturchi: Donyor.
 */
class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var pickerShown = false
    private var tab = 0
    private var onboardPage = 0
    private var topInset = 0
    private var bottomInset = 0
    private var stepsHolder: LinearLayout? = null

    private lateinit var root: FrameLayout
    private val bufer by lazy { BuferTab(this) }

    // O'zbekona ranglar: feruza (Samarqand gumbazlari), lojuvard, oltin, paxta-krem
    internal val cTurq = Color.parseColor("#0E7C86")
    internal val cDeep = Color.parseColor("#0B3C5D")
    internal val cGold = Color.parseColor("#D9A93E")
    internal val cGoldDark = Color.parseColor("#9A7418")
    internal val cCream = Color.parseColor("#F3F6F7")
    internal val cInk = Color.parseColor("#14232B")
    internal val cMuted = Color.parseColor("#6F7C84")
    internal val cLine = Color.parseColor("#E3E9EC")
    internal val cOk = Color.parseColor("#1E9E6A")

    private fun prefs() = getSharedPreferences("ozkey", Context.MODE_PRIVATE)
    internal fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun imm() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    private fun isEnabled() = imm().enabledInputMethodList.any { it.packageName == packageName }
    private fun isSelected(): Boolean {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return false
        return id.startsWith("$packageName/")
    }
    internal fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    // ---------- kichik yordamchilar ----------

    internal fun tv(t: String, size: Float = 15f, color: Int = cInk, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    internal fun gap(h: Int = 12) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }

    internal fun rounded(color: Int, r: Int = 16) = GradientDrawable().apply { setColor(color); cornerRadius = dp(r).toFloat() }

    internal fun primaryBtn(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label; textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(cTurq, cDeep))
            .apply { cornerRadius = dp(28).toFloat() }
        elevation = dp(3).toFloat()
        setPadding(dp(20), dp(15), dp(20), dp(15))
        setOnClickListener { onClick() }
    }

    internal fun outlineBtn(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label; textSize = 14f; setTextColor(cTurq); gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        background = rounded(Color.argb(20, 14, 124, 134), 28)
        setPadding(dp(16), dp(12), dp(16), dp(12))
        setOnClickListener { onClick() }
    }

    internal fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(Color.WHITE, 24)
        elevation = dp(2).toFloat()
        setPadding(dp(18), dp(16), dp(18), dp(18))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(14) }
    }

    internal fun section(t: String) = tv(t.uppercase(), 12f, cGoldDark, true).apply {
        letterSpacing = 0.12f
        setPadding(dp(4), dp(10), 0, dp(6))
    }

    internal fun field(hintText: String) = EditText(this).apply {
        hint = hintText; setHintTextColor(cMuted); setTextColor(cInk); textSize = 15f
        background = rounded(cCream, 16)
        setPadding(dp(16), dp(13), dp(16), dp(13))
    }

    private fun logo(size: Int) = ImageView(this).apply {
        setImageResource(R.drawable.logo_app)
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
    }

    private fun header(big: Boolean): View {
        val box = LinearLayout(this).apply {
            orientation = if (big) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = if (big) Gravity.CENTER_HORIZONTAL else Gravity.CENTER_VERTICAL
            setPadding(dp(20), topInset + dp(if (big) 40 else 14), dp(20), dp(if (big) 34 else 18))
            background = HeaderDrawable(cTurq, cDeep, cGold, dp(40).toFloat(), dp(if (big) 36 else 26).toFloat(), dp(1).toFloat())
        }
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (big) Gravity.CENTER_HORIZONTAL else Gravity.START
        }
        texts.addView(tv("ÖZKEY", if (big) 40f else 23f, Color.WHITE, true).apply { letterSpacing = 0.04f })
        texts.addView(tv("O'zbek klaviaturasi", if (big) 15f else 12f, Color.argb(220, 255, 255, 255)))
        texts.addView(tv("Dasturchi: Donyor", if (big) 14f else 12f, cGold, true).apply { setPadding(0, dp(2), 0, 0) })
        if (big) {
            box.addView(logo(120))
            box.addView(gap(14))
            box.addView(texts)
        } else {
            box.addView(logo(46))
            box.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(12) })
            box.addView(tv("v" + BuildConfig.VERSION_NAME, 11f, Color.argb(190, 255, 255, 255)))
        }
        return box
    }

    // ---------- hayot sikli ----------

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = cTurq
            window.navigationBarColor = Color.WHITE
        }
        if (Build.VERSION.SDK_INT >= 26) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        root = FrameLayout(this).apply { setBackgroundColor(cCream) }
        setContentView(root)
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
    }

    private fun rebuild() {
        if (prefs().getBoolean("onboarded", false)) showMain() else showOnboarding()
    }

    override fun onResume() {
        super.onResume()
        stepsHolder?.let { fillSteps(it, auto = true) }
    }

    // ---------- 2 qadamli ulash ----------

    private fun stepRow(n: Int, label: String, done: Boolean): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, dp(5))
        }
        row.addView(TextView(this).apply {
            text = if (done) "✓" else n.toString()
            textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if (done) cOk else cTurq) }
        }, LinearLayout.LayoutParams(dp(28), dp(28)))
        row.addView(tv(label, 15f, if (done) cMuted else cInk).apply { setPadding(dp(12), 0, 0, 0) })
        return row
    }

    private fun fillSteps(h: LinearLayout, auto: Boolean) {
        h.removeAllViews()
        val en = isEnabled()
        val sel = en && isSelected()
        h.addView(stepRow(1, "ÖZKEY ni yoqing", en))
        h.addView(stepRow(2, "Klaviaturani tanlang", sel))
        h.addView(gap(10))
        when {
            !en -> h.addView(primaryBtn("Sozlamalarni ochish") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) })
            !sel -> h.addView(primaryBtn("Klaviaturani tanlash") { imm().showInputMethodPicker() })
            else -> h.addView(tv("✅ Hammasi tayyor! Istalgan ilovada yozishni boshlang.", 14f, cOk, true))
        }
        if (!en || sel) pickerShown = false
        if (en && !sel && auto && !pickerShown) {
            pickerShown = true
            handler.postDelayed({ if (!isFinishing) imm().showInputMethodPicker() }, 500)
        }
    }

    private fun setupCard(auto: Boolean): LinearLayout {
        val c = card()
        c.addView(tv("Ulash — 2 qadam", 17f, cInk, true))
        c.addView(gap(6))
        val holder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        c.addView(holder)
        stepsHolder = holder
        fillSteps(holder, auto)
        return c
    }

    // ---------- KIRISH EKRANI ----------

    private fun showOnboarding() {
        root.removeAllViews()
        stepsHolder = null
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(header(big = true))
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
                    background = rounded(Color.WHITE, 18)
                    elevation = dp(1).toFloat()
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    addView(tv(e, 22f).apply { setPadding(0, 0, dp(14), 0) })
                    addView(tv(t, 15f, cInk))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(8) }
                })
            }
        } else {
            body.addView(tv("Ulash — 2 qadam", 24f, cInk, true))
            body.addView(gap(8))
            val holder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            body.addView(holder)
            stepsHolder = holder
            fillSteps(holder, auto = false)
            body.addView(gap(12))
            body.addView(tv("Android «klaviatura ma'lumot yig'ishi mumkin» deb ogohlantiradi — bu tizimning standart xabari. " +
                "ÖZKEY yozganingizni serverga yubormaydi.", 12f, cMuted))
        }
        col.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        // nuqtalar
        col.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            for (i in 0..1) addView(View(this@MainActivity).apply {
                background = rounded(if (i == onboardPage) cTurq else cLine, 8)
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

    private fun showMain() {
        root.removeAllViews()
        stepsHolder = null
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(header(big = false))
        val page = when (tab) { 0 -> homeTab(); 1 -> themesTab(); 2 -> bufer.build(); else -> settingsTab() }
        col.addView(ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(page)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        col.addView(navBar())
        root.addView(col)
    }

    private fun navBar(): View {
        val bar = LinearLayout(this).apply {
            background = rounded(Color.WHITE, 32)
            elevation = dp(8).toFloat()
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val tabs = listOf("🏠" to "Bosh", "🎨" to "Mavzular", "📋" to "Bufer", "⚙️" to "Sozlama")
        tabs.forEachIndexed { i, (icon, name) ->
            val on = i == tab
            bar.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(8))
                background = if (on) GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(cTurq, cDeep))
                    .apply { cornerRadius = dp(26).toFloat() } else null
                addView(tv(icon, 18f).apply { alpha = if (on) 1f else 0.6f })
                addView(tv(name, 11f, if (on) Color.WHITE else cMuted, on))
                setOnClickListener { tab = i; showMain() }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        return FrameLayout(this).apply {
            setPadding(dp(16), dp(6), dp(16), dp(10) + bottomInset)
            addView(bar)
        }
    }

    internal fun pageBox() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24))
    }

    // ---------- BOSH ----------

    private fun homeTab(): View {
        val p = pageBox()
        p.addView(setupCard(auto = true))

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
        val out = tv("", 16f, cTurq, true).apply { setTextIsSelectable(true); setPadding(dp(2), dp(10), dp(2), dp(10)) }
        c3.addView(input)
        c3.addView(gap(8))
        fun convBtn(label: String, f: (String) -> String) = outlineBtn(label) { out.text = f(input.text.toString()) }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(6) }
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
        p.addView(supportCard())
        return p
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
        c.addView(primaryBtn("📤 Do'stlarga ulashish") {
            val link = if (BuildConfig.STORE_URL.isNotEmpty()) BuildConfig.STORE_URL else "https://t.me/Doniku_Admin"
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "ÖZKEY — o'zbek klaviaturasi: so'z takliflari, bufer, Lotin⇄Kirill, 30+ mavzu.\n$link")
            }, "Ulashish"))
        })
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
        c.addView(tv("ÖZKEY Keyboard 🇺🇿", 20f, cInk, true))
        c.addView(tv("O‘zbekcha yozuv — yangi daraja.", 14f, cTurq, true).apply { setPadding(0, dp(2), 0, dp(8)) })
        c.addView(tv("ÖZKEY — o‘zbek tilida qulay, zamonaviy va tez yozish uchun yaratilgan Android klaviatura. " +
            "Ilova yangi o‘zbek alifbosini qo‘llab-quvvatlaydi va yozishni yanada qulay qilishga qaratilgan.", 13f, cMuted))
        c.addView(gap(12))

        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this).apply {
            text = "D"; textSize = 22f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(cGold, cGoldDark))
                .apply { shape = GradientDrawable.OVAL }
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0) }
        col.addView(tv("👨‍💻 DASTURCHI", 11f, cGoldDark, true).apply { letterSpacing = 0.12f })
        col.addView(tv("Donyor", 20f, cInk, true))
        row.addView(col)
        c.addView(row)
        c.addView(gap(8))
        c.addView(tv("ÖZKEY — foydalanuvchilar uchun foydali va qulay bo‘ladigan mahsulotlar yaratish maqsadida ishlab chiqilmoqda. " +
            "Sizda taklif, xato yoki yangi g‘oya bo‘lsa, bizga yozishingiz mumkin.", 13f, cMuted))
        c.addView(gap(10))
        c.addView(primaryBtn("📩 Telegram: @Doniku_Admin") { openTelegram() })
        c.addView(gap(4))
        c.addView(tv("Sizning fikringiz ÖZKEY'ni yanada yaxshilashga yordam beradi.", 12f, cMuted).apply { setPadding(0, dp(6), 0, 0) })
        c.addView(gap(10))
        c.addView(tv("🚀 Kelajakdagi maqsad", 14f, cInk, true))
        c.addView(tv("ÖZKEY'ni yanada qulay, tezkor va foydali qilish hamda o‘zbek tilidagi raqamli mahsulotlarni ko‘paytirish.", 13f, cMuted))
        c.addView(gap(8))
        c.addView(tv("ÖZKEY — O‘zbekcha yozuv, yangi avlod!", 13f, cTurq, true))
        return c
    }

    // ---------- MAVZULAR ----------

    private fun themesTab(): View {
        val p = pageBox()
        val tryCard = card()
        tryCard.addView(tv("Mavzuni shu yerda sinang", 15f, cInk, true))
        tryCard.addView(gap(8))
        tryCard.addView(field("Bosing — klaviatura ochiladi"))
        p.addView(tryCard)

        p.addView(photoCard())

        val current = prefs().getString("theme", Themes.DEFAULT_ID)
        Themes.groups.forEach { g ->
            p.addView(section(g))
            Themes.byGroup(g).chunked(3).forEach { triple ->
                val row = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
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
            background = rounded(Color.WHITE, 18).apply {
                if (selected) setStroke(dp(2), cTurq)
            }
            elevation = dp(if (selected) 4 else 1).toFloat()
            setPadding(dp(6), dp(6), dp(6), dp(6))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(dp(4), dp(4), dp(4), dp(4)) }
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
        box.addView(tv(t.name + if (selected) " ✓" else "", 12f, if (selected) cTurq else cInk, selected).apply {
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
            background = rounded(cCream, 12).apply { setStroke(dp(1), cLine) }
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

    // ---------- TILLAR (Sozlama ichida) ----------

    private fun languagesCard(): View {
        val c = card()
        c.addView(tv("🌐 Tillar", 17f, cInk, true))
        c.addView(tv("Klaviaturadagi 🌐 tugmasi tillarni almashtiradi.", 12f, cMuted))
        c.addView(gap(4))
        Languages.builtins.forEach { c.addView(tv("✓  " + it.name, 15f, cInk).apply { setPadding(0, dp(6), 0, 0) }) }
        val inst = Languages.installed(this).map { it.code }.toSet()
        val extra = Languages.bundled(this)
        if (extra.isNotEmpty()) {
            c.addView(gap(10))
            c.addView(tv("Qo'shimcha tillar", 14f, cGoldDark, true))
            extra.forEach { l ->
                val has = l.code in inst
                c.addView(LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(2))
                    addView(tv(l.name, 15f, cInk), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    addView(outlineBtn(if (has) "O'chirish" else "Yuklash") {
                        if (has) Languages.remove(this@MainActivity, l.code)
                        else Languages.installBundled(this@MainActivity, l.code)
                        showMain()
                    })
                })
            }
        }
        return c
    }

    // ---------- SOZLAMA ----------

    private fun seek(lo: Int, hi: Int, value: Int, onChange: (Int) -> Unit): SeekBar {
        val bar = SeekBar(this)
        bar.max = hi - lo
        bar.progress = (value - lo).coerceIn(0, hi - lo)
        bar.progressTintList = ColorStateList.valueOf(cTurq)
        bar.thumbTintList = ColorStateList.valueOf(cTurq)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, fromUser: Boolean) { onChange(lo + v) }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        return bar
    }

    private fun toggle(label: String, key: String, default: Boolean) = Switch(this).apply {
        text = label; textSize = 15f; setTextColor(cInk)
        isChecked = prefs().getBoolean(key, default)
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        thumbTintList = ColorStateList(states, intArrayOf(cTurq, Color.parseColor("#B8BEC2")))
        trackTintList = ColorStateList(states, intArrayOf(Color.argb(110, 14, 124, 134), Color.parseColor("#DADFE2")))
        setPadding(0, dp(8), 0, dp(8))
        setOnCheckedChangeListener { _, v -> prefs().edit().putBoolean(key, v).apply() }
    }

    private fun settingsTab(): View {
        val p = pageBox()

        val c1 = card()
        c1.addView(tv("⌨️ Klaviatura", 17f, cInk, true))
        c1.addView(gap(8))
        val scale = prefs().getInt("scale", 100).coerceIn(80, 140)
        val sizeLabel = tv("Balandligi: $scale%", 14f, cMuted)
        c1.addView(sizeLabel)
        c1.addView(seek(80, 140, scale) { v -> sizeLabel.text = "Balandligi: $v%"; prefs().edit().putInt("scale", v).apply() })
        c1.addView(gap(10))
        val fs = prefs().getInt("fscale", 100).coerceIn(85, 130)
        val fontLabel = tv("Harflar o'lchami: $fs%", 14f, cMuted)
        c1.addView(fontLabel)
        c1.addView(seek(85, 130, fs) { v -> fontLabel.text = "Harflar o'lchami: $v%"; prefs().edit().putInt("fscale", v).apply() })
        c1.addView(gap(6))
        c1.addView(toggle("Tugma bosilganda tebranish", "haptic", true))
        c1.addView(toggle("Bir qo'lda yozish rejimi", "onehand", false))
        c1.addView(toggle("So'z takliflari (3 ta, o'zingiz yozganlarni o'rganadi)", "predict", true))
        c1.addView(toggle("Avto-tuzatish (faqat ishonchli xatolar, ⌫ bekor qiladi)", "autocorrect", true))
        c1.addView(toggle("Smart Touch (tugma chetiga bosilganda aniqlash)", "smarttouch", false))
        c1.addView(tv("Takliflar va o'rganilgan so'zlar faqat shu telefonda saqlanadi, serverga yuborilmaydi. Parol, email va havola maydonlarida o'rganilmaydi.", 12f, cMuted))
        c1.addView(outlineBtn("O'rganilgan so'zlarni o'chirish") { WordModel.clearUser(this); toast("O'rganilgan so'zlar o'chirildi") })
        p.addView(c1)

        val c2 = card()
        c2.addView(tv("📋 Bufer tarixi", 17f, cInk, true))
        c2.addView(tv("Nusxalagan matnlaringiz klaviaturadagi 📋 tugmasida saqlanadi. Yangi nusxa tepa panelda taklif bo'lib chiqadi. Ma'lumot faqat shu telefonda turadi.", 13f, cMuted))
        c2.addView(toggle("Bufer tarixini saqlash", "cliphist", true))
        c2.addView(toggle("Parol/OTP/karta raqamini saqlamaslik", "clipsafe", true))
        c2.addView(outlineBtn("Tarixni tozalash") { ClipStore.clearHistory(this); toast("Tarix tozalandi (Sevimlilar qoldi)") })
        p.addView(c2)

        p.addView(languagesCard())

        val c3 = card()
        c3.addView(tv("🔒 Maxfiylik", 17f, cInk, true))
        c3.addView(tv("ÖZKEY internetga chiqmaydi: ilovada INTERNET ruxsati yo'q. Yozganingiz, o'rganilgan so'zlar va bufer faqat telefoningizda qoladi. " +
            "Parol, email va havola maydonlarida avto-almashtirish va o'rganish o'chirilgan.", 13f, cMuted))
        p.addView(c3)

        p.addView(developerCard())
        p.addView(tv("ÖZKEY ${BuildConfig.VERSION_NAME} · Donyor", 12f, cMuted).apply { gravity = Gravity.CENTER })
        return p
    }
}
