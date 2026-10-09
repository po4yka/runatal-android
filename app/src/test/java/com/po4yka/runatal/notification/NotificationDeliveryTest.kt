package com.po4yka.runatal.notification

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.ReadingStats
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.repository.ReadingHistoryRepository
import com.po4yka.runatal.util.TimeProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Android notification APIs and an isolated real DataStore; JVM evidence does not establish device receipt. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class NotificationDeliveryTest {
    private val today = LocalDate.of(2026, 10, 9)
    private lateinit var scope: CoroutineScope
    private lateinit var directory: File
    private lateinit var preferences: UserPreferencesManager
    private lateinit var publisher: NotificationPublisher
    private lateinit var delivery: NotificationDelivery
    private lateinit var manager: NotificationManager
    private lateinit var readings: MutableStateFlow<ReadingStats>
    private lateinit var quotes: QuoteRepository
    private val catalogue = BundledPackCatalogue()

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        manager = context.getSystemService(NotificationManager::class.java)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        directory = kotlin.io.path.createTempDirectory("notification-test").toFile()
        preferences = UserPreferencesManager(PreferenceDataStoreFactory.create(scope = scope) {
            File(directory, "notifications.preferences_pb")
        })
        publisher = NotificationPublisher(context).apply { createChannels() }
        readings = MutableStateFlow(ReadingStats(0, 0, null))
        val history = mockk<ReadingHistoryRepository>()
        every { history.stats() } returns readings
        quotes = mockk()
        coEvery { quotes.quoteOfTheDay() } returns Quote(1L, "A test library quote", "Author", null, null, null)
        coEvery { quotes.getQuoteById(1L) } returns Quote(1L, "A test library quote", "Author", null, null, null)
        val time = mockk<TimeProvider>()
        every { time.getCurrentDate() } returns today
        delivery = NotificationDelivery(preferences, publisher, quotes, history, catalogue, time)
    }

    @After
    fun tearDown() { scope.cancel(); directory.deleteRecursively() }

    private fun allow() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
            Manifest.permission.POST_NOTIFICATIONS
        )
        publisher.refreshAccess()
    }

    @Test
    fun `a physical reservation write failure cannot post and retry delivers once`() = runTest {
        allow()
        // Create and read the real file before making its parent unwritable as a directory.
        preferences.updateDailyQuoteNotifications(true)
        val displaced = File(directory.parentFile, "${directory.name}-displaced")
        assertThat(directory.renameTo(displaced)).isTrue()
        directory.writeText("A file cannot contain a DataStore child")
        try {
            val failure = runCatching { delivery.deliver(NotificationKind.DAILY) }.exceptionOrNull()
            assertThat(failure).isInstanceOf(java.io.IOException::class.java)
            assertThat(manager.activeNotifications).isEmpty()
        } finally {
            directory.delete()
            assertThat(displaced.renameTo(directory)).isTrue()
        }
        delivery.deliver(NotificationKind.DAILY)
        assertThat(manager.activeNotifications).hasLength(1)
        assertThat(preferences.lastNotificationDay("DAILY")).isEqualTo(today.toEpochDay())
        manager.cancelAll()
        delivery.deliver(NotificationKind.DAILY)
        assertThat(manager.activeNotifications).isEmpty()
    }

    @Test
    fun `a quote hidden after preparation cannot appear in a notification`() = runTest {
        allow()
        coEvery { quotes.getQuoteById(1L) } returns null
        delivery.deliver(NotificationKind.DAILY)
        assertThat(manager.activeNotifications).isEmpty()
        assertThat(preferences.lastNotificationDay("DAILY")).isNull()
    }

    @Test
    fun `permission denial preserves the day and a subsequent grant enables actual posting`() = runTest {
        delivery.deliver(NotificationKind.DAILY)
        assertThat(manager.activeNotifications).isEmpty()
        assertThat(preferences.lastNotificationDay("DAILY")).isNull()
        allow()
        delivery.deliver(NotificationKind.DAILY)
        assertThat(manager.activeNotifications).hasLength(1)
        val posted = manager.activeNotifications.single()
        assertThat(posted.notification.channelId).isEqualTo(NotificationKind.DAILY.channelId)
        assertThat(posted.notification.extras.getCharSequence("android.text").toString())
            .contains("A test library quote")
        assertThat(preferences.lastNotificationDay("DAILY")).isEqualTo(today.toEpochDay())
        delivery.deliver(NotificationKind.DAILY)
        assertThat(manager.activeNotifications).hasLength(1)
    }

    @Test
    fun `disabled preference and blocked channel stop delivery without consuming dedup state`() = runTest {
        allow()
        preferences.updateDailyQuoteNotifications(false)
        delivery.deliver(NotificationKind.DAILY)
        assertThat(manager.activeNotifications).isEmpty()
        preferences.updateDailyQuoteNotifications(true)
        manager.deleteNotificationChannel(NotificationKind.DAILY.channelId)
        manager.createNotificationChannel(NotificationChannel(NotificationKind.DAILY.channelId, "Blocked",
            NotificationManager.IMPORTANCE_NONE))
        publisher.createChannels()
        delivery.deliver(NotificationKind.DAILY)
        assertThat(manager.activeNotifications).isEmpty()
        assertThat(preferences.lastNotificationDay("DAILY")).isNull()
        assertThat(publisher.snapshot().allows(NotificationKind.DAILY)).isFalse()
    }

    @Test
    fun `streak requires yesterday real reading and skips today read empty or expired streaks`() = runTest {
        allow()
        listOf(ReadingStats(0, 0, null), ReadingStats(0, 1, today.minusDays(2), today.minusDays(2)),
            ReadingStats(2, 2, today.minusDays(1), today)).forEach { stats ->
            readings.value = stats
            delivery.deliver(NotificationKind.STREAK)
            assertThat(manager.activeNotifications).isEmpty()
        }
        readings.value = ReadingStats(3, 3, today.minusDays(3), today.minusDays(1))
        delivery.deliver(NotificationKind.STREAK)
        assertThat(manager.activeNotifications.single().notification.channelId)
            .isEqualTo(NotificationKind.STREAK.channelId)
        assertThat(preferences.lastNotificationDay("STREAK")).isEqualTo(today.toEpochDay())
    }

    @Test
    fun `pack events use real bundled content changes and first install is a silent baseline`() = runTest {
        allow()
        delivery.deliver(NotificationKind.PACKS)
        assertThat(manager.activeNotifications).isEmpty()
        val current = catalogue.fingerprints()
        assertThat(preferences.notificationPackCatalogue()).isEqualTo(current)
        delivery.deliver(NotificationKind.PACKS)
        assertThat(manager.activeNotifications).isEmpty()
        preferences.markNotificationPackCatalogue(current + ("1" to "previous bundled content digest"))
        delivery.deliver(NotificationKind.PACKS)
        assertThat(manager.activeNotifications.single().notification.extras.getCharSequence("android.text").toString())
            .isEqualTo("Hávamál Selections")
        assertThat(preferences.notificationPackCatalogue()).isEqualTo(current)
        delivery.cancel(NotificationKind.PACKS)
        assertThat(manager.activeNotifications).isEmpty()
    }
}
