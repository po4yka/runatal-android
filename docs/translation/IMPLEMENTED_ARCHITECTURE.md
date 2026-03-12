# Translation System Architecture

This directory documents the translation system that is implemented in the app today.

## Runtime scope

The app ships two separate stacks:

- `domain/transliteration/` for legacy direct letter-to-rune transliteration
- `domain/translation/` for structured historical translation and Erebor transcription

`Transliterate` remains the default user-facing mode. `Translate` is a separate feature-flagged mode with structured output layers and provenance.

## Translation engines

The translation stack is offline and asset-backed. It currently exposes three engines:

- `YoungerFutharkTranslationEngine`
  English -> normalized Old Norse -> diplomatic Latin rune spelling -> Younger Futhark glyphs
- `ElderFutharkTranslationEngine`
  English -> constrained Proto-Norse reconstruction -> Elder Futhark glyphs
- `EreborCirthTranslationEngine`
  English/Westron-style transcription -> Erebor diplomatic sequence layer -> Cirth glyphs

Each engine returns `TranslationResult` with:

- source text
- target script
- fidelity
- derivation kind
- historical stage
- normalized form
- diplomatic form
- glyph output
- resolution status
- confidence
- notes
- unresolved tokens
- provenance
- token breakdown
- engine version
- dataset version

## Data sources and stores

The runtime dataset is split into three internal stores:

- `HistoricalLexiconStore`
  Old Norse and Proto-Norse lexicon entries, paradigm tables, grammar rules, name adaptations, and fallback templates
- `RunicCorpusStore`
  gold examples, Younger phrase templates, Elder attested forms, and runic corpus references
- `EreborOrthographyStore`
  Erebor sequence tables, phrase mappings, long-vowel and long-consonant tables

The shipped provider is `AssetTranslationDatasetProvider`, which reads generated JSON assets from `app/src/main/translationSeed/translation/`.

## Selection precedence

The engines do not use one generic fallback path. They use precedence rules:

- Younger Futhark
  gold example -> curated phrase template -> token composition -> readable/decorative fallback -> strict unavailable
- Elder Futhark
  gold example -> curated attested short form/template -> readable/decorative token composition -> strict unavailable
- Erebor
  gold example -> curated phrase mapping -> sequence-table transcription -> readable character fallback -> strict unavailable

## Persistence

Structured translation output is stored in Room:

- `translation_records`
  cached translation results keyed by quote, script, fidelity, variant, engine version, and dataset version
- `translation_backfill_state`
  resumable one-time backfill progress

`TranslationRepository` owns cache lookup, persistence, lazy generation, and backfill behavior.

## UI surfaces

The translation screen can display:

- mode toggle
- script selector
- fidelity selector
- Younger variant selector
- normalized and diplomatic layers
- resolution badge
- derivation label
- provenance
- token breakdown
- unavailable explanation

Quote and share surfaces can prefer cached structured translations when available and otherwise fall back to stored transliteration output.

## Accuracy policy

`STRICT` is conservative. If the engine cannot produce a defensible result, it returns `UNAVAILABLE` with notes and unresolved tokens instead of fabricating output.

`READABLE` and `DECORATIVE` may use curated paraphrase or phonological-preservation fallbacks, but those results must be marked as approximations.
