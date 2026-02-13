package com.po4yka.runicquotes.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.po4yka.runicquotes.ui.theme.BabelStoneRunic
import com.po4yka.runicquotes.ui.theme.BabelStoneRunicRuled
import com.po4yka.runicquotes.ui.theme.NotoSansRunic

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
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current
) {
    val fontFamily = when (font.lowercase()) {
        "babelstone" -> BabelStoneRunic
        "babelstone_ruled" -> BabelStoneRunicRuled
        else -> NotoSansRunic // Default to Noto Sans Runic
    }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        textAlign = textAlign,
        style = style.copy(fontFamily = fontFamily)
    )
}
