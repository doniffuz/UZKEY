package uz.ozkey.keyboard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bufer tarixi: nusxalangan matnlar qurilmada saqlanadi (hech qayerga yuborilmaydi).
 * Qadalgan (pinned) elementlar tozalashda o'chmaydi va ro'yxat tepasida turadi.
 */
object ClipStore {
    class Item(val text: String, val pinned: Boolean)

    private const val FILE = "ozkey_clips"
    private const val KEY = "items"
    private const val MAX_UNPINNED = 30
    private const val MAX_LEN = 3000

    private fun p(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun read(c: Context): MutableList<Item> {
        val out = mutableListOf<Item>()
        try {
            val arr = JSONArray(p(c).getString(KEY, "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(Item(o.getString("t"), o.optBoolean("p", false)))
            }
        } catch (e: Exception) { /* buzilgan ma'lumot — bo'sh ro'yxat */ }
        return out
    }

    private fun write(c: Context, list: List<Item>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("t", it.text).put("p", it.pinned)) }
        p(c).edit().putString(KEY, arr.toString()).apply()
    }

    /** Qadalganlar birinchi, keyin eng yangilari. */
    fun list(c: Context): List<Item> {
        val all = read(c)
        return all.filter { it.pinned } + all.filter { !it.pinned }
    }

    fun add(c: Context, raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || text.length > MAX_LEN) return
        val all = read(c)
        val old = all.firstOrNull { it.text == text }
        all.removeAll { it.text == text }
        all.add(0, Item(text, old?.pinned ?: false))
        // eski qadalmaganlarni kesib tashlash
        var n = 0
        val trimmed = all.filter { it.pinned || (++n <= MAX_UNPINNED) }
        write(c, trimmed)
    }

    fun togglePin(c: Context, text: String) {
        write(c, read(c).map { if (it.text == text) Item(it.text, !it.pinned) else it })
    }

    fun remove(c: Context, text: String) {
        write(c, read(c).filter { it.text != text })
    }

    fun clearUnpinned(c: Context) {
        write(c, read(c).filter { it.pinned })
    }

    fun clearAll(c: Context) {
        write(c, emptyList())
    }
}
