package uz.ozkey.keyboard

/**
 * Klaviatura statistikasi (Android'siz, testlanadi). Har bir bosish BIR marta sanaladi.
 *  - keys: har qanday bosilgan tugma (harf, raqam, belgi, probel, Enter, ⌫)
 *  - letters: harf/raqam/belgi tugmalari; spaces; enters; backspaces — alohida
 *  - words: so'z so'zdan keyin chegara tugmasi (probel, Enter, . , ! ? ; :) bosilganda BIR marta sanaladi;
 *    chegaradan keyingi yana bir probel/belgi so'zni qayta sanamaydi. Chegarani ⌫ bilan o'chirib qayta bossangiz — yana sanalmaydi.
 * Hisoblagichlar [Store] ga ma'lum sondagi hodisadan keyin va [flush] da yoziladi (ilova qayta ochilganda saqlanadi).
 */
class KeyStats(private val store: Store, private val flushEvery: Int = 20) {
    interface Store {
        fun add(deltas: Map<String, Long>)
    }

    private var keys = 0
    private var letters = 0
    private var spaces = 0
    private var enters = 0
    private var backs = 0
    private var words = 0
    private var events = 0
    private var boundaryCounted = false   // oxirgi chegara tugmasi so'zni sanagan edi

    private fun isWordChar(c: Char?) = c != null && c.isLetterOrDigit()

    /** Harf/raqam/belgi tugmasi. [prev] — bosishdan OLDIN kursor oldidagi belgi. */
    fun onType(value: String, prev: Char?) {
        keys++; letters++
        val boundary = value.length == 1 && value[0] in ".,!?;:"
        boundaryCounted = false
        if (boundary && isWordChar(prev)) { words++; boundaryCounted = true }
        tick()
    }

    fun onSpace(prev: Char?) {
        keys++; spaces++
        boundaryCounted = false
        if (isWordChar(prev)) { words++; boundaryCounted = true }
        tick()
    }

    fun onEnter(prev: Char?) {
        keys++; enters++
        boundaryCounted = false
        if (isWordChar(prev)) { words++; boundaryCounted = true }
        tick()
    }

    fun onBackspace() {
        keys++; backs++
        if (boundaryCounted) { words--; boundaryCounted = false }   // so'zni tugatgan belgi o'chdi: so'z yana "ochiq"
        tick()
    }

    private fun tick() { if (++events >= flushEvery) flush() }

    fun flush() {
        if (events == 0 && keys == 0 && words == 0) return
        store.add(mapOf(
            "stat_keys" to keys.toLong(), "stat_letters" to letters.toLong(), "stat_spaces" to spaces.toLong(),
            "stat_enters" to enters.toLong(), "stat_backs" to backs.toLong(), "stat_words" to words.toLong()
        ))
        keys = 0; letters = 0; spaces = 0; enters = 0; backs = 0; words = 0; events = 0
    }

    companion object {
        val KEYS = listOf("stat_keys", "stat_letters", "stat_spaces", "stat_enters", "stat_backs", "stat_words")
    }
}
