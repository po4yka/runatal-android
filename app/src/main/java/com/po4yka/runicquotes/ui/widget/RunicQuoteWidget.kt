package com.po4yka.runicquotes.ui.widget

import android.content.Context
import android.content.res.Resources
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.po4yka.runicquotes.MainActivity
import com.po4yka.runicquotes.data.preferences.WidgetDisplayMode
import com.po4yka.runicquotes.data.preferences.WidgetUpdateMode
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.displayName
import com.po4yka.runicquotes.domain.model.getRunicText
import com.po4yka.runicquotes.domain.transliteration.CirthGlyphCompat
import com.po4yka.runicquotes.ui.components.buildRunicAccessibilityText
import com.po4yka.runicquotes.util.BitmapCache
import com.po4yka.runicquotes.util.RenderConfig
import com.po4yka.runicquotes.util.RenderTextAlign
import com.po4yka.runicquotes.util.RunicTextRenderer
import dagger.hilt.android.EntryPointAccessors
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private const val RUNIC_QUOTE_WIDGET_TAG = "RunicQuoteWidget"
private const val DEFAULT_WIDGET_WIDTH = 300
private const val DEFAULT_WIDGET_HEIGHT = 151

/**
 * Glance widget aligned to the Runatal Figma widget cluster.
 */
@Suppress("TooManyFunctions")
class RunicQuoteWidget() : GlanceAppWidget() {

    private var stateLoader: WidgetStateLoader = DefaultWidgetStateLoader()

    internal constructor(stateLoader: WidgetStateLoader) : this() {
        this.stateLoader = stateLoader
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = stateLoader.load(context, id)

        provideContent {
            WidgetContent(state)
        }
    }

    @Composable
    private fun WidgetContent(state: WidgetState) {
        val openAppAction: Action = actionStartActivity<MainActivity>()
        val primaryAction = if (state.displayMode == WidgetDisplayMode.DAILY_RANDOM_TAP) {
            actionRunCallback<RefreshQuoteAction>()
        } else {
            openAppAction
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(widgetColor(state.palette.background))
                .padding(12.dp)
                .semantics {
                    contentDescription = widgetAccessibilityDescription(state)
                }
                .clickable(onClick = primaryAction)
        ) {
            when {
                state.isLoading -> LoadingState(state)
                state.error != null -> ErrorState(state, openAppAction)
                else -> when (state.sizeClass) {
                    WidgetSizeClass.COMPACT -> CompactWidget(state)
                    WidgetSizeClass.MEDIUM -> MediumWidget(state)
                    WidgetSizeClass.EXPANDED -> LargeWidget(state)
                }
            }
        }
    }

    @Composable
    private fun LoadingState(state: WidgetState) {
        WidgetCard(state = state) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Loading quote...",
                    style = TextStyle(
                        color = widgetColor(state.palette.onSurfaceVariant),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }

    @Composable
    private fun ErrorState(state: WidgetState, openAppAction: Action) {
        WidgetCard(state = state) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Widget unavailable",
                    style = TextStyle(
                        color = widgetColor(state.palette.error),
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "Tap to open Runatal",
                    style = TextStyle(
                        color = widgetColor(state.palette.onSurfaceVariant),
                        fontSize = 10.sp
                    ),
                    modifier = GlanceModifier.clickable(onClick = openAppAction)
                )
            }
        }
    }

    @Composable
    private fun CompactWidget(state: WidgetState) {
        WidgetCard(state = state) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RuneBadge(
                    state = state,
                    size = 36.dp,
                    textSize = 13.sp
                )
                Spacer(modifier = GlanceModifier.width(14.dp))
                Column(
                    modifier = GlanceModifier.width(208.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RunicImage(
                        state = state,
                        height = 18.dp
                    )
                    if (state.author.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(3.dp))
                        Text(
                            text = "— ${state.author}",
                            style = TextStyle(
                                color = widgetColor(state.palette.onSurfaceVariant),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MediumWidget(state: WidgetState) {
        WidgetCard(state = state) {
            Column(
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RuneBadge(
                        state = state,
                        size = 24.dp,
                        textSize = 9.sp
                    )
                    Spacer(modifier = GlanceModifier.width(10.dp))
                    Text(
                        text = "Runatal · Quote of the Day",
                        style = TextStyle(
                            color = widgetColor(state.palette.onSurfaceVariant),
                            fontSize = 10.sp
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(12.dp))
                RunicImage(
                    state = state,
                    height = 24.dp
                )
                if (state.displayMode != WidgetDisplayMode.RUNE_ONLY && state.latinText.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    Text(
                        text = state.latinText,
                        style = TextStyle(
                            color = widgetColor(state.palette.onSurfaceVariant),
                            fontSize = 12.sp
                        ),
                        maxLines = 2
                    )
                }
                if (state.author.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    AuthorRow(state)
                }
            }
        }
    }

    @Composable
    private fun LargeWidget(state: WidgetState) {
        WidgetCard(
            state = state,
            paddingHorizontal = 0.dp,
            paddingVertical = 0.dp
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RuneBadge(
                            state = state,
                            size = 22.dp,
                            textSize = 7.sp
                        )
                        Spacer(modifier = GlanceModifier.width(10.dp))
                        Text(
                            text = "Runatal",
                            style = TextStyle(
                                color = widgetColor(state.palette.onSurface),
                                fontSize = 12.sp
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(64.dp))
                    ScriptChip(state)
                }
                DividerLine(state)
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    RunicImage(
                        state = state,
                        height = 52.dp
                    )
                    if (state.displayMode != WidgetDisplayMode.RUNE_ONLY && state.latinText.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(12.dp))
                        Text(
                            text = "\"${state.latinText}\"",
                            style = TextStyle(
                                color = widgetColor(state.palette.onSurfaceVariant),
                                fontSize = 12.sp
                            ),
                            maxLines = 3
                        )
                    }
                    if (state.author.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(10.dp))
                        AuthorRow(state)
                    }
                }
            }
        }
    }

    @Composable
    private fun RunicImage(state: WidgetState, height: androidx.compose.ui.unit.Dp) {
        if (state.runicBitmap != null) {
            Image(
                provider = ImageProvider(state.runicBitmap),
                contentDescription = widgetAccessibilityDescription(state),
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(height)
            )
        } else if (state.runicText.isNotEmpty()) {
            Text(
                text = state.runicText,
                style = TextStyle(
                    color = widgetColor(state.palette.runicText),
                    fontSize = if (state.sizeClass == WidgetSizeClass.EXPANDED) 15.sp else 13.sp
                ),
                maxLines = if (state.sizeClass == WidgetSizeClass.EXPANDED) 2 else 1
            )
        }
    }

    @Composable
    private fun AuthorRow(state: WidgetState) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = GlanceModifier
                    .width(18.dp)
                    .height(1.5.dp)
                    .background(widgetColor(state.palette.onSurfaceVariant))
            ) {}
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = state.author,
                style = TextStyle(
                    color = widgetColor(state.palette.onSurfaceVariant),
                    fontSize = 11.sp
                )
            )
        }
    }

    @Composable
    private fun ScriptChip(state: WidgetState) {
        Box(
            modifier = GlanceModifier
                .background(widgetColor(state.palette.surfaceMuted))
                .cornerRadius(10.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = state.scriptLabel,
                style = TextStyle(
                    color = widgetColor(state.palette.onSurfaceVariant),
                    fontSize = 9.sp
                )
            )
        }
    }

    @Composable
    private fun RuneBadge(
        state: WidgetState,
        size: androidx.compose.ui.unit.Dp,
        textSize: androidx.compose.ui.unit.TextUnit
    ) {
        Box(
            modifier = GlanceModifier
                .size(size)
                .background(widgetColor(state.palette.surfaceMuted))
                .cornerRadius(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\u16B1",
                style = TextStyle(
                    color = widgetColor(state.palette.onSurfaceVariant),
                    fontSize = textSize,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    @Composable
    private fun DividerLine(state: WidgetState) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(widgetColor(state.palette.outline))
        ) {}
    }

    @Composable
    private fun WidgetCard(
        state: WidgetState,
        paddingHorizontal: androidx.compose.ui.unit.Dp = 17.dp,
        paddingVertical: androidx.compose.ui.unit.Dp = 15.dp,
        content: @Composable () -> Unit
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(widgetColor(state.palette.outline))
                .cornerRadius(16.dp)
                .padding(1.dp)
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(widgetColor(state.palette.surface))
                    .cornerRadius(15.dp)
                    .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
            ) {
                content()
            }
        }
    }

    private fun widgetColor(color: Int): ColorProvider = ColorProvider(Color(color))
}

internal fun widgetAccessibilityDescription(state: WidgetState): String {
    return when {
        state.isLoading -> "Runatal widget loading quote"
        state.error != null -> "Runatal widget unavailable. Tap to open app."
        else -> buildRunicAccessibilityText(
            latinText = state.latinText.ifBlank { state.runicText },
            author = state.author,
            scriptLabel = state.scriptLabel,
            prefix = if (state.displayMode == WidgetDisplayMode.DAILY_RANDOM_TAP) {
                "Runatal widget. Double tap to refresh quote"
            } else {
                "Runatal widget. Double tap to open quote"
            }
        )
    }
}

internal interface WidgetStateLoader {
    suspend fun load(context: Context, id: GlanceId): WidgetState
}

internal class DefaultWidgetStateLoader : WidgetStateLoader {

    override suspend fun load(context: Context, id: GlanceId): WidgetState {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val quoteRepository = entryPoint.quoteRepository()
        val preferencesManager = entryPoint.userPreferencesManager()
        val ioDispatcher = entryPoint.ioDispatcher()

        val widgetKey = id.toString()
        val widgetSize = GlanceAppWidgetManager(context).getAppWidgetSizes(id).firstOrNull()
        val widgetWidth = widgetSize?.width?.value?.toInt() ?: DEFAULT_WIDGET_WIDTH
        val widgetHeight = widgetSize?.height?.value?.toInt() ?: DEFAULT_WIDGET_HEIGHT
        val sizeClass = RunicQuoteWidgetMetrics.resolveSizeClass(widgetHeight)

        return withContext(ioDispatcher) {
            try {
                val today = LocalDate.now()
                val preferences = preferencesManager.userPreferencesFlow.first()
                val displayMode = WidgetDisplayMode.fromPersistedValue(preferences.widgetDisplayMode)
                val updateMode = WidgetUpdateMode.fromPersistedValue(preferences.widgetUpdateMode)
                val randomRequested = WidgetInteractionState.consumeRandomQuoteRequest(widgetKey)
                val palette = resolveWidgetPalette(context, preferences)

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
                    quoteRepository.randomQuote()
                } else {
                    quoteRepository.quoteOfTheDay()
                }

                if (quote != null) {
                    val runicText = quote.getRunicText(
                        script = preferences.selectedScript,
                        transliterationFactory = entryPoint.transliterationFactory()
                    )
                    val normalizedRunicText = CirthGlyphCompat.normalizeLegacyPuaGlyphs(runicText)
                    val textSize = RunicQuoteWidgetMetrics.runicTextSize(sizeClass)
                    val maxWidth = RunicQuoteWidgetMetrics.maxRunicWidthPx(
                        resources = context.resources,
                        widgetWidthDp = widgetWidth,
                        sizeClass = sizeClass
                    )
                    val fontResource = RunicTextRenderer.getFontResource(preferences.selectedFont)
                    val cacheKey = BitmapCache.generateKey(
                        text = "${sizeClass.name}:$normalizedRunicText",
                        fontResource = fontResource,
                        textSize = textSize,
                        maxWidth = maxWidth
                    )
                    val runicBitmap = BitmapCache.get(cacheKey) ?: try {
                        val bitmap = RunicTextRenderer.renderTextToBitmap(
                            context = context,
                            config = RenderConfig(
                                text = normalizedRunicText,
                                fontResource = fontResource,
                                textSizeSp = textSize,
                                textColor = palette.runicText,
                                backgroundColor = null,
                                maxWidth = maxWidth,
                                textAlign = RenderTextAlign.START,
                                maxLines = if (sizeClass == WidgetSizeClass.EXPANDED) 2 else 1
                            )
                        )
                        BitmapCache.put(cacheKey, bitmap)
                        bitmap
                    } catch (e: IOException) {
                        Log.e(RUNIC_QUOTE_WIDGET_TAG, "Failed to render runic text bitmap", e)
                        null
                    } catch (e: OutOfMemoryError) {
                        Log.e(RUNIC_QUOTE_WIDGET_TAG, "Out of memory rendering widget bitmap", e)
                        null
                    }

                    val newState = WidgetState(
                        runicText = normalizedRunicText,
                        runicBitmap = runicBitmap,
                        latinText = quote.textLatin,
                        author = quote.author,
                        scriptLabel = preferences.selectedScript.displayName,
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
                        latinText = "No quote available",
                        scriptLabel = RunicScript.DEFAULT.displayName,
                        modeLabel = displayMode.displayName,
                        updateModeLabel = updateMode.displayName,
                        palette = palette,
                        sizeClass = sizeClass,
                        displayMode = displayMode
                    )
                }
            } catch (e: IOException) {
                Log.e(RUNIC_QUOTE_WIDGET_TAG, "IO error loading widget state", e)
                WidgetState(
                    latinText = "",
                    palette = WidgetPalette.default(),
                    sizeClass = sizeClass,
                    displayMode = WidgetDisplayMode.RUNE_LATIN,
                    error = e.message
                )
            }
        }
    }
}

internal object RunicQuoteWidgetMetrics {
    private const val COMPACT_WIDGET_HEIGHT = 100
    private const val MEDIUM_WIDGET_HEIGHT = 180
    private const val TEXT_SIZE_COMPACT = 12f
    private const val TEXT_SIZE_MEDIUM = 14f
    private const val TEXT_SIZE_LARGE = 15f

    fun runicTextSize(sizeClass: WidgetSizeClass): Float {
        return when (sizeClass) {
            WidgetSizeClass.COMPACT -> TEXT_SIZE_COMPACT
            WidgetSizeClass.MEDIUM -> TEXT_SIZE_MEDIUM
            WidgetSizeClass.EXPANDED -> TEXT_SIZE_LARGE
        }
    }

    fun maxRunicWidthPx(
        resources: Resources,
        widgetWidthDp: Int,
        sizeClass: WidgetSizeClass
    ): Int {
        val density = resources.displayMetrics.density
        val contentWidthDp = when (sizeClass) {
            WidgetSizeClass.COMPACT -> widgetWidthDp - 98
            WidgetSizeClass.MEDIUM -> widgetWidthDp - 70
            WidgetSizeClass.EXPANDED -> widgetWidthDp - 72
        }.coerceAtLeast(120)
        return (contentWidthDp * density).toInt()
    }

    fun resolveSizeClass(widgetHeight: Int): WidgetSizeClass {
        return when {
            widgetHeight < COMPACT_WIDGET_HEIGHT -> WidgetSizeClass.COMPACT
            widgetHeight < MEDIUM_WIDGET_HEIGHT -> WidgetSizeClass.MEDIUM
            else -> WidgetSizeClass.EXPANDED
        }
    }
}
