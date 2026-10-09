# Translation System Architecture

This directory documents the translation system that is implemented in the app today.

## Runtime scope

The app ships two separate stacks:

- `domain/transliteration/` for direct Latin spelling-to-rune transliteration
- `domain/translation/` for structured historical translation and Erebor transcription

`Transliterate` remains the default user-facing mode. `Translate` is a separate feature-flagged mode with structured output layers and provenance.

## Translation engines

The translation stack is offline and asset-backed. It currently exposes three engines:

- `YoungerFutharkTranslationEngine` English -> normalized Old Norse -> diplomatic Latin rune spelling -> Younger Futhark glyphs
- `ElderFutharkTranslationEngine` English -> located attested forms or curated Proto-Norse approximation/source preservation -> Elder Futhark glyphs
- `EreborCirthTranslationEngine` cited title-page English word/phrase profile -> genuine UCSUR Cirth glyphs; other English spelling uses explicit educational approximation

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

- `HistoricalLexiconStore` Old Norse and Proto-Norse lexicon entries, paradigm tables, grammar rules, name adaptations, and fallback templates
- `RunicCorpusStore` gold examples, Younger phrase templates, Elder attested forms, and runic corpus references
- `EreborOrthographyStore` the cited published title-page phrase and eight witnessed word forms

The shipped provider is `AssetTranslationDatasetProvider`, which reads generated JSON assets from `app/src/main/translationSeed/translation/`.

## Selection precedence

The engines do not use one generic fallback path. They use precedence rules:

- Younger Futhark source-span scanner -> lexicon-backed AST -> grammatical role assignment -> explicit cited forms -> diplomatic spelling and glyph rendering
  - unsupported syntax, agreement, government, or forms: `STRICT` returns `UNAVAILABLE`
  - `READABLE`/`DECORATIVE` can fall back to an explicitly `APPROXIMATED` lexical or phonological rendering
- Elder Futhark eligible attested gold example -> located attested form/template -> readable/decorative token composition -> strict unavailable
- Cirth exact published title-page phrase -> witnessed profile words -> strict unavailable / readable UCSUR glyph approximation. Gold examples cannot bypass this profile.

Younger gold examples and phrase templates remain dataset comparison records; they do not bypass the grammar pipeline.

## Younger grammar scope

`EnglishGrammarParser` represents noun phrases, pronoun/noun subjects, governed prepositional phrases,
infinitive fragments, and a single finite declarative clause. `OldNorseGrammarStage` assigns person, number,
case, and gender before selecting forms from the lexical entry. English finite agreement is also checked;
for example, `I hunt` is supported but `I hunts` is unavailable in `STRICT`.

The current cited form tables provide:

- all six person/number combinations of present and past `veiða` and `vera`
- cited third-person singular present/past forms for `ganga`, `gera`, and `taka`; other persons are unavailable
- singular/plural case and definite noun forms for wolf, king, mountain, work, and journey; partial night forms
- strong `mikill` adjective agreement by case, gender, and number
- accusative objects for `veiða`, and nominative copular noun/adjective predicates
- `with` -> `með` + dative, `to` -> `til` + genitive, and temporal `at night` -> `um` + accusative
- locative `under` -> `undir` + dative, including movement within that location; notes explicitly state this reading,
  and a directional goal is not inferred

For example, `I hunt` -> `ek veiði`, `with king` -> `með konungi`, `great work` -> `mikit verk`,
and `mountains` -> `fjǫll`. Singular nominative lexical headwords can also be used without a separate form table.

Unsupported forms are not synthesized from suffix guesses. Weak adjective forms in definite noun phrases,
unknown predicate gender, other object/preposition government, and more complex syntax remain unavailable in
`STRICT`. Pronoun notes make the current singular `you` and masculine plural `they` interpretations explicit;
`you all` selects plural.

The source scanner preserves raw non-whitespace spans and original offsets, normalizes lookup spelling to NFC,
and recognizes internal smart apostrophes. Numbers, identifiers, and unsupported Unicode spans cannot disappear:
`STRICT` rejects them, while approximation modes preserve them explicitly. Empty, punctuation-only, or
invisible-only input never produces a successful translation.

Elder `STRICT` gold and template selection requires a located published inscription, matching historical stage,
and a normalized/diplomatic form documented by that source. Gold glyphs must also match the attested renderer.
The current positive corpus uses the National Museum of Denmark's Gallehus inscription and its documented
personal names. Its capital `R` transcription marker maps to the Elder rune normalized as `z`; output represents
rune identities rather than a facsimile. Unverified wolf/king reconstructions remain explicit approximation data,
and the old wolf-night regression record cannot authorize `STRICT` output.

Elder approximation uses only the Proto-Norse lexical store for target-language forms. An unknown source word
is preserved explicitly as `PRESERVED_SOURCE`; it never borrows the Old Norse paraphrase table. Combining a
Proto-Norse form and preserved input yields `MIXED_PROTO_NORSE_SOURCE`, with approximation status and notes.
Long/nasal target vowel markings remain in the language layer and reduce to the appropriate Elder rune class
for glyph output. Unsupported Unicode source spans remain visibly preserved rather than being mistaken for
a historical-language reconstruction.

## Persistence

Structured translation output is stored in Room:

- `translation_records` cached translation results keyed by quote, script, fidelity, variant, engine version, and dataset version
- `translation_backfill_state` resumable backfill progress, restarted when engine or dataset versions change

`TranslationRepository` owns cache lookup, persistence, lazy generation, and backfill behavior. Both exact selection
and latest-available lookup require the current engine and dataset versions.

New quote saves store direct transliterations in `quotes.runic*`; historical results are stored exclusively in
`translation_records`. Migration 8 -> 9 regenerates proven legacy Younger strings and quote fields that exactly
match a historical record's source text, script, and glyphs. It preserves historical records, quote identity and
metadata, and unknown/manual or orphaned strings.

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

Quote and share surfaces can prefer current-version cached structured translations when available and otherwise
fall back to stored direct transliterations. Existing unverified stored strings are preserved by migration.

## Accuracy policy

`STRICT` is conservative. If the engine cannot produce a defensible result, it returns `UNAVAILABLE` with notes and unresolved tokens instead of fabricating output.

`READABLE` and `DECORATIVE` may use curated paraphrase or phonological-preservation fallbacks, but those results must
be marked as approximations. Supported Younger grammar remains reconstructed in every fidelity mode.

Younger grammatical reconstructions combine lexical citations with inflection and government citations from the
source manifest, including Barnes's *A New Introduction to Old Norse I* and Zoëga. Missing lexical/inflection
sources or citations cannot authorize `STRICT` reconstruction. Runic corpus references are not a substitute for
these grammatical proofs, and reconstructed forms are not presented as inscription attestation.

Cirth encoding, font licensing, bounded English profile, and legacy repair are documented in [CIRTH_FONT.md](../fonts/CIRTH_FONT.md).
