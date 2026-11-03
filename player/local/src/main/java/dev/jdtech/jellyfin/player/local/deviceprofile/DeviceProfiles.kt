package dev.jdtech.jellyfin.player.local.deviceprofile

import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaFormat
import dev.jdtech.jellyfin.settings.domain.models.PlayerBackend
import org.jellyfin.sdk.model.api.CodecType
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.deviceprofile.DeviceProfileBuilder
import org.jellyfin.sdk.model.deviceprofile.buildDeviceProfile

/** Device profiles describing what each player backend can play. */
internal object DeviceProfiles {
    private const val MAX_BITRATE = 1_000_000_000

    private const val MIME_VIDEO_VC1 = "video/wvc1"

    private val exoPlayerContainers =
        arrayOf(
            "3gp",
            "avi",
            "flv",
            "hls",
            "m4v",
            "mkv",
            "mov",
            "mp4",
            "mpeg",
            "mpegts",
            "ogg",
            "ogv",
            "ts",
            "webm",
        )

    private val exoPlayerVideoCodecs =
        listOf(
            "av1" to MediaFormat.MIMETYPE_VIDEO_AV1,
            "h264" to MediaFormat.MIMETYPE_VIDEO_AVC,
            "hevc" to MediaFormat.MIMETYPE_VIDEO_HEVC,
            "mpeg2video" to MediaFormat.MIMETYPE_VIDEO_MPEG2,
            "mpeg4" to MediaFormat.MIMETYPE_VIDEO_MPEG4,
            "vc1" to MIME_VIDEO_VC1,
            "vp8" to MediaFormat.MIMETYPE_VIDEO_VP8,
            "vp9" to MediaFormat.MIMETYPE_VIDEO_VP9,
        )

    private val exoPlayerAudioCodecs =
        arrayOf(
            "aac",
            "ac3",
            "alac",
            "dca",
            "dts",
            "eac3",
            "flac",
            "mlp",
            "mp2",
            "mp3",
            "opus",
            "pcm_alaw",
            "pcm_mulaw",
            "pcm_s16le",
            "pcm_s20le",
            "pcm_s24le",
            "truehd",
            "vorbis",
        )

    private val hlsAudioCodecs = arrayOf("aac", "ac3", "eac3", "mp3")

    private val textSubtitleFormats =
        arrayOf("ass", "srt", "ssa", "subrip", "ttml", "vtt", "webvtt")
    private val imageSubtitleFormats = arrayOf("dvbsub", "dvdsub", "pgssub")

    // ffprobe H.264 level, e.g. 4.1 is 41
    private val avcLevels =
        listOf(
            CodecProfileLevel.AVCLevel1 to 10,
            CodecProfileLevel.AVCLevel11 to 11,
            CodecProfileLevel.AVCLevel12 to 12,
            CodecProfileLevel.AVCLevel13 to 13,
            CodecProfileLevel.AVCLevel2 to 20,
            CodecProfileLevel.AVCLevel21 to 21,
            CodecProfileLevel.AVCLevel22 to 22,
            CodecProfileLevel.AVCLevel3 to 30,
            CodecProfileLevel.AVCLevel31 to 31,
            CodecProfileLevel.AVCLevel32 to 32,
            CodecProfileLevel.AVCLevel4 to 40,
            CodecProfileLevel.AVCLevel41 to 41,
            CodecProfileLevel.AVCLevel42 to 42,
            CodecProfileLevel.AVCLevel5 to 50,
            CodecProfileLevel.AVCLevel51 to 51,
            CodecProfileLevel.AVCLevel52 to 52,
            CodecProfileLevel.AVCLevel6 to 60,
            CodecProfileLevel.AVCLevel61 to 61,
            CodecProfileLevel.AVCLevel62 to 62,
        )

    // ffprobe HEVC level, e.g. 4.1 is 123
    private val hevcLevels =
        listOf(
            CodecProfileLevel.HEVCMainTierLevel1 to 30,
            CodecProfileLevel.HEVCMainTierLevel2 to 60,
            CodecProfileLevel.HEVCMainTierLevel21 to 63,
            CodecProfileLevel.HEVCMainTierLevel3 to 90,
            CodecProfileLevel.HEVCMainTierLevel31 to 93,
            CodecProfileLevel.HEVCMainTierLevel4 to 120,
            CodecProfileLevel.HEVCMainTierLevel41 to 123,
            CodecProfileLevel.HEVCMainTierLevel5 to 150,
            CodecProfileLevel.HEVCMainTierLevel51 to 153,
            CodecProfileLevel.HEVCMainTierLevel52 to 156,
            CodecProfileLevel.HEVCMainTierLevel6 to 180,
            CodecProfileLevel.HEVCMainTierLevel61 to 183,
            CodecProfileLevel.HEVCMainTierLevel62 to 186,
        )

    fun create(playerBackend: PlayerBackend): DeviceProfile =
        when (playerBackend) {
            PlayerBackend.EXOPLAYER -> exoPlayer(MediaCodecCapabilities())
            PlayerBackend.MPV -> mpv()
        }

    private fun mpv(): DeviceProfile = buildDeviceProfile {
        name = "Findroid mpv"
        maxStaticBitrate = MAX_BITRATE
        maxStreamingBitrate = MAX_BITRATE

        directPlayProfile { type = DlnaProfileType.VIDEO }

        hlsTranscodingProfile(videoCodecs = arrayOf("hevc", "h264"))

        subtitleProfiles(embedded = textSubtitleFormats + imageSubtitleFormats)
    }

    private fun exoPlayer(capabilities: MediaCodecCapabilities): DeviceProfile =
        buildDeviceProfile {
            name = "Findroid ExoPlayer"
            maxStaticBitrate = MAX_BITRATE
            maxStreamingBitrate = MAX_BITRATE

            val videoCodecs = exoPlayerVideoCodecs.filter { (_, mime) ->
                capabilities.supports(mime)
            }

            directPlayProfile {
                type = DlnaProfileType.VIDEO
                container(*exoPlayerContainers)
                videoCodec(*videoCodecs.map { (codec, _) -> codec }.toTypedArray())
                audioCodec(*exoPlayerAudioCodecs)
            }

            val hlsVideoCodecs =
                if (capabilities.supportsHardware(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                    arrayOf("hevc", "h264")
                } else {
                    arrayOf("h264")
                }
            hlsTranscodingProfile(videoCodecs = hlsVideoCodecs)

            for ((codec, mime) in videoCodecs) {
                val resolution = capabilities.maxResolution(mime) ?: continue
                codecProfile {
                    type = CodecType.VIDEO
                    this.codec = codec
                    conditions {
                        ProfileConditionValue.WIDTH lowerThanOrEquals resolution.width
                        ProfileConditionValue.HEIGHT lowerThanOrEquals resolution.height
                    }
                }
            }

            val mimeAvc = MediaFormat.MIMETYPE_VIDEO_AVC
            if (capabilities.supports(mimeAvc)) {
                val profiles = mutableListOf("high", "main", "baseline", "constrained baseline")
                if (capabilities.supportsProfile(mimeAvc, CodecProfileLevel.AVCProfileHigh10)) {
                    profiles.add("high 10")
                }
                val level =
                    avcLevels.toFfprobeLevel(
                        capabilities.maxLevel(mimeAvc, CodecProfileLevel.AVCProfileHigh)
                    )
                codecProfile {
                    type = CodecType.VIDEO
                    codec = "h264"
                    conditions { ProfileConditionValue.VIDEO_PROFILE inCollection profiles }
                }
                if (level != null)
                    videoLevelProfile(codec = "h264", maxLevel = level, highestLevel = 62)
            }

            val mimeHevc = MediaFormat.MIMETYPE_VIDEO_HEVC
            if (capabilities.supports(mimeHevc)) {
                val profiles = mutableListOf("main")
                if (capabilities.supportsProfile(mimeHevc, CodecProfileLevel.HEVCProfileMain10)) {
                    profiles.add("main 10")
                }
                val level =
                    hevcLevels.toFfprobeLevel(
                        capabilities.maxLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain)
                    )
                codecProfile {
                    type = CodecType.VIDEO
                    codec = "hevc"
                    conditions { ProfileConditionValue.VIDEO_PROFILE inCollection profiles }
                }
                if (level != null)
                    videoLevelProfile(codec = "hevc", maxLevel = level, highestLevel = 186)
            }

            // 10-bit AV1 is still "Main" profile
            val mimeAv1 = MediaFormat.MIMETYPE_VIDEO_AV1
            if (
                capabilities.supports(mimeAv1) &&
                    !capabilities.supportsProfile(mimeAv1, CodecProfileLevel.AV1ProfileMain10)
            ) {
                codecProfile {
                    type = CodecType.VIDEO
                    codec = "av1"
                    conditions { ProfileConditionValue.VIDEO_BIT_DEPTH lowerThanOrEquals 8 }
                }
            }

            subtitleProfiles(embedded = textSubtitleFormats + imageSubtitleFormats)
        }

    // Ignore out of range levels like 255
    private fun DeviceProfileBuilder.videoLevelProfile(
        codec: String,
        maxLevel: Int,
        highestLevel: Int,
    ) {
        codecProfile {
            type = CodecType.VIDEO
            this.codec = codec
            conditions { ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals maxLevel }
            applyConditions { ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals highestLevel }
        }
    }

    private fun List<Pair<Int, Int>>.toFfprobeLevel(codecLevel: Int): Int? =
        lastOrNull { (level, _) ->
            codecLevel >= level
        }
        ?.second

    private fun DeviceProfileBuilder.hlsTranscodingProfile(videoCodecs: Array<String>) {
        transcodingProfile {
            type = DlnaProfileType.VIDEO
            context = EncodingContext.STREAMING
            container = "ts"
            protocol = MediaStreamProtocol.HLS
            videoCodec(*videoCodecs)
            audioCodec(*hlsAudioCodecs)
        }
    }

    private fun DeviceProfileBuilder.subtitleProfiles(embedded: Array<String>) {
        for (format in embedded) subtitleProfile(format, SubtitleDeliveryMethod.EMBED)

        subtitleProfile("srt", SubtitleDeliveryMethod.EXTERNAL)
        subtitleProfile("ass", SubtitleDeliveryMethod.EXTERNAL)

        for (format in imageSubtitleFormats) {
            subtitleProfile(format, SubtitleDeliveryMethod.ENCODE)
        }
    }
}
