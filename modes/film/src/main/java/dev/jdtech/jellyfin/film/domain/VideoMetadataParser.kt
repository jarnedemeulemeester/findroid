package dev.jdtech.jellyfin.film.domain

import dev.jdtech.jellyfin.models.AudioChannel
import dev.jdtech.jellyfin.models.AudioCodec
import dev.jdtech.jellyfin.models.DisplayProfile
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.Resolution
import dev.jdtech.jellyfin.models.VideoCodec
import dev.jdtech.jellyfin.models.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.VideoRangeType

class VideoMetadataParser {
    suspend fun parse(source: FindroidSource): VideoMetadata {
        val resolution = mutableListOf<Resolution>()
        val videoCodecs = mutableListOf<VideoCodec?>()
        val audioChannels = mutableListOf<AudioChannel>()
        val displayProfiles = mutableListOf<DisplayProfile>()
        val audioCodecs = mutableListOf<AudioCodec?>()
        val isAtmosAudio = mutableListOf<Boolean>()

        withContext(Dispatchers.Default) {
            source.mediaStreams.filter { stream ->
                when (stream.type) {
                    MediaStreamType.AUDIO -> {
                        /**
                         * Match audio profile from
                         * [org.jellyfin.sdk.model.api.MediaStream.channelLayout]
                         */
                        audioChannels.add(
                            when (stream.channelLayout) {
                                AudioChannel.CH_2_1.raw -> AudioChannel.CH_2_1
                                AudioChannel.CH_5_1.raw -> AudioChannel.CH_5_1
                                AudioChannel.CH_7_1.raw -> AudioChannel.CH_7_1
                                else -> AudioChannel.CH_2_0
                            }
                        )

                        /**
                         * Match [org.jellyfin.sdk.model.api.MediaStream.displayTitle] for Dolby
                         * Atmos
                         */
                        stream.displayTitle?.apply { isAtmosAudio.add(contains("ATMOS", true)) }

                        /** Match audio codec from [org.jellyfin.sdk.model.api.MediaStream.codec] */
                        audioCodecs.add(
                            when (stream.codec.lowercase()) {
                                AudioCodec.FLAC.toString() -> AudioCodec.FLAC
                                AudioCodec.AAC.toString() -> AudioCodec.AAC
                                AudioCodec.AC3.toString() -> AudioCodec.AC3
                                AudioCodec.EAC3.toString() -> AudioCodec.EAC3
                                AudioCodec.VORBIS.toString() -> AudioCodec.VORBIS
                                AudioCodec.OPUS.toString() -> AudioCodec.OPUS
                                AudioCodec.TRUEHD.toString() -> AudioCodec.TRUEHD
                                AudioCodec.DTS.toString() -> AudioCodec.DTS
                                else -> null
                            }
                        )
                        true
                    }

                    MediaStreamType.VIDEO -> {
                        with(stream) {
                            /**
                             * Match dynamic range from
                             * [org.jellyfin.sdk.model.api.MediaStream.videoRangeType]
                             */
                            when (videoRangeType) {
                                VideoRangeType.SDR -> DisplayProfile.SDR
                                VideoRangeType.HDR10 -> DisplayProfile.HDR10
                                VideoRangeType.HDR10_PLUS -> DisplayProfile.HDR10_PLUS
                                VideoRangeType.HLG -> DisplayProfile.HLG
                                VideoRangeType.DOVI,
                                VideoRangeType.DOVI_WITH_EL,
                                VideoRangeType.DOVI_WITH_ELHDR10_PLUS,
                                VideoRangeType.DOVI_WITH_HLG,
                                VideoRangeType.DOVI_WITH_SDR,
                                VideoRangeType.DOVI_WITH_HDR10,
                                VideoRangeType.DOVI_WITH_HDR10_PLUS -> DisplayProfile.DOLBY_VISION
                                else -> null
                            }?.let { displayProfiles.add(it) }

                            /**
                             * Force stream [org.jellyfin.sdk.model.api.MediaStream.height] and
                             * [org.jellyfin.sdk.model.api.MediaStream.width] as not null since we
                             * are inside [MediaStreamType.VIDEO] block
                             */
                            resolution.add(
                                when {
                                    height!! <= 1080 && width!! <= 1920 -> {
                                        Resolution.HD
                                    }

                                    height!! <= 2160 && width!! <= 3840 -> {
                                        Resolution.UHD
                                    }

                                    else -> Resolution.SD
                                }
                            )

                            videoCodecs.add(
                                when (stream.codec.lowercase()) {
                                    VideoCodec.H264.toString() -> VideoCodec.H264
                                    VideoCodec.HEVC.toString() -> VideoCodec.HEVC
                                    VideoCodec.VVC.toString() -> VideoCodec.VVC
                                    VideoCodec.AV1.toString() -> VideoCodec.AV1
                                    else -> null
                                }
                            )
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        return VideoMetadata(
            resolution = resolution,
            videoCodecs = videoCodecs.toSet().toList(),
            displayProfiles = displayProfiles.toSet().toList(),
            audioChannels = audioChannels.toSet().toList(),
            audioCodecs = audioCodecs.toSet().toList(),
            isAtmos = isAtmosAudio,
        )
    }
}
