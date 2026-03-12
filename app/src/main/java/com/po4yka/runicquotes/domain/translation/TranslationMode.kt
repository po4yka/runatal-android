package com.po4yka.runicquotes.domain.translation

/**
 * Presentation mode for the translation screen.
 */
internal enum class TranslationMode {
    TRANSLITERATE,
    TRANSLATE;

    /** Default persisted presentation mode. */
    companion object {
        val DEFAULT = TRANSLITERATE
    }
}

/**
 * Compact label for UI controls.
 */
internal val TranslationMode.label: String
    get() = when (this) {
        TranslationMode.TRANSLITERATE -> "Transliterate"
        TranslationMode.TRANSLATE -> "Translate"
    }
