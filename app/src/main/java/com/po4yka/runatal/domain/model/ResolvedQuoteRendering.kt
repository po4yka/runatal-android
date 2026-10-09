package com.po4yka.runatal.domain.model

import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.translation.TranslationProvenanceEntry
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.transliteration.WordTransliterationPair

/** The single resolved output consumed by quote, library, widget and share surfaces. */
data class ResolvedQuoteRendering(
    val glyphOutput: String,
    val wordBreakdown: List<WordTransliterationPair>,
    val mode: TranslationMode,
    val label: String,
    val resolutionStatus: TranslationResolutionStatus? = null,
    val provenance: List<TranslationProvenanceEntry> = emptyList(),
    val notes: List<String> = emptyList()
) {
    /** Historical unavailability never falls back to direct glyphs. */
    val isAvailable: Boolean get() =
        glyphOutput.isNotBlank() && resolutionStatus != TranslationResolutionStatus.UNAVAILABLE
}
