package com.messledger.app.updates

import android.Manifest
import android.app.Notification
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
import androidx.core.net.toUri
import com.messledger.app.MainActivity
import com.messledger.app.R
import com.messledger.app.domain.UpdateInfo

/** The single notification this feature is allowed to raise. */
class UpdateNotifier(private val context: Context) {

    fun createChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "App updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "New versions of Food Journal"
            enableVibration(false)
            setSound(null, null)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    /** Android 13+ needs the runtime permission; older devices are fine. */
    fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(update: UpdateInfo) {
        createChannel()
        if (!canPost()) return
        // Inline guard as well: lint (and the runtime) require the permission
        // check at the call site, not only behind a helper.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val openApp = Intent(context, MainActivity::class.java).apply {
            action = ACTION_SHOW_UPDATE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val openPending = PendingIntent.getActivity(
            context, REQUEST_OPEN, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val download = Intent(Intent.ACTION_VIEW, update.apkUrl.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val downloadPending = PendingIntent.getActivity(
            context, REQUEST_DOWNLOAD, download,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val later = Intent(context, UpdateNotificationReceiver::class.java).apply {
            action = ACTION_DISMISS_UPDATE
        }
        val laterPending = PendingIntent.getBroadcast(
            context, REQUEST_LATER, later,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "Version ${update.versionName} is ready to install"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_groove)
            .setContentTitle("Food Journal update available")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .addAction(R.drawable.ic_notify_download, "Download update", downloadPending)
            .addAction(R.drawable.ic_notify_later, "Later", laterPending)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    fun dismiss() {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }

    companion object {
        const val CHANNEL_ID = "food_journal_updates"
        const val NOTIFICATION_ID = 20_260_801
        const val ACTION_SHOW_UPDATE = "com.messledger.app.action.SHOW_UPDATE"
        const val ACTION_DISMISS_UPDATE = "com.messledger.app.action.DISMISS_UPDATE"

        private const val REQUEST_OPEN = 801
        private const val REQUEST_DOWNLOAD = 802
        private const val REQUEST_LATER = 803
    }
}
