package com.po4yka.runatal.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
}
