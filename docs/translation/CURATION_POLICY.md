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

Every curated row must carry a stable `id`.

This includes:

- lexical entries
- runic corpus references
- phrase templates
- gold examples
- Erebor phrase mappings

Stable ids are required because the app persists provenance and uses `referenceId` in `TranslationProvenanceEntry`.

## Validation rules

The Gradle task `validateTranslationCuration` runs before asset generation and fails the build if:

- a required file is missing
- ids are duplicated
- a `sourceId` is unknown
- a strict lexical entry has no citation
- a template or gold result is missing script, fidelity, or derivation metadata
- a strict provenance row points at a missing source or reference id
- an Erebor phrase mapping points at a missing reference id

Unit tests in `CuratedTranslationAssetsTest` cover the same policy at the test layer.

## Strict mode requirements

`STRICT` output must be backed by one of:

- a gold example
- a curated phrase template
- an attested short form
- a cited lexical entry used in token composition

If those conditions are not met, the engine must return `UNAVAILABLE`.

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
3. wire strict rows to citations and reference ids
4. run `./gradlew :app:testDebugUnitTest` and `./gradlew :app:assembleDebug`

Do not document speculative modules or future corpora here as if they are already implemented.
