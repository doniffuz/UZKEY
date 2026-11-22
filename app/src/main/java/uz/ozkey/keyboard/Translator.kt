package uz.ozkey.keyboard

/**
 * Tarjima funksiyasi 1.0.1 da O'CHIRILGAN: ilova internetga umuman chiqmaydi (INTERNET ruxsati yo'q).
 * Chaqiruvchi kod (klaviatura/sozlamalar) o'zgarmasligi uchun sodda API saqlab qolingan:
 * [available] doim false bo'lgani sababli tarjima tugmalari ko'rinmaydi.
 */
object Translator {
    val targets = listOf("uz", "uz-cyr", "ru", "en")

    const val available: Boolean = false

    @Suppress("UNUSED_PARAMETER")
    fun translate(text: String, target: String): String =
        throw IllegalStateException("translate disabled: app is offline")
}
