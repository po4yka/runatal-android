

# Old Norse Inflection Tables

This document defines the **core inflection tables required by the Old Norse grammar engine** used in the translation pipeline.

These tables are used by:

- `norse-inflection`
- `norse-grammar`
- `norse-translation`

They provide **deterministic morphology generation** for nouns, verbs, and adjectives.

The goal is to generate **grammatically correct normalized Old West Norse forms** suitable for later runic conversion.

## Implementation status

The current app uses these tables as **guidance** for a heuristic engine, not as a one-to-one API contract. The implemented types live in `domain/translation/`, and persisted output lives in `translation_records`.

---

# Supported Morphology Scope

The engine should support the most common Old Norse inflection classes:

Nouns

- Strong masculine (a‑stem)
- Strong feminine (ō‑stem)
- Strong neuter (a‑stem)

Verbs

- Strong verbs (Classes I–VII simplified)
- Weak verbs (Class I–III)

Adjectives

- Strong adjective declension

These cover the majority of lexical entries required for translation use‑cases.

---

# Noun Declension Tables

## Masculine Strong (a‑stem)

Example lemma: **úlfr** (wolf)

| Case | Singular | Plural |
|-----|----------|-------|
| Nominative | úlfr | úlfar |
| Accusative | úlf | úlfa |
| Genitive | úlfs | úlfa |
| Dative | úlfi | úlfum |

Inflection pattern:

```
stem + r
stem
stem + s
stem + i
```

Plural:

```
stem + ar
stem + a
stem + a
stem + um
```

---

## Feminine Strong (ō‑stem)

Example lemma: **saga** (story)

| Case | Singular | Plural |
|-----|----------|-------|
| Nominative | saga | sǫgur |
| Accusative | sǫgu | sǫgur |
| Genitive | sǫgu | sagna |
| Dative | sǫgu | sǫgum |

---

## Neuter Strong (a‑stem)

Example lemma: **skip** (ship)

| Case | Singular | Plural |
|-----|----------|-------|
| Nominative | skip | skip |
| Accusative | skip | skip |
| Genitive | skips | skipa |
| Dative | skipi | skipum |

Rule:

Neuter nominative = accusative

---

# Verb Conjugation Tables

## Weak Verb (Class I)

Example: **kalla** (to call)

### Present

| Person | Form |
|------|------|
| 1sg | kalla |
| 2sg | kallar |
| 3sg | kallar |
| 1pl | kǫllum |
| 2pl | kallið |
| 3pl | kalla |

### Past

| Person | Form |
|------|------|
| 1sg | kallaða |
| 2sg | kallaðir |
| 3sg | kallaði |
| 1pl | kǫlluðum |
| 2pl | kǫlluðuð |
| 3pl | kǫlluðu |

---

## Strong Verb Example

Example: **bíta** (to bite)

### Present

| Person | Form |
|------|------|
| 1sg | bít |
| 2sg | bítr |
| 3sg | bítr |
| 1pl | bítum |
| 2pl | bítið |
| 3pl | bíta |

### Past

| Person | Form |
|------|------|
| 1sg | beit |
| 2sg | beitt |
| 3sg | beit |
| 1pl | bitum |
| 2pl | bituð |
| 3pl | bitu |

---

# Adjective Declension

Example: **stórr** (big)

| Case | Masc | Fem | Neut |
|----|----|----|----|
| Nom sg | stórr | stór | stórt |
| Acc sg | stóran | stóra | stórt |
| Gen sg | stórs | stórrar | stórs |
| Dat sg | stórum | stórri | stóru |

Plural example:

| Case | Masc | Fem | Neut |
|----|----|----|----|
| Nom pl | stórir | stórar | stór |
| Acc pl | stóra | stórar | stór |
| Gen pl | stórra | stórra | stórra |
| Dat pl | stórum | stórum | stórum |

---

# Inflection Engine Rules

The inflection engine should follow this order:

```
lemma
→ detect lexical class
→ choose inflection table
→ apply stem transformation
→ generate final form
```

Example:

```
lemma: úlfr
case: nominative
number: singular

→ úlfr
```

---

# Kotlin Data Model

Current implementation structures:

```kotlin
data class OldNorseLexiconEntry(
    val english: String,
    val partOfSpeech: String,
    val lemma: String,
    val declensionClass: String?,
    val present3sg: String?,
    val past3sg: String?
)

data class InflectionTablesData(
    val strongMasculineSuffixes: Map<String, String>,
    val weakVerbSuffixes: Map<String, String>
)
```

Current implementation entry points:

```kotlin
class OldNorseInflector {
    fun inflect(entry: OldNorseLexiconEntry, token: ParsedEnglishToken): String
}
```

---

# Example End‑to‑End Inflection

English:

```
The wolf hunts
```

Pipeline:

```
wolf → úlfr (noun)
hunt → veiða (verb)

subject = nominative singular
verb = present 3sg
```

Result:

```
úlfr veiðir
```

---

# Future Expansion

The grammar engine may later support:

- u‑stem nouns
- i‑stem nouns
- irregular verbs
- subjunctive mood
- participles

These are optional for initial implementation but recommended for high fidelity.

---

# Summary

This file defines the **minimal Old Norse morphology system required for accurate Viking‑age text generation**.

The pipeline is:

```
English
→ dictionary lookup
→ grammatical analysis
→ inflection tables
→ normalized Old Norse output
```

The resulting Old Norse text is then passed to the **runic phonology and rune mapping engines**.
