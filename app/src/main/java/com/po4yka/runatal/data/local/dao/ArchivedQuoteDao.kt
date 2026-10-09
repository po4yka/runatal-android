package com.po4yka.runatal.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.domain.model.QuoteLifecycleChange
import com.po4yka.runatal.domain.model.QuoteLifecycleState
import kotlinx.coroutines.flow.Flow

/** Authoritative quote lifecycle operations; visibility changes retain all quote fields and related rows. */
@Dao
internal interface ArchivedQuoteDao {
    @Query("SELECT * FROM quotes WHERE lifecycleState != 'ACTIVE' ORDER BY lifecycleChangedAt DESC, id DESC")
    fun getRetainedFlow(): Flow<List<QuoteEntity>>

    /** Full retained snapshot; normal reader surfaces use the active-only QuoteDao lookup. */
    @Query("SELECT * FROM quotes WHERE id = :id")
    suspend fun getRetainedById(id: Long): QuoteEntity?

    @Transaction
    suspend fun transition(
        id: Long,
        expectedState: QuoteLifecycleState,
        nextState: QuoteLifecycleState,
        mutationId: String,
        changedAt: Long,
        onlyUserCreated: Boolean = false
    ): QuoteLifecycleChange {
        require(expectedState != nextState)
        val quote = checkNotNull(getRetainedById(id)) { "The quote no longer exists." }
        check(quote.lifecycleState == expectedState.name && (!onlyUserCreated || quote.isUserCreated)) {
            "The quote state changed. Reload it before trying again."
        }
        check(updateState(id, expectedState.name, nextState.name, mutationId, changedAt) == 1)
        return QuoteLifecycleChange(id, expectedState, nextState, quote.lifecycleChangedAt, mutationId)
    }

    @Query(
        "UPDATE quotes SET lifecycleState = :nextState, lifecycleChangedAt = :changedAt, " +
            "lifecycleMutationId = :mutationId WHERE id = :id AND lifecycleState = :expectedState"
    )
    suspend fun updateState(
        id: Long, expectedState: String, nextState: String, mutationId: String, changedAt: Long
    ): Int

    /** Restores the complete batch or none if one snapshot is stale or a database write fails. */
    @Transaction
    suspend fun restoreBatch(
        quotes: List<Pair<Long, QuoteLifecycleState>>, mutationId: String, changedAt: Long
    ): List<QuoteLifecycleChange> {
        require(quotes.map { it.first }.distinct().size == quotes.size)
        require(quotes.all { it.second != QuoteLifecycleState.ACTIVE })
        return quotes.map { (id, state) -> transition(id, state, QuoteLifecycleState.ACTIVE, mutationId, changedAt) }
    }

    /** Token checks prevent late snackbar undo from replacing a newer lifecycle action or a purged quote. */
    @Transaction
    suspend fun undoBatch(changes: List<QuoteLifecycleChange>) {
        require(changes.map { it.quoteId }.distinct().size == changes.size)
        changes.forEach { change ->
            val undone = undoState(
                change.quoteId, change.state.name, change.previousState.name,
                change.previousChangedAt, change.mutationId
            )
            check(undone == 1) {
                "The quote changed after this action and cannot be undone."
            }
        }
    }

    @Query(
        "UPDATE quotes SET lifecycleState = :previousState, lifecycleChangedAt = :previousChangedAt, " +
            "lifecycleMutationId = NULL WHERE id = :id AND lifecycleState = :state " +
            "AND lifecycleMutationId = :mutationId"
    )
    suspend fun undoState(
        id: Long, state: String, previousState: String, previousChangedAt: Long, mutationId: String
    ): Int

    /** Only the explicit purge operation physically deletes retained quotes and their cascading metadata. */
    @Transaction
    suspend fun emptyTrash(purgedAt: Long) {
        rememberCanonicalPurges(purgedAt)
        purgeTrash()
    }

    @Query(
        "INSERT OR IGNORE INTO canonical_quote_tombstones(canonicalKey, purgedAt) " +
            "SELECT canonicalKey, :purgedAt FROM quotes WHERE lifecycleState = 'TRASH' AND canonicalKey IS NOT NULL"
    )
    suspend fun rememberCanonicalPurges(purgedAt: Long)

    @Query("DELETE FROM quotes WHERE lifecycleState = 'TRASH'")
    suspend fun purgeTrash()
}
