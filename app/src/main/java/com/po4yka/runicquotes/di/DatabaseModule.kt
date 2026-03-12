package com.po4yka.runicquotes.di

import android.content.Context
import androidx.room.Room
import com.po4yka.runicquotes.data.local.RunicQuotesDatabase
import com.po4yka.runicquotes.data.local.dao.ArchivedQuoteDao
import com.po4yka.runicquotes.data.local.dao.QuoteDao
import com.po4yka.runicquotes.data.local.dao.QuotePackDao
import com.po4yka.runicquotes.data.local.dao.RuneReferenceDao
import com.po4yka.runicquotes.data.local.dao.TranslationBackfillStateDao
import com.po4yka.runicquotes.data.local.dao.TranslationRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Room database and DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    /** Provides the Room database instance. */
    @Provides
    @Singleton
    fun provideRunicQuotesDatabase(
        @ApplicationContext context: Context
    ): RunicQuotesDatabase {
        return Room.databaseBuilder(
            context,
            RunicQuotesDatabase::class.java,
            "runic_quotes.db"
        )
            .addMigrations(
                RunicQuotesDatabase.MIGRATION_1_2,
                RunicQuotesDatabase.MIGRATION_2_3,
                RunicQuotesDatabase.MIGRATION_3_4,
                RunicQuotesDatabase.MIGRATION_4_5,
                RunicQuotesDatabase.MIGRATION_5_6,
                RunicQuotesDatabase.MIGRATION_6_7
            )
            .build()
    }

    /** Provides the [QuoteDao] from the database. */
    @Provides
    @Singleton
    fun provideQuoteDao(database: RunicQuotesDatabase): QuoteDao {
        return database.quoteDao()
    }

    /** Provides the [QuotePackDao] from the database. */
    @Provides
    @Singleton
    fun provideQuotePackDao(database: RunicQuotesDatabase): QuotePackDao {
        return database.quotePackDao()
    }

    /** Provides the [ArchivedQuoteDao] from the database. */
    @Provides
    @Singleton
    fun provideArchivedQuoteDao(database: RunicQuotesDatabase): ArchivedQuoteDao {
        return database.archivedQuoteDao()
    }

    /** Provides the [RuneReferenceDao] from the database. */
    @Provides
    @Singleton
    fun provideRuneReferenceDao(database: RunicQuotesDatabase): RuneReferenceDao {
        return database.runeReferenceDao()
    }

    /** Provides the [TranslationRecordDao] from the database. */
    @Provides
    @Singleton
    fun provideTranslationRecordDao(database: RunicQuotesDatabase): TranslationRecordDao {
        return database.translationRecordDao()
    }

    /** Provides the [TranslationBackfillStateDao] from the database. */
    @Provides
    @Singleton
    fun provideTranslationBackfillStateDao(database: RunicQuotesDatabase): TranslationBackfillStateDao {
        return database.translationBackfillStateDao()
    }
}
