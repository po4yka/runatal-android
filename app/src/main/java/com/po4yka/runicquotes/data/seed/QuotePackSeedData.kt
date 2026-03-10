package com.po4yka.runicquotes.data.seed

import com.po4yka.runicquotes.data.local.entity.QuotePackEntity

/**
 * Seed data for curated quote packs.
 * Each pack represents a themed collection of Norse/runic wisdom quotes.
 */
internal object QuotePackSeedData {

    fun getInitialPacks(): List<QuotePackEntity> = listOf(
        QuotePackEntity(
            id = 1,
            name = "Hávamál Selections",
            description = "Verses from the Words of the High One — Odin's counsel " +
                "on wisdom, hospitality, and caution.",
            coverRune = "\u16BA",
            quoteCount = 24,
            isInLibrary = false
        ),
        QuotePackEntity(
            id = 2,
            name = "Elder Voices",
            description = "Core teachings drawn from Elder Futhark lore and early rune traditions.",
            coverRune = "\u16A0",
            quoteCount = 18,
            isInLibrary = true
        ),
        QuotePackEntity(
            id = 3,
            name = "Path of the Wanderer",
            description = "Journeys, discovery, and the lessons found on unfamiliar roads.",
            coverRune = "\u16B1",
            quoteCount = 12,
            isInLibrary = false
        ),
        QuotePackEntity(
            id = 4,
            name = "Hearthfire Wisdom",
            description = "Home, kinship, generosity, and the warmth of shared meals.",
            coverRune = "\u16B2",
            quoteCount = 15,
            isInLibrary = false
        ),
        QuotePackEntity(
            id = 5,
            name = "Seasonal Runes",
            description = "Quotes aligned to solstices, equinoxes, and the turning year.",
            coverRune = "\u16CA",
            quoteCount = 8,
            isInLibrary = true
        ),
        QuotePackEntity(
            id = 6,
            name = "Warrior's Counsel",
            description = "Strength, resolve, and endurance for the path ahead.",
            coverRune = "\u16CF",
            quoteCount = 16,
            isInLibrary = false
        )
    )
}
