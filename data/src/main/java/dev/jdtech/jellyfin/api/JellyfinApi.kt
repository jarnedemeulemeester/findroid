package dev.jdtech.jellyfin.api

import android.content.Context
import android.content.SharedPreferences
import android.security.KeyChain
import dev.jdtech.jellyfin.data.BuildConfig
import dev.jdtech.jellyfin.settings.domain.Constants
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import okhttp3.OkHttpClient
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.client.extensions.authenticationApi
import org.jellyfin.sdk.api.client.extensions.brandingApi
import org.jellyfin.sdk.api.client.extensions.deviceApi
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.client.extensions.mediaSegmentApi
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.api.client.extensions.showApi
import org.jellyfin.sdk.api.client.extensions.suggestionApi
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.trickPlayApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userDataApi
import org.jellyfin.sdk.api.client.extensions.userViewApi
import org.jellyfin.sdk.api.client.extensions.videoApi
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo

/**
 * Jellyfin API class using org.jellyfin.sdk:jellyfin-core
 *
 * @param androidContext The context
 * @param socketTimeout The socket timeout
 * @constructor Creates a new [JellyfinApi] instance
 */
class JellyfinApi(
    androidContext: Context,
    requestTimeout: Long = Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT,
    connectTimeout: Long = Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT,
    socketTimeout: Long = Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT,
) {
    private val sharedPreferences: SharedPreferences =
        androidContext.getSharedPreferences(
            androidContext.packageName + "_preferences",
            Context.MODE_PRIVATE,
        )

    val httpClient =
        createHttpClient(
            androidContext = androidContext,
            requestTimeout = requestTimeout,
            connectTimeout = connectTimeout,
            socketTimeout = socketTimeout,
        )

    val jellyfin = createJellyfin {
        clientInfo =
            ClientInfo(
                name =
                    androidContext.applicationInfo
                        .loadLabel(androidContext.packageManager)
                        .toString(),
                version = BuildConfig.VERSION_NAME,
            )
        context = androidContext
        apiClientFactory = OkHttpFactory(httpClient)
    }

    private fun createHttpClient(
        androidContext: Context,
        requestTimeout: Long,
        connectTimeout: Long,
        socketTimeout: Long,
    ): OkHttpClient {
        val options =
            HttpClientOptions(
                requestTimeout = requestTimeout.toDuration(DurationUnit.MILLISECONDS),
                connectTimeout = connectTimeout.toDuration(DurationUnit.MILLISECONDS),
                socketTimeout = socketTimeout.toDuration(DurationUnit.MILLISECONDS),
            )
        val aliasPreference = Constants.NETWORK_CLIENT_CERTIFICATE_ALIAS
        val keyManager =
            object : X509ExtendedKeyManager() {
                private fun alias() = sharedPreferences.getString(aliasPreference, null)

                override fun chooseClientAlias(
                    keyType: Array<out String>?,
                    issuers: Array<out Principal>?,
                    socket: Socket?,
                ) = alias()

                override fun chooseEngineClientAlias(
                    keyType: Array<out String>?,
                    issuers: Array<out Principal>?,
                    engine: SSLEngine?,
                ) = alias()

                override fun getClientAliases(
                    keyType: String?,
                    issuers: Array<out Principal>?,
                ) = alias()?.let(::arrayOf)

                override fun getPrivateKey(alias: String?): PrivateKey? = alias?.let {
                    KeyChain.getPrivateKey(androidContext, it)
                }

                override fun getCertificateChain(alias: String?): Array<X509Certificate>? =
                    alias?.let {
                        KeyChain.getCertificateChain(androidContext, it)
                    }

                override fun chooseServerAlias(
                    keyType: String?,
                    issuers: Array<out Principal>?,
                    socket: Socket?,
                ): String? = null

                override fun getServerAliases(
                    keyType: String?,
                    issuers: Array<out Principal>?,
                ): Array<String>? = null

                override fun chooseEngineServerAlias(
                    keyType: String?,
                    issuers: Array<out Principal>?,
                    engine: SSLEngine?,
                ): String? = null
            }
        val keyManagers = arrayOf<KeyManager>(keyManager)
        val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
        sslContext.init(keyManagers, null, null)
        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(null as java.security.KeyStore?)
            }
        val trustManager = trustManagerFactory.trustManagers.single() as X509TrustManager
        return OkHttpFactory()
            .createClient(options)
            .newBuilder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }

    val api =
        jellyfin.createApi(
            httpClientOptions =
                HttpClientOptions(
                    requestTimeout = requestTimeout.toDuration(DurationUnit.MILLISECONDS),
                    connectTimeout = connectTimeout.toDuration(DurationUnit.MILLISECONDS),
                    socketTimeout = socketTimeout.toDuration(DurationUnit.MILLISECONDS),
                )
        )
    var userId: UUID? = null

    val brandingApi = api.brandingApi
    val authenticationApi = api.authenticationApi
    val deviceApi = api.deviceApi
    val libraryApi = api.libraryApi
    val mediaInfoApi = api.mediaInfoApi
    val mediaSegmentApi = api.mediaSegmentApi
    val sessionApi = api.sessionApi
    val showApi = api.showApi
    val suggestionApi = api.suggestionApi
    val systemApi = api.systemApi
    val trickPlayApi = api.trickPlayApi
    val userApi = api.userApi
    val userDataApi = api.userDataApi
    val videoApi = api.videoApi
    val userViewApi = api.userViewApi

    companion object {
        @Volatile private var INSTANCE: JellyfinApi? = null

        fun getInstance(
            context: Context,
            requestTimeout: Long = Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT,
            connectTimeout: Long = Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT,
            socketTimeout: Long = Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT,
        ): JellyfinApi {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance =
                        JellyfinApi(
                            androidContext = context.applicationContext,
                            requestTimeout = requestTimeout,
                            connectTimeout = connectTimeout,
                            socketTimeout = socketTimeout,
                        )
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}
