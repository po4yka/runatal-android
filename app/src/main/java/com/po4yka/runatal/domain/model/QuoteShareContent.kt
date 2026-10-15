package com.po4yka.runatal.domain.model

/** Prepared quote content shared by the preview and exported image. */
data class QuoteShareContent(
    val quote: Quote,
    val script: RunicScript,
    val font: String,
    val rendering: ResolvedQuoteRendering
) {
    /** Exactly the resolved glyphs used by every other quote surface. */
    val runicText: String get() = rendering.glyphOutput

    /** Original quote text. */
    val textLatin: String get() = quote.textLatin

    /** Quote attribution. */
    val author: String get() = quote.author

    /** Selected alphabet label. */
    val scriptLabel: String get() = rendering.label
}
