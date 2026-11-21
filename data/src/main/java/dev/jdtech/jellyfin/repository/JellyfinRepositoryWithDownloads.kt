package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.FindroidSource
import java.util.UUID
import kotlinx.coroutines.CancellationException
import org.jellyfin.sdk.model.api.ItemFields

/**
 * The online repository, except that a downloaded item is served from its local copy when the
 * server cannot be reached. Downloads then open and play without turning on offline mode first.
 */
class JellyfinRepositoryWithDownloads(
    private val online: JellyfinRepository,
    private val downloads: JellyfinRepository,
) : JellyfinRepository by online {

    override suspend fun getEpisode(itemId: UUID): FindroidEpisode =
        orDownloaded({ getEpisode(itemId) }) { getEpisode(itemId) }

    override suspend fun getMovie(itemId: UUID): FindroidMovie =
        orDownloaded({ getMovie(itemId) }) { getMovie(itemId) }

    override suspend fun getShow(itemId: UUID): FindroidShow =
        orDownloaded({ getShow(itemId) }) { getShow(itemId) }

    override suspend fun getSeason(itemId: UUID): FindroidSeason =
        orDownloaded({ getSeason(itemId) }) { getSeason(itemId) }

    override suspend fun getSeasons(seriesId: UUID, offline: Boolean): List<FindroidSeason> =
        orDownloaded({ getSeasons(seriesId, offline) }) { getSeasons(seriesId) }

    override suspend fun getNextUp(seriesId: UUID?): List<FindroidEpisode> =
        // Home keeps reporting its own error: only the next up of a single show falls back.
        if (seriesId == null) online.getNextUp()
        else orDownloaded({ getNextUp(seriesId) }) { getNextUp(seriesId) }

    override suspend fun getEpisodes(
        seriesId: UUID,
        seasonId: UUID,
        fields: List<ItemFields>?,
        startItemId: UUID?,
        limit: Int?,
        offline: Boolean,
    ): List<FindroidEpisode> =
        orDownloaded({ getEpisodes(seriesId, seasonId, fields, startItemId, limit, offline) }) {
            getEpisodes(seriesId, seasonId, fields, startItemId, limit)
        }

    override suspend fun getMediaSources(itemId: UUID, includePath: Boolean): List<FindroidSource> =
        orDownloaded({ getMediaSources(itemId, includePath) }) {
            getMediaSources(itemId, includePath)
        }

    private suspend fun <T> orDownloaded(
        fromServer: suspend JellyfinRepository.() -> T,
        fromDownloads: suspend JellyfinRepository.() -> T,
    ): T =
        try {
            online.fromServer()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            try {
                downloads.fromDownloads()
            } catch (_: Exception) {
                // Not downloaded: the caller gets the error from the server.
                throw e
            }
        }
}
