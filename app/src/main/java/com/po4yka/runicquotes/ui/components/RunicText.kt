package com.po4yka.runicquotes.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.po4yka.runicquotes.domain.transliteration.CirthGlyphCompat
import com.po4yka.runicquotes.ui.theme.BabelStoneRunic
import com.po4yka.runicquotes.ui.theme.BabelStoneRunicRuled
import com.po4yka.runicquotes.ui.theme.LocalRunicFontScale
import com.po4yka.runicquotes.ui.theme.NotoSansRunic
import com.po4yka.runicquotes.domain.model.RunicScript

/**
 * Composable for displaying text in runic fonts.
 *
 * @param text The runic text to display
 * @param modifier Modifier for styling
 * @param font The font to use ("noto", "babelstone", "babelstone_ruled")
 * @param color Text color
 * @param fontSize Font size
 * @param textAlign Text alignment
 * @param style Additional text style
 */
@Composable
fun RunicText(
    text: String,
    modifier: Modifier = Modifier,
    font: String = "noto",
    script: RunicScript = RunicScript.DEFAULT,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current
) {
    val runicFontScale = LocalRunicFontScale.current
    val normalizedText = CirthGlyphCompat.normalizeLegacyPuaGlyphs(text)

    val fontFamily = when (font.lowercase()) {
        "babelstone" -> BabelStoneRunic
        "babelstone_ruled" -> BabelStoneRunicRuled
        else -> NotoSansRunic // Default to Noto Sans Runic
    }

    val tunedStyle = style.copy(
        fontFamily = fontFamily,
        letterSpacing = when (script) {
            RunicScript.ELDER_FUTHARK -> 0.35.sp
            RunicScript.YOUNGER_FUTHARK -> 0.15.sp
            RunicScript.CIRTH -> 0.25.sp
        },
        lineHeight = when (script) {
            RunicScript.ELDER_FUTHARK -> 44.sp
            RunicScript.YOUNGER_FUTHARK -> 42.sp
            RunicScript.CIRTH -> 46.sp
        } * runicFontScale
    )

    val baseFontSize = if (fontSize != TextUnit.Unspecified) {
        fontSize
    } else {
        when (script) {
            RunicScript.ELDER_FUTHARK -> 33.sp
            RunicScript.YOUNGER_FUTHARK -> 31.sp
            RunicScript.CIRTH -> 35.sp
        }
    }
    val tunedFontSize = baseFontSize * runicFontScale

    Text(
        text = normalizedText,
        modifier = modifier,
        color = color,
        fontSize = tunedFontSize,
        textAlign = textAlign,
        style = tunedStyle
    )
}
