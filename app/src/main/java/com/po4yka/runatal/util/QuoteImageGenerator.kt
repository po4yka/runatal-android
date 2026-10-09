package com.po4yka.runatal.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import com.po4yka.runatal.domain.model.QuoteShareContent
import com.po4yka.runatal.ui.theme.runicSharePalette
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Renders complete quote content with one layout for previews and exported images. */
@Singleton
class QuoteImageGenerator @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /** Produces the selected full-resolution export without truncating any text block. */
    fun generateQuoteImage(
        content: QuoteShareContent,
        template: ShareTemplate = ShareTemplate.CARD,
        appearance: ShareAppearance = ShareAppearance.DARK
    ): Bitmap {
        val layout = prepareLayout(content, template, appearance)
        val bitmap = createBitmap(layout.width, layout.height)
        val canvas = Canvas(bitmap)
        val palette = runicSharePalette(appearance)
        canvas.drawColor(palette.background.toArgb())
        canvas.drawRoundRect(layout.panel, PANEL_RADIUS, PANEL_RADIUS, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.surface.toArgb()
        })
        layout.blocks.forEach { block ->
            canvas.withTranslation(block.left, block.top) { block.layout.draw(this) }
        }
        return bitmap
    }

    internal fun prepareLayout(
        content: QuoteShareContent,
        template: ShareTemplate,
        appearance: ShareAppearance
    ): ShareImageLayout {
        val landscape = template == ShareTemplate.LANDSCAPE
        val width = if (landscape) LANDSCAPE_WIDTH else PORTRAIT_WIDTH
        val height = if (landscape) LANDSCAPE_HEIGHT else PORTRAIT_HEIGHT
        val margin = if (landscape) LANDSCAPE_MARGIN else PORTRAIT_MARGIN
        val panel = RectF(margin, margin, width - margin, height - margin)
        val textWidth = (panel.width() - CONTENT_PADDING * 2).toInt()
        val availableHeight = panel.height() - CONTENT_PADDING * 2
        val specs = textSpecs(content, template, appearance)
        var lower = MIN_SCALE
        var upper = 1f
        repeat(FIT_ITERATIONS) {
            val candidate = (lower + upper) / 2
            if (totalHeight(layoutBlocks(specs, textWidth, candidate), candidate) <= availableHeight) {
                lower = candidate
            } else {
                upper = candidate
            }
        }
        val blocks = layoutBlocks(specs, textWidth, lower)
        check(totalHeight(blocks, lower) <= availableHeight) { "Quote is too large for the share image" }
        var top = panel.top + (panel.height() - totalHeight(blocks, lower)) / 2
        val positioned = blocks.map { block ->
            ShareImageBlock(panel.left + CONTENT_PADDING, top, block).also {
                top += block.height + BLOCK_GAP * lower
            }
        }
        return ShareImageLayout(width, height, panel, positioned)
    }

    private fun textSpecs(
        content: QuoteShareContent,
        template: ShareTemplate,
        appearance: ShareAppearance
    ): List<ShareTextSpec> {
        val palette = runicSharePalette(appearance)
        val runes = ShareTextSpec(
            text = content.runicText,
            size = if (template == ShareTemplate.CARD) RUNE_SIZE else SUPPORTING_RUNE_SIZE,
            color = palette.primaryText.toArgb(),
            typeface = RunicTextRenderer.loadTypeface(
                context, RunicTextRenderer.getFontResource(content.font, content.script)
            )
        )
        val latin = ShareTextSpec(
            text = "“${content.textLatin}”",
            size = LATIN_SIZE,
            color = palette.secondaryText.toArgb(),
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        )
        val author = ShareTextSpec(
            text = "— ${content.author}",
            size = AUTHOR_SIZE,
            color = palette.secondaryText.toArgb(),
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        )
        val brand = ShareTextSpec(
            text = "Runatal · ${content.scriptLabel}",
            size = BRAND_SIZE,
            color = palette.tertiaryText.toArgb(),
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        )
        return if (template == ShareTemplate.CARD) listOf(brand, runes, latin, author)
        else listOf(brand, latin, runes, author)
    }

    private fun layoutBlocks(specs: List<ShareTextSpec>, width: Int, scale: Float): List<StaticLayout> =
        specs.map { spec ->
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = spec.size * scale
                color = spec.color
                typeface = spec.typeface
            }
            StaticLayout.Builder.obtain(spec.text, 0, spec.text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(true)
                .build()
        }

    private fun totalHeight(blocks: List<StaticLayout>, scale: Float): Float =
        blocks.sumOf { it.height }.toFloat() + BLOCK_GAP * scale * (blocks.size - 1)

    private data class ShareTextSpec(val text: String, val size: Float, val color: Int, val typeface: Typeface)

    private companion object {
        const val PORTRAIT_WIDTH = 1080
        const val PORTRAIT_HEIGHT = 1920
        const val LANDSCAPE_WIDTH = 1600
        const val LANDSCAPE_HEIGHT = 900
        const val PORTRAIT_MARGIN = 140f
        const val LANDSCAPE_MARGIN = 90f
        const val CONTENT_PADDING = 60f
        const val PANEL_RADIUS = 48f
        const val RUNE_SIZE = 60f
        const val SUPPORTING_RUNE_SIZE = 42f
        const val LATIN_SIZE = 48f
        const val AUTHOR_SIZE = 36f
        const val BRAND_SIZE = 24f
        const val BLOCK_GAP = 40f
        const val MIN_SCALE = 0.1f
        const val FIT_ITERATIONS = 12
    }
}

internal data class ShareImageLayout(
    val width: Int,
    val height: Int,
    val panel: RectF,
    val blocks: List<ShareImageBlock>
)

internal data class ShareImageBlock(val left: Float, val top: Float, val layout: StaticLayout)
