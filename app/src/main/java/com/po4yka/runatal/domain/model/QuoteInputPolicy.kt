package com.po4yka.runatal.domain.model

/** Common editable and saved-translation text bounds, measured in the existing UTF-16 unit budget. */
internal object QuoteInputPolicy {
    const val MIN_QUOTE_LENGTH = 1
    const val MAX_QUOTE_LENGTH = 280
    const val MAX_AUTHOR_LENGTH = 60

    /** Prepares the saved source without changing interior whitespace, punctuation or Unicode spelling. */
    fun prepareSourceText(text: String): String = text.trim()

    fun quoteTextError(text: String): String? = when {
        prepareSourceText(text).length < MIN_QUOTE_LENGTH -> "Quote is required"
        text.length > MAX_QUOTE_LENGTH -> "Keep quote under $MAX_QUOTE_LENGTH characters"
        else -> null
    }

    fun authorError(author: String): String? = when {
        author.isBlank() -> "Author is required"
        author.length > MAX_AUTHOR_LENGTH -> "Keep author under $MAX_AUTHOR_LENGTH characters"
        else -> null
    }
}
