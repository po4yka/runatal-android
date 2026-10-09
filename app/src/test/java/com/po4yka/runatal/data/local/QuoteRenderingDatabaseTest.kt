package com.po4yka.runatal.data.local

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.repository.QuoteRepositoryImpl
import com.po4yka.runatal.data.repository.TranslationRepositoryImpl
import com.po4yka.runatal.data.translation.bundledTranslationTestData
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.QuoteShareContent
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.translation.ElderFutharkTranslationEngine
import com.po4yka.runatal.domain.translation.EreborCirthTranslationEngine
import com.po4yka.runatal.domain.translation.HistoricalTranslationService
import com.po4yka.runatal.domain.translation.TranslationEngineFactory
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.YoungerFutharkTranslationEngine
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.transliteration.YoungerFutharkTransliterator
import com.po4yka.runatal.domain.usecase.quote.BuildQuotePresentationUseCase
import com.po4yka.runatal.domain.usecase.quote.ResolveQuoteRenderingUseCase
import com.po4yka.runatal.util.TimeProvider
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Actual assets, native Room persistence and the same resolver used by all four display surfaces. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class QuoteRenderingDatabaseTest {
    private lateinit var database: RunatalDatabase
    private lateinit var quotes: QuoteRepositoryImpl
    private lateinit var translations: TranslationRepositoryImpl
    private lateinit var resolve: ResolveQuoteRenderingUseCase
    private val factory = TransliterationFactory(
        ElderFutharkTransliterator(), YoungerFutharkTransliterator(), CirthTransliterator()
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), RunatalDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).build()
        val dataset = bundledTranslationTestData()
        val engines = TranslationEngineFactory(
            ElderFutharkTranslationEngine(dataset, dataset, ElderFutharkTransliterator()),
            YoungerFutharkTranslationEngine(dataset, dataset), EreborCirthTranslationEngine(dataset, dataset,
                CirthTransliterator()))
        translations = TranslationRepositoryImpl(database.quoteDao(), database.translationRecordDao(),
            database.translationBackfillCompletionDao(), HistoricalTranslationService(engines), engines)
        quotes = QuoteRepositoryImpl(database.quoteDao(), object : TimeProvider {
            override fun getCurrentDate(): LocalDate = LocalDate.of(2026, 10, 9)
            override fun getCurrentDayOfYear(): Int = 282
        }, mockk())
        resolve = ResolveQuoteRenderingUseCase(factory, translations)
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun `saved historical selection has one resolved output for Today and prepared surfaces`() = runTest {
        val source = Quote(0L, "with king", "Author", null, null, null, isUserCreated = true,
            renderingMode = TranslationMode.TRANSLATE, renderingFidelity = TranslationFidelity.STRICT,
            renderingYoungerVariant = YoungerFutharkVariant.SHORT_TWIG)
        val prepared = translations.translateAndCache(0L, source.textLatin, RunicScript.YOUNGER_FUTHARK,
            source.renderingFidelity, source.renderingYoungerVariant)
        val id = translations.saveUserQuoteWithTranslations(source, listOf(prepared))
        val stored = checkNotNull(quotes.getQuoteById(id))
        assertThat(stored.renderingMode).isEqualTo(TranslationMode.TRANSLATE)
        assertThat(stored.renderingFidelity).isEqualTo(TranslationFidelity.STRICT)
        assertThat(stored.renderingYoungerVariant).isEqualTo(YoungerFutharkVariant.SHORT_TWIG)
        val today = BuildQuotePresentationUseCase(resolve)(stored, RunicScript.YOUNGER_FUTHARK, emptyList())
        val library = resolve(stored, RunicScript.YOUNGER_FUTHARK)
        val share = QuoteShareContent(stored, RunicScript.YOUNGER_FUTHARK, "noto", library)
        assertThat(today.rendering).isEqualTo(library)
        assertThat(share.rendering).isEqualTo(library)
        assertThat(com.po4yka.runatal.ui.widget.widgetQuoteContent(stored, library).runicText)
            .isEqualTo(prepared.glyphOutput)
        assertThat(library.glyphOutput).isEqualTo(prepared.glyphOutput)
        assertThat(library.glyphOutput)
            .isNotEqualTo(factory.transliterate(source.textLatin, RunicScript.YOUNGER_FUTHARK))
        assertThat(library.resolutionStatus).isEqualTo(TranslationResolutionStatus.RECONSTRUCTED)
        assertThat(library.provenance).isNotEmpty()
    }

    @Test
    fun `automatic backfill never changes normal direct rendering or its saved choice`() = runTest {
        database.quoteDao().insert(QuoteEntity(1L, "with king", "Author"))
        val before = checkNotNull(quotes.getQuoteById(1L))
        translations.backfillQuote(before)
        val after = checkNotNull(quotes.getQuoteById(1L))
        assertThat(after).isEqualTo(before)
        assertThat(resolve(after, RunicScript.YOUNGER_FUTHARK).glyphOutput)
            .isEqualTo(factory.transliterate(after.textLatin, RunicScript.YOUNGER_FUTHARK))
        assertThat(resolve(after, RunicScript.YOUNGER_FUTHARK).mode).isEqualTo(TranslationMode.TRANSLITERATE)
    }

    @Test
    fun `source edit preserves historical choice and unavailable output cannot show stored direct glyphs`() = runTest {
        val quote = Quote(0L, "with king", "Author", null, null, null, isUserCreated = true,
            renderingMode = TranslationMode.TRANSLATE, renderingYoungerVariant = YoungerFutharkVariant.SHORT_TWIG)
        val id = quotes.saveUserQuote(quote)
        val current = checkNotNull(quotes.getQuoteById(id))
        val edited = quotes.updateUserQuoteContent(
            current.copy(textLatin = "unsupported xyz123", runicYounger = "manual"), current.textLatin, current.author
        )
        assertThat(edited.renderingMode).isEqualTo(current.renderingMode)
        assertThat(edited.renderingYoungerVariant).isEqualTo(current.renderingYoungerVariant)
        val output = resolve(edited, RunicScript.YOUNGER_FUTHARK)
        assertThat(output.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
        assertThat(output.glyphOutput).isEmpty()
        assertThat(output.label).contains("Unavailable")
    }
}
