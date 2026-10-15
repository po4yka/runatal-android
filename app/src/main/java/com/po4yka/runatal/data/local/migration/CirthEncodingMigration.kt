package com.po4yka.runatal.data.local.migration

import androidx.sqlite.SQLiteConnection
import com.po4yka.runatal.data.seed.QuoteSeedData
import com.po4yka.runatal.data.seed.RuneReferenceSeedData
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.translation.EnglishSyntaxParser
import com.po4yka.runatal.domain.translation.ParsedEnglishTokenType
import com.po4yka.runatal.domain.translation.stitchTokens
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One-time proof-based repair. Obsolete encodings never participate in runtime rendering. */
internal object CirthEncodingMigration {
    private const val PRIVATE_USE_START = 0xE000
    private const val PRIVATE_USE_END = 0xF8FF
    private const val HISTORY_PARAMETER = 5
    private const val REFERENCE_PARAMETER = 6
    private const val LEGACY_GLYPH_PARAMETER = 7
    private val current = CirthTransliterator()
    private val json = Json

    fun migrate(connection: SQLiteConnection) {
        repairQuotes(connection)
        repairRecords(connection)
        repairReferences(connection)
    }

    private fun repairQuotes(connection: SQLiteConnection) {
        val seeds = QuoteSeedData.getLegacyQuotes()
        val rows = connection.prepare(
            "SELECT q.id, q.textLatin, q.author, q.runicCirth, EXISTS(" +
                "SELECT 1 FROM translation_records t WHERE t.quoteId=q.id AND t.script='CIRTH' " +
                "AND t.sourceText=q.textLatin AND t.glyphOutput=q.runicCirth " +
                "AND t.engineVersion='cirth-translation-v4') " +
                "FROM quotes q WHERE q.runicCirth IS NOT NULL"
        ).use { statement ->
            buildList {
                while (statement.step()) add(QuoteRendering(statement.getLong(0), statement.getText(1),
                    statement.getText(2), statement.getText(3), statement.getLong(4) != 0L))
            }
        }
        rows.filter { row ->
            row.text.none { it.code in PRIVATE_USE_START..PRIVATE_USE_END } &&
                ((row.snapshot && isProvenStructured(row.text, row.glyphs)) ||
                    row.glyphs == legacyDirect(row.text) || seeds.any {
                    it.textLatin == row.text && it.author == row.author && it.runicCirth == row.glyphs
                })
        }.forEach { row ->
            connection.prepare("UPDATE quotes SET runicCirth=? WHERE id=? AND runicCirth=?").use {
                it.bindText(1, current.transliterate(row.text))
                it.bindLong(2, row.id)
                it.bindText(3, row.glyphs)
                it.step()
            }
        }
    }

    private fun isProvenStructured(text: String, glyphs: String): Boolean =
        knownPhrases[text.lowercase()] == glyphs || legacyStructured(text) == glyphs

    private fun repairRecords(connection: SQLiteConnection) {
        val rows = connection.prepare(
            "SELECT id, sourceText, glyphOutput, tokenBreakdownJson FROM translation_records " +
                "WHERE script='CIRTH' AND engineVersion='cirth-translation-v4' AND glyphOutput<>''"
        ).use { statement ->
            buildList {
                while (statement.step()) add(RecordRendering(statement.getLong(0), statement.getText(1),
                    statement.getText(2), statement.getText(3)))
            }
        }
        rows.filter { row ->
            row.text.none { it.code in PRIVATE_USE_START..PRIVATE_USE_END } &&
                isProvenStructured(row.text, row.glyphs)
        }.forEach { row ->
            val originalTokens = runCatching { json.parseToJsonElement(row.tokens).jsonArray }.getOrNull()
                ?: return@forEach
            val provenTokens = originalTokens.all { token ->
                val fields = token as? JsonObject ?: return@all false
                val normalized = (fields["normalizedToken"] as? JsonPrimitive)?.content ?: return@all false
                val glyphs = (fields["glyphToken"] as? JsonPrimitive)?.content ?: return@all false
                legacyWord(normalized.lowercase()) == glyphs
            }
            if (!provenTokens) return@forEach
            val tokens = originalTokens.map { token ->
                val fields = token.jsonObject.toMutableMap()
                fields["glyphToken"]?.jsonPrimitive?.content?.let {
                    fields["glyphToken"] = JsonPrimitive(convertEncoding(it))
                }
                JsonObject(fields)
            }
            connection.prepare(
                "UPDATE translation_records SET glyphOutput=?, tokenBreakdownJson=? WHERE id=? AND glyphOutput=?"
            ).use {
                it.bindText(1, convertEncoding(row.glyphs))
                it.bindText(2, JsonArray(tokens).toString())
                it.bindLong(3, row.id)
                it.bindText(4, row.glyphs)
                it.step()
            }
        }
    }

    private fun repairReferences(connection: SQLiteConnection) {
        val references = RuneReferenceSeedData.getCirthRunes()
        legacyReferences.forEach { (name, identity) ->
            val target = references.firstOrNull { it.pronunciation == identity.second } ?: return@forEach
            connection.prepare(
                "UPDATE rune_references SET character=?, name=?, pronunciation=?, meaning=?, history=? " +
                    "WHERE script='cirth' AND name=? AND character=?"
            ).use {
                it.bindText(1, target.character)
                it.bindText(2, target.name)
                it.bindText(3, target.pronunciation)
                it.bindText(4, target.meaning)
                it.bindText(HISTORY_PARAMETER, target.history)
                it.bindText(REFERENCE_PARAMETER, name)
                it.bindText(LEGACY_GLYPH_PARAMETER, identity.first)
                it.step()
            }
        }
        references.forEach { target ->
            connection.prepare(
                "INSERT INTO rune_references (character,name,pronunciation,meaning,history,script) " +
                    "SELECT ?,?,?,?,?, 'cirth' WHERE NOT EXISTS(" +
                    "SELECT 1 FROM rune_references WHERE script='cirth' AND character=?)"
            ).use {
                it.bindText(1, target.character)
                it.bindText(2, target.name)
                it.bindText(3, target.pronunciation)
                it.bindText(4, target.meaning)
                it.bindText(HISTORY_PARAMETER, target.history)
                it.bindText(REFERENCE_PARAMETER, target.character)
                it.step()
            }
        }
    }

    private fun convertEncoding(glyphs: String): String = glyphs.map { encodingMap[it] ?: it }.joinToString("")

    private fun legacyDirect(text: String): String {
        var value = text.lowercase()
        listOf("th", "ch", "sh", "ng").forEach { value = value.replace(it, legacySequences.getValue(it)) }
        return value.map { legacyLetters[it.toString()] ?: it.toString() }.joinToString("")
    }

    private fun legacyStructured(text: String): String {
        val tokens = EnglishSyntaxParser().parse(text).tokens
        return stitchTokens(tokens.map { token ->
            if (token.type == ParsedEnglishTokenType.WORD) legacyWord(token.normalized) else token.raw
        })
    }

    private fun legacyWord(token: String): String {
        var remaining = token
        return buildString {
            while (remaining.isNotEmpty()) {
                val sequence = legacySequences.keys.sortedByDescending { it.length }
                    .firstOrNull { remaining.startsWith(it) }
                if (sequence != null) {
                    append(legacySequences.getValue(sequence))
                    remaining = remaining.drop(sequence.length)
                } else {
                    append(legacyLetters[remaining.first().toString()] ?: remaining.first())
                    remaining = remaining.drop(1)
                }
            }
        }
    }

    private data class QuoteRendering(val id: Long, val text: String, val author: String,
        val glyphs: String, val snapshot: Boolean)
    private data class RecordRendering(val id: Long, val text: String, val glyphs: String, val tokens: String)

    private val legacyLetters = mapOf(
        "p" to "\ue080",
        "b" to "\ue081",
        "f" to "\ue082",
        "v" to "\ue083",
        "t" to "\ue088",
        "d" to "\ue089",
        "k" to "\ue090",
        "g" to "\ue091",
        "h" to "\ue092",
        "s" to "\ue09c",
        "z" to "\ue09d",
        "r" to "\ue0a0",
        "l" to "\ue0a8",
        "m" to "\ue0b0",
        "n" to "\ue0b4",
        "w" to "\ue0b8",
        "j" to "\ue0bc",
        "y" to "\ue0bd",
        "i" to "\ue0c8",
        "e" to "\ue0c9",
        "a" to "\ue0ca",
        "o" to "\ue0cb",
        "u" to "\ue0cc",
        "c" to "\ue090",
        "q" to "\ue090",
        "x" to "\ue09c",
        " " to " ",
        "." to ".",
        "," to ",",
        "!" to "!",
        "?" to "?",
        "'" to "'",
        "\"" to "\"",
        "-" to "-",
        ":" to ":",
        ";" to ";",
        "\u00fe" to "\ue08a"
    )
    private val legacySequences = mapOf(
        "ll" to "\ue0be",
        "mm" to "\ue0b0\ue0b0",
        "nn" to "\ue0b4\ue0b4",
        "rr" to "\ue0a0\ue0a0",
        "tt" to "\ue088\ue088",
        "aa" to "\ue0ca\ue0ca",
        "ee" to "\ue0c9\ue0c9",
        "ii" to "\ue0c8\ue0c8",
        "oo" to "\ue0cb\ue0cb",
        "uu" to "\ue0cc\ue0cc",
        "th" to "\ue08a",
        "sh" to "\ue09e",
        "ch" to "\ue093",
        "ng" to "\ue0b5",
        "gh" to "\ue0bb",
        "ph" to "\ue0b3",
        "qu" to "\ue0b2",
        "ai" to "\ue0ca\ue0c8",
        "ay" to "\ue0ca\ue0bd",
        "au" to "\ue0ca\ue0cc",
        "aw" to "\ue0ca\ue0b8",
        "ea" to "\ue0c9\ue0ca",
        "ei" to "\ue0c9\ue0c8",
        "ie" to "\ue0c8\ue0c9",
        "oa" to "\ue0cb\ue0ca",
        "ou" to "\ue0cb\ue0cc",
        "ow" to "\ue0cb\ue0b8"
    )
    private val encodingMap = mapOf(
        '\uE080' to '\uE080',
        '\uE081' to '\uE081',
        '\uE082' to '\uE082',
        '\uE083' to '\uE083',
        '\uE088' to '\uE087',
        '\uE089' to '\uE088',
        '\uE08A' to '\uE089',
        '\uE090' to '\uE091',
        '\uE091' to '\uE092',
        '\uE092' to '\uE0B9',
        '\uE09C' to '\uE0A1',
        '\uE09D' to '\uE0A3',
        '\uE0A0' to '\uE09C',
        '\uE0A8' to '\uE09E',
        '\uE0B0' to '\uE085',
        '\uE0B4' to '\uE08B',
        '\uE0B8' to '\uE0AC',
        '\uE0BC' to '\uE08D',
        '\uE0BD' to '\uE0E1',
        '\uE0C8' to '\uE0A7',
        '\uE0C9' to '\uE0AF',
        '\uE0CA' to '\uE0B1',
        '\uE0CB' to '\uE0B3',
        '\uE0CC' to '\uE0AA',
        '\uE093' to '\uE08C',
        '\uE09E' to '\uE08E',
        '\uE0B5' to '\uE095',
        '\uE0BB' to '\uE094',
        '\uE0B3' to '\uE082',
        '\uE0B2' to '\uE096',
        '\uE0BE' to '\uE0CE'
    )
    private val knownPhrases = mapOf(
        "the wolf hunts at night" to "\ue08a\ue0c9 \ue0b8\ue0cb\ue0a8\ue082 \ue092" +
            "\ue0cc\ue0b4\ue088\ue09c \ue0ca\ue088 \ue0b4" +
            "\ue0c8\ue0bb\ue088",
        "under the mountain" to "\ue0cc\ue0b4\ue089\ue0c9\ue0a0 \ue08a\ue0c9 " +
            "\ue0b0\ue0cb\ue0cc\ue0b4\ue088\ue0ca\ue0c8\ue0b4"
    )
    private val legacyReferences = mapOf(
        "Certh 1" to ("\uE080" to "p"),
        "Certh 2" to ("\uE081" to "b"),
        "Certh 3" to ("\uE082" to "f"),
        "Certh 4" to ("\uE083" to "v"),
        "Certh 9" to ("\uE088" to "t"),
        "Certh 10" to ("\uE089" to "d"),
        "Certh 13" to ("\uE08C" to "ch"),
        "Certh 14" to ("\uE08D" to "j"),
        "Certh 17" to ("\uE090" to "k"),
        "Certh 18" to ("\uE091" to "g"),
        "Certh 25" to ("\uE098" to "qu"),
        "Certh 29" to ("\uE09C" to "s"),
        "Certh 33" to ("\uE0A0" to "r"),
        "Certh 37" to ("\uE0A4" to "ng"),
        "Certh 41" to ("\uE0A8" to "l"),
        "Certh 49" to ("\uE0B0" to "m"),
        "Certh 53" to ("\uE0B4" to "n"),
        "Certh 57" to ("\uE0B8" to "w"),
        "Certh 61" to ("\uE0BC" to "y"),
        "Certh 73" to ("\uE0C8" to "i"),
        "Certh 74" to ("\uE0C9" to "e"),
        "Certh 75" to ("\uE0CA" to "a"),
        "Certh 76" to ("\uE0CB" to "o"),
        "Certh 77" to ("\uE0CC" to "u")
    )
}
