package com.po4yka.runatal.ui.screens.packs

import com.po4yka.runatal.domain.model.QuotePack

internal data class PackPresentation(
    val sourceLabel: String,
    val readTimeLabel: String,
    val previewQuotes: List<PackPreviewQuote>
)

internal data class PackPreviewQuote(
    val rune: String,
    val label: String,
    val text: String,
    val isHighlighted: Boolean = false
)

internal object PackPresentationCatalog {
    private val presentations = mapOf(
        1L to PackPresentation(
            sourceLabel = "Poetic Edda",
            readTimeLabel = "~5 min read",
            previewQuotes = listOf(
                PackPreviewQuote(
                    rune = "ᚺ",
                    label = "Hávamál, st. 7",
                    text = "The cautious guest who comes to a meal speaks sparingly. He listens " +
                        "with care and watches with eyes — thus every wise one acts."
                ),
                PackPreviewQuote(
                    rune = "ᚠ",
                    label = "Hávamál, st. 76",
                    text = "Cattle die, kindred die, every man is mortal. But the good name " +
                        "never dies of one who has done well.",
                    isHighlighted = true
                ),
                PackPreviewQuote(
                    rune = "ᚷ",
                    label = "Hávamál, st. 42",
                    text = "A man should be loyal through life to friends, and return gift for gift. " +
                        "Laugh with the joyful, but with the liar repay a false tale with lies."
                ),
                PackPreviewQuote(
                    rune = "ᛏ",
                    label = "Hávamál, st. 16",
                    text = "The unwise man thinks he will live forever if he avoids battle; but old age " +
                        "gives him no peace, though spears may spare him."
                )
            )
        ),
        2L to PackPresentation(
            sourceLabel = "Elder Futhark",
            readTimeLabel = "~4 min read",
            previewQuotes = listOf(
                PackPreviewQuote(
                    rune = "ᚠ",
                    label = "Fehu",
                    text = "Wealth must be shared wisely, or it becomes a burden heavier than iron."
                ),
                PackPreviewQuote(
                    rune = "ᚨ",
                    label = "Ansuz",
                    text = "A word well-spoken can open halls that brute strength never enters."
                ),
                PackPreviewQuote(
                    rune = "ᚱ",
                    label = "Raido",
                    text = "The right road reveals itself only to the traveler willing to keep moving."
                ),
                PackPreviewQuote(
                    rune = "ᛏ",
                    label = "Tiwaz",
                    text = "Justice asks for courage before it grants peace."
                )
            )
        ),
        3L to PackPresentation(
            sourceLabel = "Mixed sources",
            readTimeLabel = "~4 min read",
            previewQuotes = listOf(
                PackPreviewQuote(
                    rune = "ᚱ",
                    label = "Trail saying",
                    text = "The path that teaches most is rarely the path that feels easiest at dawn."
                ),
                PackPreviewQuote(
                    rune = "ᚹ",
                    label = "Wayfarer's note",
                    text = "A wanderer keeps wisdom in the same satchel as bread and weather sense."
                ),
                PackPreviewQuote(
                    rune = "ᚾ",
                    label = "Road counsel",
                    text = "Ask the road no promise except that it will change the one who walks it."
                ),
                PackPreviewQuote(
                    rune = "ᛞ",
                    label = "Journey proverb",
                    text = "What is learned between milestones belongs to you longer than what was given at home."
                )
            )
        ),
        4L to PackPresentation(
            sourceLabel = "Norse proverbs",
            readTimeLabel = "~4 min read",
            previewQuotes = listOf(
                PackPreviewQuote(
                    rune = "ᚲ",
                    label = "Hall proverb",
                    text = "A warm fire and a truthful host turn winter into kinship."
                ),
                PackPreviewQuote(
                    rune = "ᚷ",
                    label = "Guest law",
                    text = "Generosity is remembered long after the feast itself is gone."
                ),
                PackPreviewQuote(
                    rune = "ᛒ",
                    label = "Bench wisdom",
                    text = "The table that welcomes many is stronger than the chest that hoards."
                ),
                PackPreviewQuote(
                    rune = "ᛗ",
                    label = "Home saying",
                    text = "Peace in the hearth is built each day in small and loyal acts."
                )
            )
        ),
        5L to PackPresentation(
            sourceLabel = "Calendar tradition",
            readTimeLabel = "~3 min read",
            previewQuotes = listOf(
                PackPreviewQuote(
                    rune = "ᛊ",
                    label = "Winter turning",
                    text = "Darkness is not an ending but a season for gathering strength."
                ),
                PackPreviewQuote(
                    rune = "ᛃ",
                    label = "Harvest mark",
                    text = "What was sown in patience returns when the year decides it is time."
                ),
                PackPreviewQuote(
                    rune = "ᛚ",
                    label = "Spring note",
                    text = "The thaw teaches that movement can begin quietly and still change everything."
                ),
                PackPreviewQuote(
                    rune = "ᚨ",
                    label = "Sun path",
                    text = "Each season gives a different answer to the same old need for light."
                )
            )
        ),
        6L to PackPresentation(
            sourceLabel = "Saga literature",
            readTimeLabel = "~4 min read",
            previewQuotes = listOf(
                PackPreviewQuote(
                    rune = "ᛏ",
                    label = "Battle counsel",
                    text = "Resolve matters most at the moment when fear asks to make the decision."
                ),
                PackPreviewQuote(
                    rune = "ᚢ",
                    label = "Shield saying",
                    text = "The one who stands firm for others is remembered longer than the one who boasts."
                ),
                PackPreviewQuote(
                    rune = "ᚺ",
                    label = "Field note",
                    text = "Honor is tested less by victory than by what you refuse to become to secure it."
                ),
                PackPreviewQuote(
                    rune = "ᛇ",
                    label = "Last watch",
                    text = "Endurance is courage spread across time."
                )
            )
        )
    )

    fun sourceLabel(pack: QuotePack): String = presentations[pack.id]?.sourceLabel ?: "Curated collection"

    fun readTimeLabel(pack: QuotePack): String = presentations[pack.id]?.readTimeLabel ?: "~4 min read"

    fun previewQuotes(pack: QuotePack): List<PackPreviewQuote> {
        return presentations[pack.id]?.previewQuotes ?: listOf(
            PackPreviewQuote(
                rune = pack.coverRune,
                label = pack.name,
                text = pack.description
            )
        )
    }
}
