package com.po4yka.runatal.di

import android.content.Context
import androidx.room.Room
import com.po4yka.runatal.data.local.RunatalDatabase
import com.po4yka.runatal.data.local.dao.ArchivedQuoteDao
import com.po4yka.runatal.data.local.dao.QuoteDao
import com.po4yka.runatal.data.local.dao.QuotePackDao
import com.po4yka.runatal.data.local.dao.RuneReferenceDao
import com.po4yka.runatal.data.local.dao.TranslationBackfillStateDao
import com.po4yka.runatal.data.local.dao.TranslationRecordDao
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
    fun provideRunatalDatabase(
        @ApplicationContext context: Context
    ): RunatalDatabase {
        return Room.databaseBuilder(
            context,
            RunatalDatabase::class.java,
            "runic_quotes.db"
        )
            .addMigrations(
                RunatalDatabase.MIGRATION_1_2,
                RunatalDatabase.MIGRATION_2_3,
                RunatalDatabase.MIGRATION_3_4,
                RunatalDatabase.MIGRATION_4_5,
                RunatalDatabase.MIGRATION_5_6,
                RunatalDatabase.MIGRATION_6_7
            )
            .build()
    }

    /** Provides the [QuoteDao] from the database. */
    @Provides
    @Singleton
    fun provideQuoteDao(database: RunatalDatabase): QuoteDao {
        return database.quoteDao()
    }

    /** Provides the [QuotePackDao] from the database. */
    @Provides
    @Singleton
    fun provideQuotePackDao(database: RunatalDatabase): QuotePackDao {
        return database.quotePackDao()
    }

    /** Provides the [ArchivedQuoteDao] from the database. */
    @Provides
    @Singleton
    fun provideArchivedQuoteDao(database: RunatalDatabase): ArchivedQuoteDao {
        return database.archivedQuoteDao()
    }

    /** Provides the [RuneReferenceDao] from the database. */
    @Provides
    @Singleton
    fun provideRuneReferenceDao(database: RunatalDatabase): RuneReferenceDao {
        return database.runeReferenceDao()
    }

    /** Provides the [TranslationRecordDao] from the database. */
    @Provides
    @Singleton
    fun provideTranslationRecordDao(database: RunatalDatabase): TranslationRecordDao {
        return database.translationRecordDao()
    }

    /** Provides the [TranslationBackfillStateDao] from the database. */
    @Provides
    @Singleton
    fun provideTranslationBackfillStateDao(database: RunatalDatabase): TranslationBackfillStateDao {
        return database.translationBackfillStateDao()
    }
}
