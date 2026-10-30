package dev.jdtech.jellyfin.player.core.domain.models

import android.os.Parcelable
import java.util.UUID
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerItem(
    val name: String,
    val itemId: UUID,
    val mediaSourceId: String,
    val playbackPosition: Long,
    val mediaSourceUri: String = "",
    val parentIndexNumber: Int? = null,
    val indexNumber: Int? = null,
    val indexNumberEnd: Int? = null,
    val externalSubtitles: List<ExternalSubtitle> = emptyList(),
    val chapters: List<PlayerChapter> = emptyList(),
    val trickplayInfo: TrickplayInfo? = null,
    val playSessionId: String? = null,
    val isTranscoding: Boolean = false,
    val isDownloaded: Boolean = false,
    val videoBitrate: Int? = null,
    val videoCodec: String? = null,
    val audioStreams: List<PlayerStream> = emptyList(),
    val subtitleStreams: List<PlayerStream> = emptyList(),
    val audioStreamIndex: Int? = null,
    val subtitleStreamIndex: Int? = null,
) : Parcelable {
    // Subtitle streams in the order the player lists its text tracks
    val playerSubtitleStreams: List<PlayerStream>
        get() =
            if (isTranscoding) {
                subtitleStreams.filter { it.isExternal }
            } else {
                subtitleStreams.filter { it.isEmbedded } + subtitleStreams.filter { it.isExternal }
            }
}
