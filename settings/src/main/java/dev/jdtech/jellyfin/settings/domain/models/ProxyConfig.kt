package dev.jdtech.jellyfin.settings.domain.models

/**
 * Represents the type of proxy connection
 */
enum class ProxyType {
    NONE,
    HTTP,
    HTTPS,
    SOCKS5,
    ;

    companion object {
        fun fromString(value: String?): ProxyType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

/**
 * Configuration data class for proxy settings
 */
data class ProxyConfig(
    val type: ProxyType = ProxyType.NONE,
    val host: String = "",
    val port: Int = 8080,
    val authRequired: Boolean = false,
    val username: String = "",
    val password: String = "",
) {
    val isEnabled: Boolean
        get() = type != ProxyType.NONE && host.isNotBlank()

    /**
     * Returns the proxy URL for MPV player
     * Format: protocol://[username:password@]host:port
     */
    fun toMpvProxyUrl(): String? {
        if (!isEnabled) return null

        val protocol = when (type) {
            ProxyType.HTTP, ProxyType.HTTPS -> "http"
            ProxyType.SOCKS5 -> "socks5"
            ProxyType.NONE -> return null
        }

        return if (authRequired && username.isNotBlank()) {
            "$protocol://$username:$password@$host:$port"
        } else {
            "$protocol://$host:$port"
        }
    }
}
