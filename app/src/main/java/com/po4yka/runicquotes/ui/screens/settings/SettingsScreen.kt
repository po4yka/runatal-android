package com.po4yka.runicquotes.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.po4yka.runicquotes.R
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.ui.components.SettingItem
import com.po4yka.runicquotes.ui.components.SettingSection
import com.po4yka.runicquotes.ui.theme.LocalReduceMotion
import com.po4yka.runicquotes.ui.widget.WidgetDisplayMode
import com.po4yka.runicquotes.util.rememberHapticFeedback

/**
 * Settings screen for configuring app preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()
    val haptics = rememberHapticFeedback()
    val reducedMotion = LocalReduceMotion.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = true,
            enter = if (reducedMotion) {
                EnterTransition.None
            } else {
                fadeIn() + slideInVertically(initialOffsetY = { it / 6 })
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Runic Script Section
                SettingSection(title = "Runic Script") {
                    SettingItem(
                        title = stringResource(R.string.script_elder_futhark),
                        subtitle = "Elder Futhark (150-800 AD)",
                        selected = preferences.selectedScript == RunicScript.ELDER_FUTHARK,
                        onClick = {
                            haptics.lightToggle()
                            viewModel.updateSelectedScript(RunicScript.ELDER_FUTHARK)
                        }
                    )
                    SettingItem(
                        title = stringResource(R.string.script_younger_futhark),
                        subtitle = "Younger Futhark (800-1100 AD)",
                        selected = preferences.selectedScript == RunicScript.YOUNGER_FUTHARK,
                        onClick = {
                            haptics.lightToggle()
                            viewModel.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
                        }
                    )
                    SettingItem(
                        title = stringResource(R.string.script_cirth),
                        subtitle = "Tolkien's Cirth (Angerthas)",
                        selected = preferences.selectedScript == RunicScript.CIRTH,
                        onClick = {
                            haptics.lightToggle()
                            viewModel.updateSelectedScript(RunicScript.CIRTH)
                        }
                    )
                }

                HorizontalDivider()

                // Font Section
                SettingSection(title = "Font") {
                    SettingItem(
                        title = stringResource(R.string.font_noto_sans),
                        subtitle = "Clean and modern",
                        selected = preferences.selectedFont == "noto",
                        onClick = {
                            haptics.lightToggle()
                            viewModel.updateSelectedFont("noto")
                        }
                    )
                    SettingItem(
                        title = stringResource(R.string.font_babelstone),
                        subtitle = "Traditional appearance",
                        selected = preferences.selectedFont == "babelstone",
                        onClick = {
                            haptics.lightToggle()
                            viewModel.updateSelectedFont("babelstone")
                        }
                    )
                }

                HorizontalDivider()

                // Display Section
                SettingSection(title = "Display") {
                    SettingItem(
                        title = "Show Transliteration",
                        subtitle = "Display Latin text alongside runes",
                        trailing = {
                            Switch(
                                checked = preferences.showTransliteration,
                                onCheckedChange = {
                                    haptics.lightToggle()
                                    viewModel.updateShowTransliteration(it)
                                }
                            )
                        }
                    )
                }

                HorizontalDivider()

                SettingSection(title = "Widget Mode") {
                    WidgetDisplayMode.entries.forEach { mode ->
                        SettingItem(
                            title = mode.displayName,
                            subtitle = mode.subtitle,
                            selected = preferences.widgetDisplayMode == mode.persistedValue,
                            onClick = {
                                haptics.lightToggle()
                                viewModel.updateWidgetDisplayMode(mode)
                            }
                        )
                    }
                }

                HorizontalDivider()

                // Theme Section
                SettingSection(title = "Theme Mode") {
                    SettingItem(
                        title = "Light",
                        subtitle = "Bright UI surfaces",
                        selected = preferences.themeMode == "light",
                        onClick = {
                            haptics.lightToggle()
                            viewModel.updateThemeMode("light")
                        }
                    )
                    SettingItem(
                        title = "Dark",
                        subtitle = "Low-light UI surfaces",
                        selected = preferences.themeMode == "dark",
                        onClick = {
                            haptics.lightToggle()
                            viewModel.updateThemeMode("dark")
                        }
                    )
                    SettingItem(
                        title = "System",
                        subtitle = "Follow device appearance",
                        selected = preferences.themeMode == "system",
                        onClick = {
                            haptics.lightToggle()
                            viewModel.updateThemeMode("system")
                        }
                    )
                }

                HorizontalDivider()

                // Theme Pack Section
                SettingSection(title = "Theme Pack") {
                    SettingItem(
                        title = "Stone",
                        subtitle = "Cool mineral tones",
                        selected = preferences.themePack == "stone",
                        onClick = {
                            haptics.lightToggle()
                            viewModel.updateThemePack("stone")
                        }
                    )
                    SettingItem(
                        title = "Parchment",
                        subtitle = "Warm manuscript tones",
                        selected = preferences.themePack == "parchment",
                        onClick = {
                            haptics.lightToggle()
                            viewModel.updateThemePack("parchment")
                        }
                    )
                    SettingItem(
                        title = "Night Ink",
                        subtitle = "Deep blue-black contrast",
                        selected = preferences.themePack == "night_ink",
                        onClick = {
                            haptics.lightToggle()
                            viewModel.updateThemePack("night_ink")
                        }
                    )
                }

                HorizontalDivider()

                SettingSection(title = "Accessibility Presets") {
                    SettingItem(
                        title = "Large Runes",
                        subtitle = "Increase rune size across core reading surfaces",
                        trailing = {
                            Switch(
                                checked = preferences.largeRunesEnabled,
                                onCheckedChange = {
                                    haptics.lightToggle()
                                    viewModel.updateLargeRunesEnabled(it)
                                }
                            )
                        }
                    )
                    SettingItem(
                        title = "High Contrast",
                        subtitle = "Use stronger foreground/background contrast",
                        trailing = {
                            Switch(
                                checked = preferences.highContrastEnabled,
                                onCheckedChange = {
                                    haptics.lightToggle()
                                    viewModel.updateHighContrastEnabled(it)
                                }
                            )
                        }
                    )
                    SettingItem(
                        title = "Reduced Motion",
                        subtitle = "Minimize transitions and animated reveals",
                        trailing = {
                            Switch(
                                checked = preferences.reducedMotionEnabled,
                                onCheckedChange = {
                                    haptics.lightToggle()
                                    viewModel.updateReducedMotionEnabled(it)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
