@file:Suppress("MagicNumber")

package com.po4yka.runatal.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.po4yka.runatal.domain.transliteration.CirthGlyphCompat
import com.po4yka.runatal.ui.theme.runicSharePalette
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates images from quotes for sharing.
 */
@Singleton
@Suppress("TooManyFunctions")
class QuoteImageGenerator @Inject constructor() {

    private data class SharePalette(
        val background: Int,
        val surface: Int,
        val primaryText: Int,
        val secondaryText: Int,
        val tertiaryText: Int,
        val outline: Int,
        val rule: Int
    )

    /**
     * Image sizes for the exported templates.
     */
    companion object {
        private const val PORTRAIT_WIDTH = 1080
        private const val PORTRAIT_HEIGHT = 1920
        private const val LANDSCAPE_WIDTH = 1600
        private const val LANDSCAPE_HEIGHT = 900
    }

    /**
     * Generates a shareable bitmap for the selected template and appearance.
     */
    fun generateQuoteImage(
        runicText: String,
        latinText: String,
        author: String,
        template: ShareTemplate = ShareTemplate.CARD,
        appearance: ShareAppearance = ShareAppearance.DARK
    ): Bitmap {
        val normalizedRunicText = CirthGlyphCompat.normalizeLegacyPuaGlyphs(runicText)
        return when (template) {
            ShareTemplate.CARD -> generateCardImage(
                runicText = normalizedRunicText,
                latinText = latinText,
                author = author,
                palette = paletteFor(appearance)
            )

            ShareTemplate.VERSE -> generateVerseImage(
                runicText = normalizedRunicText,
                latinText = latinText,
                author = author,
                palette = paletteFor(appearance)
            )

            ShareTemplate.LANDSCAPE -> generateLandscapeImage(
                runicText = normalizedRunicText,
                latinText = latinText,
                author = author,
                palette = paletteFor(appearance)
            )
        }
    }

    private fun generateCardImage(
        runicText: String,
        latinText: String,
        author: String,
        palette: SharePalette
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(PORTRAIT_WIDTH, PORTRAIT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.background)

        val cardRect = RectF(150f, 310f, PORTRAIT_WIDTH - 150f, 1320f)
        drawRoundedPanel(canvas, cardRect, 48f, palette)

        val centerX = cardRect.centerX()
        var currentY = cardRect.top + 112f

        drawRuleWithRune(canvas, centerX, currentY, cardRect.width() - 180f, palette)
        currentY += 118f

        val runicPaint = paint(
            color = palette.primaryText,
            textSize = 60f,
            typeface = Typeface.MONOSPACE,
            textAlign = Paint.Align.CENTER,
            letterSpacing = 0.12f
        )
        val runicLines = wrapText(runicText, runicPaint, (cardRect.width() - 180f).toInt())
        currentY = drawCenteredLines(canvas, runicLines, centerX, currentY, 74f, runicPaint)
        currentY += 60f

        drawCenterRule(canvas, centerX, currentY, 96f, palette.rule)
        currentY += 56f

        val latinPaint = paint(
            color = palette.secondaryText,
            textSize = 42f,
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC),
            textAlign = Paint.Align.CENTER
        )
        val latinLines = wrapText("“$latinText”", latinPaint, (cardRect.width() - 180f).toInt())
        currentY = drawCenteredLines(canvas, latinLines, centerX, currentY, 54f, latinPaint)
        currentY += 32f

        val authorPaint = paint(
            color = palette.secondaryText,
            textSize = 36f,
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD),
            textAlign = Paint.Align.CENTER
        )
        canvas.drawText("— $author", centerX, currentY, authorPaint)

        val footerPaint = paint(
            color = palette.tertiaryText,
            textSize = 22f,
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD),
            textAlign = Paint.Align.CENTER,
            letterSpacing = 0.08f
        )
        canvas.drawText(
            "Runatal · Elder Futhark",
            centerX,
            cardRect.bottom - 56f,
            footerPaint
        )

        return bitmap
    }

    private fun generateVerseImage(
        runicText: String,
        latinText: String,
        author: String,
        palette: SharePalette
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(PORTRAIT_WIDTH, PORTRAIT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.background)

        val cardRect = RectF(150f, 280f, PORTRAIT_WIDTH - 150f, 1370f)
        drawRoundedPanel(canvas, cardRect, 48f, palette)

        val centerX = cardRect.centerX()
        var currentY = cardRect.top + 120f

        drawDecorativeDots(canvas, centerX, currentY, palette.secondaryText)
        currentY += 110f

        val quotePaint = paint(
            color = palette.primaryText,
            textSize = 56f,
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC),
            textAlign = Paint.Align.CENTER
        )
        val quoteLines = wrapText("“$latinText”", quotePaint, (cardRect.width() - 200f).toInt())
        currentY = drawCenteredLines(canvas, quoteLines, centerX, currentY, 68f, quotePaint)
        currentY += 44f

        drawDividerWithDots(canvas, centerX, currentY, palette.rule)
        currentY += 48f

        val runicPaint = paint(
            color = palette.tertiaryText,
            textSize = 26f,
            typeface = Typeface.MONOSPACE,
            textAlign = Paint.Align.CENTER,
            letterSpacing = 0.08f
        )
        val runicLines = wrapText(runicText, runicPaint, (cardRect.width() - 200f).toInt())
        currentY = drawCenteredLines(canvas, runicLines.take(2), centerX, currentY, 34f, runicPaint)
        currentY += 52f

        val authorPaint = paint(
            color = palette.secondaryText,
            textSize = 34f,
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD),
            textAlign = Paint.Align.CENTER
        )
        canvas.drawText(author, centerX, currentY, authorPaint)

        val footerPaint = paint(
            color = palette.tertiaryText,
            textSize = 24f,
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD),
            textAlign = Paint.Align.CENTER,
            letterSpacing = 0.08f
        )
        canvas.drawText("ᚱ  Runatal", centerX, cardRect.bottom - 56f, footerPaint)

        return bitmap
    }

    private fun generateLandscapeImage(
        runicText: String,
        latinText: String,
        author: String,
        palette: SharePalette
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.background)

        val cardRect = RectF(110f, 140f, LANDSCAPE_WIDTH - 110f, LANDSCAPE_HEIGHT - 140f)
        drawRoundedPanel(canvas, cardRect, 42f, palette)

        val brandPaint = paint(
            color = palette.tertiaryText,
            textSize = 20f,
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD),
            letterSpacing = 0.06f
        )
        canvas.drawText(
            "ᚱ  Runatal · Elder Futhark",
            cardRect.left + 38f,
            cardRect.top + 46f,
            brandPaint
        )

        val centerX = cardRect.centerX()
        var currentY = cardRect.top + 180f

        val quotePaint = paint(
            color = palette.primaryText,
            textSize = 44f,
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC),
            textAlign = Paint.Align.CENTER
        )
        val quoteLines = wrapText("“$latinText”", quotePaint, (cardRect.width() - 220f).toInt())
        currentY = drawCenteredLines(canvas, quoteLines, centerX, currentY, 54f, quotePaint)
        currentY += 34f

        drawAuthorRule(canvas, centerX, currentY, author, palette)
        currentY += 66f

        val runicPaint = paint(
            color = palette.tertiaryText,
            textSize = 20f,
            typeface = Typeface.MONOSPACE,
            textAlign = Paint.Align.CENTER,
            letterSpacing = 0.06f
        )
        canvas.drawText(
            truncateToWidth(runicText, runicPaint, cardRect.width() - 220f),
            centerX,
            currentY,
            runicPaint
        )

        return bitmap
    }

    private fun drawRoundedPanel(
        canvas: Canvas,
        rect: RectF,
        radius: Float,
        palette: SharePalette
    ) {
        val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.surface
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, radius, radius, surfacePaint)

        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.outline
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, radius, radius, outlinePaint)
    }

    private fun drawRuleWithRune(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        width: Float,
        palette: SharePalette
    ) {
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.rule
            strokeWidth = 2f
        }
        val runePaint = paint(
            color = palette.tertiaryText,
            textSize = 34f,
            typeface = Typeface.MONOSPACE,
            textAlign = Paint.Align.CENTER
        )
        val gap = 38f
        canvas.drawLine(centerX - width / 2, centerY, centerX - gap, centerY, rulePaint)
        canvas.drawLine(centerX + gap, centerY, centerX + width / 2, centerY, rulePaint)
        canvas.drawText("ᚱ", centerX, centerY + 12f, runePaint)
    }

    private fun drawCenterRule(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        width: Float,
        color: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 2f
        }
        canvas.drawLine(centerX - width / 2, centerY, centerX + width / 2, centerY, paint)
    }

    private fun drawDecorativeDots(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        color: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        listOf(-22f to 6f, 0f to 8f, 22f to 6f).forEach { (offset, radius) ->
            canvas.drawCircle(centerX + offset, centerY, radius, paint)
        }
    }

    private fun drawDividerWithDots(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        color: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 2f
        }
        canvas.drawLine(centerX - 68f, centerY, centerX - 56f, centerY, paint)
        canvas.drawLine(centerX - 48f, centerY, centerX - 16f, centerY, paint)
        canvas.drawLine(centerX + 16f, centerY, centerX + 48f, centerY, paint)
        canvas.drawLine(centerX + 56f, centerY, centerX + 68f, centerY, paint)
    }

    private fun drawAuthorRule(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        author: String,
        palette: SharePalette
    ) {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.rule
            strokeWidth = 2f
        }
        val authorPaint = paint(
            color = palette.secondaryText,
            textSize = 24f,
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD),
            textAlign = Paint.Align.CENTER
        )

        canvas.drawLine(centerX - 180f, centerY, centerX - 118f, centerY, linePaint)
        canvas.drawLine(centerX + 118f, centerY, centerX + 180f, centerY, linePaint)
        canvas.drawText(author, centerX, centerY + 8f, authorPaint)
    }

    private fun drawCenteredLines(
        canvas: Canvas,
        lines: List<String>,
        centerX: Float,
        startY: Float,
        lineHeight: Float,
        paint: Paint
    ): Float {
        var currentY = startY
        lines.forEach { line ->
            canvas.drawText(line, centerX, currentY, paint)
            currentY += lineHeight
        }
        return currentY
    }

    private fun paletteFor(appearance: ShareAppearance): SharePalette {
        val palette = runicSharePalette(appearance)
        return SharePalette(
            background = palette.background.toArgb(),
            surface = palette.surface.toArgb(),
            primaryText = palette.primaryText.toArgb(),
            secondaryText = palette.secondaryText.toArgb(),
            tertiaryText = palette.tertiaryText.toArgb(),
            outline = palette.outline.toArgb(),
            rule = palette.rule.toArgb()
        )
    }

    private fun paint(
        color: Int,
        textSize: Float,
        typeface: Typeface,
        textAlign: Paint.Align = Paint.Align.LEFT,
        letterSpacing: Float = 0f
    ): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = textSize
            this.typeface = typeface
            this.textAlign = textAlign
            this.letterSpacing = letterSpacing
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        if (text.isEmpty()) {
            return emptyList()
        }

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        val lineBuilder = StringBuilder(text.length)
        val spaceWidth = paint.measureText(" ")
        var currentLineWidth = 0f

        words.forEach { word ->
            val wordWidth = paint.measureText(word)
            val widthWithWord = if (lineBuilder.isEmpty()) {
                wordWidth
            } else {
                currentLineWidth + spaceWidth + wordWidth
            }

            if (lineBuilder.isNotEmpty() && widthWithWord > maxWidth) {
                lines.add(lineBuilder.toString())
                lineBuilder.clear()
                lineBuilder.append(word)
                currentLineWidth = wordWidth
            } else {
                if (lineBuilder.isNotEmpty()) {
                    lineBuilder.append(' ')
                }
                lineBuilder.append(word)
                currentLineWidth = widthWithWord
            }
        }

        if (lineBuilder.isNotEmpty()) {
            lines.add(lineBuilder.toString())
        }

        return lines
    }

    private fun truncateToWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) {
            return text
        }

        val ellipsis = "…"
        var endIndex = text.length
        while (endIndex > 0) {
            val candidate = text.substring(0, endIndex).trimEnd() + ellipsis
            if (paint.measureText(candidate) <= maxWidth) {
                return candidate
            }
            endIndex -= 1
        }
        return ellipsis
    }
}
