package com.po4yka.runatal.notification

import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.domain.model.displayName
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.repository.ReadingHistoryRepository
import com.po4yka.runatal.util.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Prepares genuine local events and serializes posting with persisted deduplication and cancellation. */
@Singleton
internal class NotificationDelivery @Inject constructor(
    private val preferences: UserPreferencesManager,
    private val publisher: NotificationPublisher,
    private val quotes: QuoteRepository,
    private val readings: ReadingHistoryRepository,
    private val catalogue: BundledPackCatalogue,
    private val time: TimeProvider
) {
    private val deliveryMutex = Mutex()

    suspend fun deliver(kind: NotificationKind) = deliveryMutex.withLock {
        val prefs = preferences.notificationPreferencesFlow.first()
        val enabled = enabled(prefs, kind)
        if (!enabled || !publisher.snapshot().allows(kind)) return@withLock
        if (kind == NotificationKind.PACKS) {
            deliverPackUpdates()
            return@withLock
        }
        val day = time.getCurrentDate()
        val previousDay = preferences.lastNotificationDay(kind.name)
        if (previousDay == day.toEpochDay()) return@withLock
        val payload = preparePayload(kind, day) ?: return@withLock
        currentCoroutineContext().ensureActive()
        val latest = preferences.notificationPreferencesFlow.first()
        val stillEnabled = if (kind == NotificationKind.DAILY) latest.dailyQuote else latest.streak
        val nowRead = kind == NotificationKind.STREAK && readings.stats().first().lastReadDate == day
        if (time.getCurrentDate() != day) return@withLock
        val title = if (kind == NotificationKind.DAILY) "Daily quote · ${latest.script.displayName}" else payload.title
        if (!stillEnabled || nowRead || !publisher.snapshot().allows(kind)) return@withLock
        postReserved(kind, title, payload.body,
            reserve = { preferences.markNotificationDelivered(kind.name, day.toEpochDay()) },
            rollback = { preferences.rollbackNotificationDay(kind.name, day.toEpochDay(), previousDay) },
            beforePost = { stillEligible(kind, day, payload) })
    }

    private suspend fun stillEligible(
        kind: NotificationKind, day: java.time.LocalDate, payload: PreparedNotification
    ): Boolean {
        val current = preferences.notificationPreferencesFlow.first()
        val alreadyRead = kind == NotificationKind.STREAK && readings.stats().first().lastReadDate == day
        return enabled(current, kind) && !alreadyRead && time.getCurrentDate() == day && sourceIsCurrent(payload)
    }

    private fun enabled(
        prefs: com.po4yka.runatal.data.preferences.NotificationPreferencesSnapshot,
        kind: NotificationKind
    ): Boolean = when (kind) {
        NotificationKind.DAILY -> prefs.dailyQuote
        NotificationKind.STREAK -> prefs.streak
        NotificationKind.PACKS -> prefs.packUpdates
    }

    private suspend fun preparePayload(kind: NotificationKind, day: java.time.LocalDate): PreparedNotification? =
        when (kind) {
            NotificationKind.DAILY -> quotes.quoteOfTheDay()?.let {
                PreparedNotification("Daily quote",
                    "${it.textLatin}\n— ${it.author}\nOpen Runatal to view this quote.", it)
            }
            NotificationKind.STREAK -> {
                val stats = readings.stats().first()
                if (stats.lastReadDate != day.minusDays(1) || stats.streakDays <= 0) null
                else PreparedNotification("Continue your reading streak",
                    "You've read on ${stats.streakDays} consecutive days. Read a quote today to continue.")
            }
            NotificationKind.PACKS -> null
        }

    private suspend fun sourceIsCurrent(payload: PreparedNotification): Boolean {
        val expected = payload.sourceQuote ?: return true
        val current = quotes.getQuoteById(expected.id) ?: return false
        return current.textLatin == expected.textLatin && current.author == expected.author
    }

    private data class PreparedNotification(
        val title: String, val body: String, val sourceQuote: com.po4yka.runatal.domain.model.Quote? = null
    )

    private suspend fun deliverPackUpdates() {
        val current = catalogue.fingerprints()
        val seen = preferences.notificationPackCatalogue()
        if (seen == null) {
            // First install establishes a baseline; it is not an invented update event.
            preferences.markNotificationPackCatalogue(current)
            return
        }
        val changed = catalogue.changedPackNames(seen, current)
        if (changed.isEmpty()) return
        currentCoroutineContext().ensureActive()
        if (!preferences.notificationPreferencesFlow.first().packUpdates ||
            !publisher.snapshot().allows(NotificationKind.PACKS)) return
        postReserved(NotificationKind.PACKS, "Bundled quote packs updated", changed.joinToString("\n"),
            reserve = { preferences.markNotificationPackCatalogue(current) },
            rollback = { preferences.rollbackNotificationCatalogue(current, seen) },
            beforePost = { preferences.notificationPreferencesFlow.first().packUpdates })
    }

    /** Reserves before posting: process death in between may miss an optional reminder, but cannot duplicate it. */
    private suspend fun postReserved(
        kind: NotificationKind, title: String, body: String,
        reserve: suspend () -> Unit, rollback: suspend () -> Unit, beforePost: suspend () -> Boolean
    ) {
        try {
            reserve()
            currentCoroutineContext().ensureActive()
            if (!beforePost() || !publisher.post(kind, title, body)) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { rollback() }
            }
        } catch (exception: kotlinx.coroutines.CancellationException) {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { rollback() }
            } catch (rollbackFailure: java.io.IOException) {
                exception.addSuppressed(rollbackFailure)
            }
            throw exception
        }
    }

    suspend fun cancel(kind: NotificationKind) = deliveryMutex.withLock { publisher.cancel(kind) }
}
