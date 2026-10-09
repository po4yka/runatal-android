package com.po4yka.runatal.domain.transliteration

import java.util.Locale

/**
 * Glyph identities from the UCSUR Cirth registry, not an official Unicode allocation.
 * https://www.kreativekorp.com/ucsur/charts/PDF/UE080.pdf
 * Letter substitution is an educational approximation, not an English orthography claim.
 */
object CirthAlphabet {
    val letters: Map<String, String> = mapOf(
        "p" to "\uE080",
        "b" to "\uE081",
        "f" to "\uE082",
        "v" to "\uE083",
        "t" to "\uE087",
        "d" to "\uE088",
        "þ" to "\uE089",
        "ð" to "\uE08A",
        "n" to "\uE08B",
        "j" to "\uE08D",
        "k" to "\uE091",
        "c" to "\uE091",
        "g" to "\uE092",
        "r" to "\uE09C",
        "l" to "\uE09E",
        "s" to "\uE0A1",
        "z" to "\uE0A3",
        "i" to "\uE0A7",
        "u" to "\uE0AA",
        "w" to "\uE0AC",
        "e" to "\uE0AF",
        "a" to "\uE0B1",
        "o" to "\uE0B3",
        "h" to "\uE0B9",
        "m" to "\uE085",
        "y" to "\uE0E1",
        "q" to "\uE096"
    )
    val sequences: Map<String, String> = mapOf(
        "th" to "\uE089",
        "dh" to "\uE08A",
        "ch" to "\uE08C",
        "sh" to "\uE08E",
        "ng" to "\uE095",
        "kh" to "\uE093",
        "gh" to "\uE094",
        "ph" to "\uE082",
        "qu" to "\uE096",
        "ll" to "\uE0CE"
    )

    /** Transcribes modern Latin spelling into the documented Cirth glyph repertoire. */
    fun transliterate(text: String): String {
        var remaining = text.lowercase(Locale.ROOT)
        return buildString {
            while (remaining.isNotEmpty()) {
                val sequence = sequences.keys.firstOrNull { remaining.startsWith(it) }
                if (sequence != null) {
                    append(sequences.getValue(sequence))
                    remaining = remaining.drop(sequence.length)
                } else {
                    val character = remaining.first().toString()
                    append(
                        if (character == "x") letters.getValue("k") + letters.getValue("s")
                        else letters[character] ?: character
                    )
                    remaining = remaining.drop(1)
                }
            }
        }
    }
}
