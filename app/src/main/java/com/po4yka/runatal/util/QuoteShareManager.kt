package com.po4yka.runatal.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.po4yka.runatal.BuildConfig
import com.po4yka.runatal.di.IoDispatcher
import com.po4yka.runatal.di.MainDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sharing quotes as images.
 */
@Singleton
class QuoteShareManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageGenerator: QuoteImageGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher
) {
    /** Sharing constants. */
    companion object {
        private const val TAG = "QuoteShareManager"
        private const val SHARE_DIR = "shared_quotes"
        private const val FILE_EXTENSION = "png"
        private const val MAX_CACHED_FILES = 12
        private const val UNSIGNED_BYTE_MASK = 0xff
        private const val HEX_PADDING = 0x100
        private const val HEX_RADIX = 16
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
        template: ShareTemplate = ShareTemplate.CARD,
        appearance: ShareAppearance = ShareAppearance.DARK
    ): Boolean = withContext(ioDispatcher) {
        try {
            val shareDir = File(context.cacheDir, SHARE_DIR).apply {
                if (!exists()) mkdirs()
            }
            val imageFile = File(
                shareDir,
                "${shareFileKey(runicText, latinText, author, template, appearance)}.$FILE_EXTENSION"
            )

            if (!imageFile.exists() || imageFile.length() == 0L) {
                val bitmap = imageGenerator.generateQuoteImage(
                    runicText = runicText,
                    latinText = latinText,
                    author = author,
                    template = template,
                    appearance = appearance
                )
                writeBitmap(bitmap = bitmap, imageFile = imageFile)
                pruneShareDir(shareDir)
            }

            val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
            val imageUri: Uri = FileProvider.getUriForFile(
                context,
                authority,
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, "$latinText\n— $author")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Quote").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            withContext(mainDispatcher) {
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
     * Copies quote text to the system clipboard.
     */
    fun copyQuoteToClipboard(latinText: String, author: String) {
        val clipText = "$latinText\n— $author"
        copyPlainTextToClipboard(
            label = "Runic quote",
            text = clipText
        )
    }

    /**
     * Copies any plain text to the system clipboard.
     */
    fun copyPlainTextToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
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

    private fun writeBitmap(bitmap: Bitmap, imageFile: File) {
        val tempFile = File(imageFile.parentFile, "${imageFile.name}.tmp")
        FileOutputStream(tempFile).use { outputStream ->
            val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            if (!compressed) {
                throw IOException("Failed to encode share image")
            }
        }
        if (imageFile.exists()) {
            imageFile.delete()
        }
        if (!tempFile.renameTo(imageFile)) {
            tempFile.delete()
            throw IOException("Failed to move share image into cache")
        }
    }

    private fun pruneShareDir(shareDir: File) {
        val cachedFiles = shareDir.listFiles()
            ?.filter { it.extension.equals(FILE_EXTENSION, ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

        cachedFiles.drop(MAX_CACHED_FILES).forEach { cachedFile ->
            cachedFile.delete()
        }
    }

    private fun shareFileKey(
        runicText: String,
        latinText: String,
        author: String,
        template: ShareTemplate,
        appearance: ShareAppearance
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val payload = listOf(
            template.name,
            appearance.name,
            runicText,
            latinText,
            author
        ).joinToString(separator = "\u0000")
        val bytes = digest.digest(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { byte ->
            ((byte.toInt() and UNSIGNED_BYTE_MASK) + HEX_PADDING).toString(HEX_RADIX).substring(1)
        }
    }
}
