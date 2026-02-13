package com.po4yka.runicquotes.ui.screens.settings

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

/**
 * Settings screen for configuring app preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
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
                    onClick = { viewModel.updateSelectedScript(RunicScript.ELDER_FUTHARK) }
                )
                SettingItem(
                    title = stringResource(R.string.script_younger_futhark),
                    subtitle = "Younger Futhark (800-1100 AD)",
                    selected = preferences.selectedScript == RunicScript.YOUNGER_FUTHARK,
                    onClick = { viewModel.updateSelectedScript(RunicScript.YOUNGER_FUTHARK) }
                )
                SettingItem(
                    title = stringResource(R.string.script_cirth),
                    subtitle = "Tolkien's Cirth (Angerthas)",
                    selected = preferences.selectedScript == RunicScript.CIRTH,
                    onClick = { viewModel.updateSelectedScript(RunicScript.CIRTH) }
                )
            }

            HorizontalDivider()

            // Font Section
            SettingSection(title = "Font") {
                SettingItem(
                    title = stringResource(R.string.font_noto_sans),
                    subtitle = "Clean and modern",
                    selected = preferences.selectedFont == "noto",
                    onClick = { viewModel.updateSelectedFont("noto") }
                )
                SettingItem(
                    title = stringResource(R.string.font_babelstone),
                    subtitle = "Traditional appearance",
                    selected = preferences.selectedFont == "babelstone",
                    onClick = { viewModel.updateSelectedFont("babelstone") }
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
                            onCheckedChange = { viewModel.updateShowTransliteration(it) }
                        )
                    }
                )
            }

            HorizontalDivider()

            // Theme Section
            SettingSection(title = "Theme") {
                SettingItem(
                    title = "Light",
                    subtitle = "White background",
                    selected = preferences.themeMode == "light",
                    onClick = { viewModel.updateThemeMode("light") }
                )
                SettingItem(
                    title = "Dark",
                    subtitle = "Black background",
                    selected = preferences.themeMode == "dark",
                    onClick = { viewModel.updateThemeMode("dark") }
                )
                SettingItem(
                    title = "System",
                    subtitle = "Follow system theme",
                    selected = preferences.themeMode == "system",
                    onClick = { viewModel.updateThemeMode("system") }
                )
            }
        }
    }
}
