package uz.ozkey.keyboard

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class OZKeyInputMethod : InputMethodService() {
    private var caps = false
    private var newAlphabet = true
    private var language = "UZ"
    private lateinit var keyboard: LinearLayout

    private val rows = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("⇧","z","x","c","v","b","n","m","⌫"),
        listOf("?123","Ğ","Ö","Ş","Ç","🌐","space","↵")
    )

    override fun onCreateInputView(): View {
        keyboard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(5, 8, 5, 8)
            setBackgroundColor(Color.rgb(18, 24, 38))
        }
        addView(keyboard, rows)
        return keyboard
    }

    private fun addView(container: LinearLayout, keyRows: List<List<String>>) {
        container.removeAllViews()
        keyRows.forEach { row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.forEach { key ->
                val button = makeKey(key)
                val weight = when (key) {
                    "space" -> 3.2f
                    "⇧", "⌫", "↵", "?123", "🌐" -> 1.35f
                    else -> 1f
                }
                rowLayout.addView(button, LinearLayout.LayoutParams(0, dp(48), weight).apply {
                    setMargins(dp(2), dp(3), dp(2), dp(3))
                })
            }
            container.addView(rowLayout, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun makeKey(key: String): Button {
        val special = key in listOf("⇧", "⌫", "↵", "?123", "🌐", "space")
        return Button(this).apply {
            text = when (key) {
                "space" -> "O'ZKEY"
                else -> displayKey(key)
            }
            textSize = if (key.length > 1) 13f else 19f
            isAllCaps = false
            setTextColor(if (special) Color.WHITE else Color.rgb(25, 34, 52))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = GradientDrawable().apply {
                setColor(if (special) Color.rgb(55, 83, 135) else Color.rgb(238, 242, 250))
                cornerRadius = dp(10).toFloat()
            }
            minWidth = 0
            setPadding(0, 0, 0, 0)
            setOnClickListener { handleKey(key) }
        }
    }

    private fun displayKey(key: String): String {
        if (key.length != 1) return key
        return if (caps) key.uppercase() else key
    }

    private fun handleKey(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "⇧" -> { caps = !caps; refresh() }
            "⌫" -> {
                val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
                if (before.isNotEmpty()) {
                    val count = if (before.length >= 2 && before.last().isWhitespace()) 1 else 1
                    ic.deleteSurroundingText(count, 0)
                }
            }
            "↵" -> {
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                if (action != null && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(action)
                } else {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
            }
            "space" -> ic.commitText(" ", 1)
            "🌐" -> {
                language = when (language) { "UZ" -> "EN"; "EN" -> "RU"; else -> "UZ" }
                refresh()
            }
            "?123" -> {
                // Numeric/symbol layout will be added in the next iteration.
                ic.commitText("123", 1)
            }
            else -> {
                val value = if (key.length == 1 && caps) key.uppercase() else key
                ic.commitText(value, 1)
                if (newAlphabet) convertDigraph(value)
                if (caps) { caps = false; refresh() }
            }
        }
    }

    private fun convertDigraph(typed: String) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        if (before.length < 2) return

        val first = before[0].lowercaseChar()
        val second = before[1]
        val upper = before[0].isUpperCase()

        // Uzbek apostrophe variants commonly entered from different keyboards.
        val apostrophes = setOf("'", "’", "‘", "ʻ", "ʼ", "＇")
        val replacement = when {
            first == 's' && second.lowercaseChar() == 'h' ->
                if (upper) "Ş" else "ş"
            first == 'c' && second.lowercaseChar() == 'h' ->
                if (upper) "Ç" else "ç"
            first == 'g' && second.toString() in apostrophes ->
                if (upper) "Ğ" else "ğ"
            first == 'o' && second.toString() in apostrophes ->
                if (upper) "Ö" else "ö"
            else -> null
        }

        if (replacement != null) {
            ic.deleteSurroundingText(2, 0)
            ic.commitText(replacement, 1)
        }
    }

    private fun refresh() {
        if (::keyboard.isInitialized) addView(keyboard, rows)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
