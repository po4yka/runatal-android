# Translation Data Curation Policy

This document describes how translation data is stored and validated in the repository.

## Source model

The app does not scrape or download linguistic data at runtime.

Curated source extracts are checked into:

- `app/src/main/translationSeed/translation/`

These checked-in files are the source of truth for runtime translation assets.

## Required asset families

The curated dataset is split into these files:

- `dataset_manifest.json`
- `source_manifest.json`
- `old_norse_lexicon.json`
- `proto_norse_lexicon.json`
- `paradigm_tables.json`
- `grammar_rules.json`
- `name_adaptations.json`
- `fallback_templates.json`
- `younger_phrase_templates.json`
- `elder_attested_forms.json`
- `runic_corpus_refs.json`
- `erebor_tables.json`
- `gold_examples.json`

## Stable identifiers

Curated lexical and corpus records must carry a stable `id`.

This includes:

- lexical entries
- runic corpus references
- phrase templates
- gold examples
- Erebor phrase mappings

Stable ids are required because the app persists provenance and uses `referenceId` in `TranslationProvenanceEntry`.
Inflection forms use the lexical record's id and grammatical form key; governed prepositions use their English
lookup key.

## Validation rules

The Gradle task `validateTranslationCuration` runs before asset generation and fails the build if:

- a required file is missing
- ids are duplicated
- a `sourceId` is unknown
- a strict lexical entry has no citation
- a lexical inflection table has an unknown source, missing citations, or blank forms
- a governed preposition has an unknown source, invalid case, or missing citations
- a template or gold result is missing script, fidelity, or derivation metadata
- a strict provenance row points at a missing source or reference id
- an Erebor phrase mapping points at a missing reference id

`CuratedTranslationAssetsTest` checks lexical and corpus citation links. `OldNorseGrammarAssetsTest` exercises
actual bundled forms, English agreement, case/gender/number selection, and unsupported grammar boundaries;
`TranslationSourcePreservationTest` covers unsupported and empty source input.

## Strict mode requirements

Younger `STRICT` composition requires supported syntax and grammatical roles, an eligible cited lexical entry,
and a cited form for each required inflection or government. Its AST selects person, number, case, gender,
and definiteness before rendering. Form tables are explicit; missing forms never authorize a guessed suffix.
A singular nominative dictionary headword and a cited infinitive can stand as lexical fragments.

Finite tables currently cover all present/past persons of `veiða` and `vera`, and only cited third-person singular
forms of the other supported verbs. Strong adjective forms do not authorize weak definite-phrase agreement.
Preposition entries must state their selected sense, governed case, source, and citations. Locative `under` is
explicitly interpreted as position with dative; directional government must not be inferred from that rule.

Younger gold examples and templates do not override these requirements. They are comparison records,
not runtime exceptions. Elder follows its inscription eligibility policy. Cirth STRICT uses the cited published title-page English profile; regression gold examples and secondary wiki pages establish no orthographic exemption.

Elder strict corpus references must include `attestation.locator`, `diplomaticText`, and `historicalStage`,
with a located source URL. A named fragment must be listed in `namedForms` and occur in the published text.
The runtime checks these proofs before admitting gold or template output; a homepage, arbitrary regression
fixture, or `ATTESTED` status alone is insufficient. Preserve ambiguous names instead of inventing their meaning.

If the applicable engine's requirements are not met, `STRICT` returns `UNAVAILABLE` with source diagnostics.
Readable/decorative fallback outside supported grammar must be `APPROXIMATED`, even when its individual
lexical entries are cited.

## Provenance expectations

Strict results should preserve:

- `sourceId`
- `referenceId` when a stable corpus record exists
- source label and role
- license metadata
- optional detail or citation text

## Editing guidance

When adding new curated data:

1. add or update source metadata in `source_manifest.json`
2. add stable ids for every new row
3. wire strict rows to citations and reference ids; cite inflection/government sources and each supported form
4. add actual-asset positive and boundary tests for new grammatical coverage
5. run the unit tests, curation validator, and debug build through the repository's build gate

Do not document speculative modules or future corpora here as if they are already implemented.
