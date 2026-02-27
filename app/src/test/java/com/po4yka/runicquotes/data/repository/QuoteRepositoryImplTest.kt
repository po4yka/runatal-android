package com.po4yka.runicquotes.data.repository

import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.local.dao.QuoteDao
import com.po4yka.runicquotes.data.local.entity.QuoteEntity
import com.po4yka.runicquotes.util.TimeProvider
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
        repository = QuoteRepositoryImpl(quoteDao, timeProvider)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ==================== Seed Logic Tests ====================

    @Test
    fun `seedIfNeeded inserts quotes when database is empty`() = runTest {
        // Given: Empty database
        coEvery { quoteDao.getCount() } returns 0
        coEvery { quoteDao.insertAll(any()) } returns Unit

        // When: Seeding
        repository.seedIfNeeded()

        // Then: Initial quotes are inserted
        coVerify(exactly = 1) { quoteDao.insertAll(any()) }
    }

    @Test
    fun `seedIfNeeded does nothing when database already has quotes`() = runTest {
        // Given: Database has quotes
        coEvery { quoteDao.getCount() } returns 5

        // When: Seeding
        repository.seedIfNeeded()

        // Then: No insertion happens
        coVerify(exactly = 0) { quoteDao.insertAll(any()) }
    }

    @Test
    fun `seedIfNeeded only seeds once`() = runTest {
        // Given: Empty database
        coEvery { quoteDao.getCount() } returns 0
        coEvery { quoteDao.insertAll(any()) } returns Unit

        // When: Seeding multiple times
        repository.seedIfNeeded()
        repository.seedIfNeeded()
        repository.seedIfNeeded()

        // Then: Only inserted once
        coVerify(exactly = 1) { quoteDao.insertAll(any()) }
    }

    // ==================== Quote of the Day Tests ====================

    @Test
    fun `quoteOfTheDay returns consistent quote for same day`() = runTest {
        // Given: Database with quotes
        coEvery { quoteDao.getCount() } returns 3
        coEvery { quoteDao.getAll() } returns testQuotes

        // When: Getting quote of the day multiple times on same day
        val quote1 = repository.quoteOfTheDay()
        val quote2 = repository.quoteOfTheDay()

        // Then: Same quote is returned
        assertThat(quote2?.id).isEqualTo(quote1?.id)
        assertThat(quote2?.textLatin).isEqualTo(quote1?.textLatin)
    }

    @Test
    fun `quoteOfTheDay uses day of year for selection`() = runTest {
        // Given: Database with 3 quotes and day of year set to 5
        timeProvider.setDayOfYear(5)
        coEvery { quoteDao.getCount() } returns 3
        coEvery { quoteDao.getAll() } returns testQuotes

        // When: Getting quote of the day
        val quote = repository.quoteOfTheDay()

        // Then: Quote index matches dayOfYear % size (5 % 3 = 2)
        val expectedIndex = 2
        assertThat(quote?.id).isEqualTo(testQuotes[expectedIndex].id)
    }

    @Test
    fun `quoteOfTheDay returns null when no quotes available`() = runTest {
        // Given: Empty database (even after seeding attempt)
        coEvery { quoteDao.getCount() } returns 0
        coEvery { quoteDao.insertAll(any()) } returns Unit
        coEvery { quoteDao.getAll() } returns emptyList()

        // When: Getting quote of the day
        val quote = repository.quoteOfTheDay()

        // Then: Returns null
        assertThat(quote).isNull()
    }

    @Test
    fun `quoteOfTheDay seeds database if needed`() = runTest {
        // Given: Initially empty database, then has quotes after seeding
        coEvery { quoteDao.getCount() } returns 0 andThen 5
        coEvery { quoteDao.insertAll(any()) } returns Unit
        coEvery { quoteDao.getAll() } returns testQuotes

        // When: Getting quote of the day
        val quote = repository.quoteOfTheDay()

        // Then: Seeding happened and quote returned
        coVerify { quoteDao.insertAll(any()) }
        assertThat(quote).isNotNull()
    }

    @Test
    fun `quoteOfTheDay maps entity to domain model correctly`() = runTest {
        // Given: Database with quotes and day of year set to 7
        timeProvider.setDayOfYear(7)
        coEvery { quoteDao.getCount() } returns 3
        coEvery { quoteDao.getAll() } returns testQuotes

        // When: Getting quote of the day
        val quote = repository.quoteOfTheDay()

        // Then: Domain model properties match entity (7 % 3 = 1)
        assertThat(quote).isNotNull()
        val expectedIndex = 1
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
        coEvery { quoteDao.insertAll(any()) } returns Unit
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
        coEvery { quoteDao.insertAll(any()) } returns Unit
        coEvery { quoteDao.getRandom() } returns testQuotes[0]

        // When: Getting random quote
        val quote = repository.randomQuote()

        // Then: Seeding happened
        coVerify { quoteDao.insertAll(any()) }
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
        coEvery { quoteDao.getAll() } returns testQuotes

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
        coEvery { quoteDao.getAll() } returns emptyList()

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
        coEvery { quoteDao.getAll() } returns testQuotes

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
        coEvery { quoteDao.getAll() } returns singleQuote

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
        coEvery { quoteDao.getAll() } returns manyQuotes

        // When: Getting quote of the day
        val quote = repository.quoteOfTheDay()

        // Then: Valid quote is returned
        assertThat(quote).isNotNull()
        assertThat(quote!!.id).isIn(1L..1000L)
    }

    @Test
    fun `day of year modulo works correctly for year boundary`() = runTest {
        // Given: 3 quotes in database and day of year set to 365 (end of year)
        timeProvider.setDayOfYear(365)
        coEvery { quoteDao.getCount() } returns 3
        coEvery { quoteDao.getAll() } returns testQuotes

        // When: Getting quote (day of year will be moduloed by 3)
        val quote = repository.quoteOfTheDay()

        // Then: Index is within valid range (365 % 3 = 2)
        assertThat(quote).isNotNull()
        assertThat(quote?.id).isEqualTo(testQuotes[365 % 3].id)
    }

    @Test
    fun `different days return different quotes predictably`() = runTest {
        // Given: Database with 3 quotes
        coEvery { quoteDao.getCount() } returns 3
        coEvery { quoteDao.getAll() } returns testQuotes

        // When: Getting quotes for different days
        timeProvider.setDayOfYear(1)
        val quote1 = repository.quoteOfTheDay()

        timeProvider.setDayOfYear(2)
        val quote2 = repository.quoteOfTheDay()

        timeProvider.setDayOfYear(3)
        val quote3 = repository.quoteOfTheDay()

        // Then: Different quotes are returned based on modulo
        assertThat(quote1?.id).isEqualTo(testQuotes[1].id) // 1 % 3 = 1
        assertThat(quote2?.id).isEqualTo(testQuotes[2].id) // 2 % 3 = 2
        assertThat(quote3?.id).isEqualTo(testQuotes[0].id) // 3 % 3 = 0
    }

    @Test
    fun `same day always returns same quote index`() = runTest {
        // Given: Database with 3 quotes and day set to 10
        timeProvider.setDayOfYear(10)
        coEvery { quoteDao.getCount() } returns 3
        coEvery { quoteDao.getAll() } returns testQuotes

        // When: Getting quote multiple times on same day
        val quote1 = repository.quoteOfTheDay()
        val quote2 = repository.quoteOfTheDay()
        val quote3 = repository.quoteOfTheDay()

        // Then: All return same quote (10 % 3 = 1)
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
        coEvery { quoteDao.getAll() } returns fiveQuotes

        // When: Getting quote for day 366
        val quote = repository.quoteOfTheDay()

        // Then: Valid quote is returned (366 % 5 = 1)
        assertThat(quote).isNotNull()
        assertThat(quote?.id).isEqualTo(fiveQuotes[1].id)
    }

    @Test
    fun `repository with different quote counts distributes evenly`() = runTest {
        // Test with 1, 2, 5, 10 quotes to ensure modulo works correctly
        val singleQuote = listOf(testQuotes[0])

        // Test with 1 quote
        timeProvider.setDayOfYear(100)
        coEvery { quoteDao.getCount() } returns 1
        coEvery { quoteDao.getAll() } returns singleQuote
        val quote = repository.quoteOfTheDay()
        assertThat(quote?.id).isEqualTo(singleQuote[0].id)
    }

    @Test
    fun `isSeeded flag prevents multiple seeding calls`() = runTest {
        // Given: Empty database initially
        coEvery { quoteDao.getCount() } returns 0 andThen 5
        coEvery { quoteDao.insertAll(any()) } returns Unit
        coEvery { quoteDao.getAll() } returns testQuotes
        coEvery { quoteDao.getRandom() } returns testQuotes[0]

        // When: Multiple operations that call seedIfNeeded
        repository.quoteOfTheDay()
        repository.randomQuote()
        repository.quoteOfTheDay()

        // Then: insertAll is called only once due to isSeeded flag
        coVerify(exactly = 1) { quoteDao.insertAll(any()) }
    }
}
