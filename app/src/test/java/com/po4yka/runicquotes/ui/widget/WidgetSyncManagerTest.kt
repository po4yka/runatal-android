package com.po4yka.runicquotes.ui.widget

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.preferences.WidgetUpdateMode
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetSyncManagerTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val context = mockk<Context>(relaxed = true).also {
        every { it.applicationContext } returns it
    }

    @Test
    fun `refreshAllAsync delegates to refresh runner`() = scope.runTest {
        val refreshRunner = RecordingRefreshRunner()
        val manager = WidgetSyncManager(
            scope = this,
            refreshRunner = refreshRunner,
            workScheduler = RecordingScheduler(),
            updateModeReader = FixedModeReader(WidgetUpdateMode.DAILY),
            clock = fixedClock("2026-03-11T10:15:30Z")
        )

        manager.refreshAllAsync(context)
        advanceUntilIdle()

        assertThat(refreshRunner.refreshAllCalls).isEqualTo(1)
    }

    @Test
    fun `cancelSchedule delegates to scheduler`() {
        val scheduler = RecordingScheduler()
        val manager = WidgetSyncManager(
            scope = scope,
            refreshRunner = RecordingRefreshRunner(),
            workScheduler = scheduler,
            updateModeReader = FixedModeReader(WidgetUpdateMode.DAILY),
            clock = fixedClock("2026-03-11T10:15:30Z")
        )

        manager.cancelSchedule(context)

        assertThat(scheduler.cancelCalls).isEqualTo(1)
    }

    @Test
    fun `rescheduleAsync cancels work for manual mode`() = scope.runTest {
        val scheduler = RecordingScheduler()
        val manager = WidgetSyncManager(
            scope = this,
            refreshRunner = RecordingRefreshRunner(),
            workScheduler = scheduler,
            updateModeReader = FixedModeReader(WidgetUpdateMode.MANUAL),
            clock = fixedClock("2026-03-11T10:15:30Z")
        )

        manager.rescheduleAsync(context)
        advanceUntilIdle()

        assertThat(scheduler.cancelCalls).isEqualTo(1)
        assertThat(scheduler.enqueuedIntervalHours).isNull()
    }

    @Test
    fun `rescheduleAsync schedules daily refresh for next midnight`() = scope.runTest {
        val scheduler = RecordingScheduler()
        val manager = WidgetSyncManager(
            scope = this,
            refreshRunner = RecordingRefreshRunner(),
            workScheduler = scheduler,
            updateModeReader = FixedModeReader(WidgetUpdateMode.DAILY),
            clock = fixedClock("2026-03-11T10:15:30Z")
        )

        manager.rescheduleAsync(context)
        advanceUntilIdle()

        assertThat(scheduler.enqueuedIntervalHours).isEqualTo(24L)
        assertThat(scheduler.initialDelay).isEqualTo(Duration.ofHours(13).plusMinutes(44).plusSeconds(30))
    }

    @Test
    fun `rescheduleAsync aligns six hour updates to next interval boundary`() = scope.runTest {
        val scheduler = RecordingScheduler()
        val manager = WidgetSyncManager(
            scope = this,
            refreshRunner = RecordingRefreshRunner(),
            workScheduler = scheduler,
            updateModeReader = FixedModeReader(WidgetUpdateMode.EVERY_6_HOURS),
            clock = fixedClock("2026-03-11T10:15:30Z")
        )

        manager.rescheduleAsync(context)
        advanceUntilIdle()

        assertThat(scheduler.enqueuedIntervalHours).isEqualTo(6L)
        assertThat(scheduler.initialDelay).isEqualTo(Duration.ofHours(1).plusMinutes(44).plusSeconds(30))
    }

    @Test
    fun `refreshAndRescheduleAsync performs both operations`() = scope.runTest {
        val refreshRunner = RecordingRefreshRunner()
        val scheduler = RecordingScheduler()
        val manager = WidgetSyncManager(
            scope = this,
            refreshRunner = refreshRunner,
            workScheduler = scheduler,
            updateModeReader = FixedModeReader(WidgetUpdateMode.EVERY_12_HOURS),
            clock = fixedClock("2026-03-11T23:50:00Z")
        )

        manager.refreshAndRescheduleAsync(context)
        advanceUntilIdle()

        assertThat(refreshRunner.refreshAllCalls).isEqualTo(1)
        assertThat(scheduler.enqueuedIntervalHours).isEqualTo(12L)
        assertThat(scheduler.initialDelay).isEqualTo(Duration.ofMinutes(10))
    }

    private fun fixedClock(instant: String): Clock {
        return Clock.fixed(Instant.parse(instant), ZoneId.of("UTC"))
    }

    private class RecordingRefreshRunner : WidgetRefreshRunner {
        var refreshAllCalls = 0

        override suspend fun refreshAll(context: Context) {
            refreshAllCalls += 1
        }

        override suspend fun refresh(context: Context, glanceId: androidx.glance.GlanceId) = Unit
    }

    private class RecordingScheduler : WidgetWorkScheduler {
        var enqueuedIntervalHours: Long? = null
        var initialDelay: Duration? = null
        var cancelCalls = 0

        override fun enqueuePeriodicRefresh(context: Context, intervalHours: Long, initialDelay: Duration) {
            enqueuedIntervalHours = intervalHours
            this.initialDelay = initialDelay
        }

        override fun cancel(context: Context) {
            cancelCalls += 1
        }
    }

    private class FixedModeReader(
        private val mode: WidgetUpdateMode
    ) : WidgetUpdateModeReader {
        override suspend fun read(context: Context): WidgetUpdateMode = mode
    }
}
