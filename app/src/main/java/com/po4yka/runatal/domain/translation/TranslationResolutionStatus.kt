package com.po4yka.runatal.domain.translation

/**
 * Historical confidence tier for a translation result.
 */
internal enum class TranslationResolutionStatus {
    ATTESTED,
    RECONSTRUCTED,
    APPROXIMATED,
    UNAVAILABLE
}

/**
 * Compact label for UI badges.
 */
internal val TranslationResolutionStatus.label: String
    get() = when (this) {
        TranslationResolutionStatus.ATTESTED -> "Attested"
        TranslationResolutionStatus.RECONSTRUCTED -> "Reconstructed"
        TranslationResolutionStatus.APPROXIMATED -> "Approximation"
        TranslationResolutionStatus.UNAVAILABLE -> "Unavailable"
    }
