package com.po4yka.runicquotes.ui.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
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
        private const val SMALL_WIDGET_HEIGHT = 120
        private const val MEDIUM_WIDGET_WIDTH = 250
        private const val MEDIUM_WIDGET_HEIGHT = 180
        private const val TEXT_SIZE_SMALL = 14f
        private const val TEXT_SIZE_MEDIUM = 20f
        private const val TEXT_SIZE_LARGE = 24f
        private const val MAX_WIDTH_FACTOR = 0.9f
        private const val DEFAULT_WIDGET_WIDTH = 200
        private const val DEFAULT_WIDGET_HEIGHT = 150
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val quoteRepository = entryPoint.quoteRepository()
        val preferencesManager = entryPoint.userPreferencesManager()

        val widgetKey = id.toString()
        val widgetSize = GlanceAppWidgetManager(context)
            .getAppWidgetSizes(id)
            .firstOrNull()
        val widgetWidth = widgetSize?.width?.value?.toInt() ?: DEFAULT_WIDGET_WIDTH
        val widgetHeight = widgetSize?.height?.value?.toInt() ?: DEFAULT_WIDGET_HEIGHT
        val sizeClass = resolveSizeClass(widgetWidth, widgetHeight)

        val state = withContext(Dispatchers.IO) {
            try {
                val today = LocalDate.now()
                val preferences = preferencesManager.userPreferencesFlow.first()
                val displayMode = WidgetDisplayMode.fromPersistedValue(preferences.widgetDisplayMode)
                val updateMode = WidgetUpdateMode.fromPersistedValue(preferences.widgetUpdateMode)
                val randomRequested = WidgetInteractionState.consumeRandomQuoteRequest(widgetKey)
                val palette = resolveWidgetPalette(context, preferences)

                // Reuse cache only when we are not forcing a random quote refresh.
                if (!randomRequested) {
                    val cachedState = WidgetStateCache.get(
                        widgetKey = widgetKey,
                        currentDate = today,
                        preferences = preferences,
                        widgetWidth = widgetWidth,
                        widgetHeight = widgetHeight
                    )
                    if (cachedState != null) {
                        return@withContext cachedState.copy(
                            sizeClass = sizeClass,
                            palette = palette
                        )
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
                    val textSize = runicTextSize(widgetWidth, widgetHeight)
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
                                textColor = palette.runicText,
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
                        scriptLabel = scriptLabel(preferences.selectedScript.name),
                        modeLabel = displayMode.displayName,
                        updateModeLabel = updateMode.displayName,
                        palette = palette,
                        sizeClass = sizeClass,
                        displayMode = displayMode,
                        isLoading = false
                    )
                    WidgetStateCache.put(
                        widgetKey = widgetKey,
                        date = today,
                        preferences = preferences,
                        widgetWidth = widgetWidth,
                        widgetHeight = widgetHeight,
                        state = newState
                    )
                    newState
                } else {
                    WidgetState(
                        runicText = "",
                        latinText = "No quote available",
                        author = "",
                        scriptLabel = scriptLabel(preferences.selectedScript.name),
                        modeLabel = displayMode.displayName,
                        updateModeLabel = updateMode.displayName,
                        palette = palette,
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
                    scriptLabel = "",
                    modeLabel = "Unavailable",
                    updateModeLabel = "",
                    palette = WidgetPalette.default(),
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
        val openAppAction: Action = actionStartActivity<MainActivity>()
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(widgetColor(state.palette.background))
                .padding(16.dp)
                .clickable(onClick = openAppAction),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                state.isLoading -> LoadingState(state)
                state.error != null -> ErrorState(state)
                else -> LoadedState(state, openAppAction)
            }
        }
    }

    @Composable
    private fun LoadingState(state: WidgetState) {
        Text(
                text = "Loading...",
                style = TextStyle(
                    color = widgetColor(state.palette.onBackground),
                    fontSize = 14.sp
                )
            )
    }

    @Composable
    private fun ErrorState(state: WidgetState) {
        val error = state.error.orEmpty()
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
                    color = widgetColor(state.palette.error),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp)
            )
            Text(
                text = "↻ Retry",
                style = TextStyle(
                    color = widgetColor(state.palette.primary),
                    fontSize = 14.sp
                ),
                modifier = GlanceModifier
                    .clickable(onClick = actionRunCallback<RefreshQuoteAction>())
                    .padding(8.dp)
            )
        }
    }

    @Composable
    private fun LoadedState(state: WidgetState, openAppAction: Action) {
        val showContextBadge = state.sizeClass != WidgetSizeClass.COMPACT
        if (showContextBadge) {
            Text(
                text = "${state.scriptLabel} • ${state.modeLabel}",
                style = TextStyle(
                    color = widgetColor(state.palette.onPrimaryContainer),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier
                    .background(widgetColor(state.palette.primaryContainer))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Text(
                text = state.updateModeLabel,
                style = TextStyle(
                    color = widgetColor(state.palette.primary),
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        if (state.runicBitmap != null) {
            Image(
                provider = ImageProvider(state.runicBitmap),
                contentDescription = "Runic quote: ${state.latinText}",
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        } else if (state.runicText.isNotEmpty()) {
            Text(
                text = state.runicText,
                style = TextStyle(
                    color = widgetColor(state.palette.runicText),
                    fontSize = when (state.sizeClass) {
                        WidgetSizeClass.COMPACT -> 13.sp
                        WidgetSizeClass.MEDIUM -> 16.sp
                        WidgetSizeClass.EXPANDED -> 18.sp
                    },
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp)
            )
        }

        val showLatin = state.displayMode != WidgetDisplayMode.RUNE_ONLY
        val showAuthor = showLatin && state.sizeClass == WidgetSizeClass.EXPANDED

        if (showLatin && state.latinText.isNotEmpty()) {
            Text(
                text = state.latinText,
                style = TextStyle(
                    color = widgetColor(state.palette.onBackground),
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
                    color = widgetColor(state.palette.onSurface),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp)
            )
        }

        ActionRow(
            state = state,
            openAppAction = openAppAction
        )
    }

    @Composable
    private fun ActionRow(
        state: WidgetState,
        openAppAction: Action
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Open",
                style = TextStyle(
                    color = widgetColor(state.palette.onPrimaryContainer),
                    fontSize = 10.sp
                ),
                modifier = GlanceModifier
                    .background(widgetColor(state.palette.primaryContainer))
                    .clickable(onClick = openAppAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            if (state.displayMode == WidgetDisplayMode.DAILY_RANDOM_TAP) {
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "Random",
                    style = TextStyle(
                        color = widgetColor(state.palette.onPrimaryContainer),
                        fontSize = 10.sp
                    ),
                    modifier = GlanceModifier
                        .background(widgetColor(state.palette.primaryContainer))
                        .clickable(onClick = actionRunCallback<RefreshQuoteAction>())
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    private fun runicTextSize(widgetWidth: Int, widgetHeight: Int): Float {
        return when {
            widgetWidth < SMALL_WIDGET_WIDTH || widgetHeight < SMALL_WIDGET_HEIGHT -> TEXT_SIZE_SMALL
            widgetWidth < MEDIUM_WIDGET_WIDTH || widgetHeight < MEDIUM_WIDGET_HEIGHT -> TEXT_SIZE_MEDIUM
            else -> TEXT_SIZE_LARGE
        }
    }

    private fun resolveSizeClass(widgetWidth: Int, widgetHeight: Int): WidgetSizeClass {
        return when {
            widgetWidth < SMALL_WIDGET_WIDTH || widgetHeight < SMALL_WIDGET_HEIGHT ->
                WidgetSizeClass.COMPACT
            widgetWidth < MEDIUM_WIDGET_WIDTH || widgetHeight < MEDIUM_WIDGET_HEIGHT ->
                WidgetSizeClass.MEDIUM
            else -> WidgetSizeClass.EXPANDED
        }
    }

    private fun scriptLabel(scriptName: String): String {
        return when (scriptName) {
            "YOUNGER_FUTHARK" -> "Younger"
            "CIRTH" -> "Cirth"
            else -> "Elder"
        }
    }

    private fun widgetColor(color: Int): ColorProvider {
        return ColorProvider(Color(color))
    }
}
