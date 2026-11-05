package uz.ozkey.keyboard

import android.content.Context
import java.util.TreeSet

/**
 * So'z takliflari, keyingi so'z bashorati va xavfsiz avto-tuzatish.
 * Hammasi QURILMADA: yozganlaringiz hech qayerga yuborilmaydi. Lug'at — assets/words_uz.txt va
 * assets/bigrams_uz.txt (standart lotin yozuvida; yuklanganda yangi alifboga o'tkaziladi).
 * O'rganilgan so'zlar faqat shu ilovaning shaxsiy xotirasida turadi.
 */
object WordModel {
    private const val PREFS = "ozkey_words"
    private const val MAX_USER = 3000
    private const val MAX_BI = 4000
    private const val ALPHA = "abcdefghijklmnopqrstuvwxyzşçğö"

    class Ctx(val prefix: String, val prev: String?, val sentenceStart: Boolean)

    private var ready = false
    private var appCtx: Context? = null
    private val dict = TreeSet<String>()
    private val base = HashMap<String, Int>()               // lug'atdagi tartib raqami (kichik = tez-tez)
    private val userCnt = HashMap<String, Int>()
    private val bi = HashMap<String, HashMap<String, Int>>()
    private var dirty = 0

    fun isWordChar(c: Char) = c.isLetter() || c == '\''

    @Synchronized
    fun init(c: Context) {
        if (ready) return
        appCtx = c.applicationContext
        try {
            c.assets.open("words_uz.txt").bufferedReader().useLines { seq ->
                var i = 0
                for (raw in seq) {
                    val w = Alphabet.toNew(raw.trim().lowercase())
                    if (w.isNotEmpty() && w !in base) { base[w] = i++; dict.add(w) }
                }
            }
        } catch (e: Exception) { /* lug'atsiz ham ishlayveradi */ }
        try {
            c.assets.open("bigrams_uz.txt").bufferedReader().useLines { seq ->
                for (raw in seq) {
                    val p = Alphabet.toNew(raw.trim().lowercase()).split(' ')
                    if (p.size == 2) bi.getOrPut(p[0]) { HashMap() }.let { it[p[1]] = (it[p[1]] ?: 0) + 3 }
                }
            }
        } catch (e: Exception) { }
        try {
            val sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            sp.getString("uw", "")?.lineSequence()?.forEach { line ->
                val t = line.split('\t')
                if (t.size == 2) {
                    val n = t[1].toIntOrNull() ?: return@forEach
                    userCnt[t[0]] = n
                    if (n >= 2) dict.add(t[0])
                }
            }
            sp.getString("ub", "")?.lineSequence()?.forEach { line ->
                val t = line.split('\t')
                if (t.size == 3) {
                    val n = t[2].toIntOrNull() ?: return@forEach
                    bi.getOrPut(t[0]) { HashMap() }[t[1]] = n
                }
            }
        } catch (e: Exception) { }
        ready = true
    }

    private fun baseScore(w: String): Int {
        val r = base[w] ?: return 0
        return 100 - r * 60 / base.size.coerceAtLeast(1)
    }

    private fun score(w: String, prev: String?): Int {
        var s = baseScore(w) + (userCnt[w] ?: 0) * 8
        if (prev != null) s += (bi[prev]?.get(w) ?: 0) * 12
        return s
    }

    fun isKnown(w: String) = w.lowercase() in dict
    fun isPrefix(p: String): Boolean {
        val q = p.lowercase()
        return dict.ceiling(q)?.startsWith(q) == true
    }

    /** Kursor oldidagi matndan: yozilayotgan so'z boshi, undan oldingi so'z, gap boshimi. */
    fun parse(before: String): Ctx {
        var i = before.length
        while (i > 0 && isWordChar(before[i - 1])) i--
        val prefix = before.substring(i)
        var j = i
        while (j > 0 && before[j - 1] == ' ') j--
        var k = j
        while (k > 0 && isWordChar(before[k - 1])) k--
        val prev = if (k < j && j < i) before.substring(k, j).lowercase() else null
        val start = prev == null && ((j == 0 && before.length < 80) || (j > 0 && before[j - 1] in ".!?\n"))
        return Ctx(prefix, prev, start)
    }

    /** Yozilayotgan so'zning davomi (yoki tuzatishi) uchun 3 tagacha taklif. */
    fun complete(prefix: String, prev: String?, n: Int = 3): List<String> {
        val p = prefix.lowercase()
        val out = ArrayList<Pair<String, Int>>()
        var seen = 0
        for (w in dict.subSet(p, true, p + '\uffff', true)) {
            if (w == p) continue
            out.add(Pair(w, score(w, prev)))
            if (++seen >= 400) break
        }
        var res = out.sortedByDescending { it.second }.map { it.first }.take(n)
        if (res.isEmpty() && p.length >= 3 && p !in dict) res = fixes(p, prev).take(n)
        return res
    }

    /** Oldingi so'zdan keyin ko'p keladigan so'zlar. */
    fun next(prev: String?, n: Int = 3): List<String> {
        if (prev == null) return emptyList()
        val m = bi[prev] ?: return emptyList()
        return m.entries.sortedByDescending { it.value }.take(n).map { it.key }
    }

    private fun swaps(w: String): List<String> {
        val out = ArrayList<String>()
        for (i in 0 until w.length - 1) if (w[i] != w[i + 1]) {
            val a = w.toCharArray(); val t = a[i]; a[i] = a[i + 1]; a[i + 1] = t
            out.add(String(a))
        }
        return out
    }

    private fun allEdits(w: String): Set<String> {
        val out = HashSet<String>()
        for (i in w.indices) out.add(w.removeRange(i, i + 1))
        for (i in 0..w.length) for (c in ALPHA) out.add(w.substring(0, i) + c + w.substring(i))
        for (i in w.indices) for (c in ALPHA) if (c != w[i]) out.add(w.substring(0, i) + c + w.substring(i + 1))
        out.addAll(swaps(w))
        out.remove(w)
        return out
    }

    /** Taklif sifatida ko'rsatiladigan (avtomatik QO'YILMAYDIGAN) tuzatishlar. */
    fun fixes(w: String, prev: String?): List<String> =
        allEdits(w).filter { it in dict }.sortedByDescending { score(it, prev) }

    /**
     * Faqat ISHONCHLI xatolar avtomatik tuzatiladi: qo'shni harflar almashgan ("bugnu"→"bugun")
     * yoki qo'shni tugma bosilgan. Noaniq bo'lsa — hech narsa qilinmaydi.
     */
    fun autoFix(w: String, prev: String?, nb: Map<Char, Set<Char>>): String? {
        if (w.length < 4 || w in dict || w.any { !it.isLetter() }) return null
        if ((userCnt[w] ?: 0) >= 1) return null            // foydalanuvchi shu so'zni oldin yozgan
        val cand = HashSet<String>(swaps(w))
        for (i in w.indices) nb[w[i]]?.forEach { cand.add(w.substring(0, i) + it + w.substring(i + 1)) }
        val hits = cand.filter { it in dict }.sortedByDescending { score(it, prev) }
        if (hits.isEmpty()) return null
        if (hits.size > 1 && score(hits[0], prev) < score(hits[1], prev) * 13 / 10) return null
        return hits[0]
    }

    @Synchronized
    fun learn(raw: String, prevRaw: String?) {
        val w = raw.lowercase()
        if (w.length < 2 || w.length > 30 || w.any { !isWordChar(it) }) return
        val c = (userCnt[w] ?: 0) + 1
        userCnt[w] = c
        if (c >= 2) dict.add(w)
        if (prevRaw != null) bi.getOrPut(prevRaw) { HashMap() }.let { it[w] = (it[w] ?: 0) + 1 }
        if (++dirty >= 30) save()
    }

    /** Foydalanuvchi tuzatishni bekor qilsa: shu so'z endi hech qachon "tuzatilmaydi". */
    @Synchronized
    fun keep(raw: String) {
        val w = raw.lowercase()
        if (w.length < 2) return
        userCnt[w] = maxOf(2, userCnt[w] ?: 0)
        dict.add(w)
        dirty++
        save()
    }

    @Synchronized
    fun save() {
        val c = appCtx ?: return
        if (dirty == 0) return
        try {
            val ws = userCnt.entries.sortedByDescending { it.value }.take(MAX_USER)
            val sbW = StringBuilder()
            ws.forEach { sbW.append(it.key).append('\t').append(it.value).append('\n') }
            val flat = ArrayList<Triple<String, String, Int>>()
            bi.forEach { (p, m) -> m.forEach { (w, n) -> if (n < 3 || p !in base) flat.add(Triple(p, w, n)) } }
            val sbB = StringBuilder()
            flat.sortedByDescending { it.third }.take(MAX_BI)
                .forEach { sbB.append(it.first).append('\t').append(it.second).append('\t').append(it.third).append('\n') }
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("uw", sbW.toString()).putString("ub", sbB.toString()).apply()
            dirty = 0
        } catch (e: Exception) { }
    }

    @Synchronized
    fun clearUser(c: Context) {
        userCnt.keys.forEach { if (it !in base) dict.remove(it) }
        userCnt.clear()
        bi.keys.removeAll { it !in base }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        dirty = 0
        // seed bigramlar saqlanib qoladi (xotirada)
    }

    fun buildNeighbors(rows: List<List<String>>): Map<Char, Set<Char>> {
        val m = HashMap<Char, MutableSet<Char>>()
        fun ch(r: Int, i: Int): Char? =
            rows.getOrNull(r)?.getOrNull(i)?.let { if (it.length == 1 && it[0].isLetter()) it[0].lowercaseChar() else null }
        for (r in rows.indices) for (i in rows[r].indices) {
            val c = ch(r, i) ?: continue
            val set = m.getOrPut(c) { HashSet() }
            ch(r, i - 1)?.let { set.add(it) }
            ch(r, i + 1)?.let { set.add(it) }
            ch(r - 1, i)?.let { set.add(it) }
            ch(r - 1, i + 1)?.let { set.add(it) }
            ch(r + 1, i)?.let { set.add(it) }
            ch(r + 1, i - 1)?.let { set.add(it) }
        }
        return m
    }
}
