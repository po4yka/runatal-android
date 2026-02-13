package com.po4yka.runicquotes.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import com.po4yka.runicquotes.R
import com.po4yka.runicquotes.domain.transliteration.CirthGlyphCompat
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

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
    private val typefaceCache = ConcurrentHashMap<Int, Typeface>()

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
        val normalizedText = CirthGlyphCompat.normalizeLegacyPuaGlyphs(config.text)

        val typeface = loadTypeface(context, config.fontResource)

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

        val padding = (textSizePx * PADDING_FACTOR).toInt()
        var measuredTextWidth = paint.measureText(normalizedText)

        // Apply max width if specified
        if (config.maxWidth > 0) {
            val drawableWidth = (config.maxWidth - (padding * 2)).coerceAtLeast(MIN_BITMAP_SIZE)
            if (measuredTextWidth > drawableWidth) {
                val scale = drawableWidth / measuredTextWidth
                paint.textSize = textSizePx * scale
                measuredTextWidth = paint.measureText(normalizedText)
            }
        }

        val fontMetrics = paint.fontMetricsInt
        var width = ceil(measuredTextWidth.toDouble()).toInt() + padding * 2
        var height = (fontMetrics.descent - fontMetrics.ascent) + padding * 2

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
        canvas.drawText(normalizedText, x, y, paint)

        return bitmap
    }

    private fun loadTypeface(context: Context, fontResource: Int): Typeface {
        typefaceCache[fontResource]?.let { return it }

        val loaded = try {
            ResourcesCompat.getFont(context, fontResource)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load font resource $fontResource", e)
            null
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Font resource $fontResource not found", e)
            null
        } ?: Typeface.DEFAULT

        typefaceCache[fontResource] = loaded
        return loaded
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
