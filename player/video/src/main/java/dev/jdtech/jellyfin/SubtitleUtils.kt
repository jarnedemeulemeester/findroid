package dev.jdtech.jellyfin

import androidx.media3.common.MimeTypes

public fun setSubtitlesMimeTypes(codec: String): String {
    return when (codec) {
        "subrip" -> MimeTypes.APPLICATION_SUBRIP
        "webvtt" -> MimeTypes.TEXT_VTT
        "ssa" -> MimeTypes.TEXT_SSA
        "pgs" -> MimeTypes.APPLICATION_PGS
        "ass" -> MimeTypes.TEXT_SSA
        "srt" -> MimeTypes.APPLICATION_SUBRIP
        "vtt" -> MimeTypes.TEXT_VTT
        "ttml" -> MimeTypes.APPLICATION_TTML
        "dfxp" -> MimeTypes.APPLICATION_TTML
        "stl" -> MimeTypes.APPLICATION_TTML
        "sbv" -> MimeTypes.APPLICATION_SUBRIP
        else -> MimeTypes.TEXT_UNKNOWN
    }
}
