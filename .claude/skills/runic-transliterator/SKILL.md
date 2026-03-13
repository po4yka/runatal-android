---
name: runic-transliterator
description: "Guide for working with the runic transliteration engine in the Runatal app. Use when: (1) adding a new runic script, (2) modifying character mappings, (3) fixing transliteration bugs, (4) understanding the transliteration architecture, or (5) adding rune-related features. Triggers on: transliterate, rune, runic, futhark, cirth, character mapping, script."
---

# Runic Transliteration Engine

## Architecture

```
RunicTransliterator (interface)
  +-- ElderFutharkTransliterator   (2nd-8th century, Unicode U+16A0-U+16FF)
  +-- YoungerFutharkTransliterator (9th-11th century Viking Age)
  +-- CirthTransliterator          (Tolkien's Angerthas, PUA U+E000+)

TransliterationFactory (@Singleton)
  - create(script: RunicScript): RunicTransliterator
  - transliterate(text: String, script: RunicScript): String
```

## File Locations

| What | Path |
|------|------|
| Interface | `domain/transliteration/RunicTransliterator.kt` |
| Elder Futhark | `domain/transliteration/ElderFutharkTransliterator.kt` |
| Younger Futhark | `domain/transliteration/YoungerFutharkTransliterator.kt` |
| Cirth | `domain/transliteration/CirthTransliterator.kt` |
| Cirth compat | `domain/transliteration/CirthGlyphCompat.kt` |
| Factory | `domain/transliteration/TransliterationFactory.kt` |
| Script enum | `domain/model/RunicScript.kt` |
| Tests | `test/.../domain/transliteration/` |
| Rune mappings doc | `docs/translation/RUNES_MAPPING.md` |

All source paths relative to `app/src/main/java/com/po4yka/runatal/`.

## How Transliteration Works

1. Input: Latin text string
2. Check digraphs first (multi-character mappings like "th", "ng")
3. Map remaining single characters to Unicode rune codepoints
4. Preserve punctuation and spaces
5. Case-insensitive (lowercase before mapping)
6. Output: Unicode string of rune characters

## Adding a New Script

1. Add enum value to `RunicScript` in `domain/model/RunicScript.kt`
2. Create `NewScriptTransliterator` implementing `RunicTransliterator`
   - Define `charMap: Map<String, String>` for single chars
   - Define `digraphMap: Map<String, String>` for multi-char sequences
   - Implement `transliterate(text: String): String`
3. Add `@Inject constructor()` for Hilt injection
4. Update `TransliterationFactory`:
   - Add constructor parameter
   - Add `when` branch in `create()`
5. Write unit tests with backtick names: `` `transliterate x to RUNE_NAME`() ``
6. Update `QuoteEntity` if pre-computed transliterations are stored
7. Update seed data if needed

## Modifying Character Mappings

1. Edit the relevant transliterator's `charMap` or `digraphMap`
2. Run existing tests: `./gradlew test --tests "ElderFutharkTransliteratorTest"`
3. Add new test cases for changed mappings
4. Check `docs/translation/RUNES_MAPPING.md` for reference

## Testing Pattern

Transliterators are pure functions -- no mocking needed:

```kotlin
class NewScriptTransliteratorTest {
    private val transliterator = NewScriptTransliterator()

    @Test
    fun `transliterate a to RUNE_NAME`() {
        assertThat(transliterator.transliterate("a")).isEqualTo("\uXXXX")
    }

    @Test
    fun `preserve spaces and punctuation`() {
        val result = transliterator.transliterate("hello world")
        assertThat(result).contains(" ")
    }

    @Test
    fun `handle empty string`() {
        assertThat(transliterator.transliterate("")).isEmpty()
    }
}
```

## Integration Points

- **QuoteRepository**: Calls `TransliterationFactory.transliterate()` when saving quotes
- **TranslationScreen**: Live transliteration as user types
- **Widget**: Displays pre-computed runic text
- **Seed data**: Pre-populated quotes include all three transliterations
