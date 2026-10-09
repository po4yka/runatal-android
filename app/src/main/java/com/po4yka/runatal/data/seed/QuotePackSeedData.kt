package com.po4yka.runatal.data.seed

import com.po4yka.runatal.data.local.entity.QuotePackEntity
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.domain.model.QuotePackContentCatalog

/**
 * Seed data for curated quote packs.
 * Each pack represents a themed collection of Norse/runic wisdom quotes.
 */
internal object QuotePackSeedData {

    fun getInitialPacks(): List<QuotePackEntity> = listOf(
        QuotePackEntity(
            id = 1,
            name = "Hávamál Selections",
            description = "Modern paraphrases inspired by Hávamál's counsel " +
                "on wisdom, hospitality, and caution.",
            coverRune = "\u16BA",
            quoteCount = 0,
            isInLibrary = false
        ),
        QuotePackEntity(
            id = 2,
            name = "Elder Voices",
            description = "Original reflections inspired by Elder Futhark rune themes.",
            coverRune = "\u16A0",
            quoteCount = 0,
            isInLibrary = true
        ),
        QuotePackEntity(
            id = 3,
            name = "Path of the Wanderer",
            description = "Journeys, discovery, and the lessons found on unfamiliar roads.",
            coverRune = "\u16B1",
            quoteCount = 0,
            isInLibrary = false
        ),
        QuotePackEntity(
            id = 4,
            name = "Hearthfire Wisdom",
            description = "Original reflections on home, kinship, generosity, and shared meals.",
            coverRune = "\u16B2",
            quoteCount = 0,
            isInLibrary = false
        ),
        QuotePackEntity(
            id = 5,
            name = "Seasonal Runes",
            description = "Quotes aligned to solstices, equinoxes, and the turning year.",
            coverRune = "\u16CA",
            quoteCount = 0,
            isInLibrary = true
        ),
        QuotePackEntity(
            id = 6,
            name = "Warrior's Counsel",
            description = "Strength, resolve, and endurance for the path ahead.",
            coverRune = "\u16CF",
            quoteCount = 0,
            isInLibrary = false
        )
    ).map { pack -> pack.copy(quoteCount = getPackQuotes(pack.id).size) }

    fun getPackQuotes(packId: Long): List<QuoteEntity> {
        val content = QuotePackContentCatalog.content(packId) ?: return emptyList()
        return content.quotes.mapIndexed { index, quote ->
            QuoteEntity(
                textLatin = quote.text,
                author = "${content.sourceLabel} · ${quote.label}",
                canonicalKey = "pack:$packId:$index"
            )
        }
    }
}
