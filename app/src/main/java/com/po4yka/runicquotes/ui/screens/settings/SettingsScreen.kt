package com.po4yka.runicquotes.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

@Composable
fun SettingsScreen(
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToReferences: () -> Unit = {},
    onNavigateToTranslation: () -> Unit = {},
    onNavigateToPacks: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val haptics = rememberHapticFeedback()
    val context = LocalContext.current
    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val refreshWidgets = {
        RunicQuotesApplication.widgetSyncManager(context).refreshAllAsync(context)
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Preferences & about",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingSection(title = "Default Script") {
                ScriptSettingItem(
                    rune = "\u16A0",
                    title = stringResource(R.string.script_elder_futhark),
                    subtitle = "24 runes \u00B7 2nd-8th century",
                    selected = preferences.selectedScript == RunicScript.ELDER_FUTHARK,
                    onClick = {
                        haptics.lightToggle()
                        viewModel.updateSelectedScript(RunicScript.ELDER_FUTHARK)
                        refreshWidgets()
                    }
                )
                ScriptSettingItem(
                    rune = "\u16A0",
                    title = stringResource(R.string.script_younger_futhark),
                    subtitle = "16 runes \u00B7 9th-11th century",
                    selected = preferences.selectedScript == RunicScript.YOUNGER_FUTHARK,
                    onClick = {
                        haptics.lightToggle()
                        viewModel.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
                        refreshWidgets()
                    }
                )
                ScriptSettingItem(
                    rune = "\u2D30",
                    title = stringResource(R.string.script_cirth),
                    subtitle = "Tolkien's Cirth system",
                    selected = preferences.selectedScript == RunicScript.CIRTH,
                    onClick = {
                        haptics.lightToggle()
                        viewModel.updateSelectedScript(RunicScript.CIRTH)
                        refreshWidgets()
                    }
                )
            }

            SettingSection(title = "Appearance") {
                ThemeSettingItem(
                    title = stringResource(R.string.settings_light),
                    subtitle = "Bright UI surfaces",
                    icon = Icons.Default.LightMode,
                    selected = preferences.themeMode == "light",
                    onClick = {
                        haptics.lightToggle()
                        viewModel.updateThemeMode("light")
                        refreshWidgets()
                    }
                )
                ThemeSettingItem(
                    title = stringResource(R.string.settings_dark),
                    subtitle = "Deep cool charcoal surfaces",
                    icon = Icons.Default.DarkMode,
                    selected = preferences.themeMode == "dark",
                    onClick = {
                        haptics.lightToggle()
                        viewModel.updateThemeMode("dark")
                        refreshWidgets()
                    }
                )
                ThemeSettingItem(
                    title = stringResource(R.string.settings_system),
                    subtitle = "Follow device appearance",
                    icon = Icons.Default.PhoneAndroid,
                    selected = preferences.themeMode == "system",
                    onClick = {
                        haptics.lightToggle()
                        viewModel.updateThemeMode("system")
                        refreshWidgets()
                    }
                )
                ToggleSettingItem(
                    title = stringResource(R.string.settings_dynamic_color_title),
                    subtitle = if (dynamicColorSupported) {
                        "Match system wallpaper (Material You)"
                    } else {
                        stringResource(R.string.settings_dynamic_color_subtitle_unsupported)
                    },
                    icon = Icons.Default.Palette,
                    checked = preferences.dynamicColorEnabled && dynamicColorSupported,
                    enabled = dynamicColorSupported,
                    onCheckedChange = {
                        haptics.lightToggle()
                        viewModel.updateDynamicColorEnabled(it)
                        refreshWidgets()
                    }
                )
                ToggleSettingItem(
                    title = "Show Latin Text",
                    subtitle = "Display original text alongside runes",
                    icon = Icons.Default.Visibility,
                    checked = preferences.showTransliteration,
                    onCheckedChange = {
                        haptics.lightToggle()
                        viewModel.updateShowTransliteration(it)
                    }
                )
            }

            SettingSection(title = "Notifications") {
                ToggleSettingItem(
                    title = "Daily Quote Alert",
                    subtitle = "Receive a new rune quote each morning",
                    icon = Icons.Default.Notifications,
                    checked = preferences.dailyQuoteNotifications,
                    onCheckedChange = {
                        haptics.lightToggle()
                        viewModel.updateDailyQuoteNotifications(it)
                    }
                )
                ToggleSettingItem(
                    title = "Community Picks",
                    subtitle = "Weekly highlights from shared quotes",
                    icon = Icons.Default.Star,
                    checked = preferences.packUpdateNotifications,
                    onCheckedChange = {
                        haptics.lightToggle()
                        viewModel.updatePackUpdateNotifications(it)
                    }
                )
                ToggleSettingItem(
                    title = "Streak Reminders",
                    subtitle = "Keep your reading streak alive",
                    icon = Icons.Default.Notifications,
                    checked = preferences.streakNotifications,
                    onCheckedChange = {
                        haptics.lightToggle()
                        viewModel.updateStreakNotifications(it)
                    }
                )
            }

            SettingSection(title = "Accessibility") {
                ToggleSettingItem(
                    title = stringResource(R.string.settings_large_runes_title),
                    subtitle = "Increase rune size across reading surfaces",
                    icon = Icons.Default.FormatSize,
                    checked = preferences.largeRunesEnabled,
                    onCheckedChange = {
                        haptics.lightToggle()
                        viewModel.updateLargeRunesEnabled(it)
                    }
                )
                ToggleSettingItem(
                    title = stringResource(R.string.settings_high_contrast_title),
                    subtitle = "Increase text and border contrast",
                    icon = Icons.Default.Contrast,
                    checked = preferences.highContrastEnabled,
                    onCheckedChange = {
                        haptics.lightToggle()
                        viewModel.updateHighContrastEnabled(it)
                        refreshWidgets()
                    }
                )
                ToggleSettingItem(
                    title = stringResource(R.string.settings_reduced_motion_title),
                    subtitle = "Disable rune reveal and screen animations",
                    icon = Icons.Default.SlowMotionVideo,
                    checked = preferences.reducedMotionEnabled,
                    onCheckedChange = {
                        haptics.lightToggle()
                        viewModel.updateReducedMotionEnabled(it)
                    }
                )
            }

            SettingSection(title = "About") {
                SettingsAboutCard()
                LinkSettingItem(
                    title = stringResource(R.string.settings_profile),
                    icon = Icons.Default.Person,
                    onClick = {
                        haptics.lightToggle()
                        onNavigateToProfile()
                    }
                )
                LinkSettingItem(
                    title = stringResource(R.string.settings_about),
                    icon = Icons.Default.Info,
                    onClick = {
                        haptics.lightToggle()
                        onNavigateToAbout()
                    }
                )
                LinkSettingItem(
                    title = "Rune References",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    onClick = {
                        haptics.lightToggle()
                        onNavigateToReferences()
                    }
                )
                LinkSettingItem(
                    title = "Translation",
                    icon = Icons.Default.Translate,
                    onClick = {
                        haptics.lightToggle()
                        onNavigateToTranslation()
                    }
                )
                LinkSettingItem(
                    title = "Quote Packs",
                    icon = Icons.Default.Star,
                    onClick = {
                        haptics.lightToggle()
                        onNavigateToPacks()
                    }
                )
                LinkSettingItem(
                    title = "Notification schedule",
                    icon = Icons.Default.Notifications,
                    onClick = {
                        haptics.lightToggle()
                        onNavigateToNotifications()
                    }
                )
            }
        }
    }
}

@Composable
private fun ScriptSettingItem(
    rune: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    SettingItem(
        title = title,
        subtitle = subtitle,
        selected = selected,
        onClick = onClick,
        leadingIcon = { RuneBadge(rune = rune, selected = selected) },
        trailing = { SelectionIndicator(selected = selected) }
    )
}

@Composable
private fun ThemeSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    SettingItem(
        title = title,
        subtitle = subtitle,
        selected = selected,
        onClick = onClick,
        leadingIcon = { SettingsIconBadge(icon = icon, emphasized = selected) },
        trailing = { SelectionIndicator(selected = selected) }
    )
}

@Composable
private fun ToggleSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingItem(
        title = title,
        subtitle = subtitle,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else {
            null
        },
        leadingIcon = { SettingsIconBadge(icon = icon, emphasized = checked) },
        trailing = {
            RunatalSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun LinkSettingItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    SettingItem(
        title = title,
        onClick = onClick,
        leadingIcon = { SettingsIconBadge(icon = icon) },
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun RuneBadge(
    rune: String,
    selected: Boolean
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rune,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    emphasized: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                color = if (emphasized) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
                },
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shape = RoundedCornerShape(999.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(999.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun RunatalSwitch(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = if (checked) {
            {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(999.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u2713",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
            null
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.surface,
            checkedTrackColor = MaterialTheme.colorScheme.secondary,
            checkedBorderColor = MaterialTheme.colorScheme.secondary,
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
            uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            disabledCheckedThumbColor = MaterialTheme.colorScheme.surface,
            disabledCheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
            disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            disabledUncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    )
}

@Composable
private fun SettingsAboutCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u16B1",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Runatal",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Nordic Runic Quotes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "Transliterates inspirational quotes into ancient runic scripts " +
                "with a calm Material 3 reading surface.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
