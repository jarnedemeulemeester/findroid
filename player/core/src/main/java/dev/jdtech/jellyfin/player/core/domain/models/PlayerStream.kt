package dev.jdtech.jellyfin.player.core.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Audio or subtitle stream of a media source, as listed by the server. [isEmbedded] streams are in
 * the media file, [isExternal] ones are loaded as external subtitles and [isBurnedIn] ones can only
 * be burned into the video.
 */
@Parcelize
data class PlayerStream(
    val index: Int,
    val title: String,
    val isEmbedded: Boolean = true,
    val isExternal: Boolean = false,
    val isBurnedIn: Boolean = false,
) : Parcelable
