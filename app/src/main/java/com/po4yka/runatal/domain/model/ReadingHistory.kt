package com.po4yka.runatal.domain.model

import java.time.LocalDate

/** A quote actually opened on a reading surface. */
data class QuoteReading(val quote: Quote, val script: RunicScript, val readAt: Long)

/** Reading activity calculated from persisted calendar days. */
data class ReadingStats(val streakDays: Int, val totalDays: Int, val firstReadDate: LocalDate?) {
    /** Computes a streak ending today or yesterday; earlier gaps end the streak. */
    companion object {
        /** Derives activity from observed dates and the current local calendar date. */
        fun fromDays(days: List<LocalDate>, today: LocalDate): ReadingStats {
            val dates = days.filter { it <= today }.toSet()
            var cursor = if (today in dates) today else today.minusDays(1)
            var streak = 0
            while (cursor in dates) {
                streak++
                cursor = cursor.minusDays(1)
            }
            return ReadingStats(streak, dates.size, dates.minOrNull())
        }
    }
}
