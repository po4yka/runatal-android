# Cirth encoding and font

Cirth uses the private-use UCSUR allocation U+E080–U+E0EB, not an allocation in the
Unicode Standard. The canonical glyph identities are defined by the
[UCSUR Cirth chart](https://www.kreativekorp.com/ucsur/charts/PDF/UE080.pdf).
`CirthAlphabet` is shared by direct transliteration and the reference chart. It
provides educational Latin spelling substitutions; it does not claim to implement
all of Tolkien's English pronunciation rules.

`fairfax_hd_cirth.ttf` is a subset of Kreative Software's Fairfax HD, distributed
under SIL OFL 1.1. The full copyright notice and license are bundled at
`app/src/main/assets/licenses/FairfaxHD-OFL.txt`. The source license declares no
Reserved Font Name. This font is selected for Cirth for every user font preference;
PUA glyphs are rendered directly and are never replaced by Germanic runes.

- Publisher: https://www.kreativekorp.com/software/fonts/fairfaxhd/
- Source: https://github.com/kreativekorp/open-relay/blob/e4b81241e8c7269a91c0bd865cf5c9e7208fbada/FairfaxHD/FairfaxHD.woff2
- Source Git blob: `67b8730cb8189ef1c3572957659cd6c70f2cc3ec`
- Source WOFF2 size: 730192 bytes
- Bundled subset SHA-256: `fd483372d980b4a314e3b684ccbf90453d8869f13efc73ef4390090357725c16`

The subset retains Cirth U+E080–U+E0FF, Latin U+0020–U+00FF, U+2019, and U+2014.
All 108 assigned Cirth codepoints have nonzero cmap glyphs and real outlines.
It was created with fontTools Subsetter, retaining all name records and license
metadata, then converting WOFF2 to an uncompressed TrueType font. No outlines
were redrawn.

The positive STRICT English profile reproduces the publisher's Cirth demo headed
“From the title page of Lord of the Rings.” Its corrected reading is independently
catalogued as DCS 2 in [Mellonath Daeron's inscription catalogue](https://forodrim.org/daeron/mdics.html):
“The Lord of the Rings translated from the Red Book.” The full phrase preserves
its published dots and boundary signs; eight distinct word forms are also usable
without guessing additional spelling rules. This is conservatively labelled
`RECONSTRUCTED`, with explicit publisher provenance, rather than `ATTESTED` from
an original Tolkien manuscript. Unwitnessed words are unavailable in STRICT and
explicit educational approximations in READABLE/DECORATIVE.

`CirthEncodingMigration` repairs only proven derived legacy quote fields and
engine-v4 glyph/token layers. It preserves identity and language/provenance
metadata; unknown or custom PUA text is not reinterpreted. Legacy encoding maps
exist only inside this one-time migration. Database version wiring is owned by
the database migration integration.
