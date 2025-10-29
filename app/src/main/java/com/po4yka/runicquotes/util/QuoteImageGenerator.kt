@file:Suppress("MagicNumber") // Image layout calculations and spacing constants

package com.po4yka.runicquotes.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.po4yka.runicquotes.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Generates images from quotes for sharing.
 */
@Singleton
class QuoteImageGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
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
     * @return Bitmap image ready for sharing
     */
    fun generateQuoteImage(
        runicText: String,
        latinText: String,
        author: String
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val backgroundColor = ContextCompat.getColor(context, R.color.surface)
        canvas.drawColor(backgroundColor)

        // Paint for runic text
        val runicPaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.on_surface)
            textSize = RUNIC_TEXT_SIZE
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Paint for Latin text
        val latinPaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.on_surface)
            textSize = LATIN_TEXT_SIZE
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Paint for author
        val authorPaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.on_surface_variant)
            textSize = AUTHOR_TEXT_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val centerX = IMAGE_WIDTH / 2f
        var currentY = IMAGE_HEIGHT / 2f

        // Draw runic text (wrapped)
        val runicLines = wrapText(runicText, runicPaint, IMAGE_WIDTH - PADDING * 2)
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

    /**
     * Wraps text to fit within a given width.
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val bounds = Rect()
            paint.getTextBounds(testLine, 0, testLine.length, bounds)

            if (bounds.width() > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}
