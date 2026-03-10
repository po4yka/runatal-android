package com.po4yka.runicquotes.ui.screens.notificationsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.ui.components.SettingItem
import com.po4yka.runicquotes.ui.components.SettingSection

@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                text = "Notifications",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Quote delivery & reminders",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingSection(title = "Notifications") {
                NotificationToggleItem(
                    title = "Daily Quote Alert",
                    subtitle = "Receive a new rune quote each morning",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    checked = state.dailyQuote,
                    onCheckedChange = { viewModel.toggleDailyQuote() }
                )
                NotificationToggleItem(
                    title = "Streak Reminders",
                    subtitle = "Keep your streak alive with daily reminders",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    checked = state.streak,
                    onCheckedChange = { viewModel.toggleStreak() }
                )
                NotificationToggleItem(
                    title = "Community Picks",
                    subtitle = "Weekly highlights from shared quotes",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    checked = state.packUpdates,
                    onCheckedChange = { viewModel.togglePackUpdates() }
                )
            }

            SettingItem(
                title = "Back to settings",
                onClick = onNavigateBack
            )
        }
    }
}

@Composable
private fun NotificationToggleItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingItem(
        title = title,
        subtitle = subtitle,
        onClick = { onCheckedChange(!checked) },
        leadingIcon = icon,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = MaterialTheme.colorScheme.secondary,
                    checkedBorderColor = MaterialTheme.colorScheme.secondary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    )
}
