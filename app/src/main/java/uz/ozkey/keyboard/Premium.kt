package uz.ozkey.keyboard

import android.content.Context

/**
 * Premium holati. Hozircha mahalliy bayroq. Play Market'ga chiqarishdan oldin
 * Google Play Billing (obuna) shu yerga ulanadi: isPremium() faqat tasdiqlangan xarid bo'yicha true qaytarsin.
 */
object Premium {
    private fun p(c: Context) = c.getSharedPreferences("ozkey", Context.MODE_PRIVATE)
    fun isPremium(c: Context) = p(c).getBoolean("premium", false)
    fun set(c: Context, v: Boolean) = p(c).edit().putBoolean("premium", v).apply()
}
