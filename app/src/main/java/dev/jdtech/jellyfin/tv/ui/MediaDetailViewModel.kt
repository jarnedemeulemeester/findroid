package dev.jdtech.jellyfin.tv.ui

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.models.ContentType.MOVIE
import org.jellyfin.sdk.model.api.BaseItemDto
import javax.inject.Inject

@HiltViewModel
internal class MediaDetailViewModel @Inject internal constructor() : ViewModel() {

    fun transformData(
        data: LiveData<BaseItemDto>,
        resources: Resources,
        transformed: (State) -> Unit
    ): LiveData<State> {
        return Transformations.map(data) { baseItemDto ->
            State(
                dto = baseItemDto,
                description = baseItemDto.overview.orEmpty(),
                year = baseItemDto.productionYear.toString(),
                officialRating = baseItemDto.officialRating.orEmpty(),
                communityRating = baseItemDto.communityRating.toString(),
                runtimeMinutes = String.format(
                    resources.getString(R.string.runtime_minutes),
                    baseItemDto.runTimeTicks?.div(600_000_000)
                ),
                genres = baseItemDto.genres?.joinToString(" / ").orEmpty(),
                trailerUrl = baseItemDto.remoteTrailers?.firstOrNull()?.url,
                isPlayed = baseItemDto.userData?.played == true,
                isFavorite = baseItemDto.userData?.isFavorite == true,
                media = if (baseItemDto.type == MOVIE.type) {
                    State.Movie(
                        title = baseItemDto.name.orEmpty()
                    )
                } else {
                    State.TvShow(
                        episode = baseItemDto.episodeTitle ?: baseItemDto.name.orEmpty(),
                        show = baseItemDto.seriesName.orEmpty()
                    )
                }
            ).also(transformed)
        }
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