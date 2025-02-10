package dev.jdtech.jellyfin

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

class AppPreferences
@Inject
constructor(
    private val sharedPreferences: SharedPreferences,
) {
    // Server
    var currentServer: String?
        get() = sharedPreferences.getString(Constants.PREF_CURRENT_SERVER, null)
        set(value) {
            sharedPreferences.edit {
                putString(Constants.PREF_CURRENT_SERVER, value)
            }
        }

    // Offline
    var offlineMode
        get() = sharedPreferences.getBoolean(Constants.PREF_OFFLINE_MODE, false)
        set(value) {
            sharedPreferences.edit {
                putBoolean(Constants.PREF_OFFLINE_MODE, value)
            }
        }

    // Appearance
    val theme get() = sharedPreferences.getString(Constants.PREF_THEME, null)

    // Network
    val requestTimeout get() = sharedPreferences.getString(
        Constants.PREF_NETWORK_REQUEST_TIMEOUT,
        Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT.toString(),
    )!!.toLongOrNull() ?: Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT

    val connectTimeout get() = sharedPreferences.getString(
        Constants.PREF_NETWORK_CONNECT_TIMEOUT,
        Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT.toString(),
    )!!.toLongOrNull() ?: Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT

    val socketTimeout get() = sharedPreferences.getString(
        Constants.PREF_NETWORK_SOCKET_TIMEOUT,
        Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT.toString(),
    )!!.toLongOrNull() ?: Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT

    // Downloads
    val downloadOverMobileData get() = sharedPreferences.getBoolean(
        Constants.PREF_DOWNLOADS_MOBILE_DATA,
        false,
    )
    val downloadWhenRoaming get() = sharedPreferences.getBoolean(
        Constants.PREF_DOWNLOADS_ROAMING,
        false,
    )

    // Sorting
    var sortBy: String
        get() = sharedPreferences.getString(
            Constants.PREF_SORT_BY,
            Constants.DEFAULT_SORT_BY,
        )!!
        set(value) {
            sharedPreferences.edit {
                putString(Constants.PREF_SORT_BY, value)
            }
        }
    var sortOrder
        get() = sharedPreferences.getString(
            Constants.PREF_SORT_ORDER,
            Constants.DEFAULT_SORT_ORDER,
        )!!
        set(value) {
            sharedPreferences.edit {
                putString(Constants.PREF_SORT_ORDER, value)
            }
        }

    fun setValue(key: String, value: String) {
        sharedPreferences.edit {
            putString(key, value)
        }
    }
}
