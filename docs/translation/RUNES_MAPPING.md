

# Runes Mapping Tables

This file contains the canonical mapping tables to use in the app for:

1. **Elder Futhark**
2. **Younger Futhark**

These tables are intended for the internal translation / transliteration pipeline and should be treated as the source of truth for rune inventory, transliteration symbols, and approximate phonemic values.

## Implementation status

The app now separates:

- `domain/transliteration/` for legacy direct transliteration
- `domain/translation/` for structured historical translation followed by rune rendering

Where the live code cannot yet express every historical edge case, the engine must emit notes and confidence rather than pretending the mapping is exact.

---

# General Rules

## 1. Do not treat these tables as direct English letter-substitution tables

- **Elder Futhark** should be used after a **Proto-Norse / Proto-Germanic-style reconstruction layer**.
- **Younger Futhark** should be used after a **normalized Old Norse layer** and then a **runic phonological spelling layer**.

## 2. Keep transliteration separate from glyph rendering

Each rune entry should support at least:

- `glyph`
- `transliteration`
- `name`
- `approximate_phonemes`
- `unicode_name`

## 3. Younger Futhark is many-to-one

Younger Futhark has only **16 runes**, so multiple Latin letters / phonemes map to the same rune.

Example:

- `k` and `g` often collapse to the same rune
- `t` and `d` often collapse to the same rune
- several vowels collapse into the same rune family depending on context

---

# Elder Futhark

## Notes

- Standard inventory: **24 runes**
- Primary use in the app: **reconstructed early Germanic / Proto-Norse output**
- Recommended default transliteration: traditional scholarly Latin transliteration

## Table

| Order | Rune | Transliteration | Approximate phoneme(s) | Conventional name | Unicode name |
|---|---|---|---|---|---|
| 1 | ᚠ | f | /f/, /ɸ/ | Fehu | RUNIC LETTER FEHU FEOH FE F |
| 2 | ᚢ | u | /u/, /uː/, /w/ in some environments | Uruz | RUNIC LETTER URUZ UR U |
| 3 | ᚦ | þ | /θ/, /ð/ | Thurisaz | RUNIC LETTER THURISAZ THURS THORN |
| 4 | ᚨ | a | /a/, /aː/ | Ansuz | RUNIC LETTER ANSUZ A |
| 5 | ᚱ | r | /r/ | Raidō | RUNIC LETTER RAIDO RAD REID R |
| 6 | ᚲ | k | /k/ | Kaunan / Kenaz | RUNIC LETTER KAUNA |
| 7 | ᚷ | g | /g/ | Gebō | RUNIC LETTER GEBO GYFU G |
| 8 | ᚹ | w | /w/ | Wunjō | RUNIC LETTER WYNN W |
| 9 | ᚺ | h | /h/ | Hagalaz | RUNIC LETTER HAGLAZ H |
| 10 | ᚾ | n | /n/ | Naudiz | RUNIC LETTER NAUDIZ NYD NAUD N |
| 11 | ᛁ | i | /i/, /iː/, /j/ in some environments | Isaz | RUNIC LETTER ISAZ IS ISS I |
| 12 | ᛃ | j | /j/ | Jēra | RUNIC LETTER JERAN J |
| 13 | ᛇ | ï / æ / eo* | uncertain; traditionally associated with /æː/, /ɪə/, or related values | Ēihwaz / Īhwaz | RUNIC LETTER IWAZ EOH |
| 14 | ᛈ | p | /p/ | Perthō | RUNIC LETTER PERTHO PEORTH P |
| 15 | ᛉ | z | Proto-Germanic /z/, later Proto-Norse /ʀ/-like development | Algiz / Elhaz* | RUNIC LETTER ALGIZ EOLHX |
| 16 | ᛊ | s | /s/ | Sōwilō | RUNIC LETTER SOWILO S |
| 17 | ᛏ | t | /t/ | Tīwaz | RUNIC LETTER TIWAZ TIR TYR T |
| 18 | ᛒ | b | /b/ | Berkanan | RUNIC LETTER BERKANAN BEORC BJARKAN B |
| 19 | ᛖ | e | /e/, /eː/ | Ehwaz | RUNIC LETTER EHWAZ EH E |
| 20 | ᛗ | m | /m/ | Mannaz | RUNIC LETTER MANNAZ MAN M |
| 21 | ᛚ | l | /l/ | Laukaz | RUNIC LETTER LAUKAZ LAGU LOGR L |
| 22 | ᛜ | ŋ / ng | /ŋ/ | Ingwaz | RUNIC LETTER INGWAZ |
| 23 | ᛞ | d | /d/ | Dagaz | RUNIC LETTER DAGAZ DAEG D |
| 24 | ᛟ | o | /o/, /oː/ | Othalan | RUNIC LETTER OTHALAN ETHEL O |

## Implementation Notes

### Recommended internal key format

Use a structured model similar to:

```json
{
  "glyph": "ᚠ",
  "transliteration": "f",
  "name": "Fehu",
  "approximatePhonemes": ["f", "ɸ"],
  "unicodeName": "RUNIC LETTER FEHU FEOH FE F"
}
```

### Important caveats

- `ᛇ` is historically one of the most uncertain runes in value and naming. Do not oversimplify it as a normal modern vowel.
- `ᛉ` should be preserved as transliteration **z** at the rune-table layer, even if later historical output may normalize it differently.
- `ᛜ` should usually be treated as a **phoneme-level rune** for /ŋ/, not as a general-purpose modern `ng` digraph replacement.

---

# Younger Futhark

## Notes

- Standard inventory: **16 runes**
- Primary use in the app: **Old Norse → runic spelling → Younger Futhark**
- Two major graphical variants must be supported:
  - **Long-branch** (Danish)
  - **Short-twig** (Swedish/Norwegian)

## Canonical 16-rune inventory

| Order | Long-branch | Short-twig | Transliteration | Approximate phoneme(s) | Conventional name | Unicode name(s) |
|---|---|---|---|---|---|---|
| 1 | ᚠ | ᚠ | f | /f/, /v/ | Fé | RUNIC LETTER FEHU FEOH FE F |
| 2 | ᚢ | ᚢ | u | /u/, /o/, /y/, /ø/, /w/, /v/ depending on context | Úr | RUNIC LETTER URUZ UR U |
| 3 | ᚦ | ᚦ | þ | /θ/, /ð/ | Þurs | RUNIC LETTER THURISAZ THURS THORN |
| 4 | ᚬ | ᚭ | ą / a / o | /a/, /ɑ/, /æ/, /o/ depending on context | Áss | long-branch: RUNIC LETTER ANSUZ A; short-twig variant commonly rendered with RUNIC LETTER OS O |
| 5 | ᚱ | ᚱ | r | /r/ | Reið | RUNIC LETTER RAIDO RAD REID R |
| 6 | ᚴ | ᚴ | k | /k/, /g/ | Kaun | RUNIC LETTER KAUNA |
| 7 | ᚼ | ᚽ | h | /h/ | Hagall | long-branch: RUNIC LETTER HAGLAZ H; short-twig: RUNIC LETTER SHORT-TWIG-HAGALAZ H |
| 8 | ᚾ | ᚿ | n | /n/ | Nauðr | long-branch: RUNIC LETTER NAUDIZ NYD NAUD N; short-twig: RUNIC LETTER SHORT-TWIG-NAUD N |
| 9 | ᛁ | ᛁ | i | /i/, /e/, /j/ depending on context | Íss | RUNIC LETTER ISAZ IS ISS I |
| 10 | ᛅ | ᛆ | a | /a/, /æ/, sometimes reduced vowel values depending on context | Ár | long-branch: RUNIC LETTER AR A; short-twig: RUNIC LETTER SHORT-TWIG-AR A |
| 11 | ᛋ | ᛌ | s | /s/ | Sól | long-branch: RUNIC LETTER SIGEL LONG-BRANCH-SOL S; short-twig: RUNIC LETTER SHORT-TWIG-SOL S |
| 12 | ᛏ | ᛐ | t | /t/, /d/ | Týr | long-branch: RUNIC LETTER TIWAZ TIR TYR T; short-twig: RUNIC LETTER SHORT-TWIG-TYR T |
| 13 | ᛒ | ᛓ | b | /b/, /p/ | Bjarkan | long-branch: RUNIC LETTER BERKANAN BEORC BJARKAN B; short-twig: RUNIC LETTER SHORT-TWIG-BJARKAN B |
| 14 | ᛘ | ᛙ | m | /m/ | Maðr | long-branch: RUNIC LETTER MANNAZ MAN M; short-twig: RUNIC LETTER SHORT-TWIG-MADR M |
| 15 | ᛚ | ᛚ | l | /l/ | Lǫgr | RUNIC LETTER LAUKAZ LAGU LOGR L |
| 16 | ᛦ | ᛧ | ʀ / yr | historical /ʀ/; later merged with /r/ in many contexts | Ýr | long-branch: RUNIC LETTER LONG-BRANCH-YR; short-twig: RUNIC LETTER SHORT-TWIG-YR |

## Recommended transliteration policy

At the rune-table layer:

- keep the final rune as **`ʀ`** internally for the 16th rune
- allow display alias **`yr`** for educational UI
- do **not** collapse it to plain `r` inside the core mapping table

## Recommended practical sound-collapse rules for preprocessing

These are not rune-table definitions, but they should inform the phonological conversion layer before mapping to Younger Futhark:

- `g` → `k` in many positions
- `d` → `t`
- `b` ↔ `p` share one rune
- `e` often maps through the `i` rune family
- `o`, `ø`, `y`, `u` often map through the `u` rune family

These rules should live in the **pre-mapping phonology layer**, not in the static rune table itself.

---

# Recommended Kotlin Data Models

## Elder Futhark

```kotlin
data class ElderRune(
    val order: Int,
    val glyph: String,
    val transliteration: String,
    val name: String,
    val approximatePhonemes: List<String>,
    val unicodeName: String,
)
```

## Younger Futhark

```kotlin
data class YoungerRune(
    val order: Int,
    val longBranchGlyph: String,
    val shortTwigGlyph: String,
    val transliteration: String,
    val name: String,
    val approximatePhonemes: List<String>,
    val unicodeNameLongBranch: String,
    val unicodeNameShortTwig: String?,
)
```

---

# Practical Implementation Guidance

## 1. Use rune tables only after language-stage conversion

Correct order:

```text
English
→ target historical language layer
→ phonological / orthographic normalization
→ rune mapping table
→ final glyph output
```

## 2. Keep mapping deterministic

The static tables in this file should not perform grammar or language inference.

They should only map:

- normalized transliteration tokens
- to the correct rune glyph variant

## 3. Separate static tables from rewrite rules

Create separate layers for:

- `HistoricalTranslationEngine`
- `PhonologyRewriteEngine`
- `RuneMappingEngine`
- `RuneRenderingEngine`

---

# Summary

Use this file as the canonical reference for:

- Elder Futhark rune inventory
- Younger Futhark rune inventory
- transliteration symbols
- glyph variants
- internal mapping data models

Do not use these tables as direct modern English substitution alphabets.
