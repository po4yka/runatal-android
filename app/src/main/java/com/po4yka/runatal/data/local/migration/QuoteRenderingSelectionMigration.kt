package com.po4yka.runatal.data.local.migration

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant

/** Adds explicit rendering intent; automatic or ambiguous cached output never chooses it. */
internal object QuoteRenderingSelectionMigration {
    fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE quotes ADD COLUMN renderingMode TEXT NOT NULL DEFAULT 'TRANSLITERATE'")
        connection.execSQL("ALTER TABLE quotes ADD COLUMN renderingFidelity TEXT NOT NULL DEFAULT 'STRICT'")
        connection.execSQL("ALTER TABLE quotes ADD COLUMN renderingYoungerVariant TEXT NOT NULL DEFAULT 'LONG_BRANCH'")
        val manualSelections = connection.prepare(
            "SELECT q.id,t.script,t.fidelity,t.variant FROM quotes q JOIN translation_records t " +
                "ON t.quoteId=q.id AND t.sourceText=q.textLatin " +
                "WHERE t.isBackfilled=0 AND t.resolutionStatus IN ('ATTESTED','RECONSTRUCTED','APPROXIMATED') " +
                "AND t.glyphOutput<>'' " +
                "AND t.script IN ('ELDER_FUTHARK','YOUNGER_FUTHARK','CIRTH')"
        ).use { statement ->
            buildList {
                while (statement.step()) {
                    add(ManualSelection(statement.getLong(0), statement.getText(1),
                        statement.getText(2), statement.getText(3)))
                }
            }
        }
        manualSelections.groupBy { it.quoteId }.forEach { (quoteId, records) ->
            val fidelities = records.map { it.fidelity }.toSet()
            val variants = records.filter { it.script == "YOUNGER_FUTHARK" }.map { it.variant }.toSet()
            val fidelity = fidelities.singleOrNull()
            val variant = if (variants.isEmpty()) YoungerFutharkVariant.LONG_BRANCH.name else variants.singleOrNull()
            if (fidelity in TranslationFidelity.entries.map { it.name } &&
                variant in YoungerFutharkVariant.entries.map { it.name }) {
                connection.prepare(
                    "UPDATE quotes SET renderingMode='TRANSLATE',renderingFidelity=?," +
                        "renderingYoungerVariant=? WHERE id=?"
                ).use { statement ->
                    statement.bindText(1, checkNotNull(fidelity))
                    statement.bindText(2, checkNotNull(variant))
                    statement.bindLong(3, quoteId)
                    statement.step()
                }
            }
        }
    }

    private data class ManualSelection(val quoteId: Long, val script: String, val fidelity: String, val variant: String)
}
