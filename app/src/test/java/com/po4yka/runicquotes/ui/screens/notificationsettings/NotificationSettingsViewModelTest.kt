package com.po4yka.runicquotes.ui.screens.notificationsettings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var preferencesFlow: MutableStateFlow<UserPreferences>
    private lateinit var viewModel: NotificationSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        userPreferencesManager = mockk(relaxed = true)
        preferencesFlow = MutableStateFlow(
            UserPreferences(
                dailyQuoteNotifications = false,
                streakNotifications = true,
                packUpdateNotifications = false
            )
        )
        every { userPreferencesManager.userPreferencesFlow } returns preferencesFlow
        coEvery { userPreferencesManager.updateDailyQuoteNotifications(any()) } coAnswers {
            preferencesFlow.value = preferencesFlow.value.copy(dailyQuoteNotifications = firstArg())
        }
        coEvery { userPreferencesManager.updateStreakNotifications(any()) } coAnswers {
            preferencesFlow.value = preferencesFlow.value.copy(streakNotifications = firstArg())
        }
        coEvery { userPreferencesManager.updatePackUpdateNotifications(any()) } coAnswers {
            preferencesFlow.value = preferencesFlow.value.copy(packUpdateNotifications = firstArg())
        }

        viewModel = NotificationSettingsViewModel(userPreferencesManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState maps notification preferences`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(NotificationSettingsUiState())
            val state = awaitItem()

            assertThat(state.dailyQuote).isFalse()
            assertThat(state.streak).isTrue()
            assertThat(state.packUpdates).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleDailyQuote flips current value and updates flow`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            awaitItem()

            viewModel.toggleDailyQuote()
            advanceUntilIdle()

            coVerify { userPreferencesManager.updateDailyQuoteNotifications(true) }
            assertThat(awaitItem().dailyQuote).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleStreak flips current value and updates flow`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            awaitItem()

            viewModel.toggleStreak()
            advanceUntilIdle()

            coVerify { userPreferencesManager.updateStreakNotifications(false) }
            assertThat(awaitItem().streak).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `togglePackUpdates flips current value and updates flow`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            awaitItem()

            viewModel.togglePackUpdates()
            advanceUntilIdle()

            coVerify { userPreferencesManager.updatePackUpdateNotifications(true) }
            assertThat(awaitItem().packUpdates).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
