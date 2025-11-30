package dev.jdtech.jellyfin.network

import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.settings.domain.models.ProxyConfig
import dev.jdtech.jellyfin.settings.domain.models.ProxyType
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Route
import java.net.InetSocketAddress
import java.net.Proxy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory class for creating OkHttpClient instances with proxy configuration.
 * Supports HTTP, HTTPS, and SOCKS5 proxies with optional authentication.
 */
@Singleton
class ProxyOkHttpClientFactory @Inject constructor(
    private val appPreferences: AppPreferences,
) {
    /**
     * Creates an OkHttpClient with proxy settings applied based on user preferences.
     *
     * @param baseClient Optional base OkHttpClient to build upon. If null, creates a new builder.
     * @return OkHttpClient configured with proxy settings if enabled
     */
    fun createClient(baseClient: OkHttpClient? = null): OkHttpClient {
        val builder = baseClient?.newBuilder() ?: OkHttpClient.Builder()
        val proxyConfig = appPreferences.getProxyConfig()

        if (proxyConfig.isEnabled) {
            applyProxyConfig(builder, proxyConfig)
        }

        return builder.build()
    }

    /**
     * Creates an OkHttpClient with a specific proxy configuration.
     *
     * @param baseClient Optional base OkHttpClient to build upon
     * @param proxyConfig The proxy configuration to apply
     * @return OkHttpClient configured with the specified proxy settings
     */
    fun createClient(baseClient: OkHttpClient? = null, proxyConfig: ProxyConfig): OkHttpClient {
        val builder = baseClient?.newBuilder() ?: OkHttpClient.Builder()

        if (proxyConfig.isEnabled) {
            applyProxyConfig(builder, proxyConfig)
        }

        return builder.build()
    }

    private fun applyProxyConfig(builder: OkHttpClient.Builder, proxyConfig: ProxyConfig) {
        val javaProxyType = when (proxyConfig.type) {
            ProxyType.HTTP, ProxyType.HTTPS -> Proxy.Type.HTTP
            ProxyType.SOCKS5 -> Proxy.Type.SOCKS
            ProxyType.NONE -> return
        }

        val proxy = Proxy(javaProxyType, InetSocketAddress(proxyConfig.host, proxyConfig.port))
        builder.proxy(proxy)

        // Add authentication if required
        if (proxyConfig.authRequired && proxyConfig.username.isNotBlank()) {
            builder.proxyAuthenticator(ProxyAuthenticator(proxyConfig.username, proxyConfig.password))
        }
    }

    /**
     * Authenticator for proxy authentication using Basic Auth
     */
    private class ProxyAuthenticator(
        private val username: String,
        private val password: String,
    ) : Authenticator {
        override fun authenticate(route: Route?, response: Response): okhttp3.Request? {
            // Avoid infinite retry loops
            if (response.request.header("Proxy-Authorization") != null) {
                return null
            }

            val credential = Credentials.basic(username, password)
            return response.request.newBuilder()
                .header("Proxy-Authorization", credential)
                .build()
        }
    }
}
