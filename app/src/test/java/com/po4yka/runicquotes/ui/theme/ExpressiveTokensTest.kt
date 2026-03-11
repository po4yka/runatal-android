package com.po4yka.runicquotes.ui.theme

import com.google.common.truth.Truth.assertThat
import androidx.compose.ui.unit.dp
import org.junit.Test

class ExpressiveTokensTest {

    @Test
    fun `runic material shapes mirror expressive shape tokens`() {
        val shapeTokens = runicShapeTokens()
        val materialShapes = runicMaterialShapes(shapeTokens)

        assertThat(materialShapes.extraSmall).isEqualTo(shapeTokens.segment)
        assertThat(materialShapes.small).isEqualTo(shapeTokens.collectionCard)
        assertThat(materialShapes.medium).isEqualTo(shapeTokens.contentCard)
        assertThat(materialShapes.large).isEqualTo(shapeTokens.panel)
        assertThat(materialShapes.extraLarge).isEqualTo(shapeTokens.heroCard)
    }

    @Test
    fun `runic expressive theme exposes layout and size token families`() {
        val spacing = runicSpacingTokens()
        val strokes = runicStrokeTokens()
        val controls = runicControlSizeTokens()
        val icons = runicIconSizeTokens()

        assertThat(spacing.standard).isEqualTo(12.dp)
        assertThat(spacing.roomy).isEqualTo(20.dp)
        assertThat(strokes.subtle).isEqualTo(1.dp)
        assertThat(strokes.emphasized).isEqualTo(2.dp)
        assertThat(controls.minimumTouchTarget).isEqualTo(48.dp)
        assertThat(controls.segmentedControlMinHeight).isEqualTo(48.dp)
        assertThat(controls.dialogActionHeight).isEqualTo(48.dp)
        assertThat(controls.settingItemMinHeight).isEqualTo(57.dp)
        assertThat(controls.leadingBadgeLarge).isEqualTo(40.dp)
        assertThat(icons.standard).isEqualTo(18.dp)
        assertThat(icons.selectedAppIcon).isEqualTo(23.dp)
    }
}
