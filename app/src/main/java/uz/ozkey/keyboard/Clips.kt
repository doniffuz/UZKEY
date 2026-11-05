package uz.ozkey.keyboard

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Bufer: nusxalangan matnlar faqat shu telefonda (SQLite) saqlanadi, hech qayerga yuborilmaydi.
 *
 *  - Tarix: oxirgi MAX_HISTORY ta oddiy nusxa (eskilari avtomatik tozalanadi).
 *  - Sevimli (⭐), Mahkamlangan (📌) va kategoriyaga solingan matnlar avtomatik O'CHMAYDI.
 *  - Ro'yxatlar faqat qisqa ko'rinish (preview) oladi — uzun matn xotirani to'ldirmaydi,
 *    to'liq matn kerak bo'lganda alohida olinadi ([text]).
 */
object ClipStore {

    class Item(
        val id: Long,
        val preview: String,
        val len: Int,
        val ts: Long,
        val pinned: Boolean,
        val fav: Boolean,
        val cat: String
    ) {
        /** Avtomatik tozalashda o'chmaydigan matn. */
        val saved: Boolean get() = pinned || fav || cat.isNotEmpty()
        /** Preview to'liq matndan qisqa bo'lsa — true. */
        val cut: Boolean get() = len > preview.length
    }

    const val TAB_HISTORY = 0
    const val TAB_FAV = 1
    const val TAB_PIN = 2

    /** Bitta matn uzunligi chegarasi (belgi). */
    const val MAX_LEN = 20_000
    /** Tarixda saqlanadigan oddiy (saqlanmagan) matnlar soni. */
    const val MAX_HISTORY = 300
    private const val PREVIEW = 240
    private const val DB_NAME = "ozkey_clips.db"
    private const val OLD_PREFS = "ozkey_clips"
    private const val OLD_KEY = "items"

    // ---------- kategoriyalar ----------

    val defaultCats = listOf("Shaxsiy", "Ish", "Ijtimoiy tarmoqlar", "Tayyor matnlar", "Boshqa")

    fun catIcon(name: String) = when (name) {
        "Shaxsiy" -> "👤"
        "Ish" -> "💼"
        "Ijtimoiy tarmoqlar" -> "📱"
        "Tayyor matnlar" -> "📝"
        "Boshqa" -> "📌"
        else -> "🏷"
    }

    fun catLabel(name: String) = catIcon(name) + " " + name

    // ---------- baza ----------

    private class Db(private val ctx: Context) : SQLiteOpenHelper(ctx, ClipStore.DB_NAME, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE clips (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, " +
                    "low TEXT NOT NULL, h INTEGER NOT NULL, len INTEGER NOT NULL, ts INTEGER NOT NULL, " +
                    "pinned INTEGER NOT NULL DEFAULT 0, fav INTEGER NOT NULL DEFAULT 0, cat TEXT NOT NULL DEFAULT '')"
            )
            db.execSQL("CREATE INDEX clips_h ON clips(h)")
            db.execSQL("CREATE INDEX clips_ts ON clips(ts)")
            db.execSQL("CREATE TABLE cats (name TEXT PRIMARY KEY, pos INTEGER NOT NULL)")
            ClipStore.migrateOld(ctx, db)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    @Volatile
    private var helper: Db? = null

    private fun db(c: Context): SQLiteDatabase {
        val h = helper ?: synchronized(this) {
            helper ?: Db(c.applicationContext).also { helper = it }
        }
        return h.writableDatabase
    }

    /** Eski versiya (SharedPreferences, 0.6.x) ma'lumotini bir marta ko'chiradi. Qadalganlar → Sevimli + Mahkamlangan. */
    private fun migrateOld(c: Context, db: SQLiteDatabase) {
        try {
            val sp = c.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE)
            val raw = sp.getString(OLD_KEY, null) ?: return
            val arr = JSONArray(raw)
            val now = System.currentTimeMillis()
            for (i in arr.length() - 1 downTo 0) {   // eskidan yangiga: tartib saqlansin
                val o = arr.getJSONObject(i)
                val pinned = o.optBoolean("p", false)
                insertRow(db, o.getString("t"), now - i * 1000L, pinned, pinned, "")
            }
            sp.edit().remove(OLD_KEY).apply()
        } catch (e: Exception) { /* buzilgan eski ma'lumot — o'tkazib yuboramiz */ }
    }

    private fun insertRow(db: SQLiteDatabase, text: String, ts: Long, pinned: Boolean, fav: Boolean, cat: String): Long {
        val v = ContentValues().apply {
            put("text", text)
            put("low", text.lowercase(Locale.ROOT))
            put("h", text.hashCode())
            put("len", text.length)
            put("ts", ts)
            put("pinned", if (pinned) 1 else 0)
            put("fav", if (fav) 1 else 0)
            put("cat", cat)
        }
        return db.insert("clips", null, v)
    }

    private fun like(q: String): String {
        val esc = q.trim().lowercase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return "%$esc%"
    }

    // ---------- maxfiy ko'rinishdagi matn ----------

    /**
     * OTP kod (4–8 ta raqam) yoki bank karta raqami (Luhn tekshiruvi) ko'rinishidagi matn.
     * Tekshiruv faqat shu telefonda bajariladi, hech narsa yuborilmaydi.
     */
    fun looksSensitive(raw: String): Boolean {
        val t = raw.trim()
        if (t.length < 4 || t.length > 24) return false
        if (t.length <= 8 && t.all { it.isDigit() }) return true
        if (t.any { !(it.isDigit() || it == ' ' || it == '-') }) return false
        val digits = t.filter { it.isDigit() }
        if (digits.length !in 13..19) return false
        var sum = 0
        var alt = false
        for (i in digits.length - 1 downTo 0) {
            var n = digits[i] - '0'
            if (alt) { n *= 2; if (n > 9) n -= 9 }
            sum += n
            alt = !alt
        }
        return sum % 10 == 0
    }

    // ---------- o'qish ----------

    /**
     * Ro'yxat (faqat preview). Mahkamlanganlar har doim tepada, keyin eng yangilari.
     * @param cat null — hamma kategoriya, aks holda aynan shu kategoriyadagilar.
     */
    fun list(c: Context, tab: Int = TAB_HISTORY, cat: String? = null, query: String = "", limit: Int = 200): List<Item> {
        val where = StringBuilder("1=1")
        val args = mutableListOf<String>()
        when (tab) {
            TAB_FAV -> where.append(" AND fav=1")
            TAB_PIN -> where.append(" AND pinned=1")
        }
        if (cat != null) { where.append(" AND cat=?"); args.add(cat) }
        if (query.isNotBlank()) { where.append(" AND low LIKE ? ESCAPE '\\'"); args.add(like(query)) }
        val sql = "SELECT id, substr(text,1,$PREVIEW), len, ts, pinned, fav, cat FROM clips WHERE $where " +
            "ORDER BY pinned DESC, ts DESC, id DESC LIMIT $limit"
        val out = ArrayList<Item>()
        try {
            db(c).rawQuery(sql, args.toTypedArray()).use { cur ->
                while (cur.moveToNext()) {
                    out.add(Item(cur.getLong(0), cur.getString(1) ?: "", cur.getInt(2), cur.getLong(3),
                        cur.getInt(4) == 1, cur.getInt(5) == 1, cur.getString(6) ?: ""))
                }
            }
        } catch (e: Exception) { /* baza o'qilmadi — bo'sh ro'yxat */ }
        return out
    }

    /** To'liq matn. */
    fun text(c: Context, id: Long): String? = try {
        db(c).rawQuery("SELECT text FROM clips WHERE id=?", arrayOf(id.toString())).use { cur ->
            if (cur.moveToFirst()) cur.getString(0) else null
        }
    } catch (e: Exception) { null }

    fun get(c: Context, id: Long): Item? = try {
        db(c).rawQuery(
            "SELECT id, substr(text,1,$PREVIEW), len, ts, pinned, fav, cat FROM clips WHERE id=?", arrayOf(id.toString())
        ).use { cur ->
            if (cur.moveToFirst()) Item(cur.getLong(0), cur.getString(1) ?: "", cur.getInt(2), cur.getLong(3),
                cur.getInt(4) == 1, cur.getInt(5) == 1, cur.getString(6) ?: "") else null
        }
    } catch (e: Exception) { null }

    /** (matnlar soni, jami belgilar) — xotira nazorati uchun. */
    fun stats(c: Context): Pair<Int, Long> = try {
        db(c).rawQuery("SELECT COUNT(*), COALESCE(SUM(len),0) FROM clips", null).use { cur ->
            if (cur.moveToFirst()) Pair(cur.getInt(0), cur.getLong(1)) else Pair(0, 0L)
        }
    } catch (e: Exception) { Pair(0, 0L) }

    // ---------- yozish ----------

    /** Yangi nusxa. Bir xil matn takrorlansa — yangilanib tepaga chiqadi (belgilari saqlanadi). */
    fun add(c: Context, raw: String): Boolean {
        val text = raw.trim()
        if (text.isEmpty() || text.length > MAX_LEN) return false
        return try {
            val d = db(c)
            val now = System.currentTimeMillis()
            var existing = -1L
            d.rawQuery("SELECT id, text FROM clips WHERE h=?", arrayOf(text.hashCode().toString())).use { cur ->
                while (cur.moveToNext()) {
                    if (cur.getString(1) == text) { existing = cur.getLong(0); break }
                }
            }
            if (existing >= 0) {
                d.execSQL("UPDATE clips SET ts=? WHERE id=?", arrayOf<Any>(now, existing))
            } else {
                insertRow(d, text, now, false, false, "")
                trim(d)
            }
            true
        } catch (e: Exception) { false }
    }

    /** Eski oddiy nusxalarni MAX_HISTORY gacha qisqartiradi. Saqlanganlarga tegmaydi. */
    private fun trim(d: SQLiteDatabase) {
        d.execSQL(
            "DELETE FROM clips WHERE fav=0 AND pinned=0 AND cat='' AND id NOT IN " +
                "(SELECT id FROM clips WHERE fav=0 AND pinned=0 AND cat='' ORDER BY ts DESC, id DESC LIMIT $MAX_HISTORY)"
        )
    }

    private fun flip(c: Context, id: Long, col: String) {
        try { db(c).execSQL("UPDATE clips SET $col = 1 - $col WHERE id=?", arrayOf<Any>(id)) } catch (e: Exception) { }
    }

    fun toggleFav(c: Context, id: Long) = flip(c, id, "fav")
    fun togglePin(c: Context, id: Long) = flip(c, id, "pinned")

    fun setCat(c: Context, id: Long, cat: String) {
        try { db(c).execSQL("UPDATE clips SET cat=? WHERE id=?", arrayOf<Any>(cat, id)) } catch (e: Exception) { }
    }

    /** Matnni tahrirlash. Bo'sh yoki juda uzun bo'lsa — false. */
    fun edit(c: Context, id: Long, raw: String): Boolean {
        val text = raw.trim()
        if (text.isEmpty() || text.length > MAX_LEN) return false
        return try {
            db(c).execSQL(
                "UPDATE clips SET text=?, low=?, h=?, len=? WHERE id=?",
                arrayOf<Any>(text, text.lowercase(Locale.ROOT), text.hashCode(), text.length, id)
            )
            true
        } catch (e: Exception) { false }
    }

    fun remove(c: Context, id: Long) {
        try { db(c).execSQL("DELETE FROM clips WHERE id=?", arrayOf<Any>(id)) } catch (e: Exception) { }
    }

    /** Faqat oddiy tarixni tozalaydi: Sevimli, Mahkamlangan va kategoriyadagilar qoladi. */
    fun clearHistory(c: Context) {
        try { db(c).execSQL("DELETE FROM clips WHERE fav=0 AND pinned=0 AND cat=''") } catch (e: Exception) { }
    }

    /** HAMMASINI o'chiradi (saqlanganlar ham). */
    fun clearAll(c: Context) {
        try { db(c).execSQL("DELETE FROM clips") } catch (e: Exception) { }
    }

    // ---------- kategoriyalar ----------

    fun categories(c: Context): List<String> {
        val custom = ArrayList<String>()
        try {
            db(c).rawQuery("SELECT name FROM cats ORDER BY pos, name", null).use { cur ->
                while (cur.moveToNext()) custom.add(cur.getString(0))
            }
        } catch (e: Exception) { }
        return defaultCats + custom.filter { it !in defaultCats }
    }

    fun addCategory(c: Context, raw: String): Boolean {
        val name = raw.trim().take(24)
        if (name.isEmpty() || name in categories(c)) return false
        return try {
            db(c).execSQL("INSERT INTO cats(name, pos) VALUES(?, (SELECT COALESCE(MAX(pos),0)+1 FROM cats))", arrayOf<Any>(name))
            true
        } catch (e: Exception) { false }
    }

    /** O'z kategoriyangizni o'chirish: ichidagi matnlar o'chmaydi, kategoriyasiz qoladi. */
    fun deleteCategory(c: Context, name: String) {
        if (name in defaultCats) return
        try {
            val d = db(c)
            d.execSQL("UPDATE clips SET cat='' WHERE cat=?", arrayOf<Any>(name))
            d.execSQL("DELETE FROM cats WHERE name=?", arrayOf<Any>(name))
        } catch (e: Exception) { }
    }

    // ---------- vaqt ----------

    fun ago(ts: Long): String {
        val s = (System.currentTimeMillis() - ts) / 1000
        return when {
            s < 60 -> "hozir"
            s < 3600 -> "${s / 60} daq oldin"
            s < 86400 -> "${s / 3600} soat oldin"
            s < 86400 * 7 -> "${s / 86400} kun oldin"
            else -> java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(java.util.Date(ts))
        }
    }

    // ---------- zaxira (Sevimli / Mahkamlangan / kategoriyadagilar) ----------

    /** Zaxiraga faqat SAQLANGAN matnlar kiradi (oddiy tarix emas). Shifrlash — [ClipBackup]. */
    fun exportSaved(c: Context): JSONObject {
        val items = JSONArray()
        try {
            db(c).rawQuery(
                "SELECT text, ts, pinned, fav, cat FROM clips WHERE fav=1 OR pinned=1 OR cat<>'' ORDER BY ts", null
            ).use { cur ->
                while (cur.moveToNext()) {
                    items.put(JSONObject().put("t", cur.getString(0)).put("ts", cur.getLong(1))
                        .put("p", cur.getInt(2) == 1).put("f", cur.getInt(3) == 1).put("c", cur.getString(4)))
                }
            }
        } catch (e: Exception) { }
        val custom = JSONArray()
        categories(c).filter { it !in defaultCats }.forEach { custom.put(it) }
        return JSONObject().put("v", 1).put("items", items).put("cats", custom)
    }

    /** Zaxirani qo'shadi (mavjud matnlar takrorlanmaydi). Qo'shilganlar sonini qaytaradi. */
    fun importSaved(c: Context, obj: JSONObject): Int {
        var n = 0
        try {
            val cats = obj.optJSONArray("cats")
            if (cats != null) for (i in 0 until cats.length()) addCategory(c, cats.getString(i))
            val items = obj.getJSONArray("items")
            val d = db(c)
            for (i in 0 until items.length()) {
                val o = items.getJSONObject(i)
                val text = o.getString("t").trim()
                if (text.isEmpty() || text.length > MAX_LEN) continue
                var existing = -1L
                d.rawQuery("SELECT id, text FROM clips WHERE h=?", arrayOf(text.hashCode().toString())).use { cur ->
                    while (cur.moveToNext()) if (cur.getString(1) == text) { existing = cur.getLong(0); break }
                }
                val pinned = o.optBoolean("p", false)
                val fav = o.optBoolean("f", false)
                val cat = o.optString("c", "")
                if (existing >= 0) {
                    d.execSQL(
                        "UPDATE clips SET pinned = MAX(pinned, ?), fav = MAX(fav, ?) WHERE id=?",
                        arrayOf<Any>(if (pinned) 1 else 0, if (fav) 1 else 0, existing)
                    )
                } else {
                    insertRow(d, text, o.optLong("ts", System.currentTimeMillis()), pinned, fav, cat)
                    n++
                }
            }
        } catch (e: Exception) { }
        return n
    }
}
