package com.po4yka.runatal.data.seed

import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.domain.transliteration.YoungerFutharkTransliterator

/** Canonical quote identities are independent of database row IDs. */
internal object QuoteSeedData {

    /** Builds canonical rows with database-assigned IDs. */
    fun getCanonicalQuotes(): List<QuoteEntity> {
        val youngerTransliterator = YoungerFutharkTransliterator()
        return getLegacyQuotes().map { quote ->
            quote.copy(id = 0L, runicYounger = youngerTransliterator.transliterate(quote.textLatin))
        }
    }

    /**
     * Frozen legacy seed values used to recognize canonical rows during migrations.
     * New canonical Younger Futhark renderings are generated from the current alphabet.
     */
    fun getLegacyQuotes(): List<QuoteEntity> {
        return listOf(
            QuoteEntity(
                id = 1,
                canonicalKey = "steve-jobs-great-work",
                textLatin = "The only way to do great work is to love what you do.",
                author = "Steve Jobs",
                runicElder = "ᚦᛖ ᛟᚾᛚᚤ ᚹᚨᚤ ᛏᛟ ᛞᛟ ᚷᚱᛖᚨᛏ ᚹᛟᚱᚲ ᛁᛋ ᛏᛟ ᛚᛟᚢᛖ ᚹᚺᚨᛏ ᚤᛟᚢ ᛞᛟ",
                runicYounger = "ᚦᛖ ᛟᚾᛚᛁ ᚹᚨᛁ ᛏᛟ ᚦᛟ ᚲᚱᛖᚨᛏ ᚹᛟᚱᚲ ᛁᛋ ᛏᛟ ᛚᛟᚢᛖ ᚹᚻᚨᛏ ᛁᛟᚢ ᚦᛟ.",
                runicCirth = "\uE088\uE0B4\uE0C9 \uE0CB\uE0B4\uE0A8\uE0C8 \uE0B8\uE0CA\uE0C8 " +
                    "\uE088\uE0CB \uE089\uE0CB \uE091\uE0A0\uE0C9\uE0CA\uE088 " +
                    "\uE0B8\uE0CB\uE0A0\uE090 \uE0C8\uE09C \uE088\uE0CB " +
                    "\uE0A8\uE0CB\uE0CC\uE0C9 \uE0B8\uE0B4\uE0CA\uE088 \uE0C8\uE0CB\uE0CC \uE089\uE0CB."
            ),
            QuoteEntity(
                id = 2,
                canonicalKey = "tolkien-wanderers",
                textLatin = "Not all those who wander are lost.",
                author = "J.R.R. Tolkien",
                runicElder = "ᚾᛟᛏ ᚨᛚᛚ ᚦᛟᛋᛖ ᚹᚺᛟ ᚹᚨᚾᛞᛖᚱ ᚨᚱᛖ ᛚᛟᛋᛏ",
                runicYounger = "ᚾᛟᛏ ᚨᛚᛚ ᚦᛟᛋᛖ ᚹᚻᛟ ᚹᚨᚾᚦᛖᚱ ᚨᚱᛖ ᛚᛟᛋᛏ.",
                runicCirth = "\uE0B4\uE0CB\uE088 \uE0CA\uE0A8\uE0A8 " +
                    "\uE088\uE0B4\uE0CB\uE09C\uE0C9 \uE0B8\uE0B4\uE0CB " +
                    "\uE0B8\uE0CA\uE0B4\uE089\uE0C9\uE0A0 \uE0CA\uE0A0\uE0C9 \uE0A8\uE0CB\uE09C\uE088."
            ),
            QuoteEntity(
                id = 3,
                canonicalKey = "einstein-opportunity",
                textLatin = "In the middle of difficulty lies opportunity.",
                author = "Albert Einstein",
                runicElder = "ᛁᚾ ᚦᛖ ᛗᛁᛞᛞᛚᛖ ᛟᚠ ᛞᛁᚠᚠᛁᚲᚢᛚᛏᚤ ᛚᛁᛖᛋ ᛟᛈᛈᛟᚱᛏᚢᚾᛁᛏᚤ",
                runicYounger = "ᛁᚾ ᚦᛖ ᛗᛁᚦᚦᛚᛖ ᛟᚠ ᚦᛁᚠᚠᛁᚲᚢᛚᛏᛁ ᛚᛁᛖᛋ ᛟᛈᛈᛟᚱᛏᚢᚾᛁᛏᛁ.",
                runicCirth = "\uE0C8\uE0B4 \uE088\uE0B4\uE0C9 \uE0B0\uE0C8\uE089\uE089\uE0A8\uE0C9 \uE0CB\uE082 " +
                    "\uE089\uE0C8\uE082\uE082\uE0C8\uE090\uE0CC\uE0A8\uE088\uE0C8 \uE0A8\uE0C8\uE0C9\uE09C " +
                    "\uE0CB\uE080\uE080\uE0CB\uE0A0\uE088\uE0CC\uE0B4\uE0C8\uE088\uE0C8."
            ),
            QuoteEntity(
                id = 4,
                canonicalKey = "wilde-yourself",
                textLatin = "Be yourself; everyone else is already taken.",
                author = "Oscar Wilde",
                runicElder = "ᛒᛖ ᚤᛟᚢᚱᛋᛖᛚᚠ ᛖᚢᛖᚱᚤᛟᚾᛖ ᛖᛚᛋᛖ ᛁᛋ ᚨᛚᚱᛖᚨᛞᚤ ᛏᚨᚲᛖᚾ",
                runicYounger = "ᛒᛖ ᛁᛟᚢᚱᛋᛖᛚᚠ; ᛖᚢᛖᚱᛁᛟᚾᛖ ᛖᛚᛋᛖ ᛁᛋ ᚨᛚᚱᛖᚨᚦᛁ ᛏᚨᚲᛖᚾ.",
                runicCirth = "\uE081\uE0C9 \uE0C8\uE0CB\uE0CC\uE0A0\uE09C\uE0C9\uE0A8\uE082; " +
                    "\uE0C9\uE0CC\uE0C9\uE0A0\uE0C8\uE0CB\uE0B4\uE0C9 \uE0C9\uE0A8\uE09C\uE0C9 \uE0C8\uE09C " +
                    "\uE0CA\uE0A8\uE0A0\uE0C9\uE0CA\uE089\uE0C8 \uE088\uE0CA\uE090\uE0C9\uE0B4."
            ),
            QuoteEntity(
                id = 5,
                canonicalKey = "lao-tzu-journey",
                textLatin = "The journey of a thousand miles begins with one step.",
                author = "Lao Tzu",
                runicElder = "ᚦᛖ ᛃᛟᚢᚱᚾᛖᚤ ᛟᚠ ᚨ ᚦᛟᚢᛋᚨᚾᛞ ᛗᛁᛚᛖᛋ ᛒᛖᚷᛁᚾᛋ ᚹᛁᚦ ᛟᚾᛖ ᛋᛏᛖᛈ",
                runicYounger = "ᚦᛖ ᛁᛟᚢᚱᚾᛖᛁ ᛟᚠ ᚨ ᚦᛟᚢᛋᚨᚾᚦ ᛗᛁᛚᛖᛋ ᛒᛖᚲᛁᚾᛋ ᚹᛁᚦ ᛟᚾᛖ ᛋᛏᛖᛈ.",
                runicCirth = "\uE088\uE0B4\uE0C9 \uE0BC\uE0CB\uE0CC\uE0A0\uE0B4\uE0C9\uE0C8 " +
                    "\uE0CB\uE082 \uE0CA \uE088\uE0B4\uE0CB\uE0CC\uE09C\uE0CA\uE0B4\uE089 " +
                    "\uE0B0\uE0C8\uE0A8\uE0C9\uE09C \uE081\uE0C9\uE091\uE0C8\uE0B4\uE09C " +
                    "\uE0B8\uE0C8\uE088\uE0B4 \uE0CB\uE0B4\uE0C9 \uE09C\uE088\uE0C9\uE080."
            )
        )
    }

}
