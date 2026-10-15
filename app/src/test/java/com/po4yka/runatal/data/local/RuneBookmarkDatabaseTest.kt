package com.po4yka.runatal.data.local

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.seed.RuneReferenceSeedData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class RuneBookmarkDatabaseTest {
    private val context = RuntimeEnvironment.getApplication()
    private val name = "rune-bookmarks.db"
    private var database: RunatalDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(name)
    }

    @Test
    fun `saving and removing a rune persists through database reopen without replacing references`() = runTest {
        val first = open()
        val seed = RuneReferenceSeedData.getElderFutharkRunes().first()
        first.runeReferenceDao().insertAll(listOf(seed))
        val reference = first.runeReferenceDao().getAllFlow().first().single()
        first.runeReferenceDao().toggleBookmark(reference.id)
        assertThat(first.runeReferenceDao().getBookmarkedFlow().first()).containsExactly(reference)
        first.close()

        val reopened = open()
        assertThat(reopened.runeReferenceDao().observeBookmark(reference.id).first()).isTrue()
        assertThat(reopened.runeReferenceDao().getBookmarkedFlow().first()).containsExactly(reference)
        val revised = reference.copy(meaning = "Updated reference explanation")
        reopened.runeReferenceDao().insertAll(listOf(revised))
        assertThat(reopened.runeReferenceDao().observeBookmark(reference.id).first()).isTrue()
        assertThat(reopened.runeReferenceDao().getBookmarkedFlow().first()).containsExactly(revised)
        reopened.runeReferenceDao().toggleBookmark(reference.id)
        assertThat(reopened.runeReferenceDao().getBookmarkedFlow().first()).isEmpty()
        assertThat(reopened.runeReferenceDao().getById(reference.id)).isEqualTo(revised)
        val missing = runCatching { reopened.runeReferenceDao().toggleBookmark(99999) }.exceptionOrNull()
        assertThat(missing).isInstanceOf(IllegalStateException::class.java)
        assertThat(reopened.runeReferenceDao().getBookmarkedFlow().first()).isEmpty()
    }

    private fun open(): RunatalDatabase = Room.databaseBuilder(context, RunatalDatabase::class.java, name)
        .setDriver(AndroidSQLiteDriver()).build().also { database = it }
}
