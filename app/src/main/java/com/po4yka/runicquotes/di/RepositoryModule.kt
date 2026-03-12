package com.po4yka.runicquotes.di

import com.po4yka.runicquotes.data.translation.AssetTranslationDatasetProvider
import com.po4yka.runicquotes.data.repository.ArchiveRepository
import com.po4yka.runicquotes.data.repository.ArchiveRepositoryImpl
import com.po4yka.runicquotes.data.repository.QuotePackRepository
import com.po4yka.runicquotes.data.repository.QuotePackRepositoryImpl
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.data.repository.QuoteRepositoryImpl
import com.po4yka.runicquotes.data.repository.RuneReferenceRepository
import com.po4yka.runicquotes.data.repository.RuneReferenceRepositoryImpl
import com.po4yka.runicquotes.data.repository.TranslationRepository
import com.po4yka.runicquotes.data.repository.TranslationRepositoryImpl
import com.po4yka.runicquotes.domain.translation.EreborOrthographyStore
import com.po4yka.runicquotes.domain.translation.HistoricalLexiconStore
import com.po4yka.runicquotes.domain.translation.RunicCorpusStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    /** Binds [QuoteRepositoryImpl] as the [QuoteRepository] implementation. */
    @Binds
    @Singleton
    abstract fun bindQuoteRepository(
        impl: QuoteRepositoryImpl
    ): QuoteRepository

    /** Binds [QuotePackRepositoryImpl] as the [QuotePackRepository] implementation. */
    @Binds
    @Singleton
    abstract fun bindQuotePackRepository(
        impl: QuotePackRepositoryImpl
    ): QuotePackRepository

    /** Binds [ArchiveRepositoryImpl] as the [ArchiveRepository] implementation. */
    @Binds
    @Singleton
    abstract fun bindArchiveRepository(
        impl: ArchiveRepositoryImpl
    ): ArchiveRepository

    /** Binds [RuneReferenceRepositoryImpl] as the [RuneReferenceRepository] implementation. */
    @Binds
    @Singleton
    abstract fun bindRuneReferenceRepository(
        impl: RuneReferenceRepositoryImpl
    ): RuneReferenceRepository

    /** Binds [TranslationRepositoryImpl] as the [TranslationRepository] implementation. */
    @Binds
    @Singleton
    abstract fun bindTranslationRepository(
        impl: TranslationRepositoryImpl
    ): TranslationRepository

    /** Binds the asset-backed lexical dataset store for historical translation. */
    @Binds
    @Singleton
    abstract fun bindHistoricalLexiconStore(
        impl: AssetTranslationDatasetProvider
    ): HistoricalLexiconStore

    /** Binds the asset-backed phrase and corpus store for runic translation. */
    @Binds
    @Singleton
    abstract fun bindRunicCorpusStore(
        impl: AssetTranslationDatasetProvider
    ): RunicCorpusStore

    /** Binds the asset-backed Erebor orthography store. */
    @Binds
    @Singleton
    abstract fun bindEreborOrthographyStore(
        impl: AssetTranslationDatasetProvider
    ): EreborOrthographyStore
}
