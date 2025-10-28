package com.po4yka.runicquotes.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sharing quotes as images.
 */
@Singleton
class QuoteShareManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageGenerator: QuoteImageGenerator
) {
    companion object {
        private const val AUTHORITY = "com.po4yka.runicquotes.fileprovider"
        private const val SHARE_DIR = "shared_quotes"
        private const val FILE_NAME = "runic_quote.png"
    }

    /**
     * Shares a quote as an image.
     *
     * @param runicText The runic transliteration
     * @param latinText The Latin text
     * @param author The quote author
     * @return true if sharing was initiated successfully
     */
    suspend fun shareQuoteAsImage(
        runicText: String,
        latinText: String,
        author: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Generate image
            val bitmap = imageGenerator.generateQuoteImage(runicText, latinText, author)

            // Save to cache directory
            val shareDir = File(context.cacheDir, SHARE_DIR).apply {
                if (!exists()) mkdirs()
            }

            val imageFile = File(shareDir, FILE_NAME)
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            // Get URI using FileProvider
            val imageUri: Uri = FileProvider.getUriForFile(
                context,
                AUTHORITY,
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
        } catch (e: Exception) {
            e.printStackTrace()
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
