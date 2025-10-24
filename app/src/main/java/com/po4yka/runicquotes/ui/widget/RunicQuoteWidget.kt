package com.po4yka.runicquotes.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Glance widget that displays a daily runic quote.
 */
class RunicQuoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Get dependencies via Hilt EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )

        val quoteRepository = entryPoint.quoteRepository()
        val preferencesManager = entryPoint.userPreferencesManager()

        // Load widget state
        val state = withContext(Dispatchers.IO) {
            try {
                val preferences = preferencesManager.userPreferencesFlow.first()
                val quote = quoteRepository.quoteOfTheDay(preferences.selectedScript)

                if (quote != null) {
                    val runicText = when (preferences.selectedScript) {
                        com.po4yka.runicquotes.domain.model.RunicScript.ELDER_FUTHARK ->
                            quote.runicElder ?: com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
                                .transliterate(quote.textLatin, preferences.selectedScript)
                        com.po4yka.runicquotes.domain.model.RunicScript.YOUNGER_FUTHARK ->
                            quote.runicYounger ?: com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
                                .transliterate(quote.textLatin, preferences.selectedScript)
                        com.po4yka.runicquotes.domain.model.RunicScript.CIRTH ->
                            quote.runicCirth ?: com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
                                .transliterate(quote.textLatin, preferences.selectedScript)
                    }

                    WidgetState(
                        runicText = runicText,
                        latinText = quote.textLatin,
                        author = quote.author,
                        isLoading = false
                    )
                } else {
                    WidgetState(
                        runicText = "",
                        latinText = "No quote available",
                        author = "",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                WidgetState(
                    runicText = "",
                    latinText = "",
                    author = "",
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
        GlanceTheme {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.background)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    state.isLoading -> {
                        Text(
                            text = "Loading...",
                            style = TextStyle(
                                color = GlanceTheme.colors.onBackground,
                                fontSize = 14.sp
                            )
                        )
                    }

                    state.error != null -> {
                        Text(
                            text = "Error loading quote",
                            style = TextStyle(
                                color = GlanceTheme.colors.error,
                                fontSize = 14.sp
                            )
                        )
                    }

                    else -> {
                        // Runic text
                        if (state.runicText.isNotEmpty()) {
                            Text(
                                text = state.runicText,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onBackground,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = GlanceModifier.padding(bottom = 8.dp)
                            )
                        }

                        // Latin text
                        if (state.latinText.isNotEmpty()) {
                            Text(
                                text = state.latinText,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onBackground,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = GlanceModifier.padding(bottom = 4.dp)
                            )
                        }

                        // Author
                        if (state.author.isNotEmpty()) {
                            Text(
                                text = "— ${state.author}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
