package com.po4yka.runatal.domain.model

import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant

/**
 * Domain model representing a quote.
 * This is separate from the data layer entity (QuoteEntity) to follow
 * clean architecture principles and maintain separation of concerns.
 */
data class Quote(
    val id: Long,
    val textLatin: String,
    val author: String,
    val runicElder: String?,
    val runicYounger: String?,
    val runicCirth: String?,
    val isUserCreated: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = 0L,
    val renderingMode: TranslationMode = TranslationMode.TRANSLITERATE,
    val renderingFidelity: TranslationFidelity = TranslationFidelity.STRICT,
    val renderingYoungerVariant: YoungerFutharkVariant = YoungerFutharkVariant.LONG_BRANCH,
    val lifecycleState: QuoteLifecycleState = QuoteLifecycleState.ACTIVE
)
