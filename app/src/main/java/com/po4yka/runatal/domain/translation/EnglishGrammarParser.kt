package com.po4yka.runatal.domain.translation

/** Lexicon-backed syntax for noun phrases, governed phrases and one finite declarative clause. */
internal class EnglishGrammarParser(private val lookup: HistoricalLexiconLookup) {
    fun parse(parsed: ParsedEnglishText): EnglishGrammarDocument? {
        val tokens = parsed.tokens.toMutableList()
        if (tokens.lastOrNull()?.raw in setOf(".", "!")) tokens.removeAt(tokens.lastIndex)
        if (tokens.isEmpty() || tokens.any { it.type != ParsedEnglishTokenType.WORD }) return null
        return EnglishGrammarReader(lookup, tokens).parse()
    }
}

internal data class EnglishGrammarWord(val token: ParsedEnglishToken, val entry: OldNorseLexiconEntry)
internal data class EnglishNounPhrase(
    val head: EnglishGrammarWord,
    val adjectives: List<EnglishGrammarWord>,
    val determiner: ParsedEnglishToken?,
    val number: GrammaticalNumber
)
internal data class EnglishGovernedPhrase(val preposition: ParsedEnglishToken, val complement: EnglishNounPhrase)
internal data class EnglishSubject(
    val pronouns: List<ParsedEnglishToken> = emptyList(),
    val nounPhrase: EnglishNounPhrase? = null,
    val person: Int,
    val number: GrammaticalNumber,
    val gender: GrammaticalGender? = null
) {
    val agreementKey: String get() = "${person}_${number.name}"
}
internal data class EnglishGrammarDocument(
    val subject: EnglishSubject? = null,
    val verb: EnglishGrammarWord? = null,
    val infinitive: EnglishGrammarWord? = null,
    val objectPhrase: EnglishNounPhrase? = null,
    val predicatePhrase: EnglishNounPhrase? = null,
    val predicateAdjective: EnglishGrammarWord? = null,
    val governedPhrases: List<EnglishGovernedPhrase> = emptyList()
)

private class EnglishGrammarReader(
    private val lookup: HistoricalLexiconLookup,
    private val tokens: List<ParsedEnglishToken>
) {
    private var position = 0
    private fun current(): ParsedEnglishToken? = tokens.getOrNull(position)
    private fun currentEntry(): OldNorseLexiconEntry? = current()?.let {
        lookup.oldNorseFor(it.normalized, TranslationFidelity.STRICT)
    }
    private fun isPreposition(): Boolean = currentEntry()?.partOfSpeech == "preposition"

    fun parse(): EnglishGrammarDocument? = when {
        tokens.size == 1 && currentEntry()?.partOfSpeech == "verb" -> readInfinitive()
        isPreposition() -> readGovernedPhrases()?.let { EnglishGrammarDocument(governedPhrases = it) }
            ?.takeIf { position == tokens.size }
        else -> readDeclaration()
    }

    private fun readInfinitive(): EnglishGrammarDocument? {
        val entry = currentEntry() ?: return null
        return if (current()?.normalized == entry.english) {
            EnglishGrammarDocument(infinitive = readWord("verb"))
        } else {
            null
        }
    }

    private fun readDeclaration(): EnglishGrammarDocument? {
        val subject = readSubject() ?: return null
        return if (position == tokens.size) {
            EnglishGrammarDocument(subject = subject)
        } else {
            val verb = readWord("verb") ?: return null
            readClause(subject, verb)
        }
    }

    private fun readClause(subject: EnglishSubject, verb: EnglishGrammarWord): EnglishGrammarDocument? {
        val document = if (current() == null || isPreposition()) {
            EnglishGrammarDocument(subject = subject, verb = verb)
        } else {
            readComplement(subject, verb) ?: return null
        }
        val phrases = readGovernedPhrases() ?: return null
        return document.copy(governedPhrases = phrases).takeIf { position == tokens.size }
    }

    private fun readComplement(subject: EnglishSubject, verb: EnglishGrammarWord): EnglishGrammarDocument? {
        if (verb.entry.english != "be") {
            return readNounPhrase()?.let { EnglishGrammarDocument(subject = subject, verb = verb, objectPhrase = it) }
        }
        val complementStart = position
        val nounPhrase = readNounPhrase()
        if (nounPhrase != null) {
            return EnglishGrammarDocument(subject = subject, verb = verb, predicatePhrase = nounPhrase)
        }
        position = complementStart
        return readWord("adjective")?.let {
            EnglishGrammarDocument(subject = subject, verb = verb, predicateAdjective = it)
        }
    }

    private fun readSubject(): EnglishSubject? {
        val token = current() ?: return null
        return if (token.normalized in lookup.grammarRules().pronounMap) {
            readPronounSubject(token)
        } else {
            readNounPhrase()?.let { noun ->
                EnglishSubject(nounPhrase = noun, person = 3, number = noun.number, gender = noun.head.entry.gender)
            }
        }
    }

    private fun readPronounSubject(token: ParsedEnglishToken): EnglishSubject {
        position++
        val pronouns = mutableListOf(token)
        val pluralYou = token.normalized == "you" && current()?.normalized == "all"
        if (pluralYou) pronouns += tokens[position++]
        val number = if (pluralYou || token.normalized in setOf("we", "they")) {
            GrammaticalNumber.PLURAL
        } else {
            GrammaticalNumber.SINGULAR
        }
        val person = when (token.normalized) { "i", "we" -> 1; "you" -> 2; else -> 3 }
        val gender = when (token.normalized) {
            "he", "they" -> GrammaticalGender.MASCULINE
            "she" -> GrammaticalGender.FEMININE
            "it" -> GrammaticalGender.NEUTER
            else -> null
        }
        return EnglishSubject(pronouns, person = person, number = number, gender = gender)
    }

    private fun readNounPhrase(): EnglishNounPhrase? {
        val determiner = current()?.takeIf { it.normalized in setOf("the", "a", "an") }
        if (determiner != null) position++
        val adjectives = mutableListOf<EnglishGrammarWord>()
        while (currentEntry()?.partOfSpeech == "adjective") adjectives += requireNotNull(readWord("adjective"))
        val head = readWord("noun") ?: return null
        val number = if (head.token.normalized in head.entry.englishPluralForms) {
            GrammaticalNumber.PLURAL
        } else {
            GrammaticalNumber.SINGULAR
        }
        if (number == GrammaticalNumber.PLURAL && determiner?.normalized in setOf("a", "an")) return null
        return EnglishNounPhrase(head, adjectives, determiner, number)
    }

    private fun readWord(partOfSpeech: String): EnglishGrammarWord? {
        val token = current() ?: return null
        val entry = currentEntry()?.takeIf { it.partOfSpeech == partOfSpeech } ?: return null
        position++
        return EnglishGrammarWord(token, entry)
    }

    private fun readGovernedPhrases(): List<EnglishGovernedPhrase>? {
        val phrases = mutableListOf<EnglishGovernedPhrase>()
        while (position < tokens.size) {
            val preposition = current()?.takeIf { isPreposition() } ?: return null
            position++
            val complement = readNounPhrase() ?: return null
            phrases += EnglishGovernedPhrase(preposition, complement)
        }
        return phrases
    }
}
