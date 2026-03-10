# Runic Translation Engine Specification

This document defines the architecture, linguistic pipeline, and correctness rules for translating English text into:

1. **Younger Futhark (Viking Age runes)**
2. **Elder Futhark (early Germanic runes)**
3. **Angerthas Erebor (Tolkien dwarf runes)**

The system must treat these as **three different writing systems with different historical languages**, not as simple alphabet substitutions.

---

# Core Design Principle

The application must **never directly map English letters to runes in strict mode**.

Each system requires an intermediate linguistic layer.

Correct pipelines:

```
English → Old Norse → Younger Futhark
English → Proto‑Norse → Elder Futhark
English → Erebor Cirth transcription
```

The app must always expose **three output layers**:

1. Normalized language form
2. Diplomatic Latin transliteration
3. Final rune / cirth glyphs

---

# System Architecture

Each translation engine returns a structured result.

Example schema:

```json
{
  "engine": "younger_futhark | elder_futhark | angerthas_erebor",
  "fidelity": "strict | readable | decorative",
  "input_english": "...",
  "historical_stage": "Old Norse | Proto-Norse | English/Westron",
  "normalized_form": "...",
  "diplomatic_form": "...",
  "glyph_output": "...",
  "variant": "...",
  "confidence": 0.0,
  "notes": []
}
```

---

# Engine 1: Younger Futhark

## Purpose

Generate historically plausible **Viking Age rune inscriptions**.

Younger Futhark contains **16 runes** and was used primarily for **Old Norse**.

## Pipeline

### 1 Input processing

Parse English input:

- sentence type
- tense
- plurality
- names

### 2 Translate meaning to Old Norse

Produce a **grammatically correct Old Norse phrase**.

Rules:

- prefer Old West Norse normalization
- preserve names phonetically unless requested otherwise
- paraphrase modern concepts

Example layer:

```
English: The wolf hunts at night
Old Norse: Úlfr veiðir um nótt
```

### 3 Normalize Latin Old Norse

Store canonical normalized form.

Example:

```
úlfr veiðir um nótt
```

### 4 Generate runic phonological spelling

Younger Futhark is **phonological** and has limited characters.

Important reductions:

```
o → u
e → i
d → t
g → k
```

Write words **as they sound**, not as modern spelling.

### 5 Rune variant selection

Allow selection:

- long‑branch
- short‑twig

### 6 Render runes

Example:

```
ᚢᛚᚠᚱ ᚢᛁᚦᛁᚱ ᚢᛘ ᚾᚢᛏ
```

### 7 Output bundle

Return:

- English input
- Old Norse
- runic transliteration
- rune glyphs
- confidence

---

# Engine 2: Elder Futhark

## Purpose

Produce **early Germanic runic forms**.

Elder Futhark corresponds to **Proto‑Norse / Proto‑Scandinavian**, not Old Norse.

Never route through Old Norse.

## Pipeline

### 1 Parse English meaning

Same parsing step as Younger Futhark.

### 2 Translate into Proto‑Norse

Use restricted lexicon.

Prefer:

- simple phrases
- names
- formulaic expressions

Avoid modern vocabulary.

### 3 Reconstruction layer

Apply phonological reconstruction rules.

Output reconstructed Latin form.

Example:

```
Proto‑Norse: wulfaz haitai
```

### 4 Map phonology to Elder Futhark

Example mapping:

```
w → ᚹ
u → ᚢ
l → ᛚ
f → ᚠ
a → ᚨ
z → ᛉ
```

### 5 Render runes

Example:

```
ᚹᚢᛚᚠᚨᛉ
```

### 6 Output bundle

Return:

- English input
- Proto‑Norse reconstruction
- Latin transliteration
- Elder Futhark glyphs
- confidence

Confidence should be lower for long sentences.

---

# Engine 3: Angerthas Erebor

## Purpose

Produce **Tolkien dwarf rune transcription**.

Important:

- Angerthas Erebor ≠ Hobbit runes
- Hobbit map uses **Anglo‑Saxon futhorc**

This engine implements **Erebor mode Cirth**.

## Pipeline

### 1 Normalize English

Convert to lowercase.

Remove unsupported punctuation.

Example:

```
The king under the mountain
```

### 2 Tokenize clusters

Process clusters before single letters.

Supported clusters include:

```
ll
the
ai
ay
au
aw
ea
ee
eu
ew
oa
oo
ou
ow
```

### 3 Map tokens to Erebor Cirth

Use table mapping tokens to runes.

### 4 Apply Erebor diacritics

Special marks include:

- over‑circumflex for long consonants
- under‑bar for long vowels
- dedicated sign for long **ll**

### 5 Render Cirth glyphs

Use custom font or vector glyph set.

### 6 Output bundle

Return:

- English input
- tokenized form
- Erebor glyph sequence
- notes

---

# Rendering

## Historical runes

Unicode block:

```
U+16A0 – U+16FF
```

Supports:

- Elder Futhark
- Younger Futhark

## Cirth

Unicode does not provide full production support.

Implementation recommendation:

- custom font
- SVG glyph layer

---

# Fidelity Modes

## Strict scholarly

- full linguistic pipeline
- show intermediate layers
- historical constraints enforced

## Readable

- relaxed reconstruction
- simplified grammar

## Decorative

- optional letter substitution
- must display warning

---

# Validation Data

Use real inscriptions to validate algorithms.

Recommended dataset:

**Scandinavian Runic-text Database (Runor)**

Contains:

- rune transcription
- normalized form
- translation
- metadata

---

# Hard Rules

The app must enforce the following:

1. No direct English → rune substitution in strict mode.
2. Younger Futhark must pass through Old Norse.
3. Elder Futhark must pass through Proto‑Norse reconstruction.
4. Angerthas Erebor must be treated as a transcription system.
5. Intermediate forms must always be visible.

---

# Summary

The correct conceptual model:

```
Younger Futhark = translation engine
Elder Futhark = reconstruction engine
Angerthas Erebor = transcription engine
```

Treating them as separate pipelines ensures historical accuracy and prevents the most common mistakes made by online rune translators.
