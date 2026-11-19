package com.messledger.app.updates

import android.content.Context
import com.messledger.app.data.ConfigDataStore
import com.messledger.app.data.UpdateSettings
import com.messledger.app.domain.GitHubUpdateChecker
import com.messledger.app.domain.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the update-check policy: when to look, when to nag, and what the UI
 * needs to render. All network work happens on Dispatchers.IO; state is
 * exposed as flows so nothing blocks a composable.
 */
class UpdateManager(
    private val settings: ConfigDataStore,
    private val checker: GitHubUpdateChecker,
    private val notifier: UpdateNotifier,
    private val scope: CoroutineScope,
    private val currentVersion: String
) {
    private val _availableUpdate = MutableStateFlow<UpdateInfo?>(null)
    val availableUpdate: StateFlow<UpdateInfo?> = _availableUpdate.asStateFlow()

    private val _sheetVisible = MutableStateFlow(false)
    val sheetVisible: StateFlow<Boolean> = _sheetVisible.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _needsNotificationPermission = MutableStateFlow(false)
    val needsNotificationPermission: StateFlow<Boolean> = _needsNotificationPermission.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: Flow<String> = _messages

    val updateSettings: StateFlow<UpdateSettings> = settings.updateSettingsFlow
        .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, UpdateSettings())

    /** Versions already announced in this process; never post the same one twice. */
    private val notifiedVersions = mutableSetOf<String>()
    private var pendingNotification: UpdateInfo? = null
    private var lastNotifiedVersion: String? = null

    /** Called once from Application.onCreate. */
    fun onAppStart() {
        notifier.createChannel()
        scope.launch(Dispatchers.IO) {
            val saved = settings.updateSettingsFlow.first()
            if (!saved.updateChecksEnabled) return@launch
            if (System.currentTimeMillis() - saved.lastUpdateCheckAt <
                GitHubUpdateChecker.CHECK_INTERVAL_MS
            ) return@launch
            performCheck(lastCheckAt = saved.lastUpdateCheckAt, manual = false)
        }
    }

    /** Manual refresh: bypasses the six-hour gate and works with checks off. */
    fun checkNow() {
        scope.launch(Dispatchers.IO) {
            val last = settings.updateSettingsFlow.first().lastUpdateCheckAt
            performCheck(lastCheckAt = last, manual = true)
        }
    }

    private suspend fun performCheck(lastCheckAt: Long, manual: Boolean) {
        if (_isChecking.value) return
        _isChecking.value = true
        try {
            val now = System.currentTimeMillis()
            settings.setLastUpdateCheckAt(now)

            val update = runCatching { checker.checkForUpdate() }.getOrNull()
            if (update == null) {
                settings.setLastUpdateInfo(null)
                if (manual) _messages.tryEmit("You're up to date. No new release found.")
                return
            }

            settings.setLastUpdateInfo(update)
            _availableUpdate.value = update

            val saved = settings.updateSettingsFlow.first()
            val lastNotified = settings.lastNotifiedVersionFlow.first()
            val eligible = GitHubUpdateChecker.shouldNotify(
                update = update,
                currentVersion = currentVersion,
                dismissedVersion = saved.dismissedUpdateVersion,
                lastCheckAt = lastCheckAt,
                now = now,
                checksEnabled = manual || saved.updateChecksEnabled,
                bypassTimeGate = manual
            ) && update.versionName != lastNotified && update.versionName !in notifiedVersions

            if (eligible) {
                _sheetVisible.value = true
                pendingNotification = update
                if (notifier.canPost()) {
                    post(update)
                } else {
                    _needsNotificationPermission.value = true
                }
            } else if (manual) {
                // A deliberate refresh should still show what was found,
                // even if this version was skipped or already announced.
                _sheetVisible.value = true
            }
        } finally {
            _isChecking.value = false
        }
    }

    private fun post(update: UpdateInfo) {
        notifier.show(update)
        notifiedVersions += update.versionName
        lastNotifiedVersion = update.versionName
        scope.launch(Dispatchers.IO) { settings.setLastNotifiedVersion(update.versionName) }
        pendingNotification = null
        _needsNotificationPermission.value = false
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        if (granted) {
            pendingNotification?.let(::post)
        }
        _needsNotificationPermission.value = false
    }

    fun onPermissionRequestShown() {
        _needsNotificationPermission.value = false
    }

    /** Content tap: open the sheet, using the cached release when offline. */
    fun showSheetFromNotification() {
        scope.launch(Dispatchers.IO) {
            val cached = settings.lastUpdateInfoFlow.first()
            withContext(Dispatchers.Main.immediate) {
                if (_availableUpdate.value == null && cached != null) {
                    _availableUpdate.value = cached
                }
                _sheetVisible.value = _availableUpdate.value != null
            }
            if (_availableUpdate.value == null) {
                // Process was killed and nothing was cached: reuse the same
                // single endpoint once so the tap still has something to show.
                performCheck(lastCheckAt = System.currentTimeMillis(), manual = true)
                withContext(Dispatchers.Main.immediate) {
                    _sheetVisible.value = _availableUpdate.value != null
                }
            }
            notifier.dismiss()
        }
    }

    fun dismissSheet() {
        _sheetVisible.value = false
    }

    /** "Later" only clears the notice; the version can be announced again. */
    fun later() {
        dismissSheet()
        notifier.dismiss()
        pendingNotification = null
        _needsNotificationPermission.value = false
        lastNotifiedVersion?.let { notifiedVersions += it }
    }

    /** Called by the notification's Later action, which has no sheet state. */
    fun markLater() {
        pendingNotification = null
        _needsNotificationPermission.value = false
        lastNotifiedVersion?.let { notifiedVersions += it }
    }

    fun skipThisVersion() {
        scope.launch(Dispatchers.IO) {
            _availableUpdate.value?.let { settings.setDismissedUpdateVersion(it.versionName) }
            settings.setLastUpdateInfo(null)
            notifier.dismiss()
            withContext(Dispatchers.Main.immediate) { _sheetVisible.value = false }
        }
    }

    fun setUpdateChecksEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            settings.setUpdateChecksEnabled(enabled)
            if (!enabled) notifier.dismiss()
        }
    }

    companion object {
        @Volatile
        private var instance: UpdateManager? = null

        fun get(context: Context): UpdateManager = instance ?: synchronized(this) {
            instance ?: create(context).also { instance = it }
        }

        private fun create(context: Context): UpdateManager {
            val appContext = context.applicationContext
            val dataStore = ConfigDataStore(appContext)
            val version = runCatching {
                appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
            }.getOrNull().orEmpty().ifEmpty { "0" }
            return UpdateManager(
                settings = dataStore,
                checker = GitHubUpdateChecker(version),
                notifier = UpdateNotifier(appContext),
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                currentVersion = version
            )
        }
    }
}
