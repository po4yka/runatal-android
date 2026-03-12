package com.po4yka.runicquotes.domain.usecase.addeditquote

import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.data.repository.TranslationRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.getRunicText
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import javax.inject.Inject

internal data class QuotePreviewSet(
    val elder: String,
    val younger: String,
    val cirth: String
)

internal data class LoadedEditableQuote(
    val quote: Quote,
    val previews: QuotePreviewSet
)

internal data class QuoteDraftEvaluation(
    val quoteTextError: String?,
    val authorError: String?,
    val quoteCharCount: Int,
    val authorCharCount: Int,
    val hasUnsavedChanges: Boolean,
    val canSave: Boolean
)

internal data class SaveEditableQuoteRequest(
    val quoteId: Long,
    val textLatin: String,
    val author: String,
    val previews: QuotePreviewSet,
    val existingQuote: Quote?,
    val createdAtMillis: Long,
    val isEditing: Boolean,
    val initialTextLatin: String
)

internal data class SaveEditableQuoteResult(
    val savedQuote: Quote,
    val translationInvalidationError: Throwable? = null
)

internal class AddEditQuoteEditorInteractors @Inject constructor(
    val loadEditableQuoteUseCase: LoadEditableQuoteUseCase,
    val buildQuotePreviewsUseCase: BuildQuotePreviewsUseCase,
    val evaluateQuoteDraftUseCase: EvaluateQuoteDraftUseCase,
    val saveEditableQuoteUseCase: SaveEditableQuoteUseCase
)

internal class BuildQuotePreviewsUseCase @Inject constructor(
    private val transliterationFactory: TransliterationFactory
) {

    operator fun invoke(
        text: String,
        preservedQuote: Quote? = null
    ): QuotePreviewSet {
        val preserved = preservedQuote?.takeIf { it.textLatin == text }
        return QuotePreviewSet(
            elder = preserved?.getRunicText(RunicScript.ELDER_FUTHARK, transliterationFactory)
                ?: transliterationFactory.transliterate(text, RunicScript.ELDER_FUTHARK),
            younger = preserved?.getRunicText(RunicScript.YOUNGER_FUTHARK, transliterationFactory)
                ?: transliterationFactory.transliterate(text, RunicScript.YOUNGER_FUTHARK),
            cirth = preserved?.getRunicText(RunicScript.CIRTH, transliterationFactory)
                ?: transliterationFactory.transliterate(text, RunicScript.CIRTH)
        )
    }
}

internal class LoadEditableQuoteUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val buildQuotePreviewsUseCase: BuildQuotePreviewsUseCase
) {

    suspend operator fun invoke(quoteId: Long): LoadedEditableQuote? {
        val quote = quoteRepository.getQuoteById(quoteId)?.takeIf { it.isUserCreated } ?: return null
        return LoadedEditableQuote(
            quote = quote,
            previews = buildQuotePreviewsUseCase(quote.textLatin, quote)
        )
    }
}

internal class EvaluateQuoteDraftUseCase @Inject constructor() {

    operator fun invoke(
        textLatin: String,
        author: String,
        isEditing: Boolean,
        initialTextLatin: String,
        initialAuthor: String,
        hasAttemptedSave: Boolean
    ): QuoteDraftEvaluation {
        val rawQuoteTextError = when {
            textLatin.trim().length < MIN_QUOTE_LENGTH -> {
                "Quote must be at least $MIN_QUOTE_LENGTH characters"
            }

            textLatin.length > MAX_QUOTE_LENGTH -> "Keep quote under $MAX_QUOTE_LENGTH characters"
            else -> null
        }

        val rawAuthorError = when {
            author.isBlank() -> "Author is required"
            author.length > MAX_AUTHOR_LENGTH -> "Keep author under $MAX_AUTHOR_LENGTH characters"
            else -> null
        }

        val quoteTextError = rawQuoteTextError?.takeIf { hasAttemptedSave || textLatin.isNotBlank() }
        val authorError = rawAuthorError?.takeIf { hasAttemptedSave || author.isNotBlank() }

        val hasUnsavedChanges = if (isEditing) {
            textLatin.trim() != initialTextLatin.trim() || author.trim() != initialAuthor.trim()
        } else {
            textLatin.isNotBlank() || author.isNotBlank()
        }

        return QuoteDraftEvaluation(
            quoteTextError = quoteTextError,
            authorError = authorError,
            quoteCharCount = textLatin.length,
            authorCharCount = author.length,
            hasUnsavedChanges = hasUnsavedChanges,
            canSave = rawQuoteTextError == null && rawAuthorError == null && hasUnsavedChanges
        )
    }

    private companion object {
        const val MAX_QUOTE_LENGTH = 280
        const val MAX_AUTHOR_LENGTH = 60
        const val MIN_QUOTE_LENGTH = 3
    }
}

internal class SaveEditableQuoteUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val translationRepository: TranslationRepository
) {

    suspend operator fun invoke(request: SaveEditableQuoteRequest): SaveEditableQuoteResult {
        val trimmedText = request.textLatin.trim()
        val trimmedAuthor = request.author.trim()
        val createdAt = if (request.isEditing && request.createdAtMillis != 0L) {
            request.createdAtMillis
        } else {
            System.currentTimeMillis()
        }

        val quote = Quote(
            id = request.quoteId,
            textLatin = trimmedText,
            author = trimmedAuthor,
            runicElder = request.previews.elder,
            runicYounger = request.previews.younger,
            runicCirth = request.previews.cirth,
            isUserCreated = true,
            isFavorite = request.existingQuote?.isFavorite ?: false,
            createdAt = createdAt
        )

        val savedQuoteId = quoteRepository.saveUserQuote(quote)
        val invalidationError = if (request.isEditing && trimmedText != request.initialTextLatin.trim()) {
            runCatching { translationRepository.deleteTranslationsForQuote(savedQuoteId) }.exceptionOrNull()
        } else {
            null
        }

        return SaveEditableQuoteResult(
            savedQuote = quote.copy(id = savedQuoteId, createdAt = createdAt),
            translationInvalidationError = invalidationError
        )
    }
}
