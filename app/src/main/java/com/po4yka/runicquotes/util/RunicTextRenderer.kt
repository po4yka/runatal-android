package com.po4yka.runicquotes.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.po4yka.runicquotes.R

/**
 * Utility class for rendering runic text to bitmaps.
 * This is necessary for widgets since Glance doesn't support custom fonts.
 */
object RunicTextRenderer {

    /**
     * Renders runic text to a bitmap using the specified font.
     *
     * @param context Application context
     * @param text The text to render
     * @param fontResource The font resource ID (e.g., R.font.noto_sans_runic)
     * @param textSizeSp Text size in SP
     * @param textColor Text color
     * @param backgroundColor Background color (or null for transparent)
     * @param maxWidth Maximum width in pixels (0 for no limit)
     * @return Bitmap containing the rendered text
     */
    fun renderTextToBitmap(
        context: Context,
        text: String,
        fontResource: Int,
        textSizeSp: Float = 20f,
        textColor: Int = Color.WHITE,
        backgroundColor: Int? = null,
        maxWidth: Int = 0
    ): Bitmap {
        // Load the custom font
        val typeface = try {
            ResourcesCompat.getFont(context, fontResource)
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        // Convert SP to pixels
        val textSizePx = textSizeSp * context.resources.displayMetrics.scaledDensity

        // Create paint for text
        val paint = Paint().apply {
            this.typeface = typeface
            this.textSize = textSizePx
            this.color = textColor
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Measure the text
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        // Calculate bitmap dimensions
        val padding = (textSizePx * 0.2f).toInt() // 20% padding
        var width = bounds.width() + padding * 2
        var height = bounds.height() + padding * 2

        // Apply max width if specified
        if (maxWidth > 0 && width > maxWidth) {
            // Need to wrap text or scale down
            // For simplicity, we'll scale down the font
            val scale = maxWidth.toFloat() / width
            paint.textSize = textSizePx * scale
            paint.getTextBounds(text, 0, text.length, bounds)
            width = bounds.width() + padding * 2
            height = bounds.height() + padding * 2
        }

        // Ensure minimum size
        width = width.coerceAtLeast(1)
        height = height.coerceAtLeast(1)

        // Create bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background if specified
        backgroundColor?.let {
            canvas.drawColor(it)
        }

        // Draw text centered
        val x = width / 2f
        val y = height / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, x, y, paint)

        return bitmap
    }

    /**
     * Gets the font resource ID based on the font name.
     */
    fun getFontResource(fontName: String): Int {
        return when (fontName.lowercase()) {
            "babelstone" -> R.font.babelstone_runic
            "babelstone_ruled" -> R.font.babelstone_runic_ruled
            else -> R.font.noto_sans_runic // Default
        }
    }
}
