package com.po4yka.runatal.data.repository

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.dao.RuneReferenceDao
import com.po4yka.runatal.data.local.entity.RuneReferenceEntity
import com.po4yka.runatal.domain.model.RuneReference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class RuneReferenceRepositoryImplTest {

    private lateinit var runeReferenceDao: RuneReferenceDao
    private lateinit var repository: RuneReferenceRepositoryImpl

    @Before
    fun setUp() {
        runeReferenceDao = mockk()
        repository = RuneReferenceRepositoryImpl(runeReferenceDao)
    }

    @Test
    fun `seedIfNeeded inserts initial rune data only when database is empty`() = runTest {
        val captured = slot<List<RuneReferenceEntity>>()
        coEvery { runeReferenceDao.getCount() } returns 0
        coEvery { runeReferenceDao.insertAll(capture(captured)) } returns Unit

        repository.seedIfNeeded()
        repository.seedIfNeeded()

        assertThat(captured.captured).isNotEmpty()
        coVerify(exactly = 1) { runeReferenceDao.insertAll(any()) }
    }

    @Test
    fun `seedIfNeeded skips insertion when dao already has data`() = runTest {
        coEvery { runeReferenceDao.getCount() } returns 5

        repository.seedIfNeeded()

        coVerify(exactly = 0) { runeReferenceDao.insertAll(any()) }
    }

    @Test
    fun `getAllRunesFlow maps entities to domain models`() = runTest {
        every { runeReferenceDao.getAllFlow() } returns flowOf(
            listOf(
                RuneReferenceEntity(
                    id = 1L,
                    character = "\u16A0",
                    name = "Fehu",
                    pronunciation = "f",
                    meaning = "wealth",
                    history = "History",
                    script = "elder_futhark"
                )
            )
        )

        val result = repository.getAllRunesFlow().first().single()

        assertThat(result).isEqualTo(
            RuneReference(
                id = 1L,
                character = "\u16A0",
                name = "Fehu",
                pronunciation = "f",
                meaning = "wealth",
                history = "History",
                script = "elder_futhark"
            )
        )
    }

    @Test
    fun `getRunesByScriptFlow filters through dao and maps results`() = runTest {
        every { runeReferenceDao.getByScriptFlow("cirth") } returns flowOf(
            listOf(
                RuneReferenceEntity(
                    id = 7L,
                    character = "\u16A0",
                    name = "Angerthas",
                    pronunciation = "a",
                    meaning = "sound",
                    history = "History",
                    script = "cirth"
                )
            )
        )

        val result = repository.getRunesByScriptFlow("cirth").first().single()

        assertThat(result.script).isEqualTo("cirth")
        assertThat(result.name).isEqualTo("Angerthas")
    }

    @Test
    fun `getRuneById maps nullable response`() = runTest {
        coEvery { runeReferenceDao.getById(2L) } returns RuneReferenceEntity(
            id = 2L,
            character = "\u16A2",
            name = "Uruz",
            pronunciation = "u",
            meaning = "strength",
            history = "History",
            script = "elder_futhark"
        )
        coEvery { runeReferenceDao.getById(99L) } returns null

        assertThat(repository.getRuneById(2L)?.name).isEqualTo("Uruz")
        assertThat(repository.getRuneById(99L)).isNull()
    }

    @Test
    fun `insertAllRunes maps domain models back to entities`() = runTest {
        val rune = RuneReference(
            id = 5L,
            character = "\u16B1",
            name = "Raidho",
            pronunciation = "r",
            meaning = "journey",
            history = "Road",
            script = "elder_futhark"
        )
        coEvery { runeReferenceDao.insertAll(any()) } returns Unit

        repository.insertAllRunes(listOf(rune))

        coVerify {
            runeReferenceDao.insertAll(
                listOf(
                    RuneReferenceEntity(
                        id = 5L,
                        character = "\u16B1",
                        name = "Raidho",
                        pronunciation = "r",
                        meaning = "journey",
                        history = "Road",
                        script = "elder_futhark"
                    )
                )
            )
        }
    }
}
