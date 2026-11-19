package com.messledger.app

import android.app.Application
import com.messledger.app.updates.UpdateManager

/** Process-wide home for the privacy-first release check. */
class FoodJournalApplication : Application() {
    val updateManager: UpdateManager by lazy { UpdateManager.get(this) }

    override fun onCreate() {
        super.onCreate()
        // Initializes the manager and, when due, schedules one IO check.
        updateManager.onAppStart()
    }
}
