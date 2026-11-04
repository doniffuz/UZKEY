package uz.ozkey.keyboard

import android.app.Activity
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
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var enableLaunched = false
    private var pickerShown = false
    private lateinit var status: TextView

    private val navy = Color.rgb(14, 22, 38)
    private val blue = Color.rgb(45, 105, 220)

    private fun prefs() = getSharedPreferences("ozkey", Context.MODE_PRIVATE)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun imm() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    private fun isEnabled() = imm().enabledInputMethodList.any { it.packageName == packageName }

    private fun isSelected(): Boolean {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return false
        return id.startsWith("$packageName/")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(28), dp(16), dp(24))
            setBackgroundColor(navy)
        }
        fun text(t: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
            text = t; textSize = size; setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        fun button(label: String, onClick: () -> Unit) = Button(this).apply {
            text = label; isAllCaps = false; textSize = 16f; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { setColor(blue); cornerRadius = dp(14).toFloat() }
            setOnClickListener { onClick() }
        }
        fun space(h: Int = 12) = TextView(this).apply { height = dp(h) }

        page.addView(text("ÖZKEY", 34f, bold = true))
        page.addView(text("PREMIUM UZBEK KEYBOARD", 12f, Color.LTGRAY).apply { letterSpacing = 0.14f })
        page.addView(space())
        page.addView(text("Yangi o'zbek alifbosida qulay va chiroyli yozing.\n\nĞ ğ     Ö ö     Ş ş     Ç ç", 18f))
        page.addView(space())

        status = text("", 15f, Color.rgb(120, 220, 160))
        page.addView(status)
        page.addView(space(8))
        page.addView(button("1. O'ZKEY ni yoqish") { openEnableSettings() })
        page.addView(space(8))
        page.addView(button("2. Klaviaturani tanlash") { imm().showInputMethodPicker() })
        page.addView(space(20))

        // ---- Sozlamalar ----
        page.addView(text("Sozlamalar", 20f, bold = true))
        page.addView(space(8))

        val sizeLabel = text("", 14f, Color.LTGRAY)
        val scale = prefs().getInt("scale", 100).coerceIn(80, 140)
        sizeLabel.text = "Klaviatura balandligi: $scale%"
        page.addView(sizeLabel)
        page.addView(SeekBar(this).apply {
            max = 60
            progress = scale - 80
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    sizeLabel.text = "Klaviatura balandligi: ${80 + p}%"
                    prefs().edit().putInt("scale", 80 + p).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })

        page.addView(CheckBox(this).apply {
            text = "Neon (rangli) mavzu"
            textSize = 16f
            setTextColor(Color.WHITE)
            isChecked = prefs().getBoolean("neon", true)
            setOnCheckedChangeListener { _, c -> prefs().edit().putBoolean("neon", c).apply() }
        })
        page.addView(space())

        page.addView(text("Tarjima (rus / ingliz → o'zbek)", 16f, bold = true))
        page.addView(text(
            "Klaviaturadagi «→ UZ» tugmasi belgilangan matnni (belgilanmasa — butun maydonni) o'zbekchaga tarjima qiladi va yangi alifboga o'tkazadi. " +
                "Claude API kaliti kiritilsa tarjima eng aniq bo'ladi; kalitsiz bepul zaxira xizmat ishlaydi.", 13f, Color.LTGRAY))
        val keyField = EditText(this).apply {
            hint = "Claude API kaliti (ixtiyoriy)"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs().getString("apikey", ""))
        }
        page.addView(keyField)
        page.addView(button("Kalitni saqlash") {
            prefs().edit().putString("apikey", keyField.text.toString().trim()).apply()
            Toast.makeText(this, "Saqlandi", Toast.LENGTH_SHORT).show()
        })
        page.addView(space())

        page.addView(text("ÖZ ⇄ standart", 16f, bold = true))
        page.addView(text(
            "«ÖZ⇄std» tugmasi matnni yangi alifbodan (Ş Ç Ğ Ö) oddiy yozuvga (sh ch gʻ oʻ) va aksincha o'giradi. " +
                "«Nusxa» tugmasi esa oddiy klaviaturada ham to'g'ri ko'rinadigan variantni buferga nusxalaydi.", 13f, Color.LTGRAY))
        page.addView(space())

        page.addView(text("Sinab ko'ring:", 14f, Color.LTGRAY))
        page.addView(EditText(this).apply {
            hint = "Shu yerga yozing…"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
        })
        page.addView(space())
        page.addView(text(
            "Maxfiylik: yozganingiz serverga yuborilmaydi. Faqat «→ UZ» bosilganda matn tarjima xizmatiga ketadi. " +
                "Parol, email va havola maydonlarida avto-almashtirish va tarjima o'chirilgan.", 12f, Color.LTGRAY))

        setContentView(ScrollView(this).apply { setBackgroundColor(navy); addView(page) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun openEnableSettings() {
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    }

    /**
     * Ruxsat avtomatik: yoqilmagan bo'lsa sozlamalar o'zi ochiladi; qaytib kelganda
     * tanlanmagan bo'lsa tanlash oynasi o'zi chiqadi. Keyinroq ruxsat o'chib qolsa,
     * ilova ochilganda yana shu jarayon takrorlanadi.
     */
    private fun refresh() {
        when {
            !isEnabled() -> {
                pickerShown = false
                status.text = "⚠ O'ZKEY yoqilmagan. Ochilgan sozlamalarda O'ZKEY ni yoqing va ortga qayting."
                if (!enableLaunched) {
                    enableLaunched = true
                    handler.postDelayed({ openEnableSettings() }, 600)
                }
            }
            !isSelected() -> {
                enableLaunched = true
                status.text = "Deyarli tayyor: ochilgan oynada O'ZKEY ni tanlang."
                if (!pickerShown) {
                    pickerShown = true
                    handler.postDelayed({ imm().showInputMethodPicker() }, 500)
                }
            }
            else -> {
                enableLaunched = false
                pickerShown = false
                status.text = "✅ O'ZKEY yoqilgan va tanlangan. Yozishni boshlang!"
            }
        }
    }
}
