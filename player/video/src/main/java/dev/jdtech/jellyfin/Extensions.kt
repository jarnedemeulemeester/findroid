package dev.jdtech.jellyfin

import androidx.media3.common.Tracks
import java.util.Locale

fun List<Tracks.Group>.getTrackNames(): Array<String> {
    return this.map { group ->
        val nameParts: MutableList<String?> = mutableListOf()
        val format = group.mediaTrackGroup.getFormat(0)
        nameParts.run {
            add(format.label)
            add(format.language?.let { Locale(it.split("-").last()).displayLanguage })
            add(format.codecs)
            filterNotNull().joinToString(separator = " - ")
        }
    }.toTypedArray()
}
