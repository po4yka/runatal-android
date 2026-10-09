package com.po4yka.runatal.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.po4yka.runatal.data.local.entity.QuoteReadEntity
import com.po4yka.runatal.data.local.entity.ReadingDayEntity
import kotlinx.coroutines.flow.Flow

/** Transactional reading history and content-free activity dates. */
@Dao
internal interface ReadingHistoryDao {
    @Transaction
    suspend fun record(read: QuoteReadEntity) {
        if (!quoteExists(read.quoteId)) return
        insertRead(read)
        insertDay(ReadingDayEntity(read.epochDay))
    }

    @Query("SELECT EXISTS(SELECT 1 FROM quotes WHERE id = :quoteId)")
    suspend fun quoteExists(quoteId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRead(read: QuoteReadEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDay(day: ReadingDayEntity)

    @Query("SELECT * FROM quote_reads ORDER BY readAt DESC, id DESC LIMIT 500")
    fun readings(): Flow<List<QuoteReadEntity>>

    @Query("SELECT epochDay FROM reading_days ORDER BY epochDay ASC")
    fun days(): Flow<List<Long>>
}
