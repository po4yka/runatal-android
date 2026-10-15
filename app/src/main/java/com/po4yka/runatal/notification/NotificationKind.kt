package com.po4yka.runatal.notification

/** Stable local notification categories and their system channels. */
internal enum class NotificationKind(val channelId: String, val label: String, val notificationId: Int) {
    DAILY("runatal_daily_quote", "Daily quotes", DAILY_NOTIFICATION_ID),
    STREAK("runatal_reading_streak", "Reading streak reminders", STREAK_NOTIFICATION_ID),
    PACKS("runatal_pack_updates", "Bundled pack updates", PACK_NOTIFICATION_ID)
}

/** System access is distinct from the user's requested delivery preferences. */
internal data class NotificationAccess(
    val permissionGranted: Boolean,
    val appEnabled: Boolean,
    val enabledChannels: Set<NotificationKind>
) {
    fun allows(kind: NotificationKind): Boolean = permissionGranted && appEnabled && kind in enabledChannels
}

private const val DAILY_NOTIFICATION_ID = 101
private const val STREAK_NOTIFICATION_ID = 102
private const val PACK_NOTIFICATION_ID = 103
