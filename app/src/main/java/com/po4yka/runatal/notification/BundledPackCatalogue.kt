package com.po4yka.runatal.notification

import com.po4yka.runatal.data.seed.QuotePackSeedData
import java.security.MessageDigest
import javax.inject.Inject

/** Fingerprints only actual bundled content; installing or removing a pack cannot generate an update event. */
internal class BundledPackCatalogue @Inject constructor() {
    fun fingerprints(): Map<String, String> = QuotePackSeedData.getInitialPacks().associate { pack ->
        val content = buildString {
            append(pack.name).append('\u0000').append(pack.description).append('\u0000').append(pack.coverRune)
            QuotePackSeedData.getPackQuotes(pack.id).forEach { quote ->
                append('\u0000').append(quote.textLatin).append('\u0000').append(quote.author)
            }
        }
        pack.id.toString() to MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    fun changedPackNames(previous: Map<String, String>, current: Map<String, String>): List<String> =
        QuotePackSeedData.getInitialPacks().filter { previous[it.id.toString()] != current[it.id.toString()] }
            .map { it.name }
}
