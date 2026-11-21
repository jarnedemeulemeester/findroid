package dev.jdtech.jellyfin.work

import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidItem
import java.util.UUID
import org.jellyfin.sdk.model.api.UpdateUserItemDataDto

/** Sends the user data changed while offline (played, position, favorite) to the server. */
internal suspend fun pushPendingUserData(
    database: ServerDatabaseDao,
    jellyfinApi: JellyfinApi,
    userId: UUID,
    items: List<FindroidItem>,
) {
    for (item in items) {
        val userData = database.getUserDataToBeSynced(userId, item.id) ?: continue

        try {
            jellyfinApi.userDataApi.updateItemUserData(
                itemId = item.id,
                userId = userId,
                data =
                    UpdateUserItemDataDto(
                        playbackPositionTicks = userData.playbackPositionTicks,
                        isFavorite = userData.favorite,
                        played = userData.played,
                    ),
            )

            database.setUserDataToBeSynced(userId, item.id, false)
        } catch (_: Exception) {}
    }
}
