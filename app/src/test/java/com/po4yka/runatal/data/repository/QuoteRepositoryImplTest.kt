package com.po4yka.runatal.data.repository

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.dao.QuoteDao
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.util.TimeProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Comprehensive unit tests for QuoteRepositoryImpl.
 * Uses MockK to mock the DAO layer and verify repository business logic.
 * Uses FakeTimeProvider to make tests deterministic and not dependent on system time.
 *
 * Coverage goals: >90%
 */
class QuoteRepositoryImplTest {

    private lateinit var quoteDao: QuoteDao
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var preferences: UserPreferencesManager
    private lateinit var repository: QuoteRepositoryImpl

    /**
     * Fake TimeProvider for testing that allows setting a specific day of year.
     */
    private class FakeTimeProvider(private var dayOfYear: Int = 1) : TimeProvider {
        fun setDayOfYear(day: Int) {
            dayOfYear = day
        }

        override fun getCurrentDayOfYear(): Int = dayOfYear

        override fun getCurrentDate(): LocalDate = LocalDate.ofYearDay(2024, dayOfYear)
    }

    private val testQuotes = listOf(
        QuoteEntity(
            id = 1,
            textLatin = "Test quote 1",
            author = "Author 1",
            runicElder = "\u16CF\u16D6\u16CA\u16CF",
            runicYounger = "\u16CF\u16D6\u16CA\u16CF",
            runicCirth = "\uE088\uE0C9\uE09C\uE088"
        ),
        QuoteEntity(
            id = 2,
            textLatin = "Test quote 2",
            author = "Author 2",
            runicElder = "\u16A6\u16D6\u16CA\u16CF",
            runicYounger = null,
            runicCirth = null
        ),
        QuoteEntity(
            id = 3,
            textLatin = "Test quote 3",
            author = "Author 3",
            runicElder = "\u16B9\u16DF\u16B1\u16DE",
            runicYounger = null,
            runicCirth = null
        )
    )

    @Before
    fun setUp() {
        quoteDao = mockk()
        timeProvider = FakeTimeProvider(dayOfYear = 1)
        preferences = mockk()
        coEvery { preferences.selectDailyQuote(any(), any()) } coAnswers {
            val ids = secondArg<suspend () -> List<Long>>().invoke()
            ids.takeIf { it.isNotEmpty() }?.get(Math.floorMod(firstArg<Long>(), ids.size.toLong()).toInt())
        }
        repository = QuoteRepositoryImpl(quoteDao, timeProvider, preferences)
        coEvery { quoteDao.seedCanonicalQuotes(any()) } returns Unit
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ==================== Seed Logic Tests ====================

    @Test
    fun `seedIfNeeded delegates canonical identities with database assigned ids`() = runTest {
        repository.seedIfNeeded()

        coVerify {
            quoteDao.seedCanonicalQuotes(match { quotes ->
                quotes.size == 5 &&
                    quotes.all { it.id == 0L && !it.isUserCreated && it.canonicalKey != null } &&
                    quotes.map { it.canonicalKey }.distinct().size == quotes.size
            })
        }
        coVerify(exactly = 0) { quoteDao.insertAll(any()) }
    }

    @Test
    fun `seedIfNeeded does not use total quote count to skip canonical quotes`() = runTest {
        repository.seedIfNeeded()

        coVerify(exactly = 1) { quoteDao.seedCanonicalQuotes(any()) }
        coVerify(exactly = 0) { quoteDao.getCount() }
    }

    @Test
    fun `seedIfNeeded delegates repeated calls to persisted transactional identity check`() = runTest {
        repeat(3) { repository.seedIfNeeded() }

        coVerify(exactly = 3) { quoteDao.seedCanonicalQuotes(any()) }
    }

    // ==================== Quote of the Day Tests ====================

    @Test
    fun `quoteOfTheDay returns consistent quote for same day`() = runTest {
        // Given: Database with quotes
        coEvery { quoteDao.getCount() } returns 3
        stubQuotes(testQuotes)

        // When: Getting quote of the day multiple times on same day
        val quote1 = repository.quoteOfTheDay()
        val quote2 = repository.quoteOfTheDay()

        // Then: Same quote is returned
        assertThat(quote2?.id).isEqualTo(quote1?.id)
        assertThat(quote2?.textLatin).isEqualTo(quote1?.textLatin)
    }

    @Test
    fun `quoteOfTheDay delegates the local epoch day and stable candidate identities`() = runTest {
        // Given: Database with 3 quotes and day of year set to 5
        timeProvider.setDayOfYear(5)
        coEvery { quoteDao.getCount() } returns 3
        stubQuotes(testQuotes)

        // When: Getting quote of the day
        val quote = repository.quoteOfTheDay()

        // Then: Repository resolves the identity selected for the complete local epoch day
        val expectedIndex = Math.floorMod(timeProvider.getCurrentDate().toEpochDay(), 3L).toInt()
        assertThat(quote?.id).isEqualTo(testQuotes[expectedIndex].id)
        coVerify { preferences.selectDailyQuote(LocalDate.ofYearDay(2024, 5).toEpochDay(), any()) }
    }

    @Test
    fun `quoteOfTheDay returns null when no quotes available`() = runTest {
        // Given: Empty database (even after seeding attempt)
        coEvery { quoteDao.getCount() } returns 0
        coEvery { quoteDao.seedCanonicalQuotes(any()) } returns Unit
        stubQuotes(emptyList())

        // When: Getting quote of the day
        val quote = repository.quoteOfTheDay()

        // Then: Returns null
        assertThat(quote).isNull()
    }

    @Test
    fun `quoteOfTheDay seeds database if needed`() = runTest {
        // Given: Initially empty database, then has quotes after seeding
        coEvery { quoteDao.getCount() } returns 0 andThen 5
        coEvery { quoteDao.seedCanonicalQuotes(any()) } returns Unit
        stubQuotes(testQuotes)

        // When: Getting quote of the day
        val quote = repository.quoteOfTheDay()

        // Then: Seeding happened and quote returned
        coVerify { quoteDao.seedCanonicalQuotes(any()) }
        assertThat(quote).isNotNull()
    }

    @Test
    fun `quoteOfTheDay maps entity to domain model correctly`() = runTest {
        // Given: Database with quotes and day of year set to 7
        timeProvider.setDayOfYear(7)
        coEvery { quoteDao.getCount() } returns 3
        stubQuotes(testQuotes)

        // When: Getting quote of the day
        val quote = repository.quoteOfTheDay()

        // Then: Domain model properties match the selected persisted entity
        assertThat(quote).isNotNull()
        val expectedIndex = Math.floorMod(timeProvider.getCurrentDate().toEpochDay(), 3L).toInt()
        val expectedEntity = testQuotes[expectedIndex]

        assertThat(quote?.id).isEqualTo(expectedEntity.id)
        assertThat(quote?.textLatin).isEqualTo(expectedEntity.textLatin)
        assertThat(quote?.author).isEqualTo(expectedEntity.author)
        assertThat(quote?.runicElder).isEqualTo(expectedEntity.runicElder)
        assertThat(quote?.runicYounger).isEqualTo(expectedEntity.runicYounger)
        assertThat(quote?.runicCirth).isEqualTo(expectedEntity.runicCirth)
    }

    // ==================== Random Quote Tests ====================

    @Test
    fun `randomQuote returns quote from DAO`() = runTest {
        // Given: DAO returns a random quote
        coEvery { quoteDao.getCount() } returns 3
        coEvery { quoteDao.getRandom() } returns testQuotes[1]

        // When: Getting random quote
        val quote = repository.randomQuote()

        // Then: Quote is returned
        assertThat(quote).isNotNull()
        assertThat(quote?.id).isEqualTo(testQuotes[1].id)
        assertThat(quote?.textLatin).isEqualTo(testQuotes[1].textLatin)
    }

    @Test
    fun `randomQuote returns null when DAO returns null`() = runTest {
        // Given: DAO returns null (even after seeding attempt)
        coEvery { quoteDao.getCount() } returns 0
        coEvery { quoteDao.seedCanonicalQuotes(any()) } returns Unit
        coEvery { quoteDao.getRandom() } returns null

        // When: Getting random quote
        val quote = repository.randomQuote()

        // Then: Returns null
        assertThat(quote).isNull()
    }

    @Test
    fun `randomQuote seeds if needed`() = runTest {
        // Given: Initially empty database
        coEvery { quoteDao.getCount() } returns 0 andThen 5
        coEvery { quoteDao.seedCanonicalQuotes(any()) } returns Unit
        coEvery { quoteDao.getRandom() } returns testQuotes[0]

        // When: Getting random quote
        val quote = repository.randomQuote()

        // Then: Seeding happened
        coVerify { quoteDao.seedCanonicalQuotes(any()) }
        assertThat(quote).isNotNull()
    }

    // ==================== Get All Quotes Flow Tests ====================

    @Test
    fun `getAllQuotesFlow maps entities to domain models`() = runTest {
        // Given: DAO returns flow of entities
        every { quoteDao.getAllAsFlow() } returns flowOf(testQuotes)

        // When: Getting all quotes as flow
        val quotes = repository.getAllQuotesFlow().first()

        // Then: Domain models are returned
        assertThat(quotes).hasSize(testQuotes.size)
        assertThat(quotes[0].id).isEqualTo(testQuotes[0].id)
        assertThat(quotes[0].textLatin).isEqualTo(testQuotes[0].textLatin)
        assertThat(quotes[1].id).isEqualTo(testQuotes[1].id)
        assertThat(quotes[2].id).isEqualTo(testQuotes[2].id)
    }

    @Test
    fun `getAllQuotesFlow returns empty list when DAO returns empty`() = runTest {
        // Given: DAO returns empty flow
        every { quoteDao.getAllAsFlow() } returns flowOf(emptyList())

        // When: Getting all quotes as flow
        val quotes = repository.getAllQuotesFlow().first()

        // Then: Empty list returned
        assertThat(quotes).isEmpty()
    }

    @Test
    fun `getAllQuotesFlow preserves all entity fields in domain model`() = runTest {
        // Given: DAO returns flow with quote
        val quote = testQuotes[0]
        every { quoteDao.getAllAsFlow() } returns flowOf(listOf(quote))

        // When: Getting all quotes as flow
        val quotes = repository.getAllQuotesFlow().first()

        // Then: All fields are mapped correctly
        val domainQuote = quotes[0]
        assertThat(domainQuote.id).isEqualTo(quote.id)
        assertThat(domainQuote.textLatin).isEqualTo(quote.textLatin)
        assertThat(domainQuote.author).isEqualTo(quote.author)
        assertThat(domainQuote.runicElder).isEqualTo(quote.runicElder)
        assertThat(domainQuote.runicYounger).isEqualTo(quote.runicYounger)
        assertThat(domainQuote.runicCirth).isEqualTo(quote.runicCirth)
    }

    // ==================== Get All Quotes Tests ====================

    @Test
    fun `getAllQuotes returns all quotes from DAO`() = runTest {
        // Given: DAO returns list of quotes
        stubQuotes(testQuotes)

        // When: Getting all quotes
        val quotes = repository.getAllQuotes()

        // Then: All quotes returned as domain models
        assertThat(quotes).hasSize(testQuotes.size)
        assertThat(quotes[0].id).isEqualTo(testQuotes[0].id)
        assertThat(quotes[1].id).isEqualTo(testQuotes[1].id)
        assertThat(quotes[2].id).isEqualTo(testQuotes[2].id)
    }

    @Test
    fun `getAllQuotes returns empty list when no quotes`() = runTest {
        // Given: DAO returns empty list
        stubQuotes(emptyList())

        // When: Getting all quotes
        val quotes = repository.getAllQuotes()

        // Then: Empty list returned
        assertThat(quotes).isEmpty()
    }

    // ==================== Get Quote Count Tests ====================

    @Test
    fun `getQuoteCount returns count from DAO`() = runTest {
        // Given: DAO returns count
        coEvery { quoteDao.getCount() } returns 42

        // When: Getting quote count
        val count = repository.getQuoteCount()

        // Then: Correct count returned
        assertThat(count).isEqualTo(42)
    }

    @Test
    fun `getQuoteCount returns zero when database is empty`() = runTest {
        // Given: DAO returns zero
        coEvery { quoteDao.getCount() } returns 0

        // When: Getting quote count
        val count = repository.getQuoteCount()

        // Then: Zero returned
        assertThat(count).isEqualTo(0)
    }

    // ==================== Domain Mapping Tests ====================

    @Test
    fun `entity to domain mapping handles null runic fields`() = runTest {
        // Given: Quote with null runic fields
        val entity = QuoteEntity(
            id = 99,
            textLatin = "Test",
            author = "Author",
            runicElder = null,
            runicYounger = null,
            runicCirth = null
        )
        coEvery { quoteDao.getCount() } returns 1
        coEvery { quoteDao.getRandom() } returns entity

        // When: Getting random quote
        val quote = repository.randomQuote()

        // Then: Null fields are preserved
        assertThat(quote).isNotNull()
        assertThat(quote?.id).isEqualTo(99L)
        assertThat(quote?.runicElder).isNull()
        assertThat(quote?.runicYounger).isNull()
        assertThat(quote?.runicCirth).isNull()
    }

    @Test
    fun `entity to domain mapping handles all non-null runic fields`() = runTest {
        // Given: Quote with all runic fields populated
        val entity = QuoteEntity(
            id = 100,
            textLatin = "Complete",
            author = "Author",
            runicElder = "\u16D6\u16DA\u16DE\u16D6\u16B1",
            runicYounger = "\u16C1\u16DF\u16A2\u16BE\u16D6\u16B1",
            runicCirth = "\uE0C9\uE0C8\uE0A0\uE088"
        )
        coEvery { quoteDao.getCount() } returns 1
        coEvery { quoteDao.getRandom() } returns entity

        // When: Getting random quote
        val quote = repository.randomQuote()

        // Then: All fields are mapped
        assertThat(quote).isNotNull()
        assertThat(quote?.runicElder).isEqualTo("\u16D6\u16DA\u16DE\u16D6\u16B1")
        assertThat(quote?.runicYounger).isEqualTo("\u16C1\u16DF\u16A2\u16BE\u16D6\u16B1")
        assertThat(quote?.runicCirth).isEqualTo("\uE0C9\uE0C8\uE0A0\uE088")
    }

    // ==================== Consistency Tests ====================

    @Test
    fun `quoteOfTheDay returns same quote on repeated calls for same day`() = runTest {
        // Given: Database with quotes
        coEvery { quoteDao.getCount() } returns 3
        stubQuotes(testQuotes)

        // When: Getting quote multiple times
        val quote1 = repository.quoteOfTheDay()
        val quote2 = repository.quoteOfTheDay()
        val quote3 = repository.quoteOfTheDay()

        // Then: Same quote returned each time
        assertThat(quote2?.id).isEqualTo(quote1?.id)
        assertThat(quote3?.id).isEqualTo(quote1?.id)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `handles database with single quote`() = runTest {
        // Given: Database with one quote
        val singleQuote = listOf(testQuotes[0])
        coEvery { quoteDao.getCount() } returns 1
        stubQuotes(singleQuote)

        // When: Getting quote of the day
        val quote = repository.quoteOfTheDay()

        // Then: That one quote is returned
        assertThat(quote).isNotNull()
        assertThat(quote?.id).isEqualTo(singleQuote[0].id)
    }

    @Test
    fun `handles large number of quotes`() = runTest {
        // Given: Database with many quotes
        val manyQuotes = (1..1000).map {
            QuoteEntity(
                id = it.toLong(),
                textLatin = "Quote $it",
                author = "Author $it",
                runicElder = "\u16B1\u16A2\u16BE\u16D6",
                runicYounger = null,
                runicCirth = null
            )
        }
        coEvery { quoteDao.getCount() } returns 1000
        stubQuotes(manyQuotes)

        // When: Getting quote of the day
        val quote = repository.quoteOfTheDay()

        // Then: Valid quote is returned
        assertThat(quote).isNotNull()
        assertThat(quote!!.id).isIn(1L..1000L)
    }

    @Test
    fun `epoch day selection works correctly for year boundary`() = runTest {
        // Given: 3 quotes in database and day of year set to 365 (end of year)
        timeProvider.setDayOfYear(365)
        coEvery { quoteDao.getCount() } returns 3
        stubQuotes(testQuotes)

        // When: Getting quote using the complete local epoch day
        val quote = repository.quoteOfTheDay()

        // Then: Selected ID maps to an existing row across the year boundary
        assertThat(quote).isNotNull()
        assertThat(quote?.id).isEqualTo(testQuotes[Math.floorMod(timeProvider.getCurrentDate().toEpochDay(), 3L).toInt()].id)
    }

    @Test
    fun `different days return different quotes predictably`() = runTest {
        // Given: Database with 3 quotes
        coEvery { quoteDao.getCount() } returns 3
        stubQuotes(testQuotes)

        // When: Getting quotes for different days
        timeProvider.setDayOfYear(1)
        val quote1 = repository.quoteOfTheDay()

        timeProvider.setDayOfYear(2)
        val quote2 = repository.quoteOfTheDay()

        timeProvider.setDayOfYear(3)
        val quote3 = repository.quoteOfTheDay()

        // Then: Different epoch days are passed to the daily identity selector
        assertThat(quote1?.id).isEqualTo(testQuotes[1].id)
        assertThat(quote2?.id).isEqualTo(testQuotes[2].id)
        assertThat(quote3?.id).isEqualTo(testQuotes[0].id)
    }

    @Test
    fun `same local day returns the selected quote identity`() = runTest {
        // Given: Database with 3 quotes and day set to 10
        timeProvider.setDayOfYear(10)
        coEvery { quoteDao.getCount() } returns 3
        stubQuotes(testQuotes)

        // When: Getting quote multiple times on same day
        val quote1 = repository.quoteOfTheDay()
        val quote2 = repository.quoteOfTheDay()
        val quote3 = repository.quoteOfTheDay()

        // Then: All resolve the same selected identity
        assertThat(quote1?.id).isEqualTo(testQuotes[1].id)
        assertThat(quote2?.id).isEqualTo(quote1?.id)
        assertThat(quote3?.id).isEqualTo(quote1?.id)
    }

    @Test
    fun `leap year day 366 is handled correctly`() = runTest {
        // Given: Database with 5 quotes and day set to 366 (leap year)
        val fiveQuotes = testQuotes + listOf(
            QuoteEntity(id = 4, textLatin = "Quote 4", author = "Author 4", runicElder = null, runicYounger = null, runicCirth = null),
            QuoteEntity(id = 5, textLatin = "Quote 5", author = "Author 5", runicElder = null, runicYounger = null, runicCirth = null)
        )
        timeProvider.setDayOfYear(366)
        coEvery { quoteDao.getCount() } returns 5
        stubQuotes(fiveQuotes)

        // When: Getting quote for day 366
        val quote = repository.quoteOfTheDay()

        // Then: Valid quote is returned for the complete leap-year date
        assertThat(quote).isNotNull()
        assertThat(quote?.id).isEqualTo(fiveQuotes[Math.floorMod(timeProvider.getCurrentDate().toEpochDay(), 5L).toInt()].id)
    }

    @Test
    fun `repository with different quote counts distributes evenly`() = runTest {
        // Test with 1, 2, 5, 10 quotes to ensure modulo works correctly
        val singleQuote = listOf(testQuotes[0])

        // Test with 1 quote
        timeProvider.setDayOfYear(100)
        coEvery { quoteDao.getCount() } returns 1
        stubQuotes(singleQuote)
        val quote = repository.quoteOfTheDay()
        assertThat(quote?.id).isEqualTo(singleQuote[0].id)
    }

    @Test
    fun `daily and random operations use persisted canonical seed check`() = runTest {
        // Given: Empty database initially
        coEvery { quoteDao.getCount() } returns 0 andThen 5
        coEvery { quoteDao.seedCanonicalQuotes(any()) } returns Unit
        stubQuotes(testQuotes)
        coEvery { quoteDao.getRandom() } returns testQuotes[0]

        // When: Multiple operations that call seedIfNeeded
        repository.quoteOfTheDay()
        repository.randomQuote()
        repository.quoteOfTheDay()

        // Each operation checks canonical identity inside the DAO transaction.
        coVerify(exactly = 3) { quoteDao.seedCanonicalQuotes(any()) }
    }

    @Test
    fun `getQuoteCount delegates to dao`() = runTest {
        coEvery { quoteDao.getCount() } returns 42

        assertThat(repository.getQuoteCount()).isEqualTo(42)
    }

    @Test
    fun `getUserQuotesFlow maps user created entities`() = runTest {
        every { quoteDao.getUserQuotesFlow() } returns flowOf(
            listOf(
                testQuotes[1].copy(isUserCreated = true, createdAt = 123L)
            )
        )

        val quotes = repository.getUserQuotesFlow().first()

        assertThat(quotes).containsExactly(
            Quote(
                id = 2L,
                textLatin = "Test quote 2",
                author = "Author 2",
                runicElder = "\u16A6\u16D6\u16CA\u16CF",
                runicYounger = null,
                runicCirth = null,
                isUserCreated = true,
                isFavorite = false,
                createdAt = 123L
            )
        )
    }

    @Test
    fun `getFavoritesFlow and getFavorites map favorite entities`() = runTest {
        val favoriteEntity = testQuotes[0].copy(isFavorite = true, createdAt = 777L)
        every { quoteDao.getFavoritesFlow() } returns flowOf(listOf(favoriteEntity))
        coEvery { quoteDao.getFavorites() } returns listOf(favoriteEntity)

        val flowQuotes = repository.getFavoritesFlow().first()
        val listQuotes = repository.getFavorites()

        assertThat(flowQuotes.single().isFavorite).isTrue()
        assertThat(listQuotes.single().createdAt).isEqualTo(777L)
    }

    @Test
    fun `toggleFavorite delegates to dao`() = runTest {
        coEvery { quoteDao.updateFavoriteStatus(3L, true) } returns Unit

        repository.toggleFavorite(3L, true)

        coVerify(exactly = 1) { quoteDao.updateFavoriteStatus(3L, true) }
    }

    @Test
    fun `saveUserQuote inserts new quotes as user created`() = runTest {
        val quote = Quote(
            id = 0L,
            textLatin = "New quote",
            author = "Builder",
            runicElder = "ᚾᛖᚹ",
            runicYounger = null,
            runicCirth = null,
            isUserCreated = false,
            isFavorite = true,
            createdAt = 55L
        )
        coEvery { quoteDao.insert(any()) } returns 9L

        val result = repository.saveUserQuote(quote)

        assertThat(result).isEqualTo(9L)
        coVerify {
            quoteDao.insert(
                QuoteEntity(
                    id = 0L,
                    textLatin = "New quote",
                    author = "Builder",
                    runicElder = "ᚾᛖᚹ",
                    runicYounger = null,
                    runicCirth = null,
                    isUserCreated = true,
                    isFavorite = true,
                    createdAt = 55L
                )
            )
        }
    }

    @Test
    fun `content command returns persisted favorite and creation metadata`() = runTest {
        val requested = Quote(
            id = 12L, textLatin = "Updated source", author = "New author", runicElder = "new glyphs",
            runicYounger = null, runicCirth = null, isUserCreated = true, isFavorite = false, createdAt = 999L
        )
        val persisted = QuoteEntity(
            id = 12L, textLatin = requested.textLatin, author = requested.author, runicElder = requested.runicElder,
            isUserCreated = true, isFavorite = true, createdAt = 42L
        )
        coEvery { quoteDao.updateUserContent(any(), "Old source", "Old author") } returns persisted

        val saved = repository.updateUserQuoteContent(requested, "Old source", "Old author")

        assertThat(saved.id).isEqualTo(12L)
        assertThat(saved.textLatin).isEqualTo("Updated source")
        assertThat(saved.isFavorite).isTrue()
        assertThat(saved.createdAt).isEqualTo(42L)
        coVerify(exactly = 1) { quoteDao.updateUserContent(any(), "Old source", "Old author") }
    }

    @Test
    fun `content command rejects a missing or concurrently changed row`() = runTest {
        coEvery { quoteDao.updateUserContent(any(), any(), any()) } returns null
        val quote = Quote(
            id = 12L, textLatin = "Updated", author = "User",
            runicElder = null, runicYounger = null, runicCirth = null, isUserCreated = true
        )

        val failure = runCatching { repository.updateUserQuoteContent(quote, "Old", "User") }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `creation rejects an existing identity without replacing it`() = runTest {
        val quote = Quote(
            id = 12L, textLatin = "Source", author = "User",
            runicElder = null, runicYounger = null, runicCirth = null, isUserCreated = true
        )

        val failure = runCatching { repository.saveUserQuote(quote) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        coVerify(exactly = 0) { quoteDao.insert(any()) }
    }

    @Test
    fun `restoreUserQuote reinserts deleted quote with original id`() = runTest {
        val quote = Quote(
            id = 12L,
            textLatin = "Restored quote",
            author = "Builder",
            runicElder = "ᚱᛖᛋᛏᛟᚱᛖᛞ",
            runicYounger = null,
            runicCirth = null,
            isUserCreated = false,
            isFavorite = true,
            createdAt = 99L
        )
        coEvery { quoteDao.insert(any()) } returns 12L

        val result = repository.restoreUserQuote(quote)

        assertThat(result).isEqualTo(12L)
        coVerify {
            quoteDao.insert(
                QuoteEntity(
                    id = 12L,
                    textLatin = "Restored quote",
                    author = "Builder",
                    runicElder = "ᚱᛖᛋᛏᛟᚱᛖᛞ",
                    runicYounger = null,
                    runicCirth = null,
                    isUserCreated = true,
                    isFavorite = true,
                    createdAt = 99L
                )
            )
        }
    }

    @Test
    fun `deleteUserQuote and getQuoteById delegate and map results`() = runTest {
        coEvery { quoteDao.deleteUserQuote(4L) } returns Unit
        coEvery { quoteDao.getById(2L) } returns testQuotes[1].copy(isFavorite = true, createdAt = 500L)
        coEvery { quoteDao.getById(8L) } returns null

        repository.deleteUserQuote(4L)
        val quote = repository.getQuoteById(2L)

        coVerify { quoteDao.deleteUserQuote(4L) }
        assertThat(quote?.isFavorite).isTrue()
        assertThat(quote?.createdAt).isEqualTo(500L)
        assertThat(repository.getQuoteById(8L)).isNull()
    }

    private fun stubQuotes(quotes: List<QuoteEntity>) {
        coEvery { quoteDao.getAll() } returns quotes
        coEvery { quoteDao.getQuoteIdentities() } returns quotes.map { it.id }
        coEvery { quoteDao.getById(any()) } answers { quotes.firstOrNull { it.id == firstArg<Long>() } }
    }
}
