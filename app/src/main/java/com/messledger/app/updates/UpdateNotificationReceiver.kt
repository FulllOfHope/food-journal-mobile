package com.messledger.app.updates

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** "Later" on the notification: clear it without skipping the version. */
class UpdateNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != UpdateNotifier.ACTION_DISMISS_UPDATE) return
        UpdateNotifier(context.applicationContext).dismiss()
        UpdateManager.get(context.applicationContext).markLater()
    }
}
