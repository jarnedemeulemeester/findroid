package dev.jdtech.jellyfin.player.local.domain

import android.os.Build
import androidx.media3.common.Tracks
import java.util.Locale

fun List<Tracks.Group>.getTrackNames(): Array<String> {
    return this.map { group ->
        val nameParts: MutableList<String?> = mutableListOf()
        val format = group.mediaTrackGroup.getFormat(0)
        nameParts.run {
            add(format.label)
            add(
                format.language?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                        Locale.of(it.split("-").last()).displayLanguage
                    } else {
                        @Suppress("DEPRECATION")
                        Locale(it.split("-").last()).displayLanguage
                    }
                },
            )
            add(format.codecs)
            filterNotNull().joinToString(separator = " - ")
        }
    }.toTypedArray()
}
