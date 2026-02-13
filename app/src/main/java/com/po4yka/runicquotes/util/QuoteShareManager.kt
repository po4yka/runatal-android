package com.po4yka.runicquotes.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.po4yka.runicquotes.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sharing quotes as images.
 */
@Singleton
class QuoteShareManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageGenerator: QuoteImageGenerator
) {
    companion object {
        private const val TAG = "QuoteShareManager"
        private const val SHARE_DIR = "shared_quotes"
        private const val FILE_NAME = "runic_quote.png"
    }

    /**
     * Shares a quote as an image.
     *
     * @param runicText The runic transliteration
     * @param latinText The Latin text
     * @param author The quote author
     * @param template Share template style preset
     * @return true if sharing was initiated successfully
     */
    suspend fun shareQuoteAsImage(
        runicText: String,
        latinText: String,
        author: String,
        template: ShareTemplate = ShareTemplate.MINIMAL
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Generate image
            val bitmap = imageGenerator.generateQuoteImage(
                runicText = runicText,
                latinText = latinText,
                author = author,
                template = template
            )

            // Save to cache directory
            val shareDir = File(context.cacheDir, SHARE_DIR).apply {
                if (!exists()) mkdirs()
            }

            val imageFile = File(shareDir, FILE_NAME)
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            // Get URI using FileProvider
            val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
            val imageUri: Uri = FileProvider.getUriForFile(
                context,
                authority,
                imageFile
            )

            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, "$latinText\n— $author")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Launch share chooser
            val chooserIntent = Intent.createChooser(shareIntent, "Share Quote").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            withContext(Dispatchers.Main) {
                context.startActivity(chooserIntent)
            }

            true
        } catch (e: IOException) {
            Log.e(TAG, "IO error sharing quote as image", e)
            false
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No app found to handle share intent", e)
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception sharing image", e)
            false
        }
    }

    /**
     * Shares quote text only (without image).
     */
    fun shareQuoteText(latinText: String, author: String) {
        try {
            val shareText = "$latinText\n— $author"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Quote").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooserIntent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No app found to handle text share intent", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception sharing text", e)
        }
    }
}
