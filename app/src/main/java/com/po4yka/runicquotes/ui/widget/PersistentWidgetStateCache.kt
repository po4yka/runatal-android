@file:Suppress("LongParameterList", "ReturnCount")

package com.po4yka.runicquotes.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.WidgetDisplayMode
import com.po4yka.runicquotes.util.BitmapCache
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * File-backed cache used to restore widget content quickly after process death.
 */
internal object PersistentWidgetStateCache {
    private const val CACHE_DIR = "widget_state"
    private const val METADATA_EXTENSION = "json"
    private const val BITMAP_EXTENSION = "png"
    private const val UNSIGNED_BYTE_MASK = 0xff
    private const val HEX_PADDING = 0x100
    private const val HEX_RADIX = 16

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Serializable
    private data class CacheRecord(
        val dateEpochDay: Long,
        val widgetWidth: Int,
        val widgetHeight: Int,
        val selectedScript: String,
        val selectedFont: String,
        val displayModePreference: String,
        val updateModePreference: String,
        val themeMode: String,
        val themePack: String,
        val highContrastEnabled: Boolean,
        val dynamicColorEnabled: Boolean,
        val runicText: String,
        val latinText: String,
        val author: String,
        val scriptLabel: String,
        val modeLabel: String,
        val updateModeLabel: String,
        val displayMode: String,
        val error: String? = null,
        val bitmapCacheKey: String? = null
    )

    fun get(
        context: Context,
        widgetKey: String,
        currentDate: LocalDate,
        preferences: UserPreferences,
        widgetWidth: Int,
        widgetHeight: Int,
        palette: WidgetPalette,
        sizeClass: WidgetSizeClass
    ): WidgetState? {
        val metadataFile = metadataFile(context, widgetKey)
        if (!metadataFile.exists()) {
            return null
        }

        val record = runCatching {
            json.decodeFromString(CacheRecord.serializer(), metadataFile.readText())
        }.getOrElse {
            clear(context, widgetKey)
            return null
        }

        val isValid = record.dateEpochDay == currentDate.toEpochDay() &&
            record.widgetWidth == widgetWidth &&
            record.widgetHeight == widgetHeight &&
            record.selectedScript == preferences.selectedScript.name &&
            record.selectedFont == preferences.selectedFont &&
            record.displayModePreference == preferences.widgetDisplayMode &&
            record.updateModePreference == preferences.widgetUpdateMode &&
            record.themeMode == preferences.themeMode &&
            record.themePack == preferences.themePack &&
            record.highContrastEnabled == preferences.highContrastEnabled &&
            record.dynamicColorEnabled == preferences.dynamicColorEnabled
        if (!isValid) {
            return null
        }

        val runicBitmap = loadBitmap(
            context = context,
            widgetKey = widgetKey,
            bitmapCacheKey = record.bitmapCacheKey
        )

        return WidgetState(
            runicText = record.runicText,
            runicBitmap = runicBitmap,
            latinText = record.latinText,
            author = record.author,
            scriptLabel = record.scriptLabel,
            modeLabel = record.modeLabel,
            updateModeLabel = record.updateModeLabel,
            palette = palette,
            sizeClass = sizeClass,
            displayMode = WidgetDisplayMode.valueOf(record.displayMode),
            isLoading = false,
            error = record.error
        )
    }

    fun put(
        context: Context,
        widgetKey: String,
        date: LocalDate,
        preferences: UserPreferences,
        widgetWidth: Int,
        widgetHeight: Int,
        state: WidgetState,
        bitmapCacheKey: String?
    ) {
        cacheRoot(context).mkdirs()

        val bitmapFile = bitmapFile(context, widgetKey)
        if (state.runicBitmap != null) {
            writeBitmap(bitmapFile, state.runicBitmap)
        } else if (bitmapFile.exists()) {
            bitmapFile.delete()
        }

        val record = CacheRecord(
            dateEpochDay = date.toEpochDay(),
            widgetWidth = widgetWidth,
            widgetHeight = widgetHeight,
            selectedScript = preferences.selectedScript.name,
            selectedFont = preferences.selectedFont,
            displayModePreference = preferences.widgetDisplayMode,
            updateModePreference = preferences.widgetUpdateMode,
            themeMode = preferences.themeMode,
            themePack = preferences.themePack,
            highContrastEnabled = preferences.highContrastEnabled,
            dynamicColorEnabled = preferences.dynamicColorEnabled,
            runicText = state.runicText,
            latinText = state.latinText,
            author = state.author,
            scriptLabel = state.scriptLabel,
            modeLabel = state.modeLabel,
            updateModeLabel = state.updateModeLabel,
            displayMode = state.displayMode.name,
            error = state.error,
            bitmapCacheKey = bitmapCacheKey
        )
        metadataFile(context, widgetKey).writeText(json.encodeToString(CacheRecord.serializer(), record))
    }

    fun clear(context: Context, widgetKey: String) {
        metadataFile(context, widgetKey).delete()
        bitmapFile(context, widgetKey).delete()
    }

    fun clear(context: Context) {
        cacheRoot(context).deleteRecursively()
    }

    private fun loadBitmap(context: Context, widgetKey: String, bitmapCacheKey: String?): Bitmap? {
        bitmapCacheKey?.let(BitmapCache::get)?.let { return it }

        val persistedBitmap = BitmapFactory.decodeFile(bitmapFile(context, widgetKey).absolutePath) ?: return null
        bitmapCacheKey?.let { BitmapCache.put(it, persistedBitmap) }
        return persistedBitmap
    }

    private fun writeBitmap(file: File, bitmap: Bitmap) {
        val tempFile = File(file.parentFile, "${file.name}.tmp")
        FileOutputStream(tempFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        if (file.exists()) {
            file.delete()
        }
        tempFile.renameTo(file)
    }

    private fun cacheRoot(context: Context): File {
        return File(context.applicationContext.cacheDir, CACHE_DIR)
    }

    private fun metadataFile(context: Context, widgetKey: String): File {
        return File(cacheRoot(context), "${fileKey(widgetKey)}.$METADATA_EXTENSION")
    }

    private fun bitmapFile(context: Context, widgetKey: String): File {
        return File(cacheRoot(context), "${fileKey(widgetKey)}.$BITMAP_EXTENSION")
    }

    private fun fileKey(widgetKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(widgetKey.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { byte ->
            ((byte.toInt() and UNSIGNED_BYTE_MASK) + HEX_PADDING).toString(HEX_RADIX).substring(1)
        }
    }
}
