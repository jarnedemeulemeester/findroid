package dev.jdtech.jellyfin

object Constants {
    // pref
    const val PREF_CURRENT_SERVER = "pref_current_server"
    const val PREF_OFFLINE_MODE = "pref_offline_mode"

    const val PREF_THEME = "theme"
    const val PREF_NETWORK_REQUEST_TIMEOUT = "pref_network_request_timeout"
    const val PREF_NETWORK_CONNECT_TIMEOUT = "pref_network_connect_timeout"
    const val PREF_NETWORK_SOCKET_TIMEOUT = "pref_network_socket_timeout"
    const val PREF_SORT_BY = "pref_sort_by"
    const val PREF_SORT_ORDER = "pref_sort_order"

    // favorites
    const val FAVORITE_TYPE_MOVIES = 0
    const val FAVORITE_TYPE_SHOWS = 1
    const val FAVORITE_TYPE_EPISODES = 2

    // network
    const val NETWORK_DEFAULT_REQUEST_TIMEOUT = 30_000L
    const val NETWORK_DEFAULT_CONNECT_TIMEOUT = 6_000L
    const val NETWORK_DEFAULT_SOCKET_TIMEOUT = 10_000L

    // sorting
    // This values must correspond to a SortString from [SortBy]
    const val DEFAULT_SORT_BY = "SortName"
    const val DEFAULT_SORT_ORDER = "Ascending"
}
