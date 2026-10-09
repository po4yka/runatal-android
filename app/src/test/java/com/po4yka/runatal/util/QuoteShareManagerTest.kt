package com.po4yka.runatal.util

import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.ResolvedQuoteRendering
import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.model.QuoteShareContent
import com.po4yka.runatal.domain.model.RunicScript
import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.security.MessageDigest
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
            imageGenerator.generateQuoteImage(any(), any(), any())
        } returns Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)

        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val manager = QuoteShareManager(
            context = context,
            imageGenerator = imageGenerator,
            ioDispatcher = dispatcher,
            mainDispatcher = dispatcher
        )

        val content = QuoteShareContent(
            Quote(1L, "Runes remember.", "Archivist", null, null, null),
            RunicScript.ELDER_FUTHARK, "noto", rendering("\u16A0\u16A2")
        )
        // Simulate an existing export from the renderer that clipped long input.
        val legacyPayload = listOf(ShareTemplate.CARD.name, ShareAppearance.DARK.name, content.script.name,
            content.font, content.runicText, content.textLatin, content.author).joinToString("\u0000")
        val legacyKey = MessageDigest.getInstance("SHA-256").digest(legacyPayload.toByteArray())
            .joinToString("") { "%02x".format(it) }
        shareDir.mkdirs()
        File(shareDir, "$legacyKey.png").writeBytes(byteArrayOf(1, 2, 3))
        val firstResult = manager.shareQuoteAsImage(
            content = content,
            template = ShareTemplate.CARD,
            appearance = ShareAppearance.DARK
        )
        val secondResult = manager.shareQuoteAsImage(
            content = content,
            template = ShareTemplate.CARD,
            appearance = ShareAppearance.DARK
        )

        assertThat(firstResult).isTrue()
        assertThat(secondResult).isTrue()
        verify(exactly = 1) {
            imageGenerator.generateQuoteImage(
                content,
                ShareTemplate.CARD,
                ShareAppearance.DARK
            )
        }

        manager.shareQuoteAsImage(content.copy(font = "babelstone"))
        manager.shareQuoteAsImage(content.copy(script = RunicScript.CIRTH))

        verify(exactly = 3) { imageGenerator.generateQuoteImage(any(), any(), any()) }
        assertThat(shareDir.listFiles()?.count { it.extension == "png" }).isEqualTo(4)
    }
    private fun rendering(glyphs: String) = ResolvedQuoteRendering(
        glyphs, emptyList(), TranslationMode.TRANSLITERATE, "Transliteration"
    )

}
