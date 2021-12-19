package dev.jdtech.jellyfin.tv.ui

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.models.ContentType.MOVIE
import org.jellyfin.sdk.model.api.BaseItemDto
import javax.inject.Inject

@HiltViewModel
internal class MediaDetailViewModel @Inject internal constructor() : ViewModel() {

    fun transformData(
        data: BaseItemDto,
        resources: Resources,
        transformed: (State) -> Unit
    ): State {
        return State(
                dto = data,
                description = data.overview.orEmpty(),
                year = data.productionYear.toString(),
                officialRating = data.officialRating.orEmpty(),
                communityRating = data.communityRating.toString(),
                runtimeMinutes = String.format(
                    resources.getString(R.string.runtime_minutes),
                    data.runTimeTicks?.div(600_000_000)
                ),
                genres = data.genres?.joinToString(" / ").orEmpty(),
                trailerUrl = data.remoteTrailers?.firstOrNull()?.url,
                isPlayed = data.userData?.played == true,
                isFavorite = data.userData?.isFavorite == true,
                media = if (data.type == MOVIE.type) {
                    State.Movie(
                        title = data.name.orEmpty()
                    )
                } else {
                    State.TvShow(
                        episode = data.episodeTitle ?: data.name.orEmpty(),
                        show = data.seriesName.orEmpty()
                    )
                }
            ).also(transformed)
    }

    data class State(
        val dto: BaseItemDto,
        val description: String,
        val year: String,
        val officialRating: String,
        val communityRating: String,
        val runtimeMinutes: String,
        val genres: String,
        val trailerUrl: String?,
        val isPlayed: Boolean,
        val isFavorite: Boolean,
        val media: Media
    ) {

        sealed class Media

        data class Movie(val title: String): Media()
        data class TvShow(val episode: String, val show: String): Media()
    }
}