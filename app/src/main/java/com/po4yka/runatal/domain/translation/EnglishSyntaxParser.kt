package com.po4yka.runatal.domain.translation

import java.text.Normalizer
import java.util.Locale

/**
 * Lightweight offline parser for best-effort grammatical segmentation.
 */
internal class EnglishSyntaxParser {

    fun parse(text: String): ParsedEnglishText {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
            .trim()
            .replace(Regex("[\\s\\p{Z}]+"), " ")
        val tokens = scanTokens(text)

        return ParsedEnglishText(originalText = text, normalizedText = normalized, tokens = tokens)
    }

    private fun scanTokens(text: String): List<ParsedEnglishToken> {
        val tokens = mutableListOf<ParsedEnglishToken>()
        var offset = 0
        while (offset < text.length) {
            val codePoint = text.codePointAt(offset)
            if (codePoint.isSourceWhitespace()) {
                offset += Character.charCount(codePoint)
                continue
            }
            val start = offset
            offset = tokenEnd(text, start)
            val raw = text.substring(start, offset)
            val normalized = Normalizer.normalize(raw, Normalizer.Form.NFC)
                .replace('’', '\'')
                .replace('‘', '\'')
                .lowercase(Locale.ROOT)
            val type = when {
                codePoint.isSourcePunctuation() -> ParsedEnglishTokenType.PUNCTUATION
                englishWordRegex.matches(normalized) -> ParsedEnglishTokenType.WORD
                else -> ParsedEnglishTokenType.UNSUPPORTED
            }
            tokens += ParsedEnglishToken(raw, normalized, type, start, offset)
        }
        return tokens
    }

    private fun tokenEnd(text: String, start: Int): Int {
        val first = text.codePointAt(start)
        var end = start + Character.charCount(first)
        if (first.isSourcePunctuation()) return end
        val isWord = Character.isLetterOrDigit(first)
        while (end < text.length) {
            val next = text.codePointAt(end)
            val continues = if (isWord) {
                isWordContinuation(text, end)
            } else {
                !next.isSourceWhitespace() && (!next.isSourcePunctuation() || isNumericSeparator(text, end)) &&
                    !Character.isLetter(next)
            }
            if (!continues) break
            end += Character.charCount(next)
        }
        return end
    }

    private fun isWordContinuation(text: String, offset: Int): Boolean {
        val codePoint = text.codePointAt(offset)
        if (Character.isLetterOrDigit(codePoint) || Character.getType(codePoint) in combiningMarkTypes) return true
        if (isNumericSeparator(text, offset)) return true
        val nextOffset = offset + Character.charCount(codePoint)
        return codePoint in apostrophes && nextOffset < text.length &&
            Character.isLetter(text.codePointAt(nextOffset))
    }

    private fun isNumericSeparator(text: String, offset: Int): Boolean {
        val separator = text.codePointAt(offset)
        val nextOffset = offset + Character.charCount(separator)
        if (separator !in literalSeparators || offset == 0 || nextOffset >= text.length) return false
        val previous = text.codePointBefore(offset)
        val next = text.codePointAt(nextOffset)
        return if (separator == '_'.code) {
            Character.isLetterOrDigit(previous) && Character.isLetterOrDigit(next)
        } else {
            Character.isDigit(previous) && Character.isDigit(next)
        }
    }

    private companion object {
        val englishWordRegex = Regex("[a-z]+(?:'[a-z]+)*")
        val literalSeparators = setOf('.'.code, ':'.code, ','.code, '/'.code, '-'.code, '_'.code)
        val apostrophes = setOf('\''.code, '’'.code, '‘'.code)
        val combiningMarkTypes = setOf(
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt()
        )
    }
}

internal data class ParsedEnglishText(
    val originalText: String,
    val normalizedText: String,
    val tokens: List<ParsedEnglishToken>
)

internal data class ParsedEnglishToken(
    val raw: String,
    val normalized: String,
    val type: ParsedEnglishTokenType,
    val startOffset: Int,
    val endOffset: Int
)

internal enum class ParsedEnglishTokenType {
    WORD,
    PUNCTUATION,
    UNSUPPORTED
}

private fun Int.isSourceWhitespace(): Boolean = Character.isWhitespace(this) || Character.isSpaceChar(this)

private fun Int.isSourcePunctuation(): Boolean = Character.getType(this) in sourcePunctuationTypes

private val sourcePunctuationTypes = setOf(
    Character.CONNECTOR_PUNCTUATION.toInt(),
    Character.DASH_PUNCTUATION.toInt(),
    Character.START_PUNCTUATION.toInt(),
    Character.END_PUNCTUATION.toInt(),
    Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
    Character.FINAL_QUOTE_PUNCTUATION.toInt(),
    Character.OTHER_PUNCTUATION.toInt()
)
