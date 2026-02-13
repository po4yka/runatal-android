package com.po4yka.runicquotes.ui.widget

/**
 * Widget refresh cadence options.
 */
enum class WidgetUpdateMode(
    val persistedValue: String,
    val displayName: String,
    val subtitle: String,
    val intervalHours: Int?
) {
    MANUAL(
        persistedValue = "manual",
        displayName = "Manual",
        subtitle = "Refresh only when tapped",
        intervalHours = null
    ),
    EVERY_6_HOURS(
        persistedValue = "every_6_hours",
        displayName = "Every 6 Hours",
        subtitle = "Frequent automatic updates",
        intervalHours = 6
    ),
    EVERY_12_HOURS(
        persistedValue = "every_12_hours",
        displayName = "Every 12 Hours",
        subtitle = "Morning and evening updates",
        intervalHours = 12
    ),
    DAILY(
        persistedValue = "daily",
        displayName = "Daily",
        subtitle = "Update at local midnight",
        intervalHours = 24
    );

    companion object {
        fun fromPersistedValue(value: String): WidgetUpdateMode {
            return entries.firstOrNull { it.persistedValue == value } ?: DAILY
        }
    }
}
