package com.po4yka.runatal.data.seed

import com.po4yka.runatal.domain.transliteration.CirthAlphabet
import com.po4yka.runatal.data.local.entity.RuneReferenceEntity

/**
 * Seed data for rune references across Elder Futhark, Younger Futhark, and Cirth scripts.
 */
internal object RuneReferenceSeedData {

    private const val ELDER_FUTHARK = "elder_futhark"
    private const val YOUNGER_FUTHARK = "younger_futhark"
    private const val CIRTH = "cirth"

    fun getElderFutharkRunes(): List<RuneReferenceEntity> = listOf(
        rune("\u16A0", "Fehu", "f", "Wealth", ELDER_FUTHARK,
            "Represents cattle and movable property in ancient Germanic society."),
        rune("\u16A2", "Uruz", "u", "Aurochs", ELDER_FUTHARK,
            "Symbolizes the wild ox, embodying untamed strength and vitality."),
        rune("\u16A6", "Thurisaz", "th", "Giant", ELDER_FUTHARK,
            "Associated with the god Thor and his protective power against chaos."),
        rune("\u16A8", "Ansuz", "a", "God", ELDER_FUTHARK,
            "Connected to Odin and the divine gift of speech and wisdom."),
        rune("\u16B1", "Raido", "r", "Ride", ELDER_FUTHARK,
            "Represents journey and the cosmic order of movement."),
        rune("\u16B2", "Kauna", "k", "Torch", ELDER_FUTHARK,
            "Symbolizes illumination and the controlled fire of knowledge."),
        rune("\u16B7", "Gebo", "g", "Gift", ELDER_FUTHARK,
            "Represents the sacred exchange of gifts that binds relationships."),
        rune("\u16B9", "Wunjo", "w", "Joy", ELDER_FUTHARK,
            "Symbolizes harmony, happiness, and fellowship among kin."),
        rune("\u16BB", "Haglaz", "h", "Hail", ELDER_FUTHARK,
            "Represents the destructive yet transformative power of nature."),
        rune("\u16BE", "Naudiz", "n", "Need", ELDER_FUTHARK,
            "Symbolizes necessity, constraint, and the friction that sparks creation."),
        rune("\u16C1", "Isaz", "i", "Ice", ELDER_FUTHARK,
            "Represents stillness, concentration, and the primordial ice of Niflheim."),
        rune("\u16C3", "Jeran", "j", "Year", ELDER_FUTHARK,
            "Symbolizes the harvest cycle and the reward of patient effort."),
        rune("\u16C7", "Iwaz", "ei", "Yew", ELDER_FUTHARK,
            "Associated with the yew tree, symbolizing endurance and resilience."),
        rune("\u16C8", "Perth", "p", "Lot cup", ELDER_FUTHARK,
            "Connected to fate, mystery, and the casting of lots."),
        rune("\u16C9", "Algiz", "z", "Elk sedge", ELDER_FUTHARK,
            "A protective rune associated with the splayed hand warding off evil."),
        rune("\u16CA", "Sowilo", "s", "Sun", ELDER_FUTHARK,
            "Represents the life-giving power of the sun and victory."),
        rune("\u16CF", "Tiwaz", "t", "Tyr", ELDER_FUTHARK,
            "Named after the god Tyr, symbolizing justice and self-sacrifice."),
        rune("\u16D2", "Berkanan", "b", "Birch", ELDER_FUTHARK,
            "Associated with the birch tree, representing renewal and new beginnings."),
        rune("\u16D6", "Ehwaz", "e", "Horse", ELDER_FUTHARK,
            "Symbolizes the sacred bond between horse and rider, trust and partnership."),
        rune("\u16D7", "Mannaz", "m", "Man", ELDER_FUTHARK,
            "Represents humanity, social order, and shared human experience."),
        rune("\u16DA", "Laguz", "l", "Water", ELDER_FUTHARK,
            "Symbolizes the primal waters, intuition, and the flow of life."),
        rune("\u16DC", "Ingwaz", "ng", "Ing", ELDER_FUTHARK,
            "Named after the fertility god Ing, representing potential and gestation."),
        rune("\u16DF", "Othalan", "o", "Heritage", ELDER_FUTHARK,
            "Represents ancestral homeland, inherited property, and noble lineage."),
        rune("\u16DE", "Dagaz", "d", "Day", ELDER_FUTHARK,
            "Symbolizes the dawn, breakthrough, and the balance between light and dark."),
    )

    fun getYoungerFutharkRunes(): List<RuneReferenceEntity> = listOf(
        rune("\u16A0", "Fe", "f", "Wealth", YOUNGER_FUTHARK,
            "Retained from Elder Futhark as the rune of prosperity and cattle."),
        rune("\u16A2", "Ur", "u", "Slag", YOUNGER_FUTHARK,
            "Meaning shifted from aurochs to slag or drizzle in the Viking Age."),
        rune("\u16A6", "Thurs", "th", "Giant", YOUNGER_FUTHARK,
            "Preserved the thorny giant meaning from the older futhark."),
        rune("\u16AC", "As", "a", "God", YOUNGER_FUTHARK,
            "Represents the Aesir gods, adapted from Elder Futhark Ansuz."),
        rune("\u16B1", "Reid", "r", "Ride", YOUNGER_FUTHARK,
            "Symbolizes riding and travel, unchanged from the elder form."),
        rune("\u16B4", "Kaun", "k", "Ulcer", YOUNGER_FUTHARK,
            "Meaning shifted from torch to sore or ulcer in Norse tradition."),
        rune("\u16BC", "Hagall", "h", "Hail", YOUNGER_FUTHARK,
            "Redesigned glyph representing hail as a seed of transformation."),
        rune("\u16BE", "Naud", "n", "Need", YOUNGER_FUTHARK,
            "Kept the concept of necessity and distress from the older futhark."),
        rune("\u16C1", "Is", "i", "Ice", YOUNGER_FUTHARK,
            "The simplest rune, a single stroke representing ice and stillness."),
        rune("\u16C5", "Ar", "a", "Plenty", YOUNGER_FUTHARK,
            "Represents a good year and bountiful harvest in Norse poetry."),
        rune("\u16CB", "Sol", "s", "Sun", YOUNGER_FUTHARK,
            "Retained the solar symbolism of power and light."),
        rune("\u16CF", "Tyr", "t", "Tyr", YOUNGER_FUTHARK,
            "Named after the one-handed god of justice and war."),
        rune("\u16D2", "Bjarkan", "b", "Birch", YOUNGER_FUTHARK,
            "Represents the birch twig, associated with fertility and growth."),
        rune("\u16D8", "Madr", "m", "Man", YOUNGER_FUTHARK,
            "Symbolizes humanity and the joy and sorrow of mortal life."),
        rune("\u16DA", "Logr", "l", "Water", YOUNGER_FUTHARK,
            "Represents the sea and waterfall, vital to Norse seafaring life."),
        rune("\u16E6", "Yr", "y", "Yew bow", YOUNGER_FUTHARK,
            "Represents the yew bow, a symbol of craftsmanship and defense."),
    )

    fun getCirthRunes(): List<RuneReferenceEntity> =
        (CirthAlphabet.letters.filterKeys { it != "c" && it != "q" } + CirthAlphabet.sequences)
            .filterKeys { it != "ph" }
            .entries.distinctBy { it.value }
            .map { (sound, glyph) ->
                rune(
                    glyph, "Cirth ${sound.uppercase()}", sound, "UCSUR glyph identity", CIRTH,
                    "UCSUR Cirth glyph catalogue: U+${glyph.single().code.toString(16).uppercase()}. " +
                        "English title-page usage can assign a different sound to this glyph. " +
                        "https://www.kreativekorp.com/ucsur/charts/PDF/UE080.pdf"
                )
            }

    private fun rune(
        character: String,
        name: String,
        pronunciation: String,
        meaning: String,
        script: String,
        history: String,
    ) = RuneReferenceEntity(
        id = 0,
        character = character,
        name = name,
        pronunciation = pronunciation,
        meaning = meaning,
        history = history,
        script = script,
    )
}
