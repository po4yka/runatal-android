package com.po4yka.runatal.data.local.migration

import androidx.sqlite.SQLiteConnection
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator

/** Repairs only proven direct renderings from the converter that dropped the second x/q sound. */
internal object ElderFutharkSequenceMigration {
    fun migrate(connection: SQLiteConnection) {
        val converter = ElderFutharkTransliterator()
        val rows = connection.prepare("SELECT id,textLatin,runicElder FROM quotes WHERE runicElder IS NOT NULL")
            .use { statement ->
                buildList {
                    while (statement.step()) {
                        add(StoredRendering(statement.getLong(0), statement.getText(1), statement.getText(2)))
                    }
                }
            }
        rows.filter { row ->
            row.source.any { it in "xXqQ" } && row.glyphs == converter.transliterate(legacySource(row.source))
        }.forEach { row ->
            connection.prepare("UPDATE quotes SET runicElder=? WHERE id=? AND runicElder=?").use { statement ->
                statement.bindText(1, converter.transliterate(row.source))
                statement.bindLong(2, row.id)
                statement.bindText(3, row.glyphs)
                statement.step()
            }
        }
    }

    // This recognition transform exists only in the one-time migration. Replacing x/q with k cannot
    // introduce the converter's th/ng digraphs, so their precedence remains identical to the old map.
    private fun legacySource(source: String): String = source.map { if (it in "xXqQ") 'k' else it }.joinToString("")

    private data class StoredRendering(val id: Long, val source: String, val glyphs: String)
}
