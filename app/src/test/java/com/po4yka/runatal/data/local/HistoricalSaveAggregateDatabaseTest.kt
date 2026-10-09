package com.po4yka.runatal.data.local

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.repository.QuoteRepositoryImpl
import com.po4yka.runatal.data.repository.TranslationRepositoryImpl
import com.po4yka.runatal.data.translation.bundledTranslationTestData
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
import com.po4yka.runatal.domain.usecase.translation.BuildHistoricalTranslationBundleUseCase
import com.po4yka.runatal.domain.usecase.translation.BuildTransliterationBundleUseCase
import com.po4yka.runatal.domain.usecase.translation.SaveTranslationRequest
import com.po4yka.runatal.domain.usecase.translation.SaveTranslationToLibraryUseCase
import com.po4yka.runatal.util.TimeProvider
import java.time.LocalDate
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Exercises complete library saves with actual bundled engines and native Room transactions. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class HistoricalSaveAggregateDatabaseTest {

    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "historical-save-aggregate.db"
    private lateinit var database: RunatalDatabase
    private lateinit var translations: TranslationRepositoryImpl
    private lateinit var service: HistoricalTranslationService
    private lateinit var save: SaveTranslationToLibraryUseCase
    private val direct = TransliterationFactory(
        ElderFutharkTransliterator(), YoungerFutharkTransliterator(), CirthTransliterator()
    )
    private val clock = object : TimeProvider {
        override fun getCurrentDate(): LocalDate = LocalDate.of(2026, 10, 9)
        override fun getCurrentDayOfYear(): Int = getCurrentDate().dayOfYear
    }

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
        database = Room.databaseBuilder(context, RunatalDatabase::class.java, databaseName)
            .setDriver(AndroidSQLiteDriver()).build()
        val dataset = bundledTranslationTestData()
        val engines = TranslationEngineFactory(
            ElderFutharkTranslationEngine(dataset, dataset, ElderFutharkTransliterator()),
            YoungerFutharkTranslationEngine(dataset, dataset),
            EreborCirthTranslationEngine(dataset, dataset, CirthTransliterator())
        )
        service = HistoricalTranslationService(engines)
        translations = TranslationRepositoryImpl(
            database.quoteDao(), database.translationRecordDao(), database.translationBackfillCompletionDao(),
            service, engines
        )
        save = SaveTranslationToLibraryUseCase(
            QuoteRepositoryImpl(database.quoteDao(), clock), translations,
            BuildTransliterationBundleUseCase(direct), BuildHistoricalTranslationBundleUseCase(service)
        )
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `failure after the first historical row rolls back and retry creates one complete aggregate`() = runTest {
        // Open the generated schema before installing a real SQLite failure at the second cache insertion.
        assertThat(database.quoteDao().getCount()).isEqualTo(0)
        executeSql(
            "CREATE TRIGGER fail_second_record BEFORE INSERT ON translation_records " +
                "WHEN NEW.script = 'YOUNGER_FUTHARK' BEGIN SELECT RAISE(ABORT, 'forced cache write failure'); END"
        )

        val failure = runCatching { save(request()) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(failure?.cause).isInstanceOf(SQLiteException::class.java)
        assertThat(database.quoteDao().getCount()).isEqualTo(0)
        assertThat(cacheCount()).isEqualTo(0)

        executeSql("DROP TRIGGER fail_second_record")
        val outcome = save(request())

        assertThat(outcome.message).isEqualTo("Saved translation to library")
        assertThat(database.quoteDao().getCount()).isEqualTo(1)
        assertCompleteAggregate()
    }

    @Test
    fun `one supported selected script saves without blank stored glyphs for unavailable other scripts`() = runTest {
        save(request("I hunt", TranslationFidelity.STRICT))

        val quote = database.quoteDao().getAll().single()
        assertThat(quote.textLatin).isEqualTo("I hunt")
        assertThat(quote.runicElder).isEqualTo(direct.transliterate("I hunt", RunicScript.ELDER_FUTHARK))
        assertThat(quote.runicYounger).isEqualTo(direct.transliterate("I hunt", RunicScript.YOUNGER_FUTHARK))
        assertThat(quote.runicCirth).isEqualTo(direct.transliterate("I hunt", RunicScript.CIRTH))
        val selected = translations.getCachedTranslation(
            quoteId = quote.id, script = RunicScript.YOUNGER_FUTHARK, sourceText = quote.textLatin,
            fidelity = TranslationFidelity.STRICT, youngerVariant = YoungerFutharkVariant.SHORT_TWIG
        )
        assertThat(selected?.normalizedForm).isEqualTo("ek veiði")
        assertThat(selected?.requestedVariant).isEqualTo("SHORT_TWIG")
    }

    private suspend fun assertCompleteAggregate() {
        val quote = database.quoteDao().getAll().single()
        val text = request().inputText.trim()
        assertThat(quote.textLatin).isEqualTo(text)
        assertThat(quote.author).isEqualTo("Runatal")
        assertThat(quote.isUserCreated).isTrue()
        assertThat(quote.runicYounger).isEqualTo(direct.transliterate(text, RunicScript.YOUNGER_FUTHARK))
        val expected = RunicScript.entries.map { script ->
            service.translate(text, script, request().fidelity, YoungerFutharkVariant.SHORT_TWIG)
        }.filter { it.resolutionStatus != TranslationResolutionStatus.UNAVAILABLE }
        assertThat(expected).hasSize(3)
        assertThat(cacheCount()).isEqualTo(expected.size)
        expected.forEach { result ->
            val cached = translations.getCachedTranslation(
                quoteId = quote.id, script = result.script, sourceText = text,
                fidelity = request().fidelity, youngerVariant = YoungerFutharkVariant.SHORT_TWIG
            )
            assertThat(cached?.sourceText).isEqualTo(text)
            assertThat(cached?.glyphOutput).isEqualTo(result.glyphOutput)
            assertThat(cached?.normalizedForm).isEqualTo(result.normalizedForm)
            assertThat(cached?.requestedVariant).isEqualTo(result.requestedVariant)
        }
    }

    private fun request(
        text: String = "The wolf hunts at night",
        fidelity: TranslationFidelity = TranslationFidelity.READABLE
    ) = SaveTranslationRequest(
        inputText = " $text ", translationMode = TranslationMode.TRANSLATE,
        selectedScript = RunicScript.YOUNGER_FUTHARK, fidelity = fidelity,
        youngerVariant = YoungerFutharkVariant.SHORT_TWIG
    )

    private fun executeSql(sql: String) {
        AndroidSQLiteDriver().open(context.getDatabasePath(databaseName).absolutePath).use { it.execSQL(sql) }
    }

    private fun cacheCount(): Int = AndroidSQLiteDriver().open(
        context.getDatabasePath(databaseName).absolutePath
    ).use { connection ->
        connection.prepare("SELECT COUNT(*) FROM translation_records").use { statement ->
            check(statement.step())
            statement.getLong(0).toInt()
        }
    }
}
