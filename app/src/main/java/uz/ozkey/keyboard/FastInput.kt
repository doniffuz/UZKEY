package uz.ozkey.keyboard

/**
 * Tez yozish uchun kichik, Android'siz mantiq (testlanadi).
 *
 * [PendingSpace]: probel tugmasi ACTION_UP da ishlaydi (chetga tegib ketishdan himoya). Tez yozuvchi esa
 * probelni ko'tarmasdan turib keyingi harfni bosishi mumkin (rollover). Shunda probel harfdan OLDIN yozilishi kerak —
 * aks holda "salom dunyo" → "salomd unyo" bo'lib qoladi.
 */
class PendingSpace {
    private var down = false
    private var done = false

    /** Probel tugmasi bosildi. */
    fun spaceDown() { down = true; done = false }

    /** Boshqa tugma bosildi. true — avval probel yozilishi kerak (u hali yozilmagan edi). */
    fun otherDown(): Boolean {
        if (down && !done) { done = true; return true }
        return false
    }

    /** Probel tugmasi ko'tarildi. true — probelni hozir yozish kerak (oldin yozilmagan, tugma ichida, surilmagan). */
    fun spaceUp(inside: Boolean, moved: Boolean): Boolean {
        val fire = down && !done && inside && !moved
        if (fire) done = true
        down = false
        return fire
    }

    /** Barmoq ACTION_CANCEL yoki klaviatura qayta qurildi: kutilayotgan probel bekor. */
    fun reset() { down = false; done = false }
}

/** ⌫ ni uzoq bosganda takrorlash oralig'i: avval 55 ms, 10 ta o'chirishdan keyin 35 ms. */
object RepeatDelay {
    const val INITIAL_MS = 400L
    fun next(count: Int): Long = if (count < 10) 55L else 35L
}
