package com.po4yka.runatal.domain.translation

import com.po4yka.runatal.domain.model.RunicScript

/**
 * Input for a historical translation engine.
 */
internal data class TranslationRequest(
    val sourceText: String,
    val script: RunicScript,
    val fidelity: TranslationFidelity = TranslationFidelity.DEFAULT,
    val youngerVariant: YoungerFutharkVariant = YoungerFutharkVariant.DEFAULT
)
