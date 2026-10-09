package com.po4yka.runatal.data.local

import android.app.Application
import androidx.room3.Room
import androidx.room3.useReaderConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.RuneReferenceEntity
import com.po4yka.runatal.data.seed.RuneReferenceSeedData
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Full schema18 migration checks canonical proof, retained identities and bookmark transfer before FK deletion. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class RuneReferenceCanonicalMigrationTest {
    private val context = RuntimeEnvironment.getApplication()
    private val name = "rune-reference-canonical-migration.db"
    private var database: RunatalDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(name)
    }

    @Test
    fun `migration proves canonical tuples and moves duplicate bookmarks without changing custom metadata`() = runTest {
        val fehu = RuneReferenceSeedData.getElderFutharkRunes().first()
        val modified = fehu.copy(id = 3L, history = "User's personal annotation")
        val legacy = RuneReferenceSeedData.getKnownLegacyReferences().first().copy(id = 4L)
        createVersion18().use { connection ->
            listOf(fehu.copy(id = 1L), fehu.copy(id = 2L), modified, legacy).forEach { insert(connection, it) }
            connection.execSQL("INSERT INTO rune_bookmarks(runeId,createdAt) VALUES(2,99),(3,300),(4,400)")
        }
        database = Room.databaseBuilder(context, RunatalDatabase::class.java, name)
            .setDriver(AndroidSQLiteDriver()).addMigrations(RunatalDatabase.MIGRATION_18_19).build()
        val migrated = checkNotNull(database)
        val dao = migrated.runeReferenceDao()
        assertThat(dao.getCount()).isEqualTo(RuneReferenceSeedData.getCanonicalReferences().size + 1)
        assertThat(dao.getById(1L)?.canonicalKey).isEqualTo("elder_futhark:Fehu")
        assertThat(dao.getById(2L)).isNull()
        assertThat(dao.getById(3L)).isEqualTo(modified)
        val updated = checkNotNull(dao.getById(4L))
        assertThat(updated.character).isEqualTo("ᛋ")
        assertThat(updated.canonicalKey).isEqualTo("younger_futhark:Sol")
        assertThat(dao.isBookmarked(1L)).isTrue()
        assertThat(dao.isBookmarked(3L)).isTrue()
        assertThat(dao.isBookmarked(4L)).isTrue()
        assertThat(bookmarkTime(migrated, 1L)).isEqualTo(99L)
        assertThat(bookmarkTime(migrated, 4L)).isEqualTo(400L)
        val violations = migrated.useReaderConnection { connection ->
            connection.usePrepared("PRAGMA foreign_key_check") { it.step() }
        }
        assertThat(violations).isFalse()
    }

    private fun createVersion18(): SQLiteConnection {
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val connection = AndroidSQLiteDriver().open(file.absolutePath)
        val schemaFile = sequenceOf(
            File("schemas/com.po4yka.runatal.data.local.RunatalDatabase/18.json"),
            File("app/schemas/com.po4yka.runatal.data.local.RunatalDatabase/18.json")
        ).first { it.isFile }
        val schema = Json.parseToJsonElement(schemaFile.readText()).jsonObject.getValue("database").jsonObject
        schema.getValue("entities").jsonArray.forEach { element ->
            val entity = element.jsonObject
            val table = entity.getValue("tableName").jsonPrimitive.content
            connection.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            entity["indices"]?.jsonArray.orEmpty().forEach { index ->
                connection.execSQL(
                    index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table)
                )
            }
        }
        schema.getValue("setupQueries").jsonArray.forEach { connection.execSQL(it.jsonPrimitive.content) }
        connection.execSQL("PRAGMA user_version = 18")
        return connection
    }

    private fun insert(connection: SQLiteConnection, reference: RuneReferenceEntity) {
        connection.prepare(
            "INSERT INTO rune_references(id,character,name,pronunciation,meaning,history,script) VALUES(?,?,?,?,?,?,?)"
        ).use { statement ->
            statement.bindLong(1, reference.id)
            statement.bindText(2, reference.character)
            statement.bindText(3, reference.name)
            statement.bindText(4, reference.pronunciation)
            statement.bindText(5, reference.meaning)
            statement.bindText(6, reference.history)
            statement.bindText(7, reference.script)
            statement.step()
        }
    }

    private suspend fun bookmarkTime(database: RunatalDatabase, id: Long): Long =
        database.useReaderConnection { connection ->
            connection.usePrepared("SELECT createdAt FROM rune_bookmarks WHERE runeId=?") { statement ->
                statement.bindLong(1, id)
                check(statement.step())
                statement.getLong(0)
            }
        }
}
