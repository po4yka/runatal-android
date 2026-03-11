package com.po4yka.runicquotes.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.po4yka.runicquotes.util.ShareAppearance

/**
 * Color roles for share previews and exported images.
 */
@Immutable
data class RunicSharePalette(
    val background: Color,
    val surface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val outline: Color,
    val rule: Color,
    val actionFill: Color,
    val actionText: Color,
    val utilityFill: Color,
    val utilityBorder: Color,
    val utilityContent: Color,
    val appearanceSwatch: Color
)

/**
 * Geometry tokens for share-specific controls and preview ornaments.
 */
@Immutable
data class RunicShareStyleTokens(
    val appearanceToggleShape: CornerBasedShape,
    val appearanceOptionShape: CornerBasedShape,
    val cardPreviewShape: CornerBasedShape,
    val landscapePreviewShape: CornerBasedShape,
    val templateTileShape: CornerBasedShape,
    val miniPreviewShape: CornerBasedShape,
    val actionButtonShape: CornerBasedShape,
    val previewCardElevation: Dp,
    val landscapePreviewElevation: Dp,
    val ruleWidth: Dp,
    val authorRuleWidth: Dp,
    val dotSmallSize: Dp,
    val dotLargeSize: Dp
)

/** Maps a share appearance mode to the shared preview and export palette roles. */
fun runicSharePalette(appearance: ShareAppearance): RunicSharePalette {
    return when (appearance) {
        ShareAppearance.DARK -> RunicSharePalette(
            background = FoundationDarkBackground,
            surface = FoundationDarkSurfaceContainerLow,
            primaryText = FoundationDarkOnSurface,
            secondaryText = FoundationDarkOutline,
            tertiaryText = FoundationDarkOutline.copy(alpha = 0.56f),
            outline = FoundationDarkOnSurface.copy(alpha = 0.08f),
            rule = FoundationDarkOutline.copy(alpha = 0.24f),
            actionFill = FoundationDarkInversePrimary,
            actionText = FoundationLightOnPrimary,
            utilityFill = FoundationDarkOnSurface.copy(alpha = 0.05f),
            utilityBorder = FoundationDarkOnSurface.copy(alpha = 0.08f),
            utilityContent = FoundationDarkOnSurface,
            appearanceSwatch = FoundationDarkSurface
        )

        ShareAppearance.LIGHT -> RunicSharePalette(
            background = FoundationLightSurface,
            surface = FoundationLightSurfaceBright,
            primaryText = FoundationLightOnSurface,
            secondaryText = FoundationLightOutline,
            tertiaryText = FoundationLightPrimary.copy(alpha = 0.56f),
            outline = FoundationLightOnSurface.copy(alpha = 0.08f),
            rule = FoundationLightOnSurface.copy(alpha = 0.10f),
            actionFill = FoundationLightPrimary,
            actionText = FoundationLightOnPrimary,
            utilityFill = FoundationLightOnSurface.copy(alpha = 0.04f),
            utilityBorder = FoundationLightOnSurface.copy(alpha = 0.08f),
            utilityContent = FoundationLightOnSurface,
            appearanceSwatch = FoundationLightSurfaceBright
        )
    }
}

/** Builds share-surface geometry tokens from the app-level shape and elevation foundations. */
fun runicShareStyleTokens(
    shapeTokens: RunicShapeTokens = runicShapeTokens(),
    elevationTokens: RunicElevationTokens = runicElevationTokens()
): RunicShareStyleTokens = RunicShareStyleTokens(
    appearanceToggleShape = shapeTokens.collectionCard,
    appearanceOptionShape = shapeTokens.segment,
    cardPreviewShape = shapeTokens.contentCard,
    landscapePreviewShape = shapeTokens.collectionCard,
    templateTileShape = shapeTokens.collectionCard,
    miniPreviewShape = shapeTokens.segment,
    actionButtonShape = shapeTokens.collectionCard,
    previewCardElevation = elevationTokens.overlay,
    landscapePreviewElevation = elevationTokens.overlay + 2.dp,
    ruleWidth = 32.dp,
    authorRuleWidth = 16.dp,
    dotSmallSize = 3.dp,
    dotLargeSize = 4.dp
)
