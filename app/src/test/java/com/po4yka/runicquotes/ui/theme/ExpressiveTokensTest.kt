package com.po4yka.runicquotes.ui.theme

import com.google.common.truth.Truth.assertThat
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
}
