package uz.ozkey.keyboard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Qurilma/emulyatorda (SQLite kerak): `./gradlew connectedDebugAndroidTest`. */
@RunWith(AndroidJUnit4::class)
class ClipStoreTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Before fun clean() { ClipStore.clearAll(ctx) }

    @Test fun duplicateIsNotAddedTwiceAndMovesToTop() {
        ClipStore.add(ctx, "alpha"); Thread.sleep(5)
        ClipStore.add(ctx, "beta"); Thread.sleep(5)
        ClipStore.add(ctx, "alpha")
        val l = ClipStore.list(ctx)
        assertEquals(2, l.size)
        assertEquals("alpha", l[0].preview)
    }

    @Test fun emptyAndOverlongTextIsRejected() {
        assertFalse(ClipStore.add(ctx, "   "))
        assertFalse(ClipStore.add(ctx, "a".repeat(ClipStore.MAX_LEN + 1)))
        assertEquals(0, ClipStore.stats(ctx).first)
    }

    @Test fun longTextIsStoredFullyButListedAsPreview() {
        val long = "a".repeat(ClipStore.MAX_LEN)
        assertTrue(ClipStore.add(ctx, long))
        val item = ClipStore.list(ctx).single()
        assertTrue(item.preview.length < long.length)
        assertEquals(long, ClipStore.text(ctx, item.id))
    }

    @Test fun pinnedAndFavoriteSurviveHistoryLimit() {
        ClipStore.add(ctx, "pinned-one")
        ClipStore.add(ctx, "fav-one")
        ClipStore.list(ctx).forEach {
            if (it.preview == "pinned-one") ClipStore.togglePin(ctx, it.id)
            if (it.preview == "fav-one") ClipStore.toggleFav(ctx, it.id)
        }
        for (i in 1..(ClipStore.MAX_HISTORY + 20)) ClipStore.add(ctx, "x$i")
        assertEquals("pinned-one", ClipStore.list(ctx, ClipStore.TAB_PIN).single().preview)
        assertEquals("fav-one", ClipStore.list(ctx, ClipStore.TAB_FAV).single().preview)
        assertEquals(ClipStore.MAX_HISTORY + 2, ClipStore.stats(ctx).first)
    }

    @Test fun clearHistoryKeepsSavedItems() {
        ClipStore.add(ctx, "keep"); ClipStore.add(ctx, "drop")
        ClipStore.list(ctx).first { it.preview == "keep" }.let { ClipStore.toggleFav(ctx, it.id) }
        ClipStore.clearHistory(ctx)
        assertEquals(listOf("keep"), ClipStore.list(ctx).map { it.preview })
    }

    @Test fun missingIdReturnsNull() {
        assertNull(ClipStore.text(ctx, -1L))
    }

    // ---------- edit(): takror, id, pin/fav, tartib ----------

    private fun idOf(text: String) = ClipStore.list(ctx, limit = 1000).first { it.preview == text }.id

    @Test fun editToNewTextKeepsIdFlagsAndOrder() {
        ClipStore.add(ctx, "a"); Thread.sleep(5); ClipStore.add(ctx, "b"); Thread.sleep(5); ClipStore.add(ctx, "c")
        val b = idOf("b"); ClipStore.togglePin(ctx, b); ClipStore.toggleFav(ctx, b); ClipStore.setCat(ctx, b, "Ish")
        val before = ClipStore.get(ctx, b)!!
        assertTrue(ClipStore.edit(ctx, b, "  b2  "))
        val after = ClipStore.get(ctx, b)!!
        assertEquals("b2", after.preview)
        assertEquals(before.ts, after.ts)
        assertTrue(after.pinned); assertTrue(after.fav); assertEquals("Ish", after.cat)
        assertEquals(listOf("b2", "c", "a"), ClipStore.list(ctx).map { it.preview })   // pin tepada, qolgani vaqt bo'yicha
    }

    @Test fun editIntoExistingTextMergesWithoutDuplicate() {
        ClipStore.add(ctx, "alpha"); Thread.sleep(5); ClipStore.add(ctx, "beta")
        val a = idOf("alpha"); val b = idOf("beta")
        assertEquals(a, ClipStore.editId(ctx, b, "alpha"))      // ishlab qolgan id — tahrirlangan yozuv
        val l = ClipStore.list(ctx)
        assertEquals(1, l.size)
        assertEquals("alpha", l[0].preview)
        assertEquals(b, l[0].id)
        assertNull(ClipStore.get(ctx, a))                       // eski takror yozuv o'chdi
        assertEquals(1, ClipStore.stats(ctx).first)
    }

    @Test fun mergeKeepsPinFavAndCategoryFromEitherRow() {
        ClipStore.add(ctx, "x1"); ClipStore.add(ctx, "x2")
        val x1 = idOf("x1"); val x2 = idOf("x2")
        ClipStore.togglePin(ctx, x1); ClipStore.setCat(ctx, x1, "Ish")      // takror (qoladigan emas) — pin + kategoriya
        ClipStore.toggleFav(ctx, x2)                                          // tahrirlanayotgan — fav
        ClipStore.editId(ctx, x2, "x1")
        val m = ClipStore.list(ctx).single()
        assertTrue(m.pinned); assertTrue(m.fav); assertEquals("Ish", m.cat)
        assertEquals(1, ClipStore.list(ctx, ClipStore.TAB_PIN).size)
        assertEquals(1, ClipStore.list(ctx, ClipStore.TAB_FAV).size)
    }

    @Test fun mergePrefersEditedRowCategoryWhenBothHaveOne() {
        ClipStore.add(ctx, "k1"); ClipStore.add(ctx, "k2")
        val k1 = idOf("k1"); val k2 = idOf("k2")
        ClipStore.setCat(ctx, k1, "Ish"); ClipStore.setCat(ctx, k2, "Shaxsiy")
        ClipStore.editId(ctx, k2, "k1")
        assertEquals("Shaxsiy", ClipStore.list(ctx).single().cat)
    }

    @Test fun mergeUsesNewerTimestampSoOrderStaysConsistent() {
        ClipStore.add(ctx, "old"); Thread.sleep(5); ClipStore.add(ctx, "mid"); Thread.sleep(5); ClipStore.add(ctx, "new")
        val newId = idOf("new")
        val oldTs = ClipStore.get(ctx, idOf("old"))!!.ts
        ClipStore.editId(ctx, idOf("old"), "new")     // eski -> yangi bilan birlashadi
        val l = ClipStore.list(ctx)
        assertEquals(listOf("new", "mid"), l.map { it.preview })
        assertTrue(l[0].ts >= oldTs)
        assertNull(ClipStore.get(ctx, newId))
    }

    @Test fun editRejectsEmptyOverlongAndMissingId() {
        ClipStore.add(ctx, "keep"); val id = idOf("keep")
        assertFalse(ClipStore.edit(ctx, id, "   "))
        assertFalse(ClipStore.edit(ctx, id, ""))
        assertFalse(ClipStore.edit(ctx, id, "a".repeat(ClipStore.MAX_LEN + 1)))
        assertFalse(ClipStore.edit(ctx, -42L, "x"))
        assertEquals(-1L, ClipStore.editId(ctx, -42L, "x"))
        assertEquals("keep", ClipStore.text(ctx, id))            // hech narsa o'zgarmadi
        assertEquals(1, ClipStore.stats(ctx).first)
    }

    @Test fun editToSameTextIsHarmlessAndCaseSensitive() {
        ClipStore.add(ctx, "Same"); ClipStore.add(ctx, "same")
        val id = idOf("Same")
        assertTrue(ClipStore.edit(ctx, id, "Same"))              // o'zi bilan "takror" emas
        assertEquals(2, ClipStore.stats(ctx).first)              // Same va same — turli matnlar
    }

    @Test fun afterMergeEditedTextIsSearchableAndFullTextStored() {
        ClipStore.add(ctx, "foo"); ClipStore.add(ctx, "bar")
        ClipStore.edit(ctx, idOf("bar"), "FOO bar baz")
        assertEquals(1, ClipStore.list(ctx, query = "foo bar").size)
        assertEquals("FOO bar baz", ClipStore.text(ctx, idOf("FOO bar baz")))
    }

    @Test fun pinFavSurviveEditWithoutDuplicate() {
        ClipStore.add(ctx, "p"); val id = idOf("p")
        ClipStore.togglePin(ctx, id); ClipStore.toggleFav(ctx, id)
        ClipStore.edit(ctx, id, "p edited")
        val it = ClipStore.get(ctx, id)!!
        assertTrue(it.pinned); assertTrue(it.fav)
    }
}
