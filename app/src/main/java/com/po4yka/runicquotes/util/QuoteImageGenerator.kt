@file:Suppress("MagicNumber") // Image layout calculations and spacing constants

package com.po4yka.runicquotes.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.po4yka.runicquotes.R
import com.po4yka.runicquotes.domain.transliteration.CirthGlyphCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates images from quotes for sharing.
 */
@Singleton
class QuoteImageGenerator @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private data class ShareStyle(
        val backgroundStart: Int,
        val backgroundEnd: Int,
        val runicColor: Int,
        val latinColor: Int,
        val authorColor: Int,
        val addFrame: Boolean
    )

    companion object {
        private const val IMAGE_WIDTH = 1080
        private const val IMAGE_HEIGHT = 1920
        private const val PADDING = 120
        private const val RUNIC_TEXT_SIZE = 80f
        private const val LATIN_TEXT_SIZE = 48f
        private const val AUTHOR_TEXT_SIZE = 40f
    }

    /**
     * Generates a shareable image from quote data.
     *
     * @param runicText The runic transliteration
     * @param latinText The Latin text
     * @param author The quote author
     * @param template Share template style preset
     * @return Bitmap image ready for sharing
     */
    fun generateQuoteImage(
        runicText: String,
        latinText: String,
        author: String,
        template: ShareTemplate = ShareTemplate.MINIMAL
    ): Bitmap {
        val normalizedRunicText = CirthGlyphCompat.normalizeLegacyPuaGlyphs(runicText)
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val templateStyle = resolveStyle(template)

        // Background (gradient for richer templates)
        val backgroundPaint = Paint().apply {
            shader = LinearGradient(
                0f,
                0f,
                IMAGE_WIDTH.toFloat(),
                IMAGE_HEIGHT.toFloat(),
                templateStyle.backgroundStart,
                templateStyle.backgroundEnd,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), IMAGE_HEIGHT.toFloat(), backgroundPaint)

        // Paint for runic text
        val runicPaint = Paint().apply {
            color = templateStyle.runicColor
            textSize = RUNIC_TEXT_SIZE
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Paint for Latin text
        val latinPaint = Paint().apply {
            color = templateStyle.latinColor
            textSize = LATIN_TEXT_SIZE
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Paint for author
        val authorPaint = Paint().apply {
            color = templateStyle.authorColor
            textSize = AUTHOR_TEXT_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        if (templateStyle.addFrame) {
            val framePaint = Paint().apply {
                color = Color.argb(150, 120, 90, 50)
                style = Paint.Style.STROKE
                strokeWidth = 6f
                isAntiAlias = true
            }
            canvas.drawRect(
                PADDING / 2f,
                PADDING / 2f,
                IMAGE_WIDTH - PADDING / 2f,
                IMAGE_HEIGHT - PADDING / 2f,
                framePaint
            )
        }

        val centerX = IMAGE_WIDTH / 2f
        var currentY = IMAGE_HEIGHT / 2f

        // Draw runic text (wrapped)
        val runicLines = wrapText(normalizedRunicText, runicPaint, IMAGE_WIDTH - PADDING * 2)
        val runicHeight = runicLines.size * RUNIC_TEXT_SIZE * 1.2f

        // Draw Latin text (wrapped)
        val latinLines = wrapText(latinText, latinPaint, IMAGE_WIDTH - PADDING * 2)
        val latinHeight = latinLines.size * LATIN_TEXT_SIZE * 1.2f

        // Calculate total height and start position
        val totalHeight = runicHeight + latinHeight + AUTHOR_TEXT_SIZE * 1.5f + 100
        currentY = (IMAGE_HEIGHT - totalHeight) / 2f + RUNIC_TEXT_SIZE

        // Draw runic text
        runicLines.forEach { line ->
            canvas.drawText(line, centerX, currentY, runicPaint)
            currentY += RUNIC_TEXT_SIZE * 1.2f
        }

        currentY += 80f // Space between runic and Latin

        // Draw Latin text
        latinLines.forEach { line ->
            canvas.drawText(line, centerX, currentY, latinPaint)
            currentY += LATIN_TEXT_SIZE * 1.2f
        }

        currentY += 60f // Space before author

        // Draw author
        canvas.drawText("— $author", centerX, currentY, authorPaint)

        return bitmap
    }

    private fun resolveStyle(template: ShareTemplate): ShareStyle {
        return when (template) {
            ShareTemplate.MINIMAL -> ShareStyle(
                backgroundStart = ContextCompat.getColor(context, R.color.surface),
                backgroundEnd = ContextCompat.getColor(context, R.color.gray_200),
                runicColor = ContextCompat.getColor(context, R.color.on_surface),
                latinColor = ContextCompat.getColor(context, R.color.on_surface),
                authorColor = ContextCompat.getColor(context, R.color.on_surface_variant),
                addFrame = false
            )

            ShareTemplate.ORNATE -> ShareStyle(
                backgroundStart = Color.parseColor("#FAEFD9"),
                backgroundEnd = Color.parseColor("#EFD7B1"),
                runicColor = Color.parseColor("#402715"),
                latinColor = Color.parseColor("#5A3B23"),
                authorColor = Color.parseColor("#7A5A3C"),
                addFrame = true
            )

            ShareTemplate.HIGH_CONTRAST -> ShareStyle(
                backgroundStart = Color.BLACK,
                backgroundEnd = Color.BLACK,
                runicColor = Color.WHITE,
                latinColor = Color.WHITE,
                authorColor = Color.parseColor("#E0E0E0"),
                addFrame = false
            )
        }
    }

    /**
     * Wraps text to fit within a given width.
     */
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
}
