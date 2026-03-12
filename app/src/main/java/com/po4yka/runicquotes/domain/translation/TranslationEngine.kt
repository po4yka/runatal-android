package com.po4yka.runicquotes.domain.translation

import com.po4yka.runicquotes.domain.model.RunicScript

/**
 * Produces structured historical translations for a single runic script.
 */
internal interface TranslationEngine {
    val script: RunicScript
    val engineVersion: String
    val datasetVersion: String

    fun translate(request: TranslationRequest): TranslationResult
}
