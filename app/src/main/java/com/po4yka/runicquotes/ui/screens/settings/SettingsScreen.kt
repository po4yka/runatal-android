package com.po4yka.runicquotes.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.R
import com.po4yka.runicquotes.RunicQuotesApplication
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.ui.components.SettingItem
import com.po4yka.runicquotes.ui.components.SettingSection
import com.po4yka.runicquotes.util.rememberHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val haptics = rememberHapticFeedback()
    val context = LocalContext.current
    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingSection(
                title = stringResource(R.string.settings_section_runic_script),
                subtitle = stringResource(R.string.settings_section_runic_script_subtitle)
            ) {
                SettingItem(
                    title = stringResource(R.string.script_elder_futhark),
                    subtitle = stringResource(R.string.settings_elder_subtitle),
                    selected = preferences.selectedScript == RunicScript.ELDER_FUTHARK,
                    onClick = {
                        haptics.lightToggle()
                        viewModel.updateSelectedScript(RunicScript.ELDER_FUTHARK)
                        RunicQuotesApplication.widgetSyncManager(context).refreshAllAsync(context)
                    }
                )
                SettingItem(
                    title = stringResource(R.string.script_younger_futhark),
                    subtitle = stringResource(R.string.settings_younger_subtitle),
                    selected = preferences.selectedScript == RunicScript.YOUNGER_FUTHARK,
                    onClick = {
                        haptics.lightToggle()
                        viewModel.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
                        RunicQuotesApplication.widgetSyncManager(context).refreshAllAsync(context)
                    }
                )
                SettingItem(
                    title = stringResource(R.string.script_cirth),
                    subtitle = stringResource(R.string.settings_cirth_subtitle),
                    selected = preferences.selectedScript == RunicScript.CIRTH,
                    onClick = {
                        haptics.lightToggle()
                        viewModel.updateSelectedScript(RunicScript.CIRTH)
                        RunicQuotesApplication.widgetSyncManager(context).refreshAllAsync(context)
                    }
                )
            }

            SettingSection(
                title = stringResource(R.string.settings_section_theme),
                subtitle = stringResource(R.string.settings_section_theme_subtitle)
            ) {
                SettingItem(
                    title = stringResource(R.string.settings_light),
                    subtitle = stringResource(R.string.settings_light_subtitle),
                    selected = preferences.themeMode == "light",
                    onClick = {
                        haptics.lightToggle()
                        viewModel.updateThemeMode("light")
                        RunicQuotesApplication.widgetSyncManager(context).refreshAllAsync(context)
                    }
                )
                SettingItem(
                    title = stringResource(R.string.settings_dark),
                    subtitle = stringResource(R.string.settings_dark_subtitle),
                    selected = preferences.themeMode == "dark",
                    onClick = {
                        haptics.lightToggle()
                        viewModel.updateThemeMode("dark")
                        RunicQuotesApplication.widgetSyncManager(context).refreshAllAsync(context)
                    }
                )
                SettingItem(
                    title = stringResource(R.string.settings_system),
                    subtitle = stringResource(R.string.settings_system_subtitle),
                    selected = preferences.themeMode == "system",
                    onClick = {
                        haptics.lightToggle()
                        viewModel.updateThemeMode("system")
                        RunicQuotesApplication.widgetSyncManager(context).refreshAllAsync(context)
                    }
                )
                SettingItem(
                    title = stringResource(R.string.settings_dynamic_color_title),
                    subtitle = if (dynamicColorSupported) {
                        stringResource(R.string.settings_dynamic_color_subtitle_supported)
                    } else {
                        stringResource(R.string.settings_dynamic_color_subtitle_unsupported)
                    },
                    onClick = if (dynamicColorSupported) {
                        {
                            haptics.lightToggle()
                            viewModel.updateDynamicColorEnabled(!preferences.dynamicColorEnabled)
                            RunicQuotesApplication.widgetSyncManager(context).refreshAllAsync(context)
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
                                RunicQuotesApplication.widgetSyncManager(context).refreshAllAsync(context)
                            },
                            enabled = dynamicColorSupported
                        )
                    }
                )
            }

            SettingSection(
                title = stringResource(R.string.settings_section_display),
                subtitle = stringResource(R.string.settings_section_display_subtitle)
            ) {
                SettingItem(
                    title = stringResource(R.string.settings_show_transliteration_title),
                    subtitle = stringResource(R.string.settings_show_transliteration_subtitle),
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

            SettingSection(
                title = stringResource(R.string.settings_section_accessibility),
                subtitle = stringResource(R.string.settings_section_accessibility_subtitle)
            ) {
                SettingItem(
                    title = stringResource(R.string.settings_large_runes_title),
                    subtitle = stringResource(R.string.settings_large_runes_subtitle),
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
                SettingItem(
                    title = stringResource(R.string.settings_high_contrast_title),
                    subtitle = stringResource(R.string.settings_high_contrast_subtitle),
                    onClick = {
                        haptics.lightToggle()
                        viewModel.updateHighContrastEnabled(!preferences.highContrastEnabled)
                        RunicQuotesApplication.widgetSyncManager(context).refreshAllAsync(context)
                    },
                    trailing = {
                        Switch(
                            checked = preferences.highContrastEnabled,
                            onCheckedChange = {
                                haptics.lightToggle()
                                viewModel.updateHighContrastEnabled(it)
                                RunicQuotesApplication.widgetSyncManager(context).refreshAllAsync(context)
                            }
                        )
                    }
                )
                SettingItem(
                    title = stringResource(R.string.settings_reduced_motion_title),
                    subtitle = stringResource(R.string.settings_reduced_motion_subtitle),
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

            SettingSection(title = stringResource(R.string.settings_section_links)) {
                SettingItem(
                    title = stringResource(R.string.settings_notifications),
                    onClick = {
                        haptics.lightToggle()
                        onNavigateToNotifications()
                    },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Navigate to notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                SettingItem(
                    title = stringResource(R.string.settings_about),
                    onClick = {
                        haptics.lightToggle()
                        onNavigateToAbout()
                    },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Navigate to about",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                SettingItem(
                    title = stringResource(R.string.settings_profile),
                    onClick = {
                        haptics.lightToggle()
                        onNavigateToProfile()
                    },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Navigate to profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}
