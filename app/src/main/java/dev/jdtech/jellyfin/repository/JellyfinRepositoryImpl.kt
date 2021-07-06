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
}