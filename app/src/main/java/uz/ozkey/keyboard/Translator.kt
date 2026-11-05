package uz.ozkey.keyboard

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Tarjima. Maqsad tillar: "uz" (lotin, ÖZ alifbosi), "uz-cyr" (kirill), "ru", "en".
 * Ustuvorlik: 1) o'z serveringiz (BuildConfig.TRANSLATE_URL, backend/worker.js),
 *             2) zaxira: bepul umumiy xizmat.
 * Faqat fon oqimida chaqiring!
 */
object Translator {
    val targets = listOf("uz", "uz-cyr", "ru", "en")

    fun translate(text: String, target: String): String {
        val raw = when {
            BuildConfig.TRANSLATE_URL.isNotBlank() -> viaServer(text, target)
            else -> viaFree(text, target)
        }.trim()
        return if (target == "uz") Alphabet.toNew(raw) else raw
    }

    private fun post(url: String, headers: Map<String, String>, body: ByteArray, read: Int = 30000): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = read
            conn.doOutput = true
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.outputStream.use { it.write(body) }
            if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally { conn.disconnect() }
    }

    private fun viaServer(text: String, target: String): String {
        val body = JSONObject().put("text", text).put("target", target).toString().toByteArray(Charsets.UTF_8)
        val resp = post(BuildConfig.TRANSLATE_URL, mapOf("content-type" to "application/json"), body)
        return JSONObject(resp).getString("translation")
    }

    /** Zaxira (rasmiy bo'lmagan umumiy xizmat). Ishlab chiqarishda o'z serveringizni ulang. */
    private fun viaFree(text: String, target: String): String {
        val tl = when (target) { "uz-cyr" -> "uz"; else -> target }
        val resp = post(
            "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$tl&dt=t",
            mapOf("content-type" to "application/x-www-form-urlencoded; charset=UTF-8"),
            ("q=" + URLEncoder.encode(text, "UTF-8")).toByteArray(Charsets.UTF_8), 20000
        )
        val parts = JSONArray(resp).getJSONArray(0)
        val sb = StringBuilder()
        for (i in 0 until parts.length()) sb.append(parts.getJSONArray(i).optString(0, ""))
        val out = sb.toString()
        return if (target == "uz-cyr") Script.latToCyr(out) else out
    }
}
