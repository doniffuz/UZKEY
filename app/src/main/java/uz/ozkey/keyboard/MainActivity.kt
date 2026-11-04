package uz.ozkey.keyboard

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var enableLaunched = false
    private var pickerShown = false
    private var status: TextView? = null
    private var tab = 0
    private var onboardPage = 0
    private var logoTaps = 0

    private lateinit var root: FrameLayout

    private val bg = Color.rgb(12, 10, 34)
    private val card = Color.rgb(26, 22, 64)
    private val cyan = Color.rgb(0, 229, 255)
    private val purple = Color.rgb(124, 77, 255)
    private val muted = Color.rgb(170, 175, 205)

    private fun prefs() = getSharedPreferences("ozkey", Context.MODE_PRIVATE)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun imm() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    private fun isEnabled() = imm().enabledInputMethodList.any { it.packageName == packageName }
    private fun isSelected(): Boolean {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return false
        return id.startsWith("$packageName/")
    }

    // ---------- kichik yordamchilar ----------

    private fun tv(t: String, size: Float = 15f, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    private fun gap(h: Int = 12) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }

    private fun rounded(color: Int, r: Int = 16) = GradientDrawable().apply { setColor(color); cornerRadius = dp(r).toFloat() }

    private fun primary(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(purple, Color.rgb(0, 160, 255)))
            .apply { cornerRadius = dp(16).toFloat() }
        setPadding(dp(16), dp(14), dp(16), dp(14))
        setOnClickListener { onClick() }
    }

    private fun secondary(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label; textSize = 15f; setTextColor(cyan); gravity = Gravity.CENTER
        background = rounded(card, 16).apply { setStroke(dp(1), cyan) }
        setPadding(dp(16), dp(12), dp(16), dp(12))
        setOnClickListener { onClick() }
    }

    private fun cardBox(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(card)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) }
    }

    private fun logo(size: Int) = ImageView(this).apply {
        setImageResource(R.drawable.logo_app)
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
    }

    // ---------- hayot sikli ----------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this).apply { setBackgroundColor(bg) }
        setContentView(root)
        if (prefs().getBoolean("onboarded", false)) showMain() else showOnboarding()
    }

    override fun onResume() {
        super.onResume()
        if (prefs().getBoolean("onboarded", false) && tab == 0) refreshStatus(autoLaunch = true)
        if (!prefs().getBoolean("onboarded", false) && onboardPage == 2) refreshStatus(autoLaunch = false)
    }

    // ---------- KIRISH EKRANI ----------

    private fun showOnboarding() {
        root.removeAllViews()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(48), dp(24), dp(28))
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
        }
        when (onboardPage) {
            0 -> {
                body.addView(logo(140))
                body.addView(gap(20))
                body.addView(tv("ÖZKEY", 38f, bold = true))
                body.addView(tv("PREMIUM O'ZBEK KLAVIATURASI", 12f, cyan).apply { letterSpacing = 0.16f })
                body.addView(gap(24))
                body.addView(tv("Lotin va kirill alifbosida tez, qulay va chiroyli yozing.", 18f).apply { gravity = Gravity.CENTER })
                body.addView(gap(16))
                body.addView(tv("Ğ ğ    Ö ö    Ş ş    Ç ç    Ў ў    Қ қ    Ғ ғ    Ҳ ҳ", 20f, cyan).apply { gravity = Gravity.CENTER })
            }
            1 -> {
                body.addView(tv("Nimalar bor?", 28f, bold = true))
                body.addView(gap(16))
                listOf(
                    "🔤" to "Lotin ⇄ Kirill — ikkala yozuv bir klaviaturada",
                    "🌍" to "Tarjima rejimi — rus, ingliz → o'zbekcha (lotin yoki kirill)",
                    "🎨" to "Neon RGB va premium mavzular",
                    "😊" to "Emoji bo'limlari: yuzlar, qo'llar, yuraklar, hayvonlar…",
                    "📥" to "Qo'shimcha tillarni keyin yuklab olish"
                ).forEach { (e, t) ->
                    body.addView(LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, dp(8), 0, dp(8))
                        addView(tv(e, 26f).apply { setPadding(0, 0, dp(14), 0) })
                        addView(tv(t, 16f))
                    })
                }
            }
            else -> {
                body.addView(tv("Ulash — 2 qadam", 28f, bold = true))
                body.addView(gap(16))
                status = tv("", 15f, Color.rgb(120, 220, 160))
                body.addView(status)
                body.addView(gap(12))
                body.addView(primary("1. ÖZKEY ni yoqish") { openEnableSettings() })
                body.addView(gap(10))
                body.addView(primary("2. Klaviaturani tanlash") { imm().showInputMethodPicker() })
                body.addView(gap(14))
                body.addView(tv("Android «klaviatura ma'lumot yig'ishi mumkin» deb ogohlantiradi — bu tizimning standart xabari. " +
                    "ÖZKEY yozganingizni serverga yubormaydi (faqat siz «→» tarjima tugmasini bosganda matn tarjimaga ketadi).", 12f, muted))
            }
        }
        col.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        // nuqtalar
        col.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            for (i in 0..2) addView(View(this@MainActivity).apply {
                background = rounded(if (i == onboardPage) cyan else Color.rgb(70, 66, 110), 8)
                layoutParams = LinearLayout.LayoutParams(dp(if (i == onboardPage) 22 else 8), dp(8)).apply {
                    leftMargin = dp(4); rightMargin = dp(4)
                }
            })
        })
        col.addView(gap(16))
        col.addView(primary(if (onboardPage < 2) "Davom etish" else "Boshlash") {
            if (onboardPage < 2) { onboardPage++; showOnboarding() }
            else { prefs().edit().putBoolean("onboarded", true).apply(); showMain() }
        })
        if (onboardPage < 2) {
            col.addView(gap(6))
            col.addView(tv("O'tkazib yuborish", 14f, muted).apply {
                gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(4))
                setOnClickListener { onboardPage = 2; showOnboarding() }
            })
        }
        root.addView(col)
        if (onboardPage == 2) refreshStatus(autoLaunch = false)
    }

    // ---------- ASOSIY EKRAN ----------

    private fun showMain() {
        root.removeAllViews()
        status = null
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        // sarlavha
        col.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(36), dp(16), dp(8))
            addView(logo(40).apply {
                // 5 marta bosish — sinov uchun premium'ni yoqish/o'chirish (nashr oldidan olib tashlang)
                setOnClickListener {
                    if (++logoTaps >= 5) {
                        logoTaps = 0
                        Premium.set(this@MainActivity, !Premium.isPremium(this@MainActivity))
                        toast("Premium (sinov): " + if (Premium.isPremium(this@MainActivity)) "YOQILDI" else "o'chirildi")
                        showMain()
                    }
                }
            })
            addView(tv("ÖZKEY", 22f, bold = true).apply { setPadding(dp(10), 0, 0, 0) })
            addView(View(this@MainActivity), LinearLayout.LayoutParams(0, 1, 1f))
            if (Premium.isPremium(this@MainActivity)) addView(tv("★ PREMIUM", 12f, Color.rgb(255, 205, 80), true))
        })
        val content = FrameLayout(this)
        col.addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        // pastki tablar
        val bar = LinearLayout(this).apply {
            setBackgroundColor(Color.rgb(20, 16, 52)); setPadding(0, dp(8), 0, dp(12))
        }
        val tabs = listOf("🏠" to "Bosh", "🎨" to "Mavzular", "🌍" to "Tillar", "⚙️" to "Sozlama")
        tabs.forEachIndexed { i, (icon, name) ->
            bar.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                addView(tv(icon, 20f).apply { alpha = if (i == tab) 1f else 0.5f })
                addView(tv(name, 11f, if (i == tab) cyan else muted))
                setOnClickListener { tab = i; showMain() }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        col.addView(bar)
        val page = when (tab) { 0 -> homeTab(); 1 -> themesTab(); 2 -> langsTab(); else -> settingsTab() }
        content.addView(ScrollView(this).apply { addView(page) })
        root.addView(col)
        if (tab == 0) refreshStatus(autoLaunch = true)
    }

    private fun pageBox() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(24))
    }

    private fun homeTab(): View {
        val p = pageBox()
        val c1 = cardBox()
        status = tv("", 15f, Color.rgb(120, 220, 160))
        c1.addView(status)
        c1.addView(gap(10))
        c1.addView(primary("1. ÖZKEY ni yoqish") { openEnableSettings() })
        c1.addView(gap(8))
        c1.addView(secondary("2. Klaviaturani tanlash") { imm().showInputMethodPicker() })
        p.addView(c1)

        val c2 = cardBox()
        c2.addView(tv("Sinab ko'ring", 17f, bold = true))
        c2.addView(gap(6))
        c2.addView(EditText(this).apply {
            hint = "Shu yerga yozing…"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
        })
        p.addView(c2)

        // Konvertor
        val c3 = cardBox()
        c3.addView(tv("Lotin ⇄ Kirill konvertor", 17f, bold = true))
        val input = EditText(this).apply {
            hint = "Matnni kiriting yoki joylang"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
        }
        val out = tv("", 16f, cyan).apply { setTextIsSelectable(true); setPadding(0, dp(8), 0, dp(8)) }
        c3.addView(input)
        fun convBtn(label: String, f: (String) -> String) = secondary(label) { out.text = f(input.text.toString()) }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(6) }
        }
        c3.addView(LinearLayout(this).apply {
            addView(convBtn("→ Lotin") { Script.cyrToLat(it).let { l -> Alphabet.toStandard(l) } })
            addView(convBtn("→ ÖZ") { Alphabet.toNew(Script.cyrToLat(it)) })
            addView(convBtn("→ Кирилл") { Script.latToCyr(it) })
        })
        c3.addView(out)
        c3.addView(secondary("Nusxa olish") {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("ozkey", out.text.toString())); toast("Nusxalandi")
        })
        p.addView(c3)
        return p
    }

    private fun themesTab(): View {
        val p = pageBox()
        val premium = Premium.isPremium(this)
        if (!premium) {
            val b = cardBox()
            b.addView(tv("★ Premium mavzular", 17f, Color.rgb(255, 205, 80), true))
            b.addView(tv("Barcha rangli va animatsion mavzularni oching.", 13f, muted))
            b.addView(gap(8))
            b.addView(primary("Premium haqida") { premiumDialog() })
            p.addView(b)
        }
        val current = prefs().getString("theme", "neon")
        Themes.all.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
            pair.forEach { t -> row.addView(themeCard(t, t.id == current, premium)) }
            if (pair.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            p.addView(row)
        }
        p.addView(gap(8))
        p.addView(tv("Tanlangan mavzu klaviaturada darhol o'zgaradi.", 12f, muted))
        return p
    }

    private fun themeCard(t: KeyTheme, selected: Boolean, premium: Boolean): View {
        val locked = t.premium && !premium
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(card, 14).apply { if (selected) setStroke(dp(2), cyan) }
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(dp(4), dp(4), dp(4), dp(4)) }
        }
        val preview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(t.bgTop, t.bgBottom))
                .apply { cornerRadius = dp(10).toFloat() }
            setPadding(dp(6), dp(6), dp(6), dp(6))
            alpha = if (locked) 0.6f else 1f
        }
        repeat(3) { r ->
            preview.addView(LinearLayout(this).apply {
                repeat(7 - r) { i ->
                    addView(View(this@MainActivity).apply {
                        background = GradientDrawable().apply {
                            setColor(t.key); cornerRadius = dp(3).toFloat()
                            if (t.stroke != 0) setStroke(dp(1), if (t.rainbow) Color.HSVToColor(floatArrayOf((i * 50f + r * 30f) % 360f, .85f, 1f)) else t.stroke)
                        }
                        layoutParams = LinearLayout.LayoutParams(0, dp(16), 1f).apply { setMargins(dp(1), dp(2), dp(1), dp(2)) }
                    })
                }
            })
        }
        box.addView(preview)
        box.addView(tv(t.name + if (locked) "  🔒" else if (selected) "  ✓" else "", 13f, Color.WHITE).apply { setPadding(dp(2), dp(6), 0, 0) })
        box.setOnClickListener {
            if (locked) premiumDialog()
            else { prefs().edit().putString("theme", t.id).apply(); toast("«${t.name}» tanlandi"); showMain() }
        }
        return box
    }

    private fun langsTab(): View {
        val p = pageBox()
        val b = cardBox()
        b.addView(tv("Asosiy tillar", 17f, bold = true))
        Languages.builtins.forEach { b.addView(tv("✓  " + it.name, 15f, Color.WHITE).apply { setPadding(0, dp(6), 0, 0) }) }
        b.addView(gap(4))
        b.addView(tv("Klaviaturadagi 🌐 tugmasi tillarni almashtiradi.", 12f, muted))
        p.addView(b)

        val inst = Languages.installed(this).map { it.code }.toSet()
        val c = cardBox()
        c.addView(tv("Qo'shimcha tillar", 17f, bold = true))
        Languages.bundled(this).forEach { l ->
            val has = l.code in inst
            c.addView(LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(8))
                addView(tv(l.name, 15f), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(secondary(if (has) "O'chirish" else "Yuklash") {
                    if (has) Languages.remove(this@MainActivity, l.code)
                    else Languages.installBundled(this@MainActivity, l.code)
                    showMain()
                })
            })
        }
        p.addView(c)

        if (BuildConfig.PACKS_URL.isNotBlank()) {
            val r = cardBox()
            r.addView(tv("Serverdagi tillar", 17f, bold = true))
            val hold = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            hold.addView(tv("Yuklanmoqda…", 13f, muted))
            r.addView(hold)
            p.addView(r)
            Thread {
                val list = try { Languages.fetchRemoteIndex() } catch (e: Exception) { null }
                runOnUiThread {
                    hold.removeAllViews()
                    if (list == null) hold.addView(tv("Ro'yxatni olib bo'lmadi. Internetni tekshiring.", 13f, muted))
                    else list.filter { it.first.uppercase() !in inst }.forEach { (code, name, url) ->
                        hold.addView(LinearLayout(this).apply {
                            gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(8))
                            addView(tv(name, 15f), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                            addView(secondary("Yuklash") {
                                Thread {
                                    val ok = try { Languages.installRemote(this@MainActivity, code, url) } catch (e: Exception) { false }
                                    runOnUiThread { if (ok) showMain() else toast("Yuklab bo'lmadi") }
                                }.start()
                            })
                        })
                    }
                }
            }.start()
        }
        return p
    }

    private fun settingsTab(): View {
        val p = pageBox()
        val c1 = cardBox()
        val scale = prefs().getInt("scale", 100).coerceIn(80, 140)
        val sizeLabel = tv("Klaviatura balandligi: $scale%", 14f, muted)
        c1.addView(sizeLabel)
        c1.addView(SeekBar(this).apply {
            max = 60; progress = scale - 80
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, fromUser: Boolean) {
                    sizeLabel.text = "Klaviatura balandligi: ${80 + v}%"
                    prefs().edit().putInt("scale", 80 + v).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        c1.addView(CheckBox(this).apply {
            text = "Tugma bosilganda tebranish"; textSize = 15f; setTextColor(Color.WHITE)
            isChecked = prefs().getBoolean("haptic", true)
            setOnCheckedChangeListener { _, v -> prefs().edit().putBoolean("haptic", v).apply() }
        })
        p.addView(c1)

        val c2 = cardBox()
        c2.addView(tv("Tarjima", 17f, bold = true))
        c2.addView(tv("Klaviaturadagi «→» tugmasi belgilangan matnni (belgilanmasa — butun maydonni) tarjima qiladi. " +
            "Maqsad tilini 🌍 tugmasi almashtiradi.", 13f, muted))
        if (BuildConfig.TRANSLATE_URL.isBlank()) {
            c2.addView(gap(8))
            val key = EditText(this).apply {
                hint = "Claude API kaliti (ixtiyoriy, ilg'or)"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setText(prefs().getString("apikey", ""))
            }
            c2.addView(key)
            c2.addView(secondary("Kalitni saqlash") {
                prefs().edit().putString("apikey", key.text.toString().trim()).apply(); toast("Saqlandi")
            })
        } else c2.addView(tv("Tarjima ÖZKEY serveri orqali ishlaydi — hech narsa kiritish shart emas.", 13f, cyan))
        p.addView(c2)

        val c3 = cardBox()
        c3.addView(tv("Maxfiylik", 17f, bold = true))
        c3.addView(tv("Yozganingiz serverga yuborilmaydi. Faqat tarjima tugmasi bosilganda tanlangan matn tarjima xizmatiga ketadi. " +
            "Parol, email va havola maydonlarida avto-almashtirish va tarjima o'chirilgan.", 13f, muted))
        c3.addView(gap(6))
        c3.addView(tv("ÖZKEY ${BuildConfig.VERSION_NAME}", 12f, muted))
        p.addView(c3)
        return p
    }

    // ---------- umumiy ----------

    private fun premiumDialog() {
        AlertDialog.Builder(this)
            .setTitle("★ Premium")
            .setMessage("Barcha premium mavzular, animatsion RGB va yangi imkoniyatlar. To'lov tizimi (Google Play) tez orada ulanadi.")
            .setPositiveButton("Tushunarli", null).show()
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun openEnableSettings() = startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))

    /** Yoqilmagan bo'lsa sozlamalarni o'zi ochadi; qaytganda tanlash oynasini ko'rsatadi. */
    private fun refreshStatus(autoLaunch: Boolean) {
        val s = status ?: return
        when {
            !isEnabled() -> {
                pickerShown = false
                s.text = "⚠ ÖZKEY yoqilmagan. Sozlamalarda ÖZKEY ni yoqing va ortga qayting."
                if (autoLaunch && !enableLaunched) {
                    enableLaunched = true
                    handler.postDelayed({ openEnableSettings() }, 600)
                }
            }
            !isSelected() -> {
                enableLaunched = true
                s.text = "Deyarli tayyor: ochilgan oynada ÖZKEY ni tanlang."
                if (autoLaunch && !pickerShown) {
                    pickerShown = true
                    handler.postDelayed({ imm().showInputMethodPicker() }, 500)
                }
            }
            else -> {
                enableLaunched = false; pickerShown = false
                s.text = "✅ ÖZKEY yoqilgan va tanlangan. Yozishni boshlang!"
            }
        }
    }
}
