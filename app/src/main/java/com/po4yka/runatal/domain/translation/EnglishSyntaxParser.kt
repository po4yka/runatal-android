package com.po4yka.runatal.domain.translation

/**
 * Lightweight offline parser for best-effort grammatical segmentation.
 */
internal class EnglishSyntaxParser {

    fun parse(text: String): ParsedEnglishText {
        val normalized = text
            .trim()
            .replace(Regex("\\s+"), " ")

        val tokens = TokenRegex.findAll(normalized).map { match ->
            val value = match.value
            ParsedEnglishToken(
                raw = value,
                normalized = value.lowercase(),
                type = if (value.all { it.isLetter() || it == '\'' }) {
                    ParsedEnglishTokenType.WORD
                } else {
                    ParsedEnglishTokenType.PUNCTUATION
                }
            )
        }.toList()

        val firstVerbIndex = tokens.indexOfFirst { token ->
            token.type == ParsedEnglishTokenType.WORD &&
                (token.normalized.endsWith("s") || token.normalized.endsWith("ed"))
        }
        val firstPrepositionIndex = tokens.indexOfFirst { token ->
            token.normalized in CommonPrepositions
        }

        return ParsedEnglishText(
            originalText = text,
            normalizedText = normalized,
            tokens = tokens,
            subjectTokens = tokens.sliceSafe(0, if (firstVerbIndex >= 0) firstVerbIndex else tokens.size),
            verbTokens = if (firstVerbIndex >= 0) listOf(tokens[firstVerbIndex]) else emptyList(),
            modifierTokens = if (firstPrepositionIndex >= 0) {
                tokens.drop(firstPrepositionIndex)
            } else {
                emptyList()
            }
        )
    }

    private companion object {
        val TokenRegex = Regex("[A-Za-z']+|[.,!?;:-]")
        val CommonPrepositions = setOf("at", "in", "on", "under", "with", "for", "from", "to", "of")
    }
}

internal data class ParsedEnglishText(
    val originalText: String,
    val normalizedText: String,
    val tokens: List<ParsedEnglishToken>,
    val subjectTokens: List<ParsedEnglishToken>,
    val verbTokens: List<ParsedEnglishToken>,
    val modifierTokens: List<ParsedEnglishToken>
)

internal data class ParsedEnglishToken(
    val raw: String,
    val normalized: String,
    val type: ParsedEnglishTokenType
)

internal enum class ParsedEnglishTokenType {
    WORD,
    PUNCTUATION
}

private fun <T> List<T>.sliceSafe(startIndex: Int, endExclusive: Int): List<T> {
    if (isEmpty() || startIndex >= size || startIndex >= endExclusive) {
        return emptyList()
    }
    return subList(startIndex.coerceAtLeast(0), endExclusive.coerceAtMost(size))
}
