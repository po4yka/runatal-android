package com.po4yka.runicquotes.domain.transliteration

import com.po4yka.runicquotes.domain.model.RunicScript
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for transliterating text to runic scripts.
 * Uses injected transliterator instances rather than creating new ones each call.
 */
@Singleton
class TransliterationFactory @Inject constructor(
    private val elderFutharkTransliterator: ElderFutharkTransliterator,
    private val youngerFutharkTransliterator: YoungerFutharkTransliterator,
    private val cirthTransliterator: CirthTransliterator
) {
    private companion object {
        val NonWhitespaceTokenRegex = Regex("\\S+")
    }

    /**
     * Returns the transliterator for the specified runic script.
     *
     * @param script The runic script to get a transliterator for
     * @return A RunicTransliterator instance for the specified script
     */
    fun create(script: RunicScript): RunicTransliterator {
        return when (script) {
            RunicScript.ELDER_FUTHARK -> elderFutharkTransliterator
            RunicScript.YOUNGER_FUTHARK -> youngerFutharkTransliterator
            RunicScript.CIRTH -> cirthTransliterator
        }
    }

    /**
     * Transliterates text to the specified runic script.
     *
     * @param text The Latin text to transliterate
     * @param script The target runic script
     * @return The transliterated text
     */
    fun transliterate(text: String, script: RunicScript): String {
        return create(script).transliterate(text)
    }

    /**
     * Transliterates text and provides a per-token breakdown based on whitespace boundaries.
     */
    fun transliterateWordByWord(text: String, script: RunicScript): TransliterationBreakdown {
        if (text.isEmpty()) {
            return TransliterationBreakdown()
        }

        val transliterator = create(script)
        val fullText = transliterator.transliterate(text)
        val wordPairs = NonWhitespaceTokenRegex.findAll(text).map { match ->
            val sourceToken = match.value
            WordTransliterationPair(
                sourceToken = sourceToken,
                runicToken = transliterator.transliterate(sourceToken)
            )
        }.toList()

        return TransliterationBreakdown(
            fullText = fullText,
            wordPairs = wordPairs
        )
    }
}
