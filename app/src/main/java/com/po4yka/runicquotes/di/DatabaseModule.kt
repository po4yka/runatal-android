package com.po4yka.runicquotes.di

import android.content.Context
import androidx.room.Room
import com.po4yka.runicquotes.data.local.RunicQuotesDatabase
import com.po4yka.runicquotes.data.local.dao.QuoteDao
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
object DatabaseModule {

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
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideQuoteDao(database: RunicQuotesDatabase): QuoteDao {
        return database.quoteDao()
    }
}
