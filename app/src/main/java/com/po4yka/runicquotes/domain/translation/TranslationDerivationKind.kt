package com.po4yka.runicquotes.domain.translation

/**
 * Explains how a historical translation result was derived.
 */
internal enum class TranslationDerivationKind {
    GOLD_EXAMPLE,
    PHRASE_TEMPLATE,
    TOKEN_COMPOSED,
    SEQUENCE_TRANSCRIPTION
}

/**
 * Compact label for educational UI.
 */
internal val TranslationDerivationKind.label: String
    get() = when (this) {
        TranslationDerivationKind.GOLD_EXAMPLE -> "Gold example"
        TranslationDerivationKind.PHRASE_TEMPLATE -> "Phrase template"
        TranslationDerivationKind.TOKEN_COMPOSED -> "Token composed"
        TranslationDerivationKind.SEQUENCE_TRANSCRIPTION -> "Sequence transcription"
    }
