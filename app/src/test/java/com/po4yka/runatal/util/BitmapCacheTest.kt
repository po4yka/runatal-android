package com.po4yka.runatal.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import android.app.Application
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BitmapCacheTest {
    @After
    fun tearDown() {
        BitmapCache.clear()
    }

    @Test
    fun `eviction preserves the bitmap retained by a widget`() {
        val retained = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        retained.eraseColor(Color.RED)
        BitmapCache.put("retained", retained)
        val oversized = Bitmap.createBitmap(1025, 1025, Bitmap.Config.ARGB_8888)

        BitmapCache.put("oversized", oversized)

        assertThat(BitmapCache.get("retained")).isNull()
        assertThat(retained.isRecycled).isFalse()
        assertThat(retained.getPixel(0, 0)).isEqualTo(Color.RED)
        assertThat(oversized.isRecycled).isFalse()
    }

    @Test
    fun `clearing the cache preserves a consumers bitmap`() {
        val retained = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        BitmapCache.put("retained", retained)

        BitmapCache.clear()

        assertThat(BitmapCache.size()).isEqualTo(0)
        assertThat(retained.isRecycled).isFalse()
    }

    @Test
    fun `render key distinguishes colors scaling and layout`() {
        val config = RenderConfig(text = "ᚠᚢ", fontResource = 1, textColor = Color.WHITE)
        val key = BitmapCache.generateKey(config, 20f)
        val alternatives = listOf(
            config.copy(textColor = Color.BLACK),
            config.copy(backgroundColor = Color.RED),
            config.copy(fontResource = 2),
            config.copy(textSizeSp = 30f),
            config.copy(maxWidth = 100),
            config.copy(maxLines = 2),
            config.copy(textAlign = RenderTextAlign.START)
        )

        alternatives.forEach { assertThat(BitmapCache.generateKey(it, 20f)).isNotEqualTo(key) }
        assertThat(BitmapCache.generateKey(config, 30f)).isNotEqualTo(key)
    }
}
