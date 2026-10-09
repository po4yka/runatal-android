package com.po4yka.runatal.data.preferences

import com.po4yka.runatal.domain.model.RunicScript

/** Requested settings read without substituting defaults for a storage failure. */
internal data class NotificationPreferencesSnapshot(
    val dailyQuote: Boolean,
    val streak: Boolean,
    val packUpdates: Boolean,
    val script: RunicScript
)
