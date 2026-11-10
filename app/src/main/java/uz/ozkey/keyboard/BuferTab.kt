package uz.ozkey.keyboard

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Ilovadagi «Bufer» bo'limi: qidiruv, Tarix | Sevimlilar | Mahkamlangan, kategoriyalar,
 * to'liq matn oynasi (Nusxa / Tahrirlash / Sevimli / Ulashish / O'chirish), Eksport / Import.
 * Ma'lumot [ClipStore] da, faqat shu telefonda.
 */
class BuferTab(private val a: MainActivity) {

    private companion object {
        const val REQ_EXPORT = 7101
        const val REQ_IMPORT = 7102
        const val LIST_LIMIT = 100
        const val MIN_PW = 6
        const val MAX_BACKUP = 8 * 1024 * 1024
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
    }

    private var tab = ClipStore.TAB_HISTORY
    private var cat: String? = null
    private var query = ""

    private var tabsBox: LinearLayout? = null
    private var catBox: LinearLayout? = null
    private var listBox: LinearLayout? = null
    private var statView: TextView? = null

    private val handler = Handler(Looper.getMainLooper())
    private val searchRun = Runnable { refreshList() }
    private var exportPw: String? = null

    private fun dp(v: Int) = a.dp(v)

    // ---------- ekran ----------

    fun build(): View {
        val p = a.pageBox()

        val head = LinearLayout(a).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(a.tv("📋 Bufer", 20f, a.cInk, true), LinearLayout.LayoutParams(0, WRAP, 1f))
        head.addView(a.tv("⋮", 26f, a.cInk, true).apply {
            setPadding(dp(16), dp(2), dp(4), dp(2))
            setOnClickListener { showMenu() }
        })
        p.addView(head)
        p.addView(a.gap(8))

        val search = a.field("🔍 Qidirish…").apply {
            setSingleLine()
            setText(query)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, af: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                    query = s?.toString().orEmpty()
                    handler.removeCallbacks(searchRun)
                    handler.postDelayed(searchRun, 150)
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        p.addView(search)
        p.addView(a.gap(10))

        tabsBox = LinearLayout(a)
        p.addView(tabsBox)
        p.addView(a.gap(8))

        catBox = LinearLayout(a)
        p.addView(HorizontalScrollView(a).apply {
            isHorizontalScrollBarEnabled = false
            addView(catBox)
        })
        p.addView(a.gap(12))

        listBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        p.addView(listBox)

        statView = a.tv("", 12f, a.cMuted).apply { setPadding(dp(4), dp(4), dp(4), 0) }
        p.addView(statView)

        refreshTabs()
        refreshCats()
        refreshList()
        return p
    }

    private fun chip(label: String, on: Boolean, onClick: () -> Unit): TextView = TextView(a).apply {
        text = label
        textSize = 13.5f
        gravity = Gravity.CENTER
        maxLines = 1
        setTextColor(if (on) Color.WHITE else a.cAccent)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        background = a.rounded(if (on) a.cAccent else a.cSurface, 14).apply { setStroke(dp(1), a.cAccent) }
        setPadding(dp(12), dp(9), dp(12), dp(9))
        setOnClickListener { onClick() }
    }

    private fun refreshTabs() {
        val box = tabsBox ?: return
        box.removeAllViews()
        listOf("Tarix", "⭐ Sevimlilar", "📌 Mahkamlangan").forEachIndexed { i, name ->
            box.addView(
                chip(name, tab == i) { tab = i; refreshTabs(); refreshList() },
                LinearLayout.LayoutParams(0, WRAP, if (i == 0) 0.9f else 1.3f).apply { if (i > 0) leftMargin = dp(6) }
            )
        }
    }

    private fun refreshCats() {
        val box = catBox ?: return
        box.removeAllViews()
        fun add(label: String, on: Boolean, delName: String? = null, click: () -> Unit) {
            val v = chip(label, on, click)
            if (delName != null) v.setOnLongClickListener { confirmDeleteCat(delName); true }
            box.addView(v, LinearLayout.LayoutParams(WRAP, WRAP).apply { rightMargin = dp(6) })
        }
        add("Hammasi", cat == null) { cat = null; refreshCats(); refreshList() }
        ClipStore.categories(a).forEach { name ->
            val custom = name !in ClipStore.defaultCats
            add(ClipStore.catLabel(name), cat == name, if (custom) name else null) {
                cat = if (cat == name) null else name
                refreshCats(); refreshList()
            }
        }
        add("＋ Yangi", false) { newCategory() }
    }

    private fun emptyText(): String = when {
        query.isNotBlank() -> "Hech narsa topilmadi."
        tab == ClipStore.TAB_FAV -> "Sevimli matn yo'q.\n«Tarix» dagi matnning ☆ tugmasini bosing — u hech qachon avtomatik o'chmaydi."
        tab == ClipStore.TAB_PIN -> "Mahkamlangan matn yo'q.\n📍 ni bosing — matn doim ro'yxat tepasida turadi."
        cat != null -> "Bu kategoriyada matn yo'q.\nMatnni oching va «🏷 Kategoriya» dan tanlang."
        else -> "Hali hech narsa nusxalanmagan.\nBiror joydan matnni nusxalang — shu yerda saqlanadi."
    }

    private fun refreshList() {
        val box = listBox ?: return
        box.removeAllViews()
        val items = ClipStore.list(a, tab, cat, query, LIST_LIMIT)
        if (items.isEmpty()) {
            box.addView(a.tv(emptyText(), 14f, a.cMuted).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(28), dp(12), dp(28))
            })
        } else {
            items.forEach { box.addView(itemCard(it)) }
            if (items.size >= LIST_LIMIT) {
                box.addView(a.tv("Eng yangi $LIST_LIMIT ta ko'rsatildi. Qolganini qidiruv orqali toping.", 12f, a.cMuted))
            }
        }
        val (n, chars) = ClipStore.stats(a)
        statView?.text = "Xotira: $n ta matn, taxminan ${maxOf(1L, chars * 2 / 1024)} KB. " +
            "Tarix oxirgi ${ClipStore.MAX_HISTORY} ta oddiy nusxa bilan cheklanadi; Sevimli, Mahkamlangan va kategoriyadagi matnlar cheklanmaydi."
    }

    private fun metaText(item: ClipStore.Item): String {
        var m = ClipStore.ago(item.ts)
        if (item.cat.isNotEmpty()) m += " · " + ClipStore.catLabel(item.cat)
        m += " · ${item.len} belgi"
        return m
    }

    // ---------- kartochka ----------

    private fun itemCard(item: ClipStore.Item): View {
        val c = a.card()
        c.setOnClickListener { showFull(item.id) }
        c.addView(a.tv(item.preview.trim(), 15f, a.cInk).apply {
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
        })
        c.addView(a.tv(metaText(item), 11.5f, a.cMuted).apply { setPadding(0, dp(4), 0, dp(8)) })

        val row = LinearLayout(a)
        fun btn(label: String, w: Float, main: Boolean = false, onClick: () -> Unit) {
            row.addView(TextView(a).apply {
                text = label
                textSize = if (main) 14f else 17f
                gravity = Gravity.CENTER
                setTextColor(if (main) Color.WHITE else a.cInk)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                background = if (main) a.rounded(a.cAccent, 12) else a.rounded(a.cField, 12).apply { setStroke(dp(1), a.cLine) }
                setPadding(0, dp(9), 0, dp(9))
                setOnClickListener { onClick() }
            }, LinearLayout.LayoutParams(0, WRAP, w).apply { leftMargin = dp(4) })
        }
        btn("📋 Nusxa", 2.4f, true) { ClipStore.text(a, item.id)?.let { copy(it) } }
        btn("✏️", 1f) { showFull(item.id, startEditing = true) }
        btn(if (item.fav) "★" else "☆", 1f) { ClipStore.toggleFav(a, item.id); refreshList() }
        btn(if (item.pinned) "📌" else "📍", 1f) { ClipStore.togglePin(a, item.id); refreshList() }
        btn("🗑", 1f) { deleteItem(item) { refreshList() } }
        c.addView(row)
        return c
    }

    private fun copy(text: String) {
        try {
            (a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("ozkey", text))
            a.toast("Nusxalandi")
        } catch (e: Exception) { a.toast("Nusxalab bo'lmadi") }
    }

    private fun share(text: String) {
        val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
        try { a.startActivity(Intent.createChooser(i, "Ulashish")) } catch (e: Exception) { a.toast("Ulashib bo'lmadi") }
    }

    /** Oddiy tarix matni — darhol; Sevimli / Mahkamlangan / kategoriyadagi — tasdiqlab o'chiriladi. */
    private fun deleteItem(item: ClipStore.Item, done: () -> Unit) {
        if (item.saved) confirm("Bu matnni o'chirmoqchimisiz?") { ClipStore.remove(a, item.id); done() }
        else { ClipStore.remove(a, item.id); done() }
    }

    private fun confirm(msg: String, yes: String = "O'chirish", onYes: () -> Unit) {
        AlertDialog.Builder(a)
            .setMessage(msg)
            .setNegativeButton("Bekor qilish", null)
            .setPositiveButton(yes) { _, _ -> onYes() }
            .show()
    }

    // ---------- to'liq matn oynasi ----------

    private fun lp(w: Float) = LinearLayout.LayoutParams(0, WRAP, w).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }

    private fun showFull(id: Long, startEditing: Boolean = false) {
        val dlg = AlertDialog.Builder(a).create()
        val holder = FrameLayout(a)
        var editing = startEditing

        fun render() {
            val item = ClipStore.get(a, id)
            if (item == null) { dlg.dismiss(); return }
            val full = ClipStore.text(a, id) ?: ""
            holder.removeAllViews()
            val col = LinearLayout(a).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(16), dp(18), dp(10))
            }
            col.addView(a.tv("📄 To'liq matn", 17f, a.cInk, true))
            col.addView(a.tv(metaText(item), 12f, a.cMuted).apply { setPadding(0, dp(2), 0, dp(10)) })

            if (editing) {
                val et = a.field("Matn").apply {
                    setText(full)
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    gravity = Gravity.TOP
                    minLines = 4
                    maxLines = 10
                }
                col.addView(et, LinearLayout.LayoutParams(MATCH, WRAP))
                col.addView(a.gap(10))
                val row = LinearLayout(a)
                row.addView(a.outlineBtn("Bekor qilish") { editing = false; render() }, lp(1f))
                row.addView(a.primaryBtn("Saqlash") {
                    if (ClipStore.edit(a, id, et.text.toString())) { editing = false; render(); refreshList() }
                    else a.toast("Matn bo'sh yoki juda uzun (${ClipStore.MAX_LEN} belgigacha)")
                }, lp(1f))
                col.addView(row)
            } else {
                val body = TextView(a).apply {
                    text = full
                    textSize = 15f
                    setTextColor(a.cInk)
                    setTextIsSelectable(true)
                    setLineSpacing(0f, 1.1f)
                }
                col.addView(
                    ScrollView(a).apply { addView(body) },
                    LinearLayout.LayoutParams(MATCH, if (full.length > 350) dp(240) else WRAP)
                )
                col.addView(a.gap(10))
                col.addView(a.outlineBtn("🏷 Kategoriya: " + (if (item.cat.isEmpty()) "yo'q" else ClipStore.catLabel(item.cat))) {
                    pickCategory(id) { render(); refreshList() }
                })
                col.addView(a.gap(6))

                fun pair(l: TextView, r: TextView) = LinearLayout(a).apply { addView(l, lp(1f)); addView(r, lp(1f)) }
                col.addView(pair(
                    a.primaryBtn("📋 Nusxa") { copy(full) },
                    a.outlineBtn("✏️ Tahrirlash") { editing = true; render() }
                ))
                col.addView(pair(
                    a.outlineBtn(if (item.fav) "★ Sevimli" else "☆ Sevimli") { ClipStore.toggleFav(a, id); render(); refreshList() },
                    a.outlineBtn(if (item.pinned) "📌 Mahkam" else "📍 Mahkamlash") { ClipStore.togglePin(a, id); render(); refreshList() }
                ))
                col.addView(pair(
                    a.outlineBtn("📤 Ulashish") { share(full) },
                    a.outlineBtn("🗑 O'chirish") { deleteItem(item) { dlg.dismiss(); refreshList() } }
                ))
            }
            holder.addView(ScrollView(a).apply { addView(col) })
        }

        dlg.setView(holder)
        dlg.setOnDismissListener { refreshList(); refreshCats() }
        dlg.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        render()
        dlg.show()
    }

    // ---------- kategoriyalar ----------

    private fun pickCategory(id: Long, done: () -> Unit) {
        val names = ClipStore.categories(a)
        val labels = (listOf("— Kategoriyasiz") + names.map { ClipStore.catLabel(it) }).toTypedArray()
        AlertDialog.Builder(a)
            .setTitle("Kategoriya")
            .setItems(labels) { _, w ->
                ClipStore.setCat(a, id, if (w == 0) "" else names[w - 1])
                done()
            }
            .show()
    }

    private fun newCategory() {
        val et = EditText(a).apply {
            hint = "Kategoriya nomi"
            setSingleLine()
            filters = arrayOf(InputFilter.LengthFilter(24))
        }
        val box = FrameLayout(a).apply { setPadding(dp(20), dp(8), dp(20), 0); addView(et) }
        AlertDialog.Builder(a)
            .setTitle("Yangi kategoriya")
            .setView(box)
            .setNegativeButton("Bekor qilish", null)
            .setPositiveButton("Qo'shish") { _, _ ->
                if (ClipStore.addCategory(a, et.text.toString())) refreshCats()
                else a.toast("Nom bo'sh yoki bunday kategoriya bor")
            }
            .show()
    }

    private fun confirmDeleteCat(name: String) {
        confirm("«$name» kategoriyasi o'chiriladi. Ichidagi matnlar o'chmaydi, kategoriyasiz qoladi.") {
            ClipStore.deleteCategory(a, name)
            if (cat == name) cat = null
            refreshCats(); refreshList()
        }
    }

    // ---------- ⋮ menyu ----------

    private fun showMenu() {
        val items = arrayOf("📤 Eksport (zaxira fayl)", "📥 Import (zaxiradan)", "🧹 Tarixni tozalash", "⚠️ Hammasini o'chirish")
        AlertDialog.Builder(a).setItems(items) { _, w ->
            when (w) {
                0 -> startExport()
                1 -> startImport()
                2 -> confirm("Oddiy tarix tozalansin? Sevimli, Mahkamlangan va kategoriyadagi matnlar qoladi.", "Tozalash") {
                    ClipStore.clearHistory(a); refreshList()
                }
                else -> confirm("HAMMA matn (Sevimlilar ham) o'chiriladi. Davom etasizmi?") {
                    ClipStore.clearAll(a); refreshList()
                }
            }
        }.show()
    }

    // ---------- zaxira (Eksport / Import) ----------

    private fun askPassword(title: String, msg: String, ok: String, onPw: (String) -> Unit) {
        val et = EditText(a).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Parol"
            setSingleLine()
        }
        val box = FrameLayout(a).apply { setPadding(dp(20), dp(8), dp(20), 0); addView(et) }
        AlertDialog.Builder(a)
            .setTitle(title)
            .setMessage(msg)
            .setView(box)
            .setNegativeButton("Bekor qilish", null)
            .setPositiveButton(ok) { _, _ -> onPw(et.text.toString()) }
            .show()
    }

    private fun startExport() {
        val n = try { ClipStore.exportSaved(a).getJSONArray("items").length() } catch (e: Exception) { 0 }
        if (n == 0) {
            a.toast("Zaxiralash uchun ⭐ Sevimli, 📌 Mahkamlangan yoki kategoriyadagi matn yo'q")
            return
        }
        askPassword(
            "📤 Zaxira (Eksport)",
            "Faylga faqat ⭐ Sevimli, 📌 Mahkamlangan va kategoriyadagi matnlar kiradi ($n ta). " +
                "Fayl siz yozgan parol bilan shifrlanadi (AES-256) — parolsiz uni hech kim o'qiy olmaydi.\n\n" +
                "Parolni unutsangiz faylni qayta ochib bo'lmaydi: ÖZKEY parolni hech qayerda saqlamaydi. " +
                "Parol kamida $MIN_PW belgi.",
            "Davom etish"
        ) { pw ->
            if (pw.length < MIN_PW) { a.toast("Parol juda qisqa (kamida $MIN_PW belgi)"); return@askPassword }
            exportPw = pw
            val i = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, "ozkey-bufer.ozkb")
            }
            try {
                @Suppress("DEPRECATION")
                a.startActivityForResult(i, REQ_EXPORT)
            } catch (e: Exception) { exportPw = null; a.toast("Fayl tanlash oynasi topilmadi") }
        }
    }

    private fun startImport() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        try {
            @Suppress("DEPRECATION")
            a.startActivityForResult(i, REQ_IMPORT)
        } catch (e: Exception) { a.toast("Fayl tanlash oynasi topilmadi") }
    }

    private fun readLimited(uri: android.net.Uri): ByteArray? = try {
        a.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val tmp = ByteArray(8192)
            var total = 0
            var ok = true
            while (true) {
                val n = input.read(tmp)
                if (n < 0) break
                total += n
                if (total > MAX_BACKUP) { ok = false; break }
                out.write(tmp, 0, n)
            }
            if (ok) out.toByteArray() else null
        }
    } catch (e: Exception) { null }

    /** MainActivity.onActivityResult dan chaqiriladi. Bizniki bo'lsa — true. */
    fun onResult(req: Int, res: Int, data: Intent?): Boolean {
        when (req) {
            REQ_EXPORT -> {
                val pw = exportPw
                exportPw = null
                val uri = data?.data
                if (res != Activity.RESULT_OK || uri == null || pw == null) return true
                a.toast("Zaxira tayyorlanmoqda… (bir necha soniya ketishi mumkin)")
                Thread {
                    var ok = false
                    try {
                        val bytes = ClipBackup.encrypt(ClipStore.exportSaved(a).toString(), pw)
                        a.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                        ok = true
                    } catch (e: Exception) { /* yozilmadi */ }
                    a.runOnUiThread { if (!a.isFinishing) a.toast(if (ok) "Zaxira saqlandi ✓" else "Zaxirani yozib bo'lmadi") }
                }.start()
                return true
            }
            REQ_IMPORT -> {
                val uri = data?.data
                if (res != Activity.RESULT_OK || uri == null) return true
                val bytes = readLimited(uri)
                if (bytes == null) { a.toast("Faylni o'qib bo'lmadi yoki juda katta"); return true }
                askPassword("📥 Import", "Zaxira faylini shifrlashda yozgan parolingizni kiriting.", "Ochish") { pw ->
                    a.toast("Tekshirilmoqda… (bir necha soniya ketishi mumkin)")
                    Thread {
                        val json = ClipBackup.decrypt(bytes, pw)
                        a.runOnUiThread {
                            if (a.isFinishing) return@runOnUiThread
                            if (json == null) {
                                a.toast("Parol noto'g'ri yoki bu ÖZKEY zaxirasi emas")
                            } else {
                                val n = try { ClipStore.importSaved(a, JSONObject(json)) } catch (e: Exception) { -1 }
                                a.toast(if (n >= 0) "$n ta yangi matn qo'shildi" else "Zaxira buzilgan")
                                refreshCats(); refreshList()
                            }
                        }
                    }.start()
                }
                return true
            }
        }
        return false
    }
}
