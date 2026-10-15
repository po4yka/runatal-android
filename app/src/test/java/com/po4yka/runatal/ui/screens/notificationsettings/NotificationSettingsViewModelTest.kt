package com.po4yka.runatal.ui.screens.notificationsettings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.preferences.NotificationPreferencesSnapshot
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.notification.NotificationAccess
import com.po4yka.runatal.notification.NotificationKind
import com.po4yka.runatal.notification.NotificationPublisher
import com.po4yka.runatal.data.preferences.UserPreferencesManager
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
    private lateinit var preferencesFlow: MutableStateFlow<NotificationPreferencesSnapshot>
    private lateinit var viewModel: NotificationSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        userPreferencesManager = mockk(relaxed = true)
        preferencesFlow = MutableStateFlow(
            NotificationPreferencesSnapshot(false, true, false, RunicScript.DEFAULT)
        )
        every { userPreferencesManager.notificationPreferencesFlow } returns preferencesFlow
        coEvery { userPreferencesManager.updateDailyQuoteNotifications(any()) } coAnswers {
            preferencesFlow.value = preferencesFlow.value.copy(dailyQuote = firstArg())
        }
        coEvery { userPreferencesManager.updateStreakNotifications(any()) } coAnswers {
            preferencesFlow.value = preferencesFlow.value.copy(streak = firstArg())
        }
        coEvery { userPreferencesManager.updatePackUpdateNotifications(any()) } coAnswers {
            preferencesFlow.value = preferencesFlow.value.copy(packUpdates = firstArg())
        }

        val publisher = mockk<NotificationPublisher>(relaxed = true)
        every { publisher.access } returns MutableStateFlow(
            NotificationAccess(true, true, NotificationKind.entries.toSet())
        )
        viewModel = NotificationSettingsViewModel(userPreferencesManager, publisher)
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

            viewModel.updateDailyQuote(true)
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

            viewModel.updateStreak(false)
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

            viewModel.updatePackUpdates(true)
            advanceUntilIdle()

            coVerify { userPreferencesManager.updatePackUpdateNotifications(true) }
            assertThat(awaitItem().packUpdates).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `failed preference write keeps committed value and exposes a retryable message`() = runTest {
        coEvery { userPreferencesManager.updateDailyQuoteNotifications(true) } throws java.io.IOException("disk")
        viewModel.uiState.test {
            awaitItem()
            awaitItem()
            viewModel.updateDailyQuote(true)
            advanceUntilIdle()
            val failed = awaitItem()
            assertThat(failed.dailyQuote).isFalse()
            assertThat(failed.errorMessage).contains("Couldn't save")
            cancelAndIgnoreRemainingEvents()
        }
    }

}
