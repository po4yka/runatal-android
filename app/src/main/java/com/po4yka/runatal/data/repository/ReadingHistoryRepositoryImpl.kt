package com.po4yka.runatal.data.repository

import com.po4yka.runatal.data.local.dao.ReadingHistoryDao
import com.po4yka.runatal.data.local.entity.QuoteReadEntity
import com.po4yka.runatal.domain.model.QuoteReading
import com.po4yka.runatal.domain.model.ReadingStats
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.repository.ReadingHistoryRepository
import com.po4yka.runatal.util.TimeProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Uses Room as the source of truth for reading history and streaks. */
@Singleton
internal class ReadingHistoryRepositoryImpl @Inject constructor(
    private val dao: ReadingHistoryDao,
    private val quotes: QuoteRepository,
    private val time: TimeProvider
) : ReadingHistoryRepository {
    override suspend fun recordRead(quoteId: Long, script: RunicScript) {
        dao.record(QuoteReadEntity(
            quoteId = quoteId, epochDay = time.getCurrentDate().toEpochDay(),
            script = script.name, readAt = System.currentTimeMillis()
        ))
    }

    override fun readings(): Flow<List<QuoteReading>> = combine(dao.readings(), quotes.getAllQuotesFlow()) {
        reads, library ->
        val byId = library.associateBy { it.id }
        reads.mapNotNull { read ->
            byId[read.quoteId]?.let { QuoteReading(it, RunicScript.valueOf(read.script), read.readAt) }
        }
    }

    override fun stats(): Flow<ReadingStats> = dao.days().map { days ->
        ReadingStats.fromDays(days.map(LocalDate::ofEpochDay), time.getCurrentDate())
    }
}
