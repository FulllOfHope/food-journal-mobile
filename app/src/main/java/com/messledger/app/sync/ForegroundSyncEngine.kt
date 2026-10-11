package com.messledger.app.sync

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ForegroundSyncEngine(
    private val connectivityManager: ConnectivityManager,
    private val scope: CoroutineScope,
    private val syncAction: suspend () -> Unit
) : DefaultLifecycleObserver {

    private var syncJob: Job? = null
    private var isForeground = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (isForeground) triggerSync()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        isForeground = true
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun onResume(owner: LifecycleOwner) {
        triggerSync()
    }

    override fun onStop(owner: LifecycleOwner) {
        isForeground = false
        connectivityManager.unregisterNetworkCallback(networkCallback)
        syncJob?.cancel()
    }

    private fun triggerSync() {
        if (syncJob?.isActive == true) return
        syncJob = scope.launch(Dispatchers.IO) {
            try {
                syncAction()
            } catch (e: Exception) {
                // Silently swallow network exceptions (offline-first)
            }
        }
    }
}
