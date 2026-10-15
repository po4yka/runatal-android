package com.po4yka.runatal.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.po4yka.runatal.MainActivity
import com.po4yka.runatal.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Posts only after current runtime permission, app and channel settings permit delivery. */
@Singleton
internal class NotificationPublisher @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val _access = MutableStateFlow(snapshot())
    val access: StateFlow<NotificationAccess> = _access.asStateFlow()

    fun createChannels() {
        NotificationKind.entries.forEach { kind ->
            manager.createNotificationChannel(NotificationChannel(kind.channelId, kind.label,
                NotificationManager.IMPORTANCE_LOW).apply {
                description = when (kind) {
                    NotificationKind.DAILY -> "A locally selected quote each day."
                    NotificationKind.STREAK -> "Reminders only for an actual reading streak awaiting today's reading."
                    NotificationKind.PACKS -> "Changes to quote packs bundled with an app update."
                }
            })
        }
        refreshAccess()
    }

    fun refreshAccess() { _access.value = snapshot() }

    fun snapshot(): NotificationAccess {
        val permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return NotificationAccess(permission, NotificationManagerCompat.from(context).areNotificationsEnabled(),
            NotificationKind.entries.filter { manager.getNotificationChannel(it.channelId)?.importance?.let { level ->
                level != NotificationManager.IMPORTANCE_NONE
            } == true }.toSet())
    }

    fun post(kind: NotificationKind, title: String, body: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) return false
        if (!snapshot().allows(kind)) return false
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(context, kind.notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, kind.channelId)
            .setSmallIcon(R.drawable.ic_notification_rune)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        return try {
            manager.notify(kind.notificationId, notification)
            true
        } catch (_: SecurityException) {
            // The user can revoke permission between the check and the system call.
            refreshAccess()
            false
        }
    }

    fun cancel(kind: NotificationKind) { manager.cancel(kind.notificationId) }
}
