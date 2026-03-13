package com.po4yka.runatal.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RunicTextRendererTest {

    @Test
    fun `renderTextToBitmap returns non empty bitmap and draws background`() {
        val bitmap = RunicTextRenderer.renderTextToBitmap(
            context = RuntimeEnvironment.getApplication(),
            config = RenderConfig(
                text = "\uE080 \uE081",
                fontResource = RunicTextRenderer.getFontResource("noto"),
                textSizeSp = 24f,
                backgroundColor = android.graphics.Color.BLACK,
                maxWidth = 300,
                textAlign = RenderTextAlign.START,
                maxLines = 2
            )
        )

        assertThat(bitmap.width).isGreaterThan(0)
        assertThat(bitmap.height).isGreaterThan(0)
    }

    @Test
    fun `getFontResource resolves supported names and defaults unknown values`() {
        assertThat(RunicTextRenderer.getFontResource("babelstone"))
            .isEqualTo(com.po4yka.runatal.R.font.babelstone_runic)
        assertThat(RunicTextRenderer.getFontResource("babelstone_ruled"))
            .isEqualTo(com.po4yka.runatal.R.font.babelstone_runic_ruled)
        assertThat(RunicTextRenderer.getFontResource("unknown"))
            .isEqualTo(com.po4yka.runatal.R.font.noto_sans_runic)
    }
}
