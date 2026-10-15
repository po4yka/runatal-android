package com.po4yka.runatal.data.repository

import android.database.sqlite.SQLiteException
import java.io.IOException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Exposes storage failures through the repository's I/O contract without masking cancellation or validation. */
internal suspend fun <T> storageWrite(block: suspend () -> T): T = try {
    block()
} catch (exception: SQLiteException) {
    currentCoroutineContext().ensureActive()
    throw IOException("Database write failed.", exception)
}
