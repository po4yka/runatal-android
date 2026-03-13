package com.po4yka.runatal.data.translation

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class CuratedTranslationAssetsTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val translationDir = sequenceOf(
        File("app/src/main/translationSeed/translation"),
        File("src/main/translationSeed/translation")
    ).first { it.isDirectory }

    @Test
    fun `strict lexical entries include citations and known sources`() {
        val sourceIds = sourceManifestSources().map { it.getString("id") }.toSet()
        val oldNorse = parseArray("old_norse_lexicon.json")
        val protoNorse = parseArray("proto_norse_lexicon.json")

        (oldNorse + protoNorse)
            .filter { it.getBoolean("strictEligible") }
            .forEach { row ->
                assertThat(row.getString("sourceId")).isIn(sourceIds)
                assertThat(row.getArray("citations")).isNotEmpty()
            }
    }

    @Test
    fun `strict templates and gold examples use stable reference ids`() {
        val referenceIds = parseArray("runic_corpus_refs.json").map { it.getString("id") }.toSet()
        val sourceIds = sourceManifestSources().map { it.getString("id") }.toSet()

        parseArray("younger_phrase_templates.json").forEach { row ->
            row.getArray("referenceIds").forEach { value ->
                assertThat(value).isIn(referenceIds)
            }
        }
        parseArray("elder_attested_forms.json").forEach { row ->
            row.getArray("referenceIds").forEach { value ->
                assertThat(value).isIn(referenceIds)
            }
        }

        parseArray("gold_examples.json").forEach { example ->
            example.getJsonArray("results").forEach { resultElement ->
                val result = resultElement.jsonObject
                if (result.getString("fidelity") != "STRICT") {
                    return@forEach
                }
                result.getJsonArray("provenance").forEach { provenanceElement ->
                    val provenance = provenanceElement.jsonObject
                    assertThat(provenance.getString("sourceId")).isIn(sourceIds)
                    provenance["referenceId"]?.jsonPrimitive?.contentOrNull?.let { referenceId ->
                        assertThat(referenceId).isIn(referenceIds)
                    }
                }
            }
        }
    }

    private fun sourceManifestSources(): List<JsonObject> {
        return json.parseToJsonElement(
            translationDir.resolve("source_manifest.json").readText()
        ).jsonObject.getJsonArray("sources").map { it.jsonObject }
    }

    private fun parseArray(fileName: String): List<JsonObject> {
        return json.parseToJsonElement(
            translationDir.resolve(fileName).readText()
        ).jsonArray.map { it.jsonObject }
    }

    private fun JsonObject.getArray(name: String): List<String> {
        return getJsonArray(name).map { it.jsonPrimitive.content }
    }

    private fun JsonObject.getBoolean(name: String): Boolean {
        return getValue(name).jsonPrimitive.boolean
    }

    private fun JsonObject.getString(name: String): String {
        return getValue(name).jsonPrimitive.content
    }

    private fun JsonObject.getJsonArray(name: String): JsonArray {
        return getValue(name).jsonArray
    }
}
