package com.bugsnag.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.support.annotation.RequiresApi

typealias NetworkChangeCallback = (connected: Boolean) -> Unit

internal interface Connectivity {
    fun registerForNetworkChanges()
    fun unregisterForNetworkChanges()
    fun hasNetworkConnection(): Boolean
    fun retrieveNetworkAccessState(): String
}

internal class ConnectivityCompat(
    context: Context,
    callback: NetworkChangeCallback?
) : Connectivity {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val connectivity: Connectivity =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> ConnectivityApi24(cm, callback)
            else -> ConnectivityLegacy(context, cm, callback)
        }

    override fun registerForNetworkChanges() = connectivity.registerForNetworkChanges()
    override fun hasNetworkConnection() = connectivity.hasNetworkConnection()
    override fun unregisterForNetworkChanges() = connectivity.unregisterForNetworkChanges()
    override fun retrieveNetworkAccessState() = connectivity.retrieveNetworkAccessState()
}

@Suppress("DEPRECATION")
internal class ConnectivityLegacy(
    private val context: Context,
    private val cm: ConnectivityManager,
    callback: NetworkChangeCallback?
) : Connectivity {

    private val changeReceiver = ConnectivityChangeReceiver(callback)

    override fun registerForNetworkChanges() {
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(changeReceiver, intentFilter)
    }

    override fun unregisterForNetworkChanges() = context.unregisterReceiver(changeReceiver)

    override fun hasNetworkConnection(): Boolean {
        return cm.activeNetworkInfo?.isConnectedOrConnecting ?: false
    }

    override fun retrieveNetworkAccessState(): String {
        return when (cm.activeNetworkInfo?.type) {
            null -> "none"
            ConnectivityManager.TYPE_WIFI -> "wifi"
            ConnectivityManager.TYPE_ETHERNET -> "ethernet"
            else -> "cellular" // all other types are cellular in some form
        }
    }

    private inner class ConnectivityChangeReceiver(private val cb: NetworkChangeCallback?) :
        BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            cb?.invoke(hasNetworkConnection())
        }
    }
}

@RequiresApi(Build.VERSION_CODES.N)
internal class ConnectivityApi24(
    private val cm: ConnectivityManager,
    callback: NetworkChangeCallback?
) : Connectivity {

    @JvmField
    @Volatile
    internal var activeNetwork: Network? = null
    private val networkCallback = ConnectivityTrackerCallback(callback)

    override fun registerForNetworkChanges() = cm.registerDefaultNetworkCallback(networkCallback)
    override fun unregisterForNetworkChanges() = cm.unregisterNetworkCallback(networkCallback)
    override fun hasNetworkConnection() = activeNetwork != null

    override fun retrieveNetworkAccessState(): String {
        val network = cm.activeNetwork
        val capabilities = if (network != null) cm.getNetworkCapabilities(network) else null

        return when {
            capabilities == null -> "none"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            else -> "unknown"
        }
    }

    private inner class ConnectivityTrackerCallback(private val cb: NetworkChangeCallback?) :
        ConnectivityManager.NetworkCallback() {
        override fun onUnavailable() {
            super.onUnavailable()
            activeNetwork = null
            cb?.invoke(false)
        }

        override fun onAvailable(network: Network?) {
            super.onAvailable(network)
            activeNetwork = network
            cb?.invoke(true)
        }
    }
}