package com.po4yka.runicquotes.ui.theme

import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.util.ShareAppearance
import org.junit.Test

class ShareTokensTest {

    @Test
    fun `dark share palette is derived from foundation colors`() {
        val palette = runicSharePalette(ShareAppearance.DARK)

        assertThat(palette.background).isEqualTo(FoundationDarkBackground)
        assertThat(palette.surface).isEqualTo(FoundationDarkSurfaceContainerLow)
        assertThat(palette.primaryText).isEqualTo(FoundationDarkOnSurface)
        assertThat(palette.actionFill).isEqualTo(FoundationDarkInversePrimary)
        assertThat(palette.utilityContent).isEqualTo(FoundationDarkOnSurface)
    }

    @Test
    fun `share style tokens reuse expressive theme geometry`() {
        val shapeTokens = runicShapeTokens()
        val elevationTokens = runicElevationTokens()
        val shareStyle = runicShareStyleTokens(shapeTokens, elevationTokens)

        assertThat(shareStyle.appearanceToggleShape).isEqualTo(shapeTokens.collectionCard)
        assertThat(shareStyle.cardPreviewShape).isEqualTo(shapeTokens.contentCard)
        assertThat(shareStyle.landscapePreviewShape).isEqualTo(shapeTokens.collectionCard)
        assertThat(shareStyle.actionButtonShape).isEqualTo(shapeTokens.collectionCard)
        assertThat(shareStyle.previewCardElevation).isEqualTo(elevationTokens.overlay)
    }
}
