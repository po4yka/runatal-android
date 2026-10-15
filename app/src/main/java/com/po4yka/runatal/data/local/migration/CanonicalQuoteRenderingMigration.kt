package com.po4yka.runatal.data.local.migration

import androidx.sqlite.SQLiteConnection
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.seed.QuoteSeedData

/** Repairs exact frozen seed renderings without replacing rows or overwriting user-owned strings. */
internal object CanonicalQuoteRenderingMigration {
    fun migrate(connection: SQLiteConnection) {
        val current = QuoteSeedData.getCanonicalQuotes().associateBy { it.canonicalKey }
        QuoteSeedData.getLegacyQuotes().forEach { legacy ->
            val replacement = current.getValue(legacy.canonicalKey)
            val fields = listOf(
                Triple("runicElder", legacy.runicElder, replacement.runicElder),
                Triple("runicYounger", legacy.runicYounger, replacement.runicYounger),
                Triple("runicCirth", legacy.runicCirth, replacement.runicCirth)
            )
            fields.forEach { (column, oldGlyphs, newGlyphs) ->
                if (oldGlyphs != null && newGlyphs != null && oldGlyphs != newGlyphs) {
                    repairField(connection, legacy, column, oldGlyphs, newGlyphs)
                }
            }
        }
    }

    private fun repairField(
        connection: SQLiteConnection,
        legacy: QuoteEntity,
        column: String,
        oldGlyphs: String,
        newGlyphs: String
    ) {
        connection.prepare(
            "UPDATE quotes SET $column = ? WHERE canonicalKey = ? AND textLatin = ? AND author = ? " +
                "AND $column = ? AND isUserCreated = 0"
        ).use { statement ->
            statement.bindText(1, newGlyphs)
            statement.bindText(2, requireNotNull(legacy.canonicalKey))
            statement.bindText(SOURCE_PARAMETER, legacy.textLatin)
            statement.bindText(AUTHOR_PARAMETER, legacy.author)
            statement.bindText(OLD_GLYPH_PARAMETER, oldGlyphs)
            statement.step()
        }
    }
    private const val SOURCE_PARAMETER = 3
    private const val AUTHOR_PARAMETER = 4
    private const val OLD_GLYPH_PARAMETER = 5

}
