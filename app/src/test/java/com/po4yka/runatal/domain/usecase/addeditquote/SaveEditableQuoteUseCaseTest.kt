package com.po4yka.runatal.domain.usecase.addeditquote

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.transliteration.YoungerFutharkTransliterator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SaveEditableQuoteUseCaseTest {

    private val quotes = mockk<QuoteRepository>()
    private val previews = BuildQuotePreviewsUseCase(
        TransliterationFactory(ElderFutharkTransliterator(), YoungerFutharkTransliterator(), CirthTransliterator())
    )
    private val useCase = SaveEditableQuoteUseCase(quotes, previews)

    @Test
    fun `new content derives all previews from trimmed authoritative source`() = runTest {
        val draft = slot<Quote>()
        coEvery { quotes.saveUserQuote(capture(draft)) } returns 7L

        val result = useCase(
            SaveEditableQuoteRequest(0L, " wolf ", " User ", null, 0L, false, "", "")
        )

        assertThat(draft.captured.textLatin).isEqualTo("wolf")
        assertThat(draft.captured.author).isEqualTo("User")
        assertThat(draft.captured.runicElder).isEqualTo("ᚹᛟᛚᚠ")
        assertThat(draft.captured.runicYounger).isEqualTo("ᚢᚢᛚᚠ")
        assertThat(draft.captured.runicCirth).isEqualTo("\uE0AC\uE0B3\uE09E\uE082")
        assertThat(result.savedQuote).isEqualTo(draft.captured.copy(id = 7L))
    }

    @Test
    fun `author edit preserves manual renderings and uses the persisted command snapshot`() = runTest {
        val loaded = Quote(
            id = 7L, textLatin = "wolf", author = "User", runicElder = "manual elder",
            runicYounger = "manual younger", runicCirth = "manual cirth", isUserCreated = true, createdAt = 42L
        )
        val persisted = loaded.copy(author = "Updated author", isFavorite = true)
        val content = slot<Quote>()
        coEvery { quotes.updateUserQuoteContent(capture(content), "wolf", "User") } returns persisted

        val result = useCase(
            SaveEditableQuoteRequest(7L, " wolf ", " Updated author ", loaded, 999L, true, "wolf", loaded.author)
        )

        assertThat(content.captured.runicElder).isEqualTo("manual elder")
        assertThat(content.captured.runicYounger).isEqualTo("manual younger")
        assertThat(content.captured.runicCirth).isEqualTo("manual cirth")
        assertThat(result.savedQuote).isEqualTo(persisted)
        coVerify(exactly = 0) { quotes.saveUserQuote(any()) }
    }

    @Test
    fun `failed content command cannot become a successful partial save`() = runTest {
        val loaded = Quote(
            id = 7L, textLatin = "wolf", author = "User",
            runicElder = null, runicYounger = null, runicCirth = null, isUserCreated = true
        )
        coEvery { quotes.updateUserQuoteContent(any(), "wolf", "User") } throws IllegalStateException("changed")

        val failure = runCatching {
            useCase(SaveEditableQuoteRequest(7L, "king", "User", loaded, 42L, true, "wolf", loaded.author))
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        coVerify(exactly = 0) { quotes.saveUserQuote(any()) }
    }
    @Test
    fun `blank or overbudget quote and author cannot bypass editor validation at the save boundary`() = runTest {
        val valid = SaveEditableQuoteRequest(0L, "I", "User", null, 0L, false, "", "")
        val invalid = listOf(
            valid.copy(textLatin = " "), valid.copy(textLatin = "a".repeat(281)),
            valid.copy(author = " "), valid.copy(author = "a".repeat(61))
        )
        invalid.forEach { request ->
            val failure = runCatching { useCase(request) }.exceptionOrNull()
            assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        }
        coVerify(exactly = 0) { quotes.saveUserQuote(any()) }
        coVerify(exactly = 0) { quotes.updateUserQuoteContent(any(), any(), any()) }
    }
}
