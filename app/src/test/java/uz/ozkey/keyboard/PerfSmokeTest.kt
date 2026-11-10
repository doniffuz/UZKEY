package uz.ozkey.keyboard

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Sof-mantiq tezligi (JVM, ish stoli protsessori). Bu QURILMA o'lchovi EMAS: telefonda sekinroq bo'ladi.
 * Chegaralar ataylab keng — faqat tasodifiy qimmat (kvadratik) regressiyani ushlash uchun.
 */
class PerfSmokeTest {
    private fun dictWords(): List<String>? {
        val f = listOf("src/main/assets/words_uz.txt", "app/src/main/assets/words_uz.txt").map(::File).firstOrNull { it.exists() }
        return f?.readLines()?.map { it.trim() }?.filter { it.isNotEmpty() }
    }

    private fun report(name: String, n: Int, nanos: Long) =
        println("PERF[JVM] $name: ${n} ta, jami ${nanos / 1_000_000} ms, o'rtacha ${nanos / n / 1000} mks/chaqiruv")

    @Test fun keyStatsHandlesHundredThousandEventsFast() {
        val st = KeyStats(object : KeyStats.Store { override fun add(deltas: Map<String, Long>) {} })
        val t0 = System.nanoTime()
        repeat(100_000) { st.onType("a", null) }
        val dt = System.nanoTime() - t0
        report("KeyStats.onType", 100_000, dt)
        assertTrue(dt < 2_000_000_000L)
    }

    @Test fun autoCorrectPlanAndSuggestionsOnRealDictionaryAreFast() {
        val words = dictWords()
        assumeTrue("words_uz.txt topilmadi", words != null)
        WordModel.resetForTest(words!!)
        val none = emptyMap<Char, Set<Char>>()
        val samples = listOf("salom bugnu", "men kitbo", "yaxshi", "qwxzpt", "o'qituvchi", "Toshkent shahrida")
        val n = 5_000
        var t0 = System.nanoTime()
        repeat(n) { i -> AutoCorrect.plan(samples[i % samples.size], null, none) }
        var dt = System.nanoTime() - t0
        report("AutoCorrect.plan", n, dt); assertTrue(dt < 5_000_000_000L)
        t0 = System.nanoTime()
        repeat(n) { i -> WordModel.complete(listOf("sa", "bug", "kitbo", "yax", "qwxz")[i % 5], null) }
        dt = System.nanoTime() - t0
        report("WordModel.complete (taklif)", n, dt); assertTrue(dt < 5_000_000_000L)
    }

    @Test fun longTextTypingThroughStatsAndAutoCorrectStaysLinear() {
        val words = dictWords()
        assumeTrue("words_uz.txt topilmadi", words != null)
        WordModel.resetForTest(words!!)
        val st = KeyStats(object : KeyStats.Store { override fun add(deltas: Map<String, Long>) {} })
        val sb = StringBuilder()
        val t0 = System.nanoTime()
        var fixes = 0
        repeat(5_000) { i ->
            val w = words[i % words.size]
            for (ch in w) { st.onType(ch.toString(), sb.lastOrNull()); sb.append(ch) }
            val before = sb.takeLast(80).toString()
            if (AutoCorrect.plan(before, null, emptyMap()) != null) fixes++   // lug'at so'zlari hech qachon tuzatilmasligi kerak
            st.onSpace(sb.lastOrNull()); sb.append(' ')
        }
        val dt = System.nanoTime() - t0
        report("uzun matn (5000 so'z)", 5_000, dt)
        assertTrue("lug'atdagi so'z tuzatildi", fixes == 0)
        assertTrue(dt < 10_000_000_000L)
    }
}
