package com.po4yka.runatal.data.repository

import android.app.Application
import android.database.sqlite.SQLiteException
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class StorageWritesTest {

    @Test
    fun `SQLite storage failure retains its cause in the I O boundary`() = runTest {
        val original = SQLiteException("disk write failed")

        val failure = runCatching { storageWrite<Unit> { throw original } }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(failure?.cause).isSameInstanceAs(original)
    }

    @Test
    fun `cancellation and validation failures propagate unchanged`() = runTest {
        val cancellation = CancellationException("cancelled")
        val validation = IllegalStateException("invalid source")

        listOf(cancellation, validation).forEach { original ->
            val failure = runCatching { storageWrite<Unit> { throw original } }.exceptionOrNull()
            assertThat(failure).isSameInstanceAs(original)
        }
    }
}
