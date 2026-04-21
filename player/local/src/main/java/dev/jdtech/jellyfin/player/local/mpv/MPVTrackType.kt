package dev.jdtech.jellyfin.player.local.mpv

import android.os.Parcelable
import androidx.media3.common.C
import kotlinx.parcelize.Parcelize

@Parcelize
enum class MPVTrackType(val type: String) : Parcelable {
    VIDEO("video"),
    AUDIO("audio"),
    SUBTITLE("sub");

    companion object {
        fun fromMedia3TrackType(trackType: Int): MPVTrackType {
            return when (trackType) {
                C.TRACK_TYPE_VIDEO -> VIDEO
                C.TRACK_TYPE_AUDIO -> AUDIO
                C.TRACK_TYPE_TEXT -> SUBTITLE
                else -> SUBTITLE
            }
        }
    }
}
