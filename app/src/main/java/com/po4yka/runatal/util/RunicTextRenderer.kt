package com.po4yka.runatal.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import com.po4yka.runatal.R
import com.po4yka.runatal.domain.transliteration.CirthGlyphCompat
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
    val maxWidth: Int = 0,
    val textAlign: RenderTextAlign = RenderTextAlign.CENTER,
    val maxLines: Int = Int.MAX_VALUE
) {
    /** Default constants for [RenderConfig]. */
    companion object {
        private const val DEFAULT_TEXT_SIZE_SP = 20f
    }
}

/** Horizontal alignment options for bitmap-rendered runic text. */
enum class RenderTextAlign {
    START,
    CENTER
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

        val paint = TextPaint().apply {
            this.typeface = typeface
            this.textSize = textSizePx
            this.color = config.textColor
            isAntiAlias = true
        }

        val padding = (textSizePx * PADDING_FACTOR).toInt()
        val drawableWidth = if (config.maxWidth > 0) {
            (config.maxWidth - (padding * 2)).coerceAtLeast(MIN_BITMAP_SIZE)
        } else {
            ceil(paint.measureText(normalizedText).toDouble()).toInt().coerceAtLeast(MIN_BITMAP_SIZE)
        }

        val layout = StaticLayout.Builder
            .obtain(normalizedText, 0, normalizedText.length, paint, drawableWidth)
            .setAlignment(
                when (config.textAlign) {
                    RenderTextAlign.START -> Layout.Alignment.ALIGN_NORMAL
                    RenderTextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
                }
            )
            .setIncludePad(false)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(config.maxLines)
            .build()

        var width = layout.width + padding * 2
        var height = layout.height + padding * 2

        // Ensure minimum size
        width = width.coerceAtLeast(MIN_BITMAP_SIZE)
        height = height.coerceAtLeast(MIN_BITMAP_SIZE)

        // Create bitmap
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        // Draw background if specified
        config.backgroundColor?.let {
            canvas.drawColor(it)
        }

        canvas.withTranslation(padding.toFloat(), padding.toFloat()) {
            layout.draw(this)
        }

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
