package dev.jdtech.jellyfin.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.jdtech.jellyfin.models.FindroidMediaStreamDto
import dev.jdtech.jellyfin.models.FindroidMovieDto
import dev.jdtech.jellyfin.models.FindroidSourceDto
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.models.ServerWithAddresses
import dev.jdtech.jellyfin.models.ServerWithAddressesAndUsers
import dev.jdtech.jellyfin.models.ServerWithUsers
import dev.jdtech.jellyfin.models.User
import java.util.UUID

@Dao
interface ServerDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertServer(server: Server)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertServerAddress(address: ServerAddress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: User)

    @Update
    fun update(server: Server)

    @Query("select * from servers where id = :id")
    fun get(id: String): Server?

    @Query("select * from users where id = :id")
    fun getUser(id: UUID): User?

    @Transaction
    @Query("select * from servers where id = :id")
    fun getServerWithAddresses(id: String): ServerWithAddresses

    @Transaction
    @Query("select * from servers where id = :id")
    fun getServerWithUsers(id: String): ServerWithUsers

    @Transaction
    @Query("select * from servers where id = :id")
    fun getServerWithAddressesAndUsers(id: String): ServerWithAddressesAndUsers?

    @Query("delete from servers")
    fun clear()

    @Query("select * from servers")
    fun getAllServers(): LiveData<List<Server>>

    @Query("select * from servers")
    fun getAllServersSync(): List<Server>

    @Query("select count(*) from servers")
    fun getServersCount(): Int

    @Query("delete from servers where id = :id")
    fun delete(id: String)

    @Query("delete from users where id = :id")
    fun deleteUser(id: UUID)

    @Query("delete from serverAddresses where id = :id")
    fun deleteServerAddress(id: UUID)

    @Query("update servers set currentUserId = :userId where id = :serverId")
    fun updateServerCurrentUser(serverId: String, userId: UUID)

    @Query("select * from users where id = (select currentUserId from servers where id = :serverId)")
    fun getServerCurrentUser(serverId: String): User?

    @Query("select * from serverAddresses where id = (select currentServerAddressId from servers where id = :serverId)")
    fun getServerCurrentAddress(serverId: String): ServerAddress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMovie(movie: FindroidMovieDto)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSource(source: FindroidSourceDto)

    @Query("SELECT * FROM movies WHERE id = :id")
    fun getMovie(id: UUID): FindroidMovieDto

    @Query("SELECT * FROM movies JOIN sources ON movies.id = sources.itemId")
    fun getMoviesAndSources(): Map<FindroidMovieDto, List<FindroidSourceDto>>

    @Query("SELECT * FROM sources WHERE itemId = :itemId")
    fun getSources(itemId: UUID): List<FindroidSourceDto>

    @Query("SELECT * FROM sources WHERE downloadId = :downloadId")
    fun getSourceByDownloadId(downloadId: Long): FindroidSourceDto?

    @Query("UPDATE sources SET downloadId = :downloadId WHERE id = :id")
    fun setSourceDownloadId(id: String, downloadId: Long)

    @Query("UPDATE sources SET path = :path WHERE id = :id")
    fun setSourcePath(id: String, path: String)

    @Query("DELETE FROM sources WHERE id = :id")
    fun deleteSource(id: String)

    @Query("DELETE FROM movies WHERE id = :id")
    fun deleteMovie(id: UUID)

    @Query("SELECT * FROM movies WHERE serverId = :serverId AND playbackPositionTicks > 0")
    fun getResumeItems(serverId: String): List<FindroidMovieDto>

    @Query("UPDATE movies SET playbackPositionTicks = :playbackPositionTicks WHERE id = :itemId")
    fun setMoviePlaybackPositionTicks(itemId: UUID, playbackPositionTicks: Long)

    @Insert
    fun insertMediaStream(mediaStream: FindroidMediaStreamDto)

    @Query("SELECT * FROM mediastreams WHERE sourceId = :sourceId")
    fun getMediaStreamsBySourceId(sourceId: String): List<FindroidMediaStreamDto>

    @Query("SELECT * FROM mediastreams WHERE downloadId = :downloadId")
    fun getMediaStreamByDownloadId(downloadId: Long): FindroidMediaStreamDto?

    @Query("UPDATE mediastreams SET downloadId = :downloadId WHERE id = :id")
    fun setMediaStreamDownloadId(id: UUID, downloadId: Long)

    @Query("UPDATE mediastreams SET path = :path WHERE id = :id")
    fun setMediaStreamPath(id: UUID, path: String)

    @Query("DELETE FROM mediastreams WHERE id = :id")
    fun deleteMediaStream(id: UUID)

    @Query("DELETE FROM mediastreams WHERE sourceId = :sourceId")
    fun deleteMediaStreamsBySourceId(sourceId: String)

    @Query("UPDATE movies SET played = :played WHERE id = :id")
    fun setPlayed(id: UUID, played: Boolean)
}
