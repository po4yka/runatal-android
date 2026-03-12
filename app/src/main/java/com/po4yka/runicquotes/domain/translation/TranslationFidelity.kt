package com.po4yka.runicquotes.domain.translation

/**
 * Fidelity level for historical translation output.
 */
internal enum class TranslationFidelity {
    STRICT,
    READABLE,
    DECORATIVE;

    /** Default fidelity used for historical translation requests. */
    companion object {
        val DEFAULT = STRICT
    }
}

/**
 * Human-readable label for fidelity controls.
 */
internal val TranslationFidelity.label: String
    get() = when (this) {
        TranslationFidelity.STRICT -> "Strict"
        TranslationFidelity.READABLE -> "Readable"
        TranslationFidelity.DECORATIVE -> "Decorative"
    }
