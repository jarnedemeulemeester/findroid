package dev.jdtech.jellyfin.core.services

import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

@AndroidEntryPoint
class ConnectivityService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var connectivityManager: ConnectivityManager

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var jellyfinApi: JellyfinApi

    @Inject
    lateinit var serverDatabase: ServerDatabaseDao

    val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            testAddresses()
        }
    }

    private fun testAddresses() {
        serviceScope.launch {
            val currentServerId = appPreferences.getValue(appPreferences.currentServer)
            if (currentServerId == null) {
                return@launch
            }
            val serverWithAddresses = serverDatabase.getServerWithAddresses(currentServerId)
            val newAddress = serverWithAddresses.addresses.firstOrNull { testAddress(it.address) }
            if (newAddress == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Could not find a suitable address to connect to the server", Toast.LENGTH_LONG).show()
                }
                return@launch
            }
            updateAddress(newAddress.address)
        }
    }

    private fun testAddress(address: String): Boolean {
        Timber.d("Testing address $address")

        var reachable = false
        try {
            val url = URL(address)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            val responseCode = connection.responseCode
            reachable = responseCode in 200..399
            connection.disconnect()
        } catch (e: IOException) {
            Timber.e(e)
        }

        Timber.d("Address $address is reachable: $reachable")

        return reachable
    }

    private suspend fun updateAddress(address: String) {
        if (jellyfinApi.api.baseUrl != address) {
            Timber.d("Changing current address to $address")
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Changed to address: $address", Toast.LENGTH_SHORT).show()
            }
            jellyfinApi.api.update(baseUrl = address)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        Timber.d("Creating service...")
        super.onCreate()
        connectivityManager = applicationContext.getSystemService(ConnectivityManager::class.java)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Timber.d("Starting service...")
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.d("Destroying service...")
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
