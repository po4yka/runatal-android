package com.po4yka.runatal.domain.transliteration

/**
 * Full transliteration output plus a whitespace-tokenized word mapping.
 */
data class TransliterationBreakdown(
    val fullText: String = "",
    val wordPairs: List<WordTransliterationPair> = emptyList()
)

/**
 * A Latin token and its runic transliteration.
 */
data class WordTransliterationPair(
    val sourceToken: String,
    val runicToken: String
)
