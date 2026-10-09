package com.po4yka.runatal.domain.repository

import com.po4yka.runatal.domain.model.QuoteReading
import com.po4yka.runatal.domain.model.ReadingStats
import com.po4yka.runatal.domain.model.RunicScript
import kotlinx.coroutines.flow.Flow

/** Persists actual reading activity, independently of quote creation. */
interface ReadingHistoryRepository {
    /** Records one reading per quote, script and day. */
    suspend fun recordRead(quoteId: Long, script: RunicScript)
    /** Most recent readings with their still-available quotes. */
    fun readings(): Flow<List<QuoteReading>>
    /** Persisted activity and contiguous reading streak. */
    fun stats(): Flow<ReadingStats>
}
