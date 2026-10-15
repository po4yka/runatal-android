package com.po4yka.runatal.ui.screens.notificationsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.notification.NotificationKind
import com.po4yka.runatal.notification.NotificationPublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Requested switches remain distinct from whether Android currently allows delivery. */
@HiltViewModel
internal class NotificationSettingsViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager,
    private val publisher: NotificationPublisher
) : ViewModel() {
    private val writeError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NotificationSettingsUiState> = combine(
        userPreferencesManager.notificationPreferencesFlow, publisher.access, writeError
    ) { prefs, access, error ->
        NotificationSettingsUiState(prefs.dailyQuote, prefs.streak, prefs.packUpdates,
            access.permissionGranted, access.appEnabled,
            access.allows(NotificationKind.DAILY), access.allows(NotificationKind.STREAK),
            access.allows(NotificationKind.PACKS), errorMessage = error)
    }.catch { exception ->
        if (exception is IOException) {
            emit(NotificationSettingsUiState(errorMessage = "Notification settings unavailable"))
        }
        else throw exception
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), NotificationSettingsUiState())

    fun refreshSystemAccess() { publisher.refreshAccess() }

    fun updateDailyQuote(enabled: Boolean) = update {
        userPreferencesManager.updateDailyQuoteNotifications(enabled)
    }

    fun updateStreak(enabled: Boolean) = update {
        userPreferencesManager.updateStreakNotifications(enabled)
    }

    fun updatePackUpdates(enabled: Boolean) = update {
        userPreferencesManager.updatePackUpdateNotifications(enabled)
    }

    private fun update(action: suspend () -> Unit) {
        viewModelScope.launch {
            writeError.value = null
            try {
                action()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: IOException) {
                writeError.value = "Couldn't save notification settings. Try again."
            }
        }
    }
}

/** Desired preferences and observed system permission/channel status. */
data class NotificationSettingsUiState(
    val dailyQuote: Boolean = true,
    val streak: Boolean = true,
    val packUpdates: Boolean = true,
    val permissionGranted: Boolean = false,
    val appEnabled: Boolean = false,
    val dailyAllowed: Boolean = false,
    val streakAllowed: Boolean = false,
    val packsAllowed: Boolean = false,
    val errorMessage: String? = null
) {
    /** Reports effective delivery honestly even when the requested preference defaults to on. */
    fun deliveryStatus(requested: Boolean, channelAllowed: Boolean): String = when {
        errorMessage != null -> errorMessage
        !requested -> "Off in Runatal"
        !permissionGranted -> "Requested · Blocked until notification permission is allowed"
        !appEnabled -> "Requested · Blocked in Android notification settings"
        !channelAllowed -> "Requested · Channel disabled in Android notification settings"
        else -> "Requested · Allowed by Android"
    }
}
