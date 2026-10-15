package com.po4yka.runatal.domain.model

internal data class QuotePackContent(
    val sourceLabel: String,
    val quotes: List<PackContentQuote>
)

internal data class PackContentQuote(
    val rune: String,
    val label: String,
    val text: String,
    val isHighlighted: Boolean = false
)

internal object QuotePackContentCatalog {
    private val contents = mapOf(
        1L to QuotePackContent(
            sourceLabel = "Modern Hávamál paraphrases",
            quotes = listOf(
                PackContentQuote(
                    rune = "ᚺ",
                    label = "Hávamál, st. 7",
                    text = "The cautious guest who comes to a meal speaks sparingly. He listens " +
                        "with care and watches with eyes — thus every wise one acts."
                ),
                PackContentQuote(
                    rune = "ᚠ",
                    label = "Hávamál, st. 76",
                    text = "Cattle die, kindred die, every man is mortal. But the good name " +
                        "never dies of one who has done well.",
                    isHighlighted = true
                ),
                PackContentQuote(
                    rune = "ᚷ",
                    label = "Hávamál, st. 42",
                    text = "A man should be loyal through life to friends, and return gift for gift. " +
                        "Laugh with the joyful, but with the liar repay a false tale with lies."
                ),
                PackContentQuote(
                    rune = "ᛏ",
                    label = "Hávamál, st. 16",
                    text = "The unwise man thinks he will live forever if he avoids battle; but old age " +
                        "gives him no peace, though spears may spare him."
                )
            )
        ),
        2L to QuotePackContent(
            sourceLabel = "Modern themed reflections",
            quotes = listOf(
                PackContentQuote(
                    rune = "ᚠ",
                    label = "Fehu",
                    text = "Wealth must be shared wisely, or it becomes a burden heavier than iron."
                ),
                PackContentQuote(
                    rune = "ᚨ",
                    label = "Ansuz",
                    text = "A word well-spoken can open halls that brute strength never enters."
                ),
                PackContentQuote(
                    rune = "ᚱ",
                    label = "Raido",
                    text = "The right road reveals itself only to the traveler willing to keep moving."
                ),
                PackContentQuote(
                    rune = "ᛏ",
                    label = "Tiwaz",
                    text = "Justice asks for courage before it grants peace."
                )
            )
        ),
        3L to QuotePackContent(
            sourceLabel = "Modern themed reflections",
            quotes = listOf(
                PackContentQuote(
                    rune = "ᚱ",
                    label = "Trail saying",
                    text = "The path that teaches most is rarely the path that feels easiest at dawn."
                ),
                PackContentQuote(
                    rune = "ᚹ",
                    label = "Wayfarer's note",
                    text = "A wanderer keeps wisdom in the same satchel as bread and weather sense."
                ),
                PackContentQuote(
                    rune = "ᚾ",
                    label = "Road counsel",
                    text = "Ask the road no promise except that it will change the one who walks it."
                ),
                PackContentQuote(
                    rune = "ᛞ",
                    label = "Journey proverb",
                    text = "What is learned between milestones belongs to you longer than what was given at home."
                )
            )
        ),
        4L to QuotePackContent(
            sourceLabel = "Modern themed reflections",
            quotes = listOf(
                PackContentQuote(
                    rune = "ᚲ",
                    label = "Hall proverb",
                    text = "A warm fire and a truthful host turn winter into kinship."
                ),
                PackContentQuote(
                    rune = "ᚷ",
                    label = "Guest law",
                    text = "Generosity is remembered long after the feast itself is gone."
                ),
                PackContentQuote(
                    rune = "ᛒ",
                    label = "Bench wisdom",
                    text = "The table that welcomes many is stronger than the chest that hoards."
                ),
                PackContentQuote(
                    rune = "ᛗ",
                    label = "Home saying",
                    text = "Peace in the hearth is built each day in small and loyal acts."
                )
            )
        ),
        5L to QuotePackContent(
            sourceLabel = "Modern themed reflections",
            quotes = listOf(
                PackContentQuote(
                    rune = "ᛊ",
                    label = "Winter turning",
                    text = "Darkness is not an ending but a season for gathering strength."
                ),
                PackContentQuote(
                    rune = "ᛃ",
                    label = "Harvest mark",
                    text = "What was sown in patience returns when the year decides it is time."
                ),
                PackContentQuote(
                    rune = "ᛚ",
                    label = "Spring note",
                    text = "The thaw teaches that movement can begin quietly and still change everything."
                ),
                PackContentQuote(
                    rune = "ᚨ",
                    label = "Sun path",
                    text = "Each season gives a different answer to the same old need for light."
                )
            )
        ),
        6L to QuotePackContent(
            sourceLabel = "Modern themed reflections",
            quotes = listOf(
                PackContentQuote(
                    rune = "ᛏ",
                    label = "Battle counsel",
                    text = "Resolve matters most at the moment when fear asks to make the decision."
                ),
                PackContentQuote(
                    rune = "ᚢ",
                    label = "Shield saying",
                    text = "The one who stands firm for others is remembered longer than the one who boasts."
                ),
                PackContentQuote(
                    rune = "ᚺ",
                    label = "Field note",
                    text = "Honor is tested less by victory than by what you refuse to become to secure it."
                ),
                PackContentQuote(
                    rune = "ᛇ",
                    label = "Last watch",
                    text = "Endurance is courage spread across time."
                )
            )
        )
    )

    fun content(packId: Long): QuotePackContent? = contents[packId]

    fun sourceLabel(pack: QuotePack): String = content(pack.id)?.sourceLabel ?: "Curated collection"

    fun readTimeLabel(pack: QuotePack): String {
        val quotes = previewQuotes(pack)
        if (quotes.isEmpty()) return "Read time unavailable"
        val words = quotes.sumOf { it.text.split(Regex("\\s+")).size }
        val minutes = ((words + WORDS_PER_MINUTE - 1) / WORDS_PER_MINUTE).coerceAtLeast(1)
        return "~$minutes min read"
    }

    fun previewQuotes(pack: QuotePack): List<PackContentQuote> = content(pack.id)?.quotes.orEmpty()

    private const val WORDS_PER_MINUTE = 200
}
