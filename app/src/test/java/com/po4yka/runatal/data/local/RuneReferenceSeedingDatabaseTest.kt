package com.po4yka.runatal.data.local

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.room3.Room
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.RuneReferenceEntity
import com.po4yka.runatal.data.repository.RuneReferenceRepositoryImpl
import com.po4yka.runatal.data.seed.RuneReferenceCanonicalIdentity
import com.po4yka.runatal.data.seed.RuneReferenceSeedData
import com.po4yka.runatal.domain.model.RuneReference
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
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

/** Real Room reconciliation tests include partial data, duplicate bookmark ownership and competing writers. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class RuneReferenceSeedingDatabaseTest {
    private val context = RuntimeEnvironment.getApplication()
    private val name = "canonical-rune-seeding.db"
    private lateinit var database: RunatalDatabase
    private lateinit var repository: RuneReferenceRepositoryImpl
    private val current = RuneReferenceSeedData.getCanonicalReferences()
    private val custom = RuneReferenceEntity(
        character = "ᚠ", name = "My reference", pronunciation = "mine", meaning = "Personal", history = "My notes",
        script = "elder_futhark"
    )

    @Before
    fun setUp() {
        context.deleteDatabase(name)
        database = Room.databaseBuilder(context, RunatalDatabase::class.java, name)
            .setDriver(AndroidSQLiteDriver()).build()
        repository = RuneReferenceRepositoryImpl(database.runeReferenceDao())
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun `partial seed preserves known ids and bookmarks without guessing modified same-name rows`() = runTest {
        val dao = database.runeReferenceDao()
        val fehu = RuneReferenceSeedData.getElderFutharkRunes().first()
        val modified = fehu.copy(id = 81L, history = "Personal annotation")
        dao.insertAll(listOf(custom.copy(id = 1L), fehu.copy(id = 55L), fehu.copy(id = 80L), modified))
        dao.insertBookmark(55L, 100L)
        dao.insertBookmark(80L, 200L)
        dao.insertBookmark(81L, 300L)
        repository.seedIfNeeded()
        repository.seedIfNeeded()

        val rows = dao.getAllFlow().first()
        assertThat(rows.count { it.canonicalKey != null }).isEqualTo(current.size)
        assertThat(rows).hasSize(current.size + 2)
        assertThat(dao.getById(55L)?.canonicalKey).isEqualTo("elder_futhark:Fehu")
        assertThat(dao.getById(80L)).isNull()
        assertThat(dao.getById(81L)).isEqualTo(modified)
        assertThat(dao.getById(1L)).isEqualTo(custom.copy(id = 1L))
        assertThat(dao.getBookmarkedFlow().first().map { it.id }).containsExactly(55L, 81L)
        assertThat(bookmarkTime(55L)).isEqualTo(100L)
        assertThat(bookmarkTime(81L)).isEqualTo(300L)
    }

    @Test
    fun `concurrent fresh seeding and a custom insert create one canonical identity each`() = runTest {
        val start = CompletableDeferred<Unit>()
        val jobs = (1..8).map {
            async(Dispatchers.IO) {
                start.await()
                RuneReferenceRepositoryImpl(database.runeReferenceDao()).seedIfNeeded()
            }
        }
        val writer = async(Dispatchers.IO) {
            start.await()
            database.runeReferenceDao().insertAll(listOf(custom))
        }
        start.complete(Unit)
        jobs.awaitAll()
        writer.await()
        val rows = database.runeReferenceDao().getAllFlow().first()
        assertThat(rows).hasSize(current.size + 1)
        assertThat(rows.mapNotNull { it.canonicalKey }.distinct()).hasSize(current.size)
        assertThat(rows.single { it.name == custom.name }.history).isEqualTo(custom.history)
    }

    @Test
    fun `versioned canonical updates preserve ids and bookmarks while manual metadata stays protected`() = runTest {
        repository.seedIfNeeded()
        val dao = database.runeReferenceDao()
        val original = dao.matchingReferences("elder_futhark:Fehu", "elder_futhark", "Fehu").single()
        dao.insertBookmark(original.id, 42L)
        val next = RuneReferenceCanonicalIdentity.owned(
            current.first { it.name == "Fehu" }.copy(meaning = "Version two")
        )
        dao.seedCanonicalReferences(listOf(next), emptyList())
        val updated = checkNotNull(dao.getById(original.id))
        assertThat(updated.meaning).isEqualTo("Version two")
        assertThat(updated.canonicalFingerprint).isEqualTo(next.canonicalFingerprint)
        assertThat(bookmarkTime(original.id)).isEqualTo(42L)
        repository.insertAllRunes(listOf(RuneReference(
            updated.id, updated.character, updated.name, updated.pronunciation, updated.meaning, "My annotation",
            updated.script
        )))
        val manual = checkNotNull(dao.getById(original.id))
        assertThat(manual.canonicalKey).isEqualTo(updated.canonicalKey)
        assertThat(manual.canonicalFingerprint).isEqualTo(updated.canonicalFingerprint)
        val third = RuneReferenceCanonicalIdentity.owned(next.copy(meaning = "Version three"))
        dao.seedCanonicalReferences(listOf(third), emptyList())
        assertThat(dao.getById(original.id)).isEqualTo(manual)
        assertThat(bookmarkTime(original.id)).isEqualTo(42L)
    }

    @Test
    fun `physical mid-seed failure rolls back identity and data and a retry synchronizes the catalog`() = runTest {
        database.runeReferenceDao().insertAll(listOf(custom.copy(id = 1L)))
        database.useWriterConnection { connection ->
            connection.usePrepared("CREATE TRIGGER fail_seed BEFORE INSERT ON rune_references " +
                "WHEN NEW.name='Uruz' BEGIN SELECT RAISE(ABORT,'seed failed'); END") { it.step() }
        }
        val failure = runCatching { repository.seedIfNeeded() }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(failure?.cause).isInstanceOf(SQLiteException::class.java)
        assertThat(database.runeReferenceDao().getAllFlow().first()).containsExactly(custom.copy(id = 1L))
        database.useWriterConnection { connection ->
            connection.usePrepared("DROP TRIGGER fail_seed") { it.step() }
        }
        repository.seedIfNeeded()
        assertThat(database.runeReferenceDao().getCount()).isEqualTo(current.size + 1)
    }

    private suspend fun bookmarkTime(id: Long): Long = database.useReaderConnection { connection ->
        connection.usePrepared("SELECT createdAt FROM rune_bookmarks WHERE runeId=?") { statement ->
            statement.bindLong(1, id)
            check(statement.step())
            statement.getLong(0)
        }
    }
}
