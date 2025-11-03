package com.po4yka.runicquotes.util

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for providing current time/date.
 * Allows for dependency injection and testability by mocking time-based operations.
 */
interface TimeProvider {
    /**
     * Returns the current day of year (1-365/366).
     */
    fun getCurrentDayOfYear(): Int

    /**
     * Returns the current LocalDate.
     */
    fun getCurrentDate(): LocalDate
}

/**
 * Default implementation that returns actual system time.
 */
@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun getCurrentDayOfYear(): Int = LocalDate.now().dayOfYear

    override fun getCurrentDate(): LocalDate = LocalDate.now()
}
