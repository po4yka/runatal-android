package com.po4yka.runicquotes.util

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class QuoteShareManagerTest {

    private val context = RuntimeEnvironment.getApplication()
    private val shareDir = File(context.cacheDir, "shared_quotes")

    @After
    fun tearDown() {
        shareDir.deleteRecursively()
    }

    @Test
    fun `shareQuoteAsImage reuses cached file for identical content`() = runTest {
        val imageGenerator = mockk<QuoteImageGenerator>()
        every {
            imageGenerator.generateQuoteImage(any(), any(), any(), any(), any())
        } returns Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)

        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val manager = QuoteShareManager(
            context = context,
            imageGenerator = imageGenerator,
            ioDispatcher = dispatcher,
            mainDispatcher = dispatcher
        )

        val firstResult = manager.shareQuoteAsImage(
            runicText = "\u16A0\u16A2",
            latinText = "Runes remember.",
            author = "Archivist",
            template = ShareTemplate.CARD,
            appearance = ShareAppearance.DARK
        )
        val secondResult = manager.shareQuoteAsImage(
            runicText = "\u16A0\u16A2",
            latinText = "Runes remember.",
            author = "Archivist",
            template = ShareTemplate.CARD,
            appearance = ShareAppearance.DARK
        )

        assertThat(firstResult).isTrue()
        assertThat(secondResult).isTrue()
        verify(exactly = 1) {
            imageGenerator.generateQuoteImage(
                "\u16A0\u16A2",
                "Runes remember.",
                "Archivist",
                ShareTemplate.CARD,
                ShareAppearance.DARK
            )
        }
    }
}
