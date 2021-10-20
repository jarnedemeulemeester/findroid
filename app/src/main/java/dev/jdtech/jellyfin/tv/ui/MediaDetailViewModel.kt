package dev.jdtech.jellyfin.tv.ui

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import dev.jdtech.jellyfin.R
import org.jellyfin.sdk.model.api.BaseItemDto

internal class MediaDetailViewModel: ViewModel() {

    fun transformData(data: LiveData<BaseItemDto>, resources: Resources, transformed: (State) -> Unit): LiveData<State> {
        return Transformations.map(data) { baseItemDto ->
            State(
                dto = baseItemDto,
                title = baseItemDto.name.orEmpty(),
                episodeTitle = baseItemDto.episodeTitle,
                description = baseItemDto.overview.orEmpty(),
                year = baseItemDto.productionYear.toString(),
                officialRating = baseItemDto.officialRating.orEmpty(),
                communityRating = baseItemDto.communityRating.toString(),
                runtimeMinutes = String.format(resources.getString(R.string.runtime_minutes),
                    baseItemDto.runTimeTicks?.div(600_000_000)
                ),
                genres = baseItemDto.genres?.joinToString(" / ").orEmpty(),
                trailerUrl = baseItemDto.remoteTrailers?.first()?.url,
                isPlayed = baseItemDto.userData?.played == true,
                isFavorite = baseItemDto.userData?.isFavorite == true
            ).also(transformed)
        }
    }

    data class State(
        val dto: BaseItemDto,
        val title: String,
        val episodeTitle: String? = null,
        val description: String,
        val year: String,
        val officialRating: String,
        val communityRating: String,
        val runtimeMinutes: String,
        val genres: String,
        val trailerUrl: String? = null,
        val isPlayed: Boolean,
        val isFavorite: Boolean
    )
}