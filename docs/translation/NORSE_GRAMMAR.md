

# Old Norse Grammar and Dictionary Integration

This document defines the **linguistic pipeline used to convert English text into grammatically correct Old Norse** before runic conversion.

This module feeds the **Younger Futhark translation engine** described in `TRANSLATION.md`.

The goal is not a naive word replacement system but a **structured historical language layer**.

---

# Core Principle

English must **never be mapped directly to runes**.

Correct pipeline:

```
English
→ semantic analysis
→ Old Norse lexical selection
→ grammatical inflection
→ normalized Old Norse output
→ phonological normalization
→ runic mapping
```

This document describes the **Old Norse layer**.

---

# Linguistic Sources

Recommended canonical sources for the internal dictionary:

### Dictionaries

Primary lexicon sources:

- **Zoëga – A Concise Dictionary of Old Icelandic**
- **Cleasby–Vigfusson Old Norse Dictionary**
- **ONP (Ordbog over det norrøne prosasprog)**

Dictionary entries should store:

```
lemma
part_of_speech
stem_class
inflection_pattern
meaning
attested_forms
```

Example dictionary entry:

```json
{
  "lemma": "úlfr",
  "part_of_speech": "noun",
  "gender": "masculine",
  "declension": "strong_masc_a",
  "meaning": "wolf"
}
```

---

# Old Norse Grammar Model

The grammar engine must support:

### Noun morphology

Features:

- gender: masculine / feminine / neuter
- number: singular / plural
- case:

```
nominative
accusative
genitive
dative
```

Example paradigm:

```
úlfr (wolf)

nom sg  úlfr
acc sg  úlf
gen sg  úlfs
dat sg  úlfi

nom pl  úlfar
acc pl  úlfa
gen pl  úlfa
dat pl  úlfum
```

---

### Verb morphology

Support:

- strong verbs
- weak verbs

Required features:

```
tense
mood
person
number
```

Minimal supported tenses:

```
present
past
```

Example strong verb:

```
veiða (to hunt)

present 3sg → veiðir
past 3sg → veiddi
```

---

### Adjective agreement

Adjectives must agree with:

```
gender
number
case
```

Example:

```
stórr úlfr
"big wolf"
```

---

### Word order

Preferred normalized order:

```
Subject – Verb – Object
```

Example:

```
Úlfr veiðir hjǫrt.
```

However Old Norse is flexible; poetic constructions are allowed in decorative mode.

---

# Translation Pipeline

## Step 1 — English parsing

Perform syntactic analysis:

```
subject
verb
object
modifiers
prepositional phrases
```

Example input:

```
The wolf hunts at night
```

Parsed form:

```
subject = wolf
verb = hunt
modifier = at night
```

---

## Step 2 — Lexical mapping

Map English tokens to dictionary lemmas.

Example:

```
wolf → úlfr
hunt → veiða
night → nótt
```

---

## Step 3 — Grammatical inflection

Apply grammatical rules.

Example:

```
subject → nominative
verb → present 3rd person
```

Result:

```
úlfr veiðir
```

---

## Step 4 — Phrase generation

Construct final Old Norse phrase.

Example:

```
úlfr veiðir um nótt
```

---

# Normalization Rules

Normalized orthography should follow **Old West Norse academic convention**.

Key characters:

```
þ  thorn
ð  eth
ǫ  open o
æ
```

Use normalized diacritics.

Example:

```
hjǫrtr
maður
```

---

# Proper Name Handling

Names should **not be translated semantically**.

Instead they should be **phonologically adapted**.

Example:

```
John → Jón
Mark → Mark
```

---

# Unknown Word Strategy

If a word is missing from the dictionary:

1. Attempt semantic synonym lookup
2. Attempt descriptive paraphrase
3. Preserve the word phonetically

Example:

```
computer
→ "reiknandi vél" (calculating machine)
```

---

# Kotlin Data Models

Dictionary entry:

```kotlin
data class NorseLexeme(
    val lemma: String,
    val partOfSpeech: PartOfSpeech,
    val gender: Gender?,
    val declensionClass: String?,
    val conjugationClass: String?,
    val meaning: String
)
```

Parsed sentence:

```kotlin
data class ParsedSentence(
    val subject: Token,
    val verb: Token,
    val objects: List<Token>,
    val modifiers: List<Token>
)
```

Generated Norse phrase:

```kotlin
data class OldNorsePhrase(
    val normalizedText: String,
    val tokens: List<String>
)
```

---

# Grammar Engine Architecture

Suggested modules:

```
norse-dictionary
norse-grammar
norse-inflection
norse-translation
```

Responsibilities:

Dictionary

```
lookup
lemma search
semantic similarity
```

Grammar engine

```
case assignment
agreement rules
word order
```

Inflection engine

```
verb conjugation
noun declension
adjective agreement
```

Translation engine

```
sentence generation
fallback logic
confidence scoring
```

---

# Validation Strategy

To ensure historical correctness:

Compare generated phrases against:

- Icelandic saga corpora
- Poetic Edda
- Prose Edda

The engine should avoid producing forms **not attested in Old Norse grammar patterns**.

---

# Summary

Old Norse generation pipeline:

```
English input
→ syntactic parsing
→ dictionary lookup
→ grammatical inflection
→ normalized Old Norse sentence
→ runic phonology layer
```

This layer ensures the app produces **historically plausible Norse before runic encoding**.
