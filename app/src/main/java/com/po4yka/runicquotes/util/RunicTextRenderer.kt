package com.po4yka.runicquotes.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import com.po4yka.runicquotes.R
import java.io.IOException

/**
 * Configuration for rendering runic text to bitmaps.
 */
data class RenderConfig(
    val text: String,
    val fontResource: Int,
    val textSizeSp: Float = DEFAULT_TEXT_SIZE_SP,
    val textColor: Int = Color.WHITE,
    val backgroundColor: Int? = null,
    val maxWidth: Int = 0
) {
    companion object {
        private const val DEFAULT_TEXT_SIZE_SP = 20f
    }
}

/**
 * Utility class for rendering runic text to bitmaps.
 * This is necessary for widgets since Glance doesn't support custom fonts.
 */
object RunicTextRenderer {
    private const val TAG = "RunicTextRenderer"
    private const val PADDING_FACTOR = 0.2f
    private const val MIN_BITMAP_SIZE = 1

    /**
     * Renders runic text to a bitmap using the specified configuration.
     *
     * @param context Application context
     * @param config Rendering configuration
     * @return Bitmap containing the rendered text
     */
    fun renderTextToBitmap(
        context: Context,
        config: RenderConfig
    ): Bitmap {
        // Load the custom font
        val typeface = try {
            ResourcesCompat.getFont(context, config.fontResource)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load font resource ${config.fontResource}", e)
            Typeface.DEFAULT
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Font resource ${config.fontResource} not found", e)
            Typeface.DEFAULT
        }

        // Convert SP to pixels using TypedValue (scaledDensity is deprecated)
        val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            config.textSizeSp,
            context.resources.displayMetrics
        )

        // Create paint for text
        val paint = Paint().apply {
            this.typeface = typeface
            this.textSize = textSizePx
            this.color = config.textColor
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Measure the text
        val bounds = Rect()
        paint.getTextBounds(config.text, 0, config.text.length, bounds)

        // Calculate bitmap dimensions
        val padding = (textSizePx * PADDING_FACTOR).toInt()
        var width = bounds.width() + padding * 2
        var height = bounds.height() + padding * 2

        // Apply max width if specified
        if (config.maxWidth > 0 && width > config.maxWidth) {
            // Need to wrap text or scale down
            // For simplicity, we'll scale down the font
            val scale = config.maxWidth.toFloat() / width
            paint.textSize = textSizePx * scale
            paint.getTextBounds(config.text, 0, config.text.length, bounds)
            width = bounds.width() + padding * 2
            height = bounds.height() + padding * 2
        }

        // Ensure minimum size
        width = width.coerceAtLeast(MIN_BITMAP_SIZE)
        height = height.coerceAtLeast(MIN_BITMAP_SIZE)

        // Create bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background if specified
        config.backgroundColor?.let {
            canvas.drawColor(it)
        }

        // Draw text centered
        val x = width / 2f
        val y = height / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(config.text, x, y, paint)

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
