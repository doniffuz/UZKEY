package uz.ozkey.keyboard

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tarjima. Maqsad tillar: "uz" (lotin, ÖZ alifbosi), "uz-cyr" (kirill), "ru", "en".
 *
 * MUHIM: tarjima FAQAT o'z serveringiz (backend/worker.js) ulanganda ishlaydi.
 * gradle.properties:  ozkeyTranslateUrl=https://...   (INTERNET ruxsati AndroidManifest.xml da bor).
 * Server ulanmagan bo'lsa tarjima tugmalari klaviaturada ko'rinmaydi va ilova internetga chiqmaydi.
 * Faqat fon oqimida chaqiring!
 */
object Translator {
    val targets = listOf("uz", "uz-cyr", "ru", "en")
    private const val MAX_RESPONSE = 200_000

    /** Server manzili berilgan bo'lsagina tarjima mavjud. */
    val available: Boolean get() = BuildConfig.TRANSLATE_URL.startsWith("https://")

    fun translate(text: String, target: String): String {
        if (!available) throw IllegalStateException("translate disabled")
        val raw = viaServer(text, target).trim()
        return if (target == "uz") Alphabet.toNew(raw) else raw
    }

    private fun post(url: String, body: ByteArray): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = false      // HTTPS dan boshqa joyga yo'naltirishga ergashmaymiz
            conn.doOutput = true
            conn.setRequestProperty("content-type", "application/json")
            conn.outputStream.use { it.write(body) }
            if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
            return Net.readCapped(conn.inputStream, MAX_RESPONSE)
        } finally { conn.disconnect() }
    }

    private fun viaServer(text: String, target: String): String {
        val body = JSONObject().put("text", text).put("target", target).toString().toByteArray(Charsets.UTF_8)
        return JSONObject(post(BuildConfig.TRANSLATE_URL, body)).getString("translation")
    }
}

/** Tarmoq javobini cheklangan hajmda o'qiydi (zararli/xato server xotirani to'ldirmasin). */
object Net {
    fun readCapped(input: java.io.InputStream, max: Int): String {
        input.use { ins ->
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var total = 0
            while (true) {
                val n = ins.read(buf)
                if (n < 0) break
                total += n
                if (total > max) throw java.io.IOException("response too large")
                out.write(buf, 0, n)
            }
            return out.toString("UTF-8")
        }
    }
}
