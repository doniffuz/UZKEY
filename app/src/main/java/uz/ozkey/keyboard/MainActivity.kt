package uz.ozkey.keyboard

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navy = Color.rgb(14, 22, 38)
        val blue = Color.rgb(45, 105, 220)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 44, 28, 28)
            setBackgroundColor(navy)
        }

        val title = TextView(this).apply {
            text = "ÖZKEY"
            textSize = 34f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        page.addView(title)

        val subtitle = TextView(this).apply {
            text = "PREMIUM UZBEK KEYBOARD"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            letterSpacing = 0.14f
        }
        page.addView(subtitle)

        val description = TextView(this).apply {
            text = "\nYangi o'zbek alifbosida qulay va chiroyli yozing.\n\nĞ ğ     Ö ö     Ş ş     Ç ç\n\nBoshlash uchun O'ZKEY klaviaturasini yoqing, keyin uni standart klaviatura sifatida tanlang."
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 28, 0, 28)
        }
        page.addView(description)

        fun actionButton(label: String, callback: () -> Unit): Button {
            return Button(this).apply {
                text = label
                isAllCaps = false
                textSize = 16f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(blue)
                    cornerRadius = 28f
                }
                setOnClickListener { callback() }
            }
        }

        page.addView(actionButton("1. O'ZKEY ni yoqish") {
            startActivity(android.content.Intent("android.settings.INPUT_METHOD_SETTINGS"))
        })

        val gap = TextView(this).apply { text = "\n" }
        page.addView(gap)

        page.addView(actionButton("2. Klaviaturani tanlash") {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        })

        val note = TextView(this).apply {
            text = "\nMaxfiylik: ushbu boshlang'ich versiya matnlarni serverga yubormaydi. Parol va maxfiy maydonlarda Android tizimi boshqa klaviaturani tanlashi mumkin."
            textSize = 12f
            setTextColor(Color.LTGRAY)
        }
        page.addView(note)
        setContentView(page)
    }
}
