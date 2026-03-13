package com.po4yka.runatal.ui.screens.notificationsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel for notification preferences toggles. */
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    /** Observable notification settings state. */
    val uiState: StateFlow<NotificationSettingsUiState> =
        userPreferencesManager.userPreferencesFlow.map { prefs ->
            NotificationSettingsUiState(
                dailyQuote = prefs.dailyQuoteNotifications,
                streak = prefs.streakNotifications,
                packUpdates = prefs.packUpdateNotifications
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            NotificationSettingsUiState()
        )

    /** Toggle daily quote notification preference. */
    fun toggleDailyQuote() {
        viewModelScope.launch {
            userPreferencesManager.updateDailyQuoteNotifications(!uiState.value.dailyQuote)
        }
    }

    /** Toggle streak reminder notification preference. */
    fun toggleStreak() {
        viewModelScope.launch {
            userPreferencesManager.updateStreakNotifications(!uiState.value.streak)
        }
    }

    /** Toggle pack update notification preference. */
    fun togglePackUpdates() {
        viewModelScope.launch {
            userPreferencesManager.updatePackUpdateNotifications(!uiState.value.packUpdates)
        }
    }
}

/** UI state for notification settings screen. */
data class NotificationSettingsUiState(
    val dailyQuote: Boolean = true,
    val streak: Boolean = true,
    val packUpdates: Boolean = true
)
