package dev.jdtech.jellyfin.mpv

import androidx.media3.common.C

enum class TrackType(val type: String) {
    VIDEO("video"),
    AUDIO("audio"),
    SUBTITLE("sub"),
    ;

    companion object {
        fun fromMedia3TrackType(trackType: Int): TrackType {
            return when (trackType) {
                C.TRACK_TYPE_VIDEO -> VIDEO
                C.TRACK_TYPE_AUDIO -> AUDIO
                C.TRACK_TYPE_TEXT -> SUBTITLE
                else -> SUBTITLE
            }
        }
    }
}
