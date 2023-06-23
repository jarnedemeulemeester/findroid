package dev.jdtech.jellyfin.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.jdtech.jellyfin.models.FindroidEpisodeDto
import dev.jdtech.jellyfin.models.FindroidMediaStreamDto
import dev.jdtech.jellyfin.models.FindroidMovieDto
import dev.jdtech.jellyfin.models.FindroidSeasonDto
import dev.jdtech.jellyfin.models.FindroidShowDto
import dev.jdtech.jellyfin.models.FindroidSourceDto
import dev.jdtech.jellyfin.models.FindroidUserDataDto
import dev.jdtech.jellyfin.models.IntroDto
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.models.ServerWithAddresses
import dev.jdtech.jellyfin.models.ServerWithAddressesAndUsers
import dev.jdtech.jellyfin.models.ServerWithUsers
import dev.jdtech.jellyfin.models.TrickPlayManifestDto
import dev.jdtech.jellyfin.models.User
import java.util.UUID

@Dao
abstract class ServerDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertServer(server: Server)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertServerAddress(address: ServerAddress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertUser(user: User)

    @Update
    abstract fun update(server: Server)

    @Query("SELECT * FROM servers WHERE id = :id")
    abstract fun get(id: String): Server?

    @Query("SELECT * FROM users WHERE id = :id")
    abstract fun getUser(id: UUID): User?

    @Transaction
    @Query("SELECT * FROM servers WHERE id = :id")
    abstract fun getServerWithAddresses(id: String): ServerWithAddresses

    @Transaction
    @Query("SELECT * FROM servers WHERE id = :id")
    abstract fun getServerWithUsers(id: String): ServerWithUsers

    @Transaction
    @Query("SELECT * FROM servers WHERE id = :id")
    abstract fun getServerWithAddressesAndUsers(id: String): ServerWithAddressesAndUsers?

    @Query("DELETE FROM servers")
    abstract fun clear()

    @Query("SELECT * FROM servers")
    abstract fun getAllServers(): LiveData<List<Server>>

    @Query("SELECT * FROM servers")
    abstract fun getAllServersSync(): List<Server>

    @Query("SELECT COUNT(*) FROM servers")
    abstract fun getServersCount(): Int

    @Query("DELETE FROM servers WHERE id = :id")
    abstract fun delete(id: String)

    @Query("DELETE FROM users WHERE id = :id")
    abstract fun deleteUser(id: UUID)

    @Query("DELETE FROM serverAddresses WHERE id = :id")
    abstract fun deleteServerAddress(id: UUID)

    @Query("UPDATE servers SET currentUserId = :userId WHERE id = :serverId")
    abstract fun updateServerCurrentUser(serverId: String, userId: UUID)

    @Query("SELECT * FROM users WHERE id = (SELECT currentUserId FROM servers WHERE id = :serverId)")
    abstract fun getServerCurrentUser(serverId: String): User?

    @Query("SELECT * FROM serverAddresses WHERE id = (SELECT currentServerAddressId FROM servers WHERE id = :serverId)")
    abstract fun getServerCurrentAddress(serverId: String): ServerAddress?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertMovie(movie: FindroidMovieDto)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertSource(source: FindroidSourceDto)

    @Query("SELECT * FROM movies WHERE id = :id")
    abstract fun getMovie(id: UUID): FindroidMovieDto

    @Query("SELECT * FROM movies JOIN sources ON movies.id = sources.itemId ORDER BY movies.name ASC")
    abstract fun getMoviesAndSources(): Map<FindroidMovieDto, List<FindroidSourceDto>>

    @Query("SELECT * FROM sources WHERE itemId = :itemId")
    abstract fun getSources(itemId: UUID): List<FindroidSourceDto>

    @Query("SELECT * FROM sources WHERE downloadId = :downloadId")
    abstract fun getSourceByDownloadId(downloadId: Long): FindroidSourceDto?

    @Query("UPDATE sources SET downloadId = :downloadId WHERE id = :id")
    abstract fun setSourceDownloadId(id: String, downloadId: Long)

    @Query("UPDATE sources SET path = :path WHERE id = :id")
    abstract fun setSourcePath(id: String, path: String)

    @Query("DELETE FROM sources WHERE id = :id")
    abstract fun deleteSource(id: String)

    @Query("DELETE FROM movies WHERE id = :id")
    abstract fun deleteMovie(id: UUID)

    @Query("UPDATE userdata SET playbackPositionTicks = :playbackPositionTicks WHERE itemId = :itemId AND userid = :userId")
    abstract fun setPlaybackPositionTicks(itemId: UUID, userId: UUID, playbackPositionTicks: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertMediaStream(mediaStream: FindroidMediaStreamDto)

    @Query("SELECT * FROM mediastreams WHERE sourceId = :sourceId")
    abstract fun getMediaStreamsBySourceId(sourceId: String): List<FindroidMediaStreamDto>

    @Query("SELECT * FROM mediastreams WHERE downloadId = :downloadId")
    abstract fun getMediaStreamByDownloadId(downloadId: Long): FindroidMediaStreamDto?

    @Query("UPDATE mediastreams SET downloadId = :downloadId WHERE id = :id")
    abstract fun setMediaStreamDownloadId(id: UUID, downloadId: Long)

    @Query("UPDATE mediastreams SET path = :path WHERE id = :id")
    abstract fun setMediaStreamPath(id: UUID, path: String)

    @Query("DELETE FROM mediastreams WHERE id = :id")
    abstract fun deleteMediaStream(id: UUID)

    @Query("DELETE FROM mediastreams WHERE sourceId = :sourceId")
    abstract fun deleteMediaStreamsBySourceId(sourceId: String)

    @Query("UPDATE userdata SET played = :played WHERE userId = :userId AND itemId = :itemId")
    abstract fun setPlayed(userId: UUID, itemId: UUID, played: Boolean)

    @Query("UPDATE userdata SET favorite = :favorite WHERE userId = :userId AND itemId = :itemId")
    abstract fun setFavorite(userId: UUID, itemId: UUID, favorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertTrickPlayManifest(trickPlayManifestDto: TrickPlayManifestDto)

    @Query("SELECT * FROM trickPlayManifests WHERE itemId = :itemId")
    abstract fun getTrickPlayManifest(itemId: UUID): TrickPlayManifestDto?

    @Query("DELETE FROM trickPlayManifests WHERE itemId = :itemId")
    abstract fun deleteTrickPlayManifest(itemId: UUID)

    @Query("SELECT * FROM movies ORDER BY name ASC")
    abstract fun getMovies(): List<FindroidMovieDto>

    @Query("SELECT * FROM movies WHERE serverId = :serverId ORDER BY name ASC")
    abstract fun getMoviesByServerId(serverId: String): List<FindroidMovieDto>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertShow(show: FindroidShowDto)

    @Query("SELECT * FROM shows WHERE id = :id")
    abstract fun getShow(id: UUID): FindroidShowDto

    @Query("SELECT * FROM shows ORDER BY name ASC")
    abstract fun getShows(): List<FindroidShowDto>

    @Query("SELECT * FROM shows WHERE serverId = :serverId ORDER BY name ASC")
    abstract fun getShowsByServerId(serverId: String): List<FindroidShowDto>

    @Query("DELETE FROM shows WHERE id = :id")
    abstract fun deleteShow(id: UUID)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertSeason(show: FindroidSeasonDto)

    @Query("SELECT * FROM seasons WHERE id = :id")
    abstract fun getSeason(id: UUID): FindroidSeasonDto

    @Query("SELECT * FROM seasons WHERE seriesId = :seriesId ORDER BY indexNumber ASC")
    abstract fun getSeasonsByShowId(seriesId: UUID): List<FindroidSeasonDto>

    @Query("DELETE FROM seasons WHERE id = :id")
    abstract fun deleteSeason(id: UUID)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertEpisode(episode: FindroidEpisodeDto)

    @Query("SELECT * FROM episodes WHERE id = :id")
    abstract fun getEpisode(id: UUID): FindroidEpisodeDto

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY parentIndexNumber ASC, indexNumber ASC")
    abstract fun getEpisodesByShowId(seriesId: UUID): List<FindroidEpisodeDto>

    @Query("SELECT * FROM episodes WHERE seasonId = :seasonId ORDER BY indexNumber ASC")
    abstract fun getEpisodesBySeasonId(seasonId: UUID): List<FindroidEpisodeDto>

    @Query("SELECT * FROM episodes WHERE serverId = :serverId ORDER BY seriesName ASC, parentIndexNumber ASC, indexNumber ASC")
    abstract fun getEpisodesByServerId(serverId: String): List<FindroidEpisodeDto>

    @Query("SELECT episodes.id, episodes.serverId, episodes.seasonId, episodes.seriesId, episodes.name, episodes.seriesName, episodes.overview, episodes.indexNumber, episodes.indexNumberEnd, episodes.parentIndexNumber, episodes.runtimeTicks, episodes.premiereDate, episodes.communityRating FROM episodes INNER JOIN userdata ON episodes.id = userdata.itemId WHERE serverId = :serverId AND playbackPositionTicks > 0 ORDER BY episodes.parentIndexNumber ASC, episodes.indexNumber ASC")
    abstract fun getEpisodeResumeItems(serverId: String): List<FindroidEpisodeDto>

    @Query("DELETE FROM episodes WHERE id = :id")
    abstract fun deleteEpisode(id: UUID)

    @Query("DELETE FROM episodes WHERE seasonId = :seasonId")
    abstract fun deleteEpisodesBySeasonId(seasonId: UUID)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertIntro(intro: IntroDto)

    @Query("SELECT * FROM intros WHERE itemId = :itemId")
    abstract fun getIntro(itemId: UUID): IntroDto?

    @Query("DELETE FROM intros WHERE itemId = :itemId")
    abstract fun deleteIntro(itemId: UUID)

    @Query("SELECT * FROM seasons")
    abstract fun getSeasons(): List<FindroidSeasonDto>

    @Query("SELECT * FROM episodes")
    abstract fun getEpisodes(): List<FindroidEpisodeDto>

    @Query("SELECT * FROM userdata WHERE itemId = :itemId AND userId = :userId")
    abstract fun getUserData(itemId: UUID, userId: UUID): FindroidUserDataDto?

    @Transaction
    open fun getUserDataOrCreateNew(itemId: UUID, userId: UUID): FindroidUserDataDto {
        var userData = getUserData(itemId, userId)

        // Create user data when there is none
        if (userData == null) {
            userData = FindroidUserDataDto(
                userId = userId,
                itemId = itemId,
                played = false,
                favorite = false,
                playbackPositionTicks = 0L,
            )
            insertUserData(userData)
        }

        return userData
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertUserData(userData: FindroidUserDataDto)

    @Query("DELETE FROM userdata WHERE itemId = :itemId")
    abstract fun deleteUserData(itemId: UUID)

    @Query("SELECT * FROM userdata WHERE userId = :userId AND itemId = :itemId AND toBeSynced = 1")
    abstract fun getUserDataToBeSynced(userId: UUID, itemId: UUID): FindroidUserDataDto?

    @Query("UPDATE userdata SET toBeSynced = :toBeSynced WHERE itemId = :itemId AND userId = :userId")
    abstract fun setUserDataToBeSynced(userId: UUID, itemId: UUID, toBeSynced: Boolean)

    @Query("SELECT * FROM movies WHERE serverId = :serverId AND name LIKE '%' || :name || '%'")
    abstract fun searchMovies(serverId: String, name: String): List<FindroidMovieDto>

    @Query("SELECT * FROM shows WHERE serverId = :serverId AND name LIKE '%' || :name || '%'")
    abstract fun searchShows(serverId: String, name: String): List<FindroidShowDto>

    @Query("SELECT * FROM episodes WHERE serverId = :serverId AND name LIKE '%' || :name || '%'")
    abstract fun searchEpisodes(serverId: String, name: String): List<FindroidEpisodeDto>
}
