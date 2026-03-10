package com.po4yka.runicquotes.data.seed

import com.po4yka.runicquotes.data.local.entity.QuotePackEntity

/**
 * Seed data for curated quote packs.
 * Each pack represents a themed collection of Norse/runic wisdom quotes.
 */
internal object QuotePackSeedData {

    fun getInitialPacks(): List<QuotePackEntity> = listOf(
        QuotePackEntity(
            name = "Havamal Selections",
            description = "Timeless verses from the Words of the High One, " +
                "offering guidance on wisdom, hospitality, and the art of living.",
            coverRune = "\u16BA",
            quoteCount = 12,
            isInLibrary = false
        ),
        QuotePackEntity(
            name = "Elder Voices",
            description = "Ancient proverbs and sayings passed down through " +
                "generations of Norse oral tradition.",
            coverRune = "\u16A8",
            quoteCount = 8,
            isInLibrary = false
        ),
        QuotePackEntity(
            name = "Path of the Wanderer",
            description = "Reflections on journey, discovery, and the wisdom " +
                "found along the open road.",
            coverRune = "\u16B1",
            quoteCount = 10,
            isInLibrary = false
        ),
        QuotePackEntity(
            name = "Wisdom of the North",
            description = "A broad collection of Norse philosophical insights " +
                "on fate, honor, courage, and the natural world.",
            coverRune = "\u16DF",
            quoteCount = 15,
            isInLibrary = false
        ),
        QuotePackEntity(
            name = "Runic Meditations",
            description = "Contemplative passages for quiet reflection, " +
                "centered on the mystical power of the runes.",
            coverRune = "\u16C1",
            quoteCount = 6,
            isInLibrary = false
        )
    )
}
