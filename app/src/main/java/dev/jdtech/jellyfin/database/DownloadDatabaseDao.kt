package dev.jdtech.jellyfin.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.jdtech.jellyfin.models.DownloadItem
import java.util.*

@Dao
interface DownloadDatabaseDao {
    @Insert()
    fun insertItem(downloadItem: DownloadItem)

    @Query("select * from downloads where id = :id limit 1")
    fun loadItem(id: UUID): DownloadItem?

    @Query("select * from downloads")
    fun loadItems(): List<DownloadItem>

    @Query("delete from downloads where id = :id")
    fun deleteItem(id: UUID)

    @Query("update downloads set playbackPosition = :playbackPosition, playedPercentage = :playedPercentage where id = :id")
    fun updatePlaybackPosition(id: UUID, playbackPosition: Long, playedPercentage: Double)

    @Query("update downloads set downloadId = :downloadId where id = :id")
    fun updateDownloadId(id: UUID, downloadId: Long)
}