package com.po4yka.runatal.domain.translation

/** Resolves grammatical roles before selecting explicit cited inflection forms. */
internal class OldNorseGrammarStage(private val lookup: HistoricalLexiconLookup) {

    private val parser = EnglishGrammarParser(lookup)

    fun resolve(parsed: ParsedEnglishText): List<TranslationTokenResolution> {
        val document = parser.parse(parsed) ?: return syntaxFailure(parsed)
        val assigned = mutableMapOf<ParsedEnglishToken, TranslationTokenResolution>()
        document.infinitive?.let { assigned[it.token] = lexical(it, it.entry.lemma) }
        document.subject?.let { subject -> resolveSubject(subject, assigned) }
        document.verb?.let { verb -> resolveVerb(verb, requireNotNull(document.subject), assigned) }
        document.objectPhrase?.let { phrase ->
            val case = document.verb?.entry?.objectCase
            if (case == null) assigned[phrase.head.token] = unavailable(phrase.head.token, "Unknown object government.")
            else resolveNounPhrase(phrase, case, assigned)
        }
        document.predicatePhrase?.let { phrase ->
            if (phrase.number != document.subject?.number) {
                assigned[phrase.head.token] = unavailable(
                    phrase.head.token, "Predicate noun number disagrees with subject."
                )
            } else {
                resolveNounPhrase(phrase, GrammaticalCase.NOMINATIVE, assigned)
            }
        }
        document.predicateAdjective?.let { adjective ->
            val subject = requireNotNull(document.subject)
            assigned[adjective.token] = adjectiveForm(
                adjective, GrammaticalCase.NOMINATIVE, subject.number, subject.gender
            )
        }
        document.governedPhrases.forEach { resolveGovernedPhrase(it, assigned) }
        return parsed.tokens.mapNotNull { token ->
            if (token.type == ParsedEnglishTokenType.PUNCTUATION) {
                TranslationTokenResolution(token.raw, token.raw, token.raw, token.raw,
                    TranslationResolutionStatus.RECONSTRUCTED, isPunctuation = true)
            } else {
                assigned[token] ?: unavailable(token, "Unresolved grammatical role.")
            }
        }
    }

    private fun resolveSubject(
        subject: EnglishSubject,
        assigned: MutableMap<ParsedEnglishToken, TranslationTokenResolution>
    ) {
        subject.nounPhrase?.let { resolveNounPhrase(it, GrammaticalCase.NOMINATIVE, assigned) }
        if (subject.pronouns.isEmpty()) return
        val spelling = subject.pronouns.joinToString(" ") { it.normalized }
        val form = lookup.grammarRules().pronounMap.getValue(spelling)
        val source = lookup.provenanceFor("barnes_nion", detail = "NION I pp.61–62: personal pronouns.")
        if (source.sourceId != "barnes_nion") {
            subject.pronouns.forEach { assigned[it] = unavailable(it, "Missing personal-pronoun source.") }
            return
        }
        val notes = when (spelling) {
            "you" -> listOf("Interpreted 'you' as singular; 'you all' selects plural.")
            "they" -> listOf("Interpreted 'they' as a masculine plural group.")
            else -> emptyList()
        }
        subject.pronouns.forEachIndexed { index, token ->
            assigned[token] = TranslationTokenResolution(
                token.raw, if (index == 0) form else "", "", "", TranslationResolutionStatus.RECONSTRUCTED,
                notes = notes, provenance = listOf(source)
            )
        }
    }

    private fun resolveVerb(
        word: EnglishGrammarWord,
        subject: EnglishSubject,
        assigned: MutableMap<ParsedEnglishToken, TranslationTokenResolution>
    ) {
        val english = word.entry.englishVerbForms[word.token.normalized]
        val form = if (english?.tense == GrammaticalTense.PAST) {
            word.entry.pastForms[subject.agreementKey]
        } else {
            word.entry.presentForms[subject.agreementKey]
        }
        assigned[word.token] = when {
            english == null || subject.agreementKey !in english.agreements ->
                unavailable(word.token, "Unsupported English subject and finite-verb agreement.")
            form == null -> unavailable(word.token, "Missing cited finite form ${subject.agreementKey}.")
            else -> inflected(word, form, "${english.tense} ${subject.agreementKey}")
        }
    }

    private fun resolveNounPhrase(
        phrase: EnglishNounPhrase,
        case: GrammaticalCase,
        assigned: MutableMap<ParsedEnglishToken, TranslationTokenResolution>
    ) {
        val definite = phrase.determiner?.normalized == "the"
        val key = "${if (definite) "DEFINITE_" else ""}${case.name}_${phrase.number.name}"
        val form = phrase.head.entry.nounForms[key]
        assigned[phrase.head.token] = when {
            form != null -> inflected(phrase.head, form, key)
            !definite && case == GrammaticalCase.NOMINATIVE && phrase.number == GrammaticalNumber.SINGULAR ->
                lexical(phrase.head, phrase.head.entry.lemma)
            else -> unavailable(phrase.head.token, "Missing cited noun form $key.")
        }
        phrase.determiner?.let { determiner ->
            assigned[determiner] = TranslationTokenResolution(
                determiner.raw, "", "", "", TranslationResolutionStatus.RECONSTRUCTED,
                notes = listOf(
                    if (definite) "Definiteness is expressed by the noun suffix."
                    else "Old Norse indefiniteness uses the bare noun."
                ),
                provenance = listOf(lookup.provenanceFor("barnes_nion", detail = "NION I pp.56–57: articles."))
            )
        }
        phrase.adjectives.forEach { adjective ->
            assigned[adjective.token] = if (definite) {
                unavailable(adjective.token, "Missing cited weak adjective forms for a definite noun phrase.")
            } else {
                adjectiveForm(adjective, case, phrase.number, phrase.head.entry.gender)
            }
        }
    }

    private fun adjectiveForm(
        word: EnglishGrammarWord,
        case: GrammaticalCase,
        number: GrammaticalNumber,
        gender: GrammaticalGender?
    ): TranslationTokenResolution {
        val key = "${case.name}_${number.name}_${gender?.name}"
        val form = word.entry.adjectiveForms[key]
        return if (form == null) unavailable(word.token, "Missing cited adjective agreement form $key.")
        else inflected(word, form, key)
    }

    private fun resolveGovernedPhrase(
        phrase: EnglishGovernedPhrase,
        assigned: MutableMap<ParsedEnglishToken, TranslationTokenResolution>
    ) {
        val rule = lookup.grammarRules().governedPrepositions[phrase.preposition.normalized]
        val eligible = rule != null && (rule.allowedHeadwords.isEmpty() ||
            phrase.complement.head.entry.english in rule.allowedHeadwords)
        if (!eligible || rule == null) {
            assigned[phrase.preposition] = unavailable(
                phrase.preposition, "Unsupported preposition sense or government."
            )
            resolveNounPhrase(phrase.complement, GrammaticalCase.NOMINATIVE, assigned)
            return
        }
        val source = lookup.provenanceFor(rule.sourceId, detail = rule.citations.joinToString())
        if (source.sourceId != rule.sourceId || rule.citations.none { it.isNotBlank() }) {
            assigned[phrase.preposition] = unavailable(phrase.preposition, "Missing preposition-government citation.")
            return
        }
        assigned[phrase.preposition] = TranslationTokenResolution(
            phrase.preposition.raw, rule.lemma, "", "", TranslationResolutionStatus.RECONSTRUCTED,
            notes = listOf("Applied ${rule.grammaticalCase} government.") + rule.notes,
            provenance = listOf(source)
        )
        resolveNounPhrase(phrase.complement, rule.grammaticalCase, assigned)
    }

    private fun lexical(word: EnglishGrammarWord, form: String): TranslationTokenResolution {
        val source = lookup.provenanceFor(word.entry)
        return if (word.entry.citations.none { it.isNotBlank() } || source.sourceId != word.entry.sourceId) {
            unavailable(word.token, "Missing cited lexical source.")
        } else {
            TranslationTokenResolution(
                word.token.raw, form, "", "", TranslationResolutionStatus.RECONSTRUCTED, provenance = listOf(source)
            )
        }
    }

    private fun inflected(word: EnglishGrammarWord, form: String, key: String): TranslationTokenResolution {
        val sourceId = word.entry.inflectionSourceId
        if (sourceId == null || word.entry.inflectionCitations.none { it.isNotBlank() }) {
            return unavailable(word.token, "Missing inflection source or citations for $key.")
        }
        val provenance = lookup.provenanceFor(sourceId, detail = word.entry.inflectionCitations.joinToString())
        if (provenance.sourceId != sourceId) return unavailable(word.token, "Unknown inflection source for $key.")
        return lexical(word, form).copy(
            notes = listOf("Selected cited inflection $key."),
            provenance = listOf(lookup.provenanceFor(word.entry), provenance)
        )
    }

    private fun syntaxFailure(parsed: ParsedEnglishText): List<TranslationTokenResolution> {
        val unsupported = parsed.tokens.filter { it.type == ParsedEnglishTokenType.UNSUPPORTED }
        val failures = unsupported.ifEmpty { parsed.tokens }
        return failures.map { token ->
            unavailable(token, "Unsupported sentence structure or missing Old Norse lemma for '${token.raw}'.")
        }
    }

    private fun unavailable(token: ParsedEnglishToken, note: String) = TranslationTokenResolution(
        token.raw, "", "", "", TranslationResolutionStatus.UNAVAILABLE,
        notes = listOf(note), unresolvedToken = token.raw
    )
}
