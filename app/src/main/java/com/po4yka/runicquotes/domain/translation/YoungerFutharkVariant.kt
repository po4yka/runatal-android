package com.po4yka.runicquotes.domain.translation

/**
 * Rendering variant for Younger Futhark.
 */
internal enum class YoungerFutharkVariant {
    LONG_BRANCH,
    SHORT_TWIG;

    /** Default Younger Futhark rendering variant. */
    companion object {
        val DEFAULT = LONG_BRANCH
    }
}

/**
 * Label for variant selection controls.
 */
internal val YoungerFutharkVariant.label: String
    get() = when (this) {
        YoungerFutharkVariant.LONG_BRANCH -> "Long-branch"
        YoungerFutharkVariant.SHORT_TWIG -> "Short-twig"
    }
