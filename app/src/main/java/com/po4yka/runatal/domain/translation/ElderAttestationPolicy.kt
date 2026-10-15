package com.po4yka.runatal.domain.translation

import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import java.net.URI
import java.util.Locale

/** Requires a located published inscription and matching transcription before admitting STRICT Elder output. */
internal class ElderAttestationPolicy(store: RunicCorpusStore) {
    private val references = store.runicCorpusReferences().associateBy { it.id }
    private val sources = store.sourceManifest().sources.associateBy { it.id }
    private val renderer = ElderFutharkTransliterator()

    fun allows(
        normalizedForm: String,
        diplomaticForm: String,
        resolutionStatus: String,
        historicalStage: String,
        referenceIds: List<String>
    ): Boolean {
        if (resolutionStatus != TranslationResolutionStatus.ATTESTED.name ||
            historicalStage != HistoricalStage.PROTO_NORSE.name) return false
        val candidate = compactTranscription(diplomaticForm)
        if (candidate.isEmpty() || candidate != compactTranscription(normalizedForm) ||
            candidate.any { it !in ATTESTED_LATIN_SIGNS }) return false
        return referenceIds.any { id -> publishedForm(candidate, historicalStage, id) }
    }

    private fun publishedForm(candidate: String, stage: String, id: String): Boolean {
        val reference = references[id]
        val evidence = reference?.attestation
        val source = reference?.sourceId?.let(sources::get)
        if (reference == null || evidence == null || source == null) return false
        val sourceUri = safeUri(source.url)
        val referenceUri = reference.url?.let(::safeUri)
        val located = sourceUri != null && referenceUri != null && locatedAtSource(sourceUri, referenceUri)
        return located && evidence.locator.isNotBlank() && evidence.historicalStage == stage &&
            matchesForm(candidate, evidence)
    }

    private fun locatedAtSource(source: URI, reference: URI): Boolean {
        val secure = source.scheme == "https" && reference.scheme == "https"
        val sameHost = !source.host.isNullOrBlank() && reference.host == source.host
        return secure && sameHost && !reference.path.isNullOrBlank()
    }

    private fun matchesForm(candidate: String, evidence: InscriptionAttestation): Boolean {
        val published = compactTranscription(evidence.diplomaticText)
        return candidate == published || evidence.namedForms.any { form ->
            candidate == compactTranscription(form) && published.contains(candidate)
        }
    }

    /** Museum transcription uses capital R for the rune conventionally normalized as z in Early Norse. */
    fun render(diplomaticText: String): String = renderer.transliterate(diplomaticText.replace('R', 'z'))

    private fun compactTranscription(text: String): String = text.replace('R', 'z')
        .lowercase(Locale.ROOT).replace(Regex("[\\s:]+"), "")

    private fun safeUri(text: String): URI? = runCatching { URI(text) }.getOrNull()

    private companion object {
        const val ATTESTED_LATIN_SIGNS = "fuþarkgwhnijzstbemlŋodpï"
    }
}
