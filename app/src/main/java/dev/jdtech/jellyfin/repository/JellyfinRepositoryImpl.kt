package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.*

class JellyfinRepositoryImpl(private val jellyfinApi: JellyfinApi) : JellyfinRepository {
    override suspend fun getItem(itemId: UUID): BaseItemDto {
        val item: BaseItemDto
        withContext(Dispatchers.IO) {
            item = jellyfinApi.userLibraryApi.getItem(jellyfinApi.userId!!, itemId).content
        }
        return item
    }

    override suspend fun getItems(parentId: UUID?): List<BaseItemDto> {
        val items: List<BaseItemDto>
        withContext(Dispatchers.IO) {
            items = jellyfinApi.itemsApi.getItems(
                jellyfinApi.userId!!,
                parentId = parentId
            ).content.items ?: listOf()
        }
        return items
    }

    override suspend fun getSeasons(seriesId: UUID): List<BaseItemDto> {
        val seasons: List<BaseItemDto>
        withContext(Dispatchers.IO) {
            seasons = jellyfinApi.showsApi.getSeasons(seriesId, jellyfinApi.userId!!).content.items
                ?: listOf()
        }
        return seasons
    }

    override suspend fun getNextUp(seriesId: UUID): List<BaseItemDto> {
        val nextUpItems: List<BaseItemDto>
        withContext(Dispatchers.IO) {
            nextUpItems = jellyfinApi.showsApi.getNextUp(
                jellyfinApi.userId!!,
                seriesId = seriesId.toString()
            ).content.items ?: listOf()
        }
        return nextUpItems
    }
}