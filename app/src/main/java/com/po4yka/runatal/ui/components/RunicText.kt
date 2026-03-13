package com.po4yka.runatal.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.po4yka.runatal.domain.transliteration.CirthGlyphCompat
import com.po4yka.runatal.ui.theme.BabelStoneRunic
import com.po4yka.runatal.ui.theme.BabelStoneRunicRuled
import com.po4yka.runatal.ui.theme.LocalRunicFontScale
import com.po4yka.runatal.ui.theme.NotoSansRunic
import com.po4yka.runatal.ui.theme.RunicTextRole
import com.po4yka.runatal.ui.theme.RunicTypeRoles
import com.po4yka.runatal.domain.model.RunicScript

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
@Suppress("CyclomaticComplexMethod") // Script-specific styling branches are inherently branchy
@Composable
fun RunicText(
    text: String,
    modifier: Modifier = Modifier,
    font: String = "noto",
    script: RunicScript = RunicScript.DEFAULT,
    role: RunicTextRole = RunicTextRole.Default,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    overrideLetterSpacing: TextUnit? = null,
    overrideLineHeight: TextUnit? = null,
    accessibilityText: String? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle? = null
) {
    val runicFontScale = LocalRunicFontScale.current
    val roleSpec = RunicTypeRoles.runic(
        role = role,
        script = script
    )
    val normalizedText = remember(text, script) {
        if (script == RunicScript.CIRTH) {
            CirthGlyphCompat.normalizeLegacyPuaGlyphs(text)
        } else {
            text
        }
    }

    val fontFamily = when (font.lowercase()) {
        "babelstone" -> BabelStoneRunic
        "babelstone_ruled" -> BabelStoneRunicRuled
        else -> NotoSansRunic // Default to Noto Sans Runic
    }

    val baseStyle = style ?: if (role == RunicTextRole.Default) {
        LocalTextStyle.current
    } else {
        roleSpec.style
    }

    val tunedStyle = baseStyle.copy(
        fontFamily = fontFamily,
        letterSpacing = overrideLetterSpacing ?: roleSpec.letterSpacing,
        lineHeight = (overrideLineHeight ?: roleSpec.lineHeight) * runicFontScale
    )

    val baseFontSize = if (fontSize != TextUnit.Unspecified) {
        fontSize
    } else {
        roleSpec.fontSize
    }
    val tunedFontSize = baseFontSize * runicFontScale
    val accessibilityModifier = if (!accessibilityText.isNullOrBlank()) {
        Modifier.clearAndSetSemantics {
            contentDescription = accessibilityText
        }
    } else {
        Modifier
    }

    Text(
        text = normalizedText,
        modifier = modifier.then(accessibilityModifier),
        color = color,
        fontSize = tunedFontSize,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        style = tunedStyle
    )
}
