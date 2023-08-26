package dev.jdtech.jellyfin.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.models.toFindroidEpisode
import dev.jdtech.jellyfin.models.toFindroidMovie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    val database: ServerDatabaseDao,
    val appPreferences: AppPreferences,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val jellyfinApi = JellyfinApi(
            androidContext = context.applicationContext,
            requestTimeout = appPreferences.requestTimeout,
            connectTimeout = appPreferences.connectTimeout,
            socketTimeout = appPreferences.socketTimeout,
        )

        return withContext(Dispatchers.IO) {
            val servers = database.getAllServersSync()

            for (server in servers) {
                val serverWithAddressesAndUsers = database.getServerWithAddressesAndUsers(server.id) ?: continue
                val serverAddress = serverWithAddressesAndUsers.addresses.firstOrNull { it.id == server.currentServerAddressId } ?: continue
                for (user in serverWithAddressesAndUsers.users) {
                    jellyfinApi.apply {
                        api.baseUrl = serverAddress.address
                        api.accessToken = user.accessToken
                        userId = user.id
                    }
                    val movies = database.getMoviesByServerId(server.id).map { it.toFindroidMovie(database, user.id) }
                    val episodes = database.getEpisodesByServerId(server.id).map { it.toFindroidEpisode(database, user.id) }

                    syncUserData(jellyfinApi, user, movies)
                    syncUserData(jellyfinApi, user, episodes)
                }
            }

            Result.success()
        }
    }

    private suspend fun syncUserData(
        jellyfinApi: JellyfinApi,
        user: User,
        items: List<FindroidItem>,
    ) {
        for (item in items) {
            val userData = database.getUserDataToBeSynced(user.id, item.id) ?: continue

            try {
                when (userData.played) {
                    true -> jellyfinApi.playStateApi.markPlayedItem(user.id, item.id)
                    false -> jellyfinApi.playStateApi.markUnplayedItem(user.id, item.id)
                }

                when (userData.favorite) {
                    true -> jellyfinApi.userLibraryApi.markFavoriteItem(user.id, item.id)
                    false -> jellyfinApi.userLibraryApi.unmarkFavoriteItem(user.id, item.id)
                }

                jellyfinApi.playStateApi.onPlaybackStopped(
                    userId = user.id,
                    itemId = item.id,
                    positionTicks = userData.playbackPositionTicks,
                )

                database.setUserDataToBeSynced(user.id, item.id, false)
            } catch (_: Exception) {}
        }
    }
}
