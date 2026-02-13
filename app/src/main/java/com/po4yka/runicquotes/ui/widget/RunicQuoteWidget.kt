package com.po4yka.runicquotes.ui.widget

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.po4yka.runicquotes.MainActivity
import com.po4yka.runicquotes.domain.model.getRunicText
import com.po4yka.runicquotes.util.BitmapCache
import com.po4yka.runicquotes.util.RenderConfig
import com.po4yka.runicquotes.util.RunicTextRenderer
import dagger.hilt.android.EntryPointAccessors
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Glance widget that displays a daily runic quote.
 */
class RunicQuoteWidget : GlanceAppWidget() {

    companion object {
        private const val TAG = "RunicQuoteWidget"
        private const val SMALL_WIDGET_WIDTH = 150
        private const val MEDIUM_WIDGET_WIDTH = 250
        private const val TEXT_SIZE_SMALL = 16f
        private const val TEXT_SIZE_MEDIUM = 20f
        private const val TEXT_SIZE_LARGE = 24f
        private const val MAX_WIDTH_FACTOR = 0.9f
        private const val DEFAULT_WIDGET_WIDTH = 200
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val quoteRepository = entryPoint.quoteRepository()
        val preferencesManager = entryPoint.userPreferencesManager()

        val widgetWidth = GlanceAppWidgetManager(context)
            .getAppWidgetSizes(id)
            .firstOrNull()
            ?.width
            ?.value
            ?.toInt()
            ?: DEFAULT_WIDGET_WIDTH
        val sizeClass = resolveSizeClass(widgetWidth)

        val state = withContext(Dispatchers.IO) {
            try {
                val today = LocalDate.now()
                val preferences = preferencesManager.userPreferencesFlow.first()
                val displayMode = WidgetDisplayMode.fromPersistedValue(preferences.widgetDisplayMode)
                val randomRequested = WidgetInteractionState.consumeRandomQuoteRequest()

                // Reuse cache only when we are not forcing a random quote refresh.
                if (!randomRequested) {
                    val cachedState = WidgetStateCache.get(today, preferences)
                    if (cachedState != null) {
                        return@withContext cachedState.copy(sizeClass = sizeClass)
                    }
                }

                val quote = if (
                    displayMode == WidgetDisplayMode.DAILY_RANDOM_TAP &&
                    randomRequested
                ) {
                    quoteRepository.randomQuote(preferences.selectedScript)
                } else {
                    quoteRepository.quoteOfTheDay(preferences.selectedScript)
                }

                if (quote != null) {
                    val runicText = quote.getRunicText(preferences.selectedScript)
                    val textSize = runicTextSize(widgetWidth)
                    val maxWidth = (
                        widgetWidth *
                            context.resources.displayMetrics.density *
                            MAX_WIDTH_FACTOR
                        ).toInt()

                    val fontResource = RunicTextRenderer.getFontResource(preferences.selectedFont)
                    val cacheKey = BitmapCache.generateKey(
                        text = runicText,
                        fontResource = fontResource,
                        textSize = textSize,
                        maxWidth = maxWidth
                    )
                    val runicBitmap = BitmapCache.get(cacheKey) ?: try {
                        val bitmap = RunicTextRenderer.renderTextToBitmap(
                            context = context,
                            config = RenderConfig(
                                text = runicText,
                                fontResource = fontResource,
                                textSizeSp = textSize,
                                textColor = Color.WHITE,
                                backgroundColor = null,
                                maxWidth = maxWidth
                            )
                        )
                        BitmapCache.put(cacheKey, bitmap)
                        bitmap
                    } catch (e: IOException) {
                        Log.e(TAG, "Failed to render runic text bitmap", e)
                        null
                    } catch (e: OutOfMemoryError) {
                        Log.e(TAG, "Out of memory rendering bitmap", e)
                        null
                    }

                    val newState = WidgetState(
                        runicText = runicText,
                        runicBitmap = runicBitmap,
                        latinText = quote.textLatin,
                        author = quote.author,
                        sizeClass = sizeClass,
                        displayMode = displayMode,
                        isLoading = false
                    )
                    WidgetStateCache.put(today, preferences, newState)
                    newState
                } else {
                    WidgetState(
                        runicText = "",
                        latinText = "No quote available",
                        author = "",
                        sizeClass = sizeClass,
                        displayMode = displayMode,
                        isLoading = false
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO error loading widget state", e)
                WidgetState(
                    runicText = "",
                    latinText = "",
                    author = "",
                    sizeClass = sizeClass,
                    displayMode = WidgetDisplayMode.RUNE_LATIN,
                    isLoading = false,
                    error = e.message
                )
            }
        }

        provideContent {
            WidgetContent(state)
        }
    }

    @Composable
    private fun WidgetContent(state: WidgetState) {
        val cardClickAction: Action = if (state.displayMode == WidgetDisplayMode.DAILY_RANDOM_TAP) {
            actionRunCallback<RefreshQuoteAction>()
        } else {
            actionStartActivity<MainActivity>()
        }

        GlanceTheme {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.background)
                    .padding(16.dp)
                    .clickable(onClick = cardClickAction),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    state.isLoading -> LoadingState()
                    state.error != null -> ErrorState(state.error)
                    else -> LoadedState(state)
                }
            }
        }
    }

    @Composable
    private fun LoadingState() {
        Text(
            text = "Loading...",
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = 14.sp
            )
        )
    }

    @Composable
    private fun ErrorState(error: String) {
        val message = when {
            error.contains("database", ignoreCase = true) -> "Database error"
            error.contains("network", ignoreCase = true) -> "Network error"
            else -> "Error loading quote"
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = TextStyle(
                    color = GlanceTheme.colors.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp)
            )
            Text(
                text = "↻ Retry",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 14.sp
                ),
                modifier = GlanceModifier
                    .clickable(onClick = actionRunCallback<RefreshQuoteAction>())
                    .padding(8.dp)
            )
        }
    }

    @Composable
    private fun LoadedState(state: WidgetState) {
        if (state.runicBitmap != null) {
            Image(
                provider = ImageProvider(state.runicBitmap),
                contentDescription = "Runic quote: ${state.latinText}",
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        val showLatin = state.displayMode != WidgetDisplayMode.RUNE_ONLY
        val showAuthor = showLatin && state.sizeClass == WidgetSizeClass.EXPANDED
        val showTapHint = state.displayMode == WidgetDisplayMode.DAILY_RANDOM_TAP

        if (showLatin && state.latinText.isNotEmpty()) {
            Text(
                text = state.latinText,
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = if (state.sizeClass == WidgetSizeClass.COMPACT) 11.sp else 12.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier.padding(bottom = if (showAuthor) 4.dp else 8.dp)
            )
        }

        if (showAuthor && state.author.isNotEmpty()) {
            Text(
                text = "— ${state.author}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp)
            )
        }

        if (showTapHint) {
            Text(
                text = "Tap widget for random quote",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    private fun runicTextSize(widgetWidth: Int): Float {
        return when {
            widgetWidth < SMALL_WIDGET_WIDTH -> TEXT_SIZE_SMALL
            widgetWidth < MEDIUM_WIDGET_WIDTH -> TEXT_SIZE_MEDIUM
            else -> TEXT_SIZE_LARGE
        }
    }

    private fun resolveSizeClass(widgetWidth: Int): WidgetSizeClass {
        return when {
            widgetWidth < SMALL_WIDGET_WIDTH -> WidgetSizeClass.COMPACT
            widgetWidth < MEDIUM_WIDGET_WIDTH -> WidgetSizeClass.MEDIUM
            else -> WidgetSizeClass.EXPANDED
        }
    }
}
