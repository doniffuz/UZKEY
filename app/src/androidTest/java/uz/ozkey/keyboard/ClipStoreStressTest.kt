package uz.ozkey.keyboard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Qurilma/emulyatorda (SQLite kerak): ketma-ket ko'p amal va chet qiymatlar crash bermasligi, ma'lumot buzilmasligi. */
@RunWith(AndroidJUnit4::class)
class ClipStoreStressTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Before fun clean() { ClipStore.clearAll(ctx) }

    @Test fun fiveHundredRapidAddsKeepHistoryLimit() {
        repeat(500) { ClipStore.add(ctx, "item $it") }
        assertEquals(ClipStore.MAX_HISTORY, ClipStore.stats(ctx).first)
        assertEquals("item 499", ClipStore.list(ctx).first().preview)
    }

    @Test fun rapidEditsAndTogglesNeverCreateDuplicates() {
        repeat(50) { ClipStore.add(ctx, "t$it") }
        val ids = ClipStore.list(ctx, limit = 100).map { it.id }
        ids.forEachIndexed { i, id ->
            ClipStore.edit(ctx, id, "t${i % 10}")           // ko'p yozuv bir xil matnga tahrirlanadi
            ClipStore.togglePin(ctx, id); ClipStore.toggleFav(ctx, id)
        }
        val texts = ClipStore.list(ctx, limit = 1000).map { it.preview }
        assertEquals(texts.size, texts.toSet().size)           // takror yo'q
        assertTrue(texts.size <= 10)
    }

    @Test fun hostileInputDoesNotCrashOrInject() {
        val evil = "'); DROP TABLE clips;-- %_\\  😀"
        ClipStore.add(ctx, evil)
        assertEquals(1, ClipStore.list(ctx, query = "DROP TABLE").size)
        assertEquals(1, ClipStore.list(ctx, query = "%").size)       // % maxsus belgi emas, oddiy belgi sifatida qidiriladi
        assertEquals(0, ClipStore.list(ctx, query = "zzz").size)
        ClipStore.setCat(ctx, ClipStore.list(ctx).first().id, "x'; DELETE FROM clips;--")
        assertEquals(1, ClipStore.stats(ctx).first)
    }

    @Test fun invalidIdsAndBackupGarbageAreHarmless() {
        ClipStore.toggleFav(ctx, -1L); ClipStore.togglePin(ctx, Long.MAX_VALUE); ClipStore.remove(ctx, 0L)
        // 1-element to'g'ri, 2-element buzuq: crash yo'q; kategoriya nomi 24 belgigacha qisqartiriladi
        ClipStore.importSaved(ctx, org.json.JSONObject("{\"items\":[{\"t\":\"ok\",\"c\":\"${"c".repeat(500)}\"}, {\"x\":1}]}"))
        assertTrue(ClipStore.stats(ctx).first <= 1)
        assertTrue(ClipStore.list(ctx).all { it.cat.length <= 24 })
    }
}
