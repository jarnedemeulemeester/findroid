package dev.jdtech.jellyfin.player.core.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerPerson(
    val name: String,
    /** Character name for actors, or the function (e.g. "Director"). */
    val role: String,
    /** Human-readable type: "Actor", "Director", "Writer", "Producer", etc. */
    val type: String,
    /** Jellyfin primary image URL for this person, or null if unavailable. */
    val imageUri: String? = null,
) : Parcelable
