package com.po4yka.runatal.data.local

import app.cash.turbine.test
import android.app.Application
import androidx.room3.Room
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import android.database.SQLException
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.PackQuoteEntity
import com.po4yka.runatal.data.local.entity.QuotePackEntity
import com.po4yka.runatal.data.repository.QuotePackRepositoryImpl
import com.po4yka.runatal.data.seed.QuotePackSeedData
import com.po4yka.runatal.domain.model.QuotePackContentCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Real generated Room transactions and native SQLite, rather than a mock membership flag. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class QuotePackContentDatabaseTest {
    private lateinit var database: RunatalDatabase
    private lateinit var repository: QuotePackRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), RunatalDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).build()
        repository = QuotePackRepositoryImpl(database.quotePackDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `truthful counts previews and installed content come from the same 24 real quotes`() = runTest {
        repository.seedIfNeeded()
        val packs = repository.getAllPacksFlow().first()
        assertThat(packs).hasSize(6)
        packs.forEach { pack ->
            val preview = QuotePackContentCatalog.previewQuotes(pack)
            assertThat(preview).hasSize(4)
            assertThat(pack.quoteCount).isEqualTo(preview.size)
            assertThat(QuotePackSeedData.getPackQuotes(pack.id).map { it.textLatin })
                .containsExactlyElementsIn(preview.map { it.text }).inOrder()
            assertThat(QuotePackContentCatalog.readTimeLabel(pack)).isEqualTo("~1 min read")
        }
        assertThat(packs.flatMap { QuotePackContentCatalog.previewQuotes(it) }.map { it.text }.distinct()).hasSize(24)
        assertThat(database.quoteDao().getAll()).hasSize(8)
        repository.setLibraryMembership(1L, true)
        val expected = QuotePackSeedData.getPackQuotes(1L)
        assertThat(database.quoteDao().getAll().map { it.textLatin })
            .containsAtLeastElementsIn(expected.map { it.textLatin })
        assertThat(database.quoteDao().getAll()).hasSize(12)
        assertThat(repository.getPackById(1L)?.isInLibrary).isTrue()
    }

    @Test
    fun `repeated add remove and reseed never duplicate or cascade installed memberships`() = runTest {
        repository.seedIfNeeded()
        repeat(3) { repository.setLibraryMembership(1L, true) }
        val firstIds = database.quoteDao().getAll().map { it.id }
        repeat(3) { QuotePackRepositoryImpl(database.quotePackDao()).seedIfNeeded() }
        assertThat(database.quoteDao().getAll().map { it.id }).containsExactlyElementsIn(firstIds)
        assertThat(membershipCount(1L)).isEqualTo(4L)
        repeat(3) { repository.setLibraryMembership(1L, false) }
        assertThat(database.quoteDao().getAll()).hasSize(8)
        assertThat(membershipCount(1L)).isEqualTo(0L)
        assertThat(membershipCount(2L)).isEqualTo(4L)
        assertThat(membershipCount(5L)).isEqualTo(4L)
        assertThat(repository.getPackById(1L)?.isInLibrary).isFalse()
    }

    @Test
    fun `remove preserves favorite user shared and independently owned library quotes`() = runTest {
        val content = QuotePackSeedData.getPackQuotes(1L)
        val user = content[0].copy(id = 77L, canonicalKey = null, isUserCreated = true, isFavorite = true)
        database.quoteDao().insert(user)
        repository.seedIfNeeded()
        repository.setLibraryMembership(1L, true)
        val installed = database.quoteDao().getAll()
        val favorite = installed.single { it.textLatin == content[1].textLatin }
        val shared = installed.single { it.textLatin == content[2].textLatin }
        database.quoteDao().updateFavoriteStatus(favorite.id, true)
        database.quotePackDao().insert(QuotePackEntity(99L, "Shared custom pack", "Shared content", "ᚠ", 1, true))
        database.quotePackDao().insertMembership(PackQuoteEntity(99L, shared.id))
        repository.setLibraryMembership(1L, false)
        assertThat(database.quoteDao().getById(user.id)).isEqualTo(user)
        assertThat(database.quoteDao().getById(favorite.id)?.isFavorite).isTrue()
        assertThat(database.quoteDao().getById(shared.id)?.textLatin).isEqualTo(shared.textLatin)
        assertThat(database.quoteDao().getAll()).hasSize(11)
        assertThat(membershipCount(1L)).isEqualTo(0L)
        assertThat(membershipCount(99L)).isEqualTo(1L)
        repository.setLibraryMembership(1L, true)
        assertThat(database.quoteDao().getAll()).hasSize(12)
        assertThat(membershipCount(1L)).isEqualTo(4L)
    }

    @Test
    fun `content collision rolls back earlier inserted quotes joins and membership flag`() = runTest {
        repository.seedIfNeeded()
        val collision = QuotePackSeedData.getPackQuotes(1L)[2].copy(textLatin = "Preserve changed content")
        val collisionId = database.quoteDao().insert(collision)
        try {
            repository.setLibraryMembership(1L, true)
            error("Expected canonical identity collision")
        } catch (expected: IllegalStateException) {
            assertThat(expected.message).contains("identity conflicts")
        }
        assertThat(database.quoteDao().getAll()).hasSize(9)
        assertThat(database.quoteDao().getById(collisionId)?.textLatin).isEqualTo("Preserve changed content")
        assertThat(membershipCount(1L)).isEqualTo(0L)
        assertThat(repository.getPackById(1L)?.isInLibrary).isFalse()
        database.useWriterConnection { connection ->
            connection.usePrepared("DELETE FROM quotes WHERE id=?") { statement ->
                statement.bindLong(1, collisionId)
                statement.step()
            }
        }
        repository.setLibraryMembership(1L, true)
        assertThat(database.quoteDao().getAll()).hasSize(12)
    }

    @Test
    fun `competing install commands use one canonical identity per quote`() = runTest {
        repository.seedIfNeeded()
        (1..6).map {
            async(Dispatchers.Default) { repository.setLibraryMembership(1L, true) }
        }.awaitAll()
        assertThat(database.quoteDao().getAll()).hasSize(12)
        assertThat(membershipCount(1L)).isEqualTo(4L)
        assertThat(repository.getPackById(1L)?.isInLibrary).isTrue()
    }

    @Test
    fun `storage failure rolls back quote insertion joins and metadata together`() = runTest {
        repository.seedIfNeeded()
        database.useWriterConnection { connection ->
            connection.usePrepared(
                "CREATE TRIGGER fail_install BEFORE UPDATE OF isInLibrary ON quote_packs " +
                    "WHEN NEW.id=1 AND NEW.isInLibrary=1 BEGIN SELECT RAISE(ABORT,'storage failure'); END"
            ) { it.step() }
        }
        try {
            repository.setLibraryMembership(1L, true)
            error("Expected SQLite storage failure")
        } catch (expected: SQLException) {
            assertThat(expected.message).contains("storage failure")
        }
        assertThat(database.quoteDao().getAll()).hasSize(8)
        assertThat(membershipCount(1L)).isEqualTo(0L)
        assertThat(repository.getPackById(1L)?.isInLibrary).isFalse()
        database.useWriterConnection { connection ->
            connection.usePrepared("DROP TRIGGER fail_install") { it.step() }
        }
        repository.setLibraryMembership(1L, true)
        assertThat(database.quoteDao().getAll()).hasSize(12)
        database.useWriterConnection { connection ->
            connection.usePrepared(
                "CREATE TRIGGER fail_remove BEFORE UPDATE OF isInLibrary ON quote_packs " +
                    "WHEN NEW.id=1 AND NEW.isInLibrary=0 BEGIN SELECT RAISE(ABORT,'remove failure'); END"
            ) { it.step() }
        }
        try {
            repository.setLibraryMembership(1L, false)
            error("Expected SQLite removal failure")
        } catch (expected: SQLException) {
            assertThat(expected.message).contains("remove failure")
        }
        assertThat(database.quoteDao().getAll()).hasSize(12)
        assertThat(membershipCount(1L)).isEqualTo(4L)
        assertThat(repository.getPackById(1L)?.isInLibrary).isTrue()
    }

    @Test
    fun `reinstall preserves retained quotes and cannot resurrect explicitly purged pack sources`() = runTest {
        repository.seedIfNeeded()
        repository.setLibraryMembership(1L, true)
        val content = requireNotNull(database.quotePackDao().findContent(
            QuotePackSeedData.getPackQuotes(1L).first().textLatin,
            QuotePackSeedData.getPackQuotes(1L).first().author,
            checkNotNull(QuotePackSeedData.getPackQuotes(1L).first().canonicalKey)
        ))
        database.archivedQuoteDao().updateState(content.id, "ACTIVE", "HIDDEN", "hide", 1L)
        repository.setLibraryMembership(1L, false)
        repository.setLibraryMembership(1L, true)
        assertThat(database.archivedQuoteDao().getRetainedById(content.id)?.lifecycleState).isEqualTo("HIDDEN")
        assertThat(database.quotePackDao().availableCount(1L)).isEqualTo(3)
        assertThat(repository.getPackById(1L)?.quoteCount).isEqualTo(3)
        database.archivedQuoteDao().updateState(content.id, "HIDDEN", "TRASH", "trash", 2L)
        database.archivedQuoteDao().emptyTrash(3L)
        repository.setLibraryMembership(1L, false)
        repository.setLibraryMembership(1L, true)
        QuotePackRepositoryImpl(database.quotePackDao()).seedIfNeeded()
        assertThat(database.quotePackDao().wasPurged(checkNotNull(content.canonicalKey))).isTrue()
        assertThat(database.quotePackDao().availableCount(1L)).isEqualTo(3)
        assertThat(repository.getPackById(1L)?.quoteCount).isEqualTo(3)
        assertThat(database.quoteDao().getAll().none { it.canonicalKey == content.canonicalKey }).isTrue()
    }

    @Test
    fun `installed counts observe committed visibility changes without another metadata write`() = runTest {
        repository.seedIfNeeded()
        repository.setLibraryMembership(1L, true)
        val quote = database.quotePackDao().findContent(
            QuotePackSeedData.getPackQuotes(1L).first().textLatin,
            QuotePackSeedData.getPackQuotes(1L).first().author,
            checkNotNull(QuotePackSeedData.getPackQuotes(1L).first().canonicalKey)
        ) ?: error("Installed content is missing")
        repository.getAllPacksFlow().test {
            assertThat(awaitItem().first { it.id == 1L }.quoteCount).isEqualTo(4)
            database.archivedQuoteDao().updateState(quote.id, "ACTIVE", "ARCHIVED", "archive", 1L)
            assertThat(awaitItem().first { it.id == 1L }.quoteCount).isEqualTo(3)
            assertThat(repository.getLibraryPacksFlow().first().first { it.id == 1L }.quoteCount).isEqualTo(3)
            assertThat(repository.searchPacks("Hávamál").first().first { it.id == 1L }.quoteCount).isEqualTo(3)
            database.archivedQuoteDao().updateState(quote.id, "ARCHIVED", "ACTIVE", "restore", 2L)
            assertThat(awaitItem().first { it.id == 1L }.quoteCount).isEqualTo(4)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun membershipCount(packId: Long): Long = database.useReaderConnection { connection ->
        connection.usePrepared("SELECT COUNT(*) FROM pack_quotes WHERE packId=?") { statement ->
            statement.bindLong(1, packId)
            check(statement.step())
            statement.getLong(0)
        }
    }
}
