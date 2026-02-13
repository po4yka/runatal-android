package com.po4yka.runicquotes.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.R
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.ui.components.SettingItem
import com.po4yka.runicquotes.ui.components.SettingSection
import com.po4yka.runicquotes.ui.theme.LocalReduceMotion
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.ui.widget.WidgetDisplayMode
import com.po4yka.runicquotes.ui.widget.WidgetSyncManager
import com.po4yka.runicquotes.ui.widget.WidgetUpdateMode
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
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val haptics = rememberHapticFeedback()
    val reducedMotion = LocalReduceMotion.current
    val motion = RunicExpressiveTheme.motion
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val s = SettingsStrings(
        runicSection = stringResource(R.string.settings_section_runic_script),
        runicSubtitle = stringResource(R.string.settings_section_runic_script_subtitle),
        fontSection = stringResource(R.string.settings_section_font),
        fontSubtitle = stringResource(R.string.settings_section_font_subtitle),
        displaySection = stringResource(R.string.settings_section_display),
        displaySubtitle = stringResource(R.string.settings_section_display_subtitle),
        widgetSection = stringResource(R.string.settings_section_widget),
        widgetSubtitle = stringResource(R.string.settings_section_widget_subtitle),
        themeSection = stringResource(R.string.settings_section_theme),
        themeSubtitle = stringResource(R.string.settings_section_theme_subtitle),
        accessibilitySection = stringResource(R.string.settings_section_accessibility),
        accessibilitySubtitle = stringResource(R.string.settings_section_accessibility_subtitle),
        searchHint = stringResource(R.string.settings_search_hint),
        noResults = stringResource(R.string.settings_no_results),
        elderSubtitle = stringResource(R.string.settings_elder_subtitle),
        youngerSubtitle = stringResource(R.string.settings_younger_subtitle),
        cirthSubtitle = stringResource(R.string.settings_cirth_subtitle),
        fontNotoSubtitle = stringResource(R.string.settings_font_noto_subtitle),
        fontBabelstoneSubtitle = stringResource(R.string.settings_font_babelstone_subtitle),
        showTransliteration = stringResource(R.string.settings_show_transliteration_title),
        showTransliterationSubtitle = stringResource(R.string.settings_show_transliteration_subtitle),
        widgetModeSection = stringResource(R.string.settings_widget_mode_section),
        widgetFrequencySection = stringResource(R.string.settings_widget_frequency_section),
        themeModeSection = stringResource(R.string.settings_theme_mode_section),
        light = stringResource(R.string.settings_light),
        lightSubtitle = stringResource(R.string.settings_light_subtitle),
        dark = stringResource(R.string.settings_dark),
        darkSubtitle = stringResource(R.string.settings_dark_subtitle),
        system = stringResource(R.string.settings_system),
        systemSubtitle = stringResource(R.string.settings_system_subtitle),
        colorSourceSection = stringResource(R.string.settings_color_source_section),
        dynamicColor = stringResource(R.string.settings_dynamic_color_title),
        dynamicColorSubtitleSupported = stringResource(R.string.settings_dynamic_color_subtitle_supported),
        dynamicColorSubtitleUnsupported = stringResource(R.string.settings_dynamic_color_subtitle_unsupported),
        themePackSection = stringResource(R.string.settings_theme_pack_section),
        stone = stringResource(R.string.settings_stone),
        stoneSubtitle = stringResource(R.string.settings_stone_subtitle),
        parchment = stringResource(R.string.settings_parchment),
        parchmentSubtitle = stringResource(R.string.settings_parchment_subtitle),
        nightInk = stringResource(R.string.settings_night_ink),
        nightInkSubtitle = stringResource(R.string.settings_night_ink_subtitle),
        accessibilityPresetSection = stringResource(R.string.settings_accessibility_presets_section),
        largeRunes = stringResource(R.string.settings_large_runes_title),
        largeRunesSubtitle = stringResource(R.string.settings_large_runes_subtitle),
        highContrast = stringResource(R.string.settings_high_contrast_title),
        highContrastSubtitle = stringResource(R.string.settings_high_contrast_subtitle),
        reducedMotion = stringResource(R.string.settings_reduced_motion_title),
        reducedMotionSubtitle = stringResource(R.string.settings_reduced_motion_subtitle)
    )

    fun matches(vararg values: String): Boolean {
        val q = searchQuery.trim()
        if (q.isBlank()) return true
        return values.any { it.contains(q, ignoreCase = true) }
    }

    val showRunicSection = matches(
        s.runicSection,
        s.runicSubtitle,
        s.elderSubtitle,
        s.youngerSubtitle,
        s.cirthSubtitle,
        stringResource(R.string.script_elder_futhark),
        stringResource(R.string.script_younger_futhark),
        stringResource(R.string.script_cirth)
    )
    val showFontSection = matches(
        s.fontSection,
        s.fontSubtitle,
        s.fontNotoSubtitle,
        s.fontBabelstoneSubtitle,
        stringResource(R.string.font_noto_sans),
        stringResource(R.string.font_babelstone)
    )
    val showDisplaySection = matches(
        s.displaySection,
        s.displaySubtitle,
        s.showTransliteration,
        s.showTransliterationSubtitle
    )
    val showWidgetSection = matches(
        s.widgetSection,
        s.widgetSubtitle,
        s.widgetModeSection,
        s.widgetFrequencySection
    ) || WidgetDisplayMode.entries.any {
        matches(it.displayName, it.subtitle)
    } || WidgetUpdateMode.entries.any {
        matches(it.displayName, it.subtitle)
    }
    val showThemeSection = matches(
        s.themeSection,
        s.themeSubtitle,
        s.themeModeSection,
        s.light,
        s.dark,
        s.system,
        s.colorSourceSection,
        s.dynamicColor,
        s.dynamicColorSubtitleSupported,
        s.dynamicColorSubtitleUnsupported,
        s.themePackSection,
        s.stone,
        s.parchment,
        s.nightInk
    )
    val showAccessibilitySection = matches(
        s.accessibilitySection,
        s.accessibilitySubtitle,
        s.accessibilityPresetSection,
        s.largeRunes,
        s.highContrast,
        s.reducedMotion
    )
    val hasVisibleResults = showRunicSection ||
        showFontSection ||
        showDisplaySection ||
        showWidgetSection ||
        showThemeSection ||
        showAccessibilitySection

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
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
                                contentDescription = stringResource(R.string.settings_back)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = true,
            enter = if (reducedMotion) {
                EnterTransition.None
            } else {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = motion.duration(
                            reducedMotion = reducedMotion,
                            base = motion.mediumDurationMillis
                        ),
                        easing = motion.standardEasing
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = motion.duration(
                            reducedMotion = reducedMotion,
                            base = motion.mediumDurationMillis
                        ),
                        easing = motion.emphasizedEasing
                    ),
                    initialOffsetY = { it / 6 }
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    placeholder = { Text(s.searchHint) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = s.searchHint
                        )
                    },
                    singleLine = true
                )

                if (showRunicSection) {
                    SettingSection(
                        title = s.runicSection,
                        subtitle = s.runicSubtitle
                    ) {
                        if (matches(stringResource(R.string.script_elder_futhark), s.elderSubtitle)) {
                            SettingItem(
                                title = stringResource(R.string.script_elder_futhark),
                                subtitle = s.elderSubtitle,
                                selected = preferences.selectedScript == RunicScript.ELDER_FUTHARK,
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateSelectedScript(RunicScript.ELDER_FUTHARK)
                                    WidgetSyncManager.refreshAllAsync(context)
                                }
                            )
                        }
                        if (matches(stringResource(R.string.script_younger_futhark), s.youngerSubtitle)) {
                            SettingItem(
                                title = stringResource(R.string.script_younger_futhark),
                                subtitle = s.youngerSubtitle,
                                selected = preferences.selectedScript == RunicScript.YOUNGER_FUTHARK,
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
                                    WidgetSyncManager.refreshAllAsync(context)
                                }
                            )
                        }
                        if (matches(stringResource(R.string.script_cirth), s.cirthSubtitle)) {
                            SettingItem(
                                title = stringResource(R.string.script_cirth),
                                subtitle = s.cirthSubtitle,
                                selected = preferences.selectedScript == RunicScript.CIRTH,
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateSelectedScript(RunicScript.CIRTH)
                                    WidgetSyncManager.refreshAllAsync(context)
                                }
                            )
                        }
                    }
                }

                if (showFontSection) {
                    SettingSection(
                        title = s.fontSection,
                        subtitle = s.fontSubtitle
                    ) {
                        if (matches(stringResource(R.string.font_noto_sans), s.fontNotoSubtitle)) {
                            SettingItem(
                                title = stringResource(R.string.font_noto_sans),
                                subtitle = s.fontNotoSubtitle,
                                selected = preferences.selectedFont == "noto",
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateSelectedFont("noto")
                                    WidgetSyncManager.refreshAllAsync(context)
                                }
                            )
                        }
                        if (matches(stringResource(R.string.font_babelstone), s.fontBabelstoneSubtitle)) {
                            SettingItem(
                                title = stringResource(R.string.font_babelstone),
                                subtitle = s.fontBabelstoneSubtitle,
                                selected = preferences.selectedFont == "babelstone",
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateSelectedFont("babelstone")
                                    WidgetSyncManager.refreshAllAsync(context)
                                }
                            )
                        }
                    }
                }

                if (showDisplaySection) {
                    SettingSection(
                        title = s.displaySection,
                        subtitle = s.displaySubtitle
                    ) {
                        if (matches(s.showTransliteration, s.showTransliterationSubtitle)) {
                            SettingItem(
                                title = s.showTransliteration,
                                subtitle = s.showTransliterationSubtitle,
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateShowTransliteration(!preferences.showTransliteration)
                                },
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
                    }
                }

                if (showWidgetSection) {
                    SettingSection(
                        title = s.widgetSection,
                        subtitle = s.widgetSubtitle
                    ) {
                        Text(
                            text = s.widgetModeSection,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        WidgetDisplayMode.entries.forEach { mode ->
                            if (matches(mode.displayName, mode.subtitle)) {
                                SettingItem(
                                    title = mode.displayName,
                                    subtitle = mode.subtitle,
                                    selected = preferences.widgetDisplayMode == mode.persistedValue,
                                    onClick = {
                                        haptics.lightToggle()
                                        viewModel.updateWidgetDisplayMode(mode)
                                        WidgetSyncManager.refreshAllAsync(context)
                                    }
                                )
                            }
                        }

                        Text(
                            text = s.widgetFrequencySection,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        WidgetUpdateMode.entries.forEach { updateMode ->
                            if (matches(updateMode.displayName, updateMode.subtitle)) {
                                SettingItem(
                                    title = updateMode.displayName,
                                    subtitle = updateMode.subtitle,
                                    selected = preferences.widgetUpdateMode == updateMode.persistedValue,
                                    onClick = {
                                        haptics.lightToggle()
                                        viewModel.updateWidgetUpdateMode(updateMode)
                                        WidgetSyncManager.refreshAndRescheduleAsync(context)
                                    }
                                )
                            }
                        }
                    }
                }

                if (showThemeSection) {
                    SettingSection(
                        title = s.themeSection,
                        subtitle = s.themeSubtitle
                    ) {
                        Text(
                            text = s.themeModeSection,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        if (matches(s.light, s.lightSubtitle)) {
                            SettingItem(
                                title = s.light,
                                subtitle = s.lightSubtitle,
                                selected = preferences.themeMode == "light",
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateThemeMode("light")
                                    WidgetSyncManager.refreshAllAsync(context)
                                }
                            )
                        }
                        if (matches(s.dark, s.darkSubtitle)) {
                            SettingItem(
                                title = s.dark,
                                subtitle = s.darkSubtitle,
                                selected = preferences.themeMode == "dark",
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateThemeMode("dark")
                                    WidgetSyncManager.refreshAllAsync(context)
                                }
                            )
                        }
                        if (matches(s.system, s.systemSubtitle)) {
                            SettingItem(
                                title = s.system,
                                subtitle = s.systemSubtitle,
                                selected = preferences.themeMode == "system",
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateThemeMode("system")
                                    WidgetSyncManager.refreshAllAsync(context)
                                }
                            )
                        }

                        Text(
                            text = s.colorSourceSection,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        if (matches(s.dynamicColor, s.dynamicColorSubtitleSupported, s.dynamicColorSubtitleUnsupported)) {
                            SettingItem(
                                title = s.dynamicColor,
                                subtitle = if (dynamicColorSupported) {
                                    s.dynamicColorSubtitleSupported
                                } else {
                                    s.dynamicColorSubtitleUnsupported
                                },
                                onClick = if (dynamicColorSupported) {
                                    {
                                        haptics.lightToggle()
                                        viewModel.updateDynamicColorEnabled(!preferences.dynamicColorEnabled)
                                        WidgetSyncManager.refreshAllAsync(context)
                                    }
                                } else {
                                    null
                                },
                                trailing = {
                                    Switch(
                                        checked = preferences.dynamicColorEnabled && dynamicColorSupported,
                                        onCheckedChange = {
                                            haptics.lightToggle()
                                            viewModel.updateDynamicColorEnabled(it)
                                            WidgetSyncManager.refreshAllAsync(context)
                                        },
                                        enabled = dynamicColorSupported
                                    )
                                }
                            )
                        }

                        Text(
                            text = s.themePackSection,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        if (matches(s.stone, s.stoneSubtitle)) {
                            SettingItem(
                                title = s.stone,
                                subtitle = s.stoneSubtitle,
                                selected = preferences.themePack == "stone",
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateThemePack("stone")
                                    WidgetSyncManager.refreshAllAsync(context)
                                }
                            )
                        }
                        if (matches(s.parchment, s.parchmentSubtitle)) {
                            SettingItem(
                                title = s.parchment,
                                subtitle = s.parchmentSubtitle,
                                selected = preferences.themePack == "parchment",
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateThemePack("parchment")
                                    WidgetSyncManager.refreshAllAsync(context)
                                }
                            )
                        }
                        if (matches(s.nightInk, s.nightInkSubtitle)) {
                            SettingItem(
                                title = s.nightInk,
                                subtitle = s.nightInkSubtitle,
                                selected = preferences.themePack == "night_ink",
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateThemePack("night_ink")
                                    WidgetSyncManager.refreshAllAsync(context)
                                }
                            )
                        }
                    }
                }

                if (showAccessibilitySection) {
                    SettingSection(
                        title = s.accessibilitySection,
                        subtitle = s.accessibilitySubtitle
                    ) {
                        Text(
                            text = s.accessibilityPresetSection,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        if (matches(s.largeRunes, s.largeRunesSubtitle)) {
                            SettingItem(
                                title = s.largeRunes,
                                subtitle = s.largeRunesSubtitle,
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateLargeRunesEnabled(!preferences.largeRunesEnabled)
                                },
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
                        }
                        if (matches(s.highContrast, s.highContrastSubtitle)) {
                            SettingItem(
                                title = s.highContrast,
                                subtitle = s.highContrastSubtitle,
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateHighContrastEnabled(!preferences.highContrastEnabled)
                                    WidgetSyncManager.refreshAllAsync(context)
                                },
                                trailing = {
                                    Switch(
                                        checked = preferences.highContrastEnabled,
                                        onCheckedChange = {
                                            haptics.lightToggle()
                                            viewModel.updateHighContrastEnabled(it)
                                            WidgetSyncManager.refreshAllAsync(context)
                                        }
                                    )
                                }
                            )
                        }
                        if (matches(s.reducedMotion, s.reducedMotionSubtitle)) {
                            SettingItem(
                                title = s.reducedMotion,
                                subtitle = s.reducedMotionSubtitle,
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.updateReducedMotionEnabled(!preferences.reducedMotionEnabled)
                                },
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

                if (!hasVisibleResults) {
                    Text(
                        text = s.noResults,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }
            }
        }
    }
}

private data class SettingsStrings(
    val runicSection: String,
    val runicSubtitle: String,
    val fontSection: String,
    val fontSubtitle: String,
    val displaySection: String,
    val displaySubtitle: String,
    val widgetSection: String,
    val widgetSubtitle: String,
    val themeSection: String,
    val themeSubtitle: String,
    val accessibilitySection: String,
    val accessibilitySubtitle: String,
    val searchHint: String,
    val noResults: String,
    val elderSubtitle: String,
    val youngerSubtitle: String,
    val cirthSubtitle: String,
    val fontNotoSubtitle: String,
    val fontBabelstoneSubtitle: String,
    val showTransliteration: String,
    val showTransliterationSubtitle: String,
    val widgetModeSection: String,
    val widgetFrequencySection: String,
    val themeModeSection: String,
    val light: String,
    val lightSubtitle: String,
    val dark: String,
    val darkSubtitle: String,
    val system: String,
    val systemSubtitle: String,
    val colorSourceSection: String,
    val dynamicColor: String,
    val dynamicColorSubtitleSupported: String,
    val dynamicColorSubtitleUnsupported: String,
    val themePackSection: String,
    val stone: String,
    val stoneSubtitle: String,
    val parchment: String,
    val parchmentSubtitle: String,
    val nightInk: String,
    val nightInkSubtitle: String,
    val accessibilityPresetSection: String,
    val largeRunes: String,
    val largeRunesSubtitle: String,
    val highContrast: String,
    val highContrastSubtitle: String,
    val reducedMotion: String,
    val reducedMotionSubtitle: String
)
