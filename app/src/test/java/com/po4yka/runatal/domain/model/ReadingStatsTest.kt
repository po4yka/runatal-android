package com.po4yka.runatal.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class ReadingStatsTest {
    private val today = LocalDate.of(2026, 1, 1)

    @Test
    fun `streak crosses year boundary and ignores duplicate days`() {
        val days = listOf(today, today.minusDays(1), today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertThat(ReadingStats.fromDays(days, today)).isEqualTo(ReadingStats(3, 4, today.minusDays(4)))
    }

    @Test
    fun `yesterday retains a streak and an earlier gap ends it`() {
        assertThat(ReadingStats.fromDays(listOf(today.minusDays(1), today.minusDays(2)), today).streakDays)
            .isEqualTo(2)
        assertThat(ReadingStats.fromDays(listOf(today.minusDays(2)), today).streakDays).isEqualTo(0)
    }

    @Test
    fun `empty and future dates do not invent reading activity`() {
        assertThat(ReadingStats.fromDays(emptyList(), today)).isEqualTo(ReadingStats(0, 0, null))
        assertThat(ReadingStats.fromDays(listOf(today.plusDays(1)), today)).isEqualTo(ReadingStats(0, 0, null))
    }
}
