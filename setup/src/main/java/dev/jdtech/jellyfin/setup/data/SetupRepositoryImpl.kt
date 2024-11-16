package dev.jdtech.jellyfin.setup.data

import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.ExceptionUiText
import dev.jdtech.jellyfin.models.ExceptionUiTexts
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.models.ServerWithAddresses
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.setup.domain.SetupRepository
import kotlinx.coroutines.flow.Flow
import org.jellyfin.sdk.discovery.RecommendedServerInfo
import org.jellyfin.sdk.discovery.RecommendedServerInfoScore
import org.jellyfin.sdk.discovery.RecommendedServerIssue
import org.jellyfin.sdk.model.api.AuthenticateUserByName
import org.jellyfin.sdk.model.api.AuthenticationResult
import org.jellyfin.sdk.model.api.QuickConnectDto
import org.jellyfin.sdk.model.api.QuickConnectResult
import org.jellyfin.sdk.model.api.ServerDiscoveryInfo
import timber.log.Timber
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.setup.R as SetupR

class SetupRepositoryImpl(
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
) : SetupRepository {
    override suspend fun discoverServers(): Flow<ServerDiscoveryInfo> {
        return jellyfinApi.jellyfin.discovery.discoverLocalServers()
    }

    override suspend fun getServers(): List<ServerWithAddresses> {
        return database.getServersWithAddresses()
    }

    override suspend fun deleteServer(serverId: String) {
        database.delete(serverId)
    }

    override suspend fun getIsQuickConnectEnabled(): Boolean {
        return jellyfinApi.quickConnectApi.getQuickConnectEnabled().content
    }

    override suspend fun initiateQuickConnect(): QuickConnectResult {
        return jellyfinApi.quickConnectApi.initiateQuickConnect().content
    }

    override suspend fun getQuickConnectState(secret: String): QuickConnectResult {
        return jellyfinApi.quickConnectApi.getQuickConnectState(secret).content
    }

    override suspend fun setCurrentServer(serverId: String) {
        val serverWithAddressAndUser = database.getServerWithAddressAndUser(serverId) ?: return
        val serverAddress = serverWithAddressAndUser.address ?: return

        jellyfinApi.apply {
            api.update(
                baseUrl = serverAddress.address,
                accessToken = null,
            )
            userId = null
        }
    }

    override suspend fun addServer(address: String): Server {
        // Check if address is not blank
        if (address.isBlank()) {
            throw ExceptionUiText(UiText.StringResource(SetupR.string.add_server_error_empty_address))
        }

        val candidates = jellyfinApi.jellyfin.discovery.getAddressCandidates(address)
        val recommended = jellyfinApi.jellyfin.discovery.getRecommendedServers(
            candidates,
            RecommendedServerInfoScore.OK,
        )
        val goodServers = mutableListOf<RecommendedServerInfo>()
        val okServers = mutableListOf<RecommendedServerInfo>()

        for (recommendedServerInfo in recommended) {
            when (recommendedServerInfo.score) {
                RecommendedServerInfoScore.GREAT -> {
                    return saveServerInDatabase(recommendedServerInfo)
                }
                RecommendedServerInfoScore.GOOD -> goodServers.add(recommendedServerInfo)
                RecommendedServerInfoScore.OK -> okServers.add(recommendedServerInfo)
                RecommendedServerInfoScore.BAD -> Unit
            }
        }

        when {
            goodServers.isNotEmpty() -> {
                return saveServerInDatabase(goodServers.first())
            }
            okServers.isNotEmpty() -> {
                val okServer = okServers.first()
                throw ExceptionUiTexts(createIssuesString(okServer))
            }
            else -> {
                throw ExceptionUiText(UiText.StringResource(SetupR.string.add_server_error_not_found))
            }
        }
    }

    private fun saveServerInDatabase(recommendedServerInfo: RecommendedServerInfo): Server {
        val serverInfo = recommendedServerInfo.systemInfo.getOrNull()
            ?: throw ExceptionUiText(UiText.StringResource(SetupR.string.add_server_error_no_id))

        Timber.d("Connecting to server: ${serverInfo.serverName}")

        val serverInDatabase = database.get(serverInfo.id!!)

        // Check if server is already in the database
        // If so only add a new address to that server if it's different
        val server = if (serverInDatabase != null) {
            val addresses = database.getServerWithAddresses(serverInDatabase.id).addresses
            // If address is not in database, add it
            if (addresses.none { it.address == recommendedServerInfo.address }) {
                val serverAddress = ServerAddress(
                    id = UUID.randomUUID(),
                    serverId = serverInDatabase.id,
                    address = recommendedServerInfo.address,
                )

                database.insertServerAddress(serverAddress)
            }
            serverInDatabase
        } else {
            val serverAddress = ServerAddress(
                id = UUID.randomUUID(),
                serverId = serverInfo.id!!,
                address = recommendedServerInfo.address,
            )

            val server = Server(
                id = serverInfo.id!!,
                name = serverInfo.serverName!!,
                currentServerAddressId = serverAddress.id,
                currentUserId = null,
            )

            database.insertServer(server)
            database.insertServerAddress(serverAddress)
            server
        }

        jellyfinApi.apply {
            api.update(
                baseUrl = recommendedServerInfo.address,
                accessToken = null,
            )
        }

        return server
    }

    /**
     * Create a presentable string of issues with a server
     *
     * @param server The server with issues
     * @return A presentable string of issues separated with \n
     */
    private fun createIssuesString(server: RecommendedServerInfo): Collection<UiText> {
        return server.issues.map {
            when (it) {
                is RecommendedServerIssue.OutdatedServerVersion -> {
                    UiText.StringResource(SetupR.string.add_server_error_outdated, it.version)
                }
                is RecommendedServerIssue.InvalidProductName -> {
                    UiText.StringResource(SetupR.string.add_server_error_not_jellyfin, it.productName ?: "")
                }
                is RecommendedServerIssue.UnsupportedServerVersion -> {
                    UiText.StringResource(SetupR.string.add_server_error_version, it.version)
                }
                is RecommendedServerIssue.SlowResponse -> {
                    UiText.StringResource(SetupR.string.add_server_error_slow, it.responseTime)
                }
                else -> {
                    UiText.StringResource(CoreR.string.unknown_error)
                }
            }
        }
    }

    override suspend fun loadDisclaimer(): String? {
        return jellyfinApi.brandingApi.getBrandingOptions().content.loginDisclaimer
    }

    override suspend fun login(username: String, password: String) {
        val authenticationResult by jellyfinApi.userApi.authenticateUserByName(
            data = AuthenticateUserByName(
                username = username,
                pw = password,
            ),
        )

        saveAuthenticationResult(authenticationResult)
    }

    override suspend fun loginWithSecret(secret: String) {
        val authenticationResult by jellyfinApi.userApi.authenticateWithQuickConnect(
            data = QuickConnectDto(secret = secret),
        )

        saveAuthenticationResult(authenticationResult)
    }

    private fun saveAuthenticationResult(authenticationResult: AuthenticationResult) {
        val user = User(
            id = authenticationResult.user!!.id,
            name = authenticationResult.user!!.name!!,
            serverId = authenticationResult.serverId!!,
            accessToken = authenticationResult.accessToken!!,
        )

        database.insertUser(user)
        database.updateServerCurrentUser(authenticationResult.serverId!!, user.id)

        jellyfinApi.apply {
            api.update(accessToken = authenticationResult.accessToken)
            userId = authenticationResult.user?.id
        }
    }
}
