package dev.jdtech.jellyfin.player.cast.devices

import org.jellyfin.sdk.model.api.CodecType
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.deviceprofile.buildDeviceProfile

object Chromecast {
    val deviceProfile = buildDeviceProfile {
        name = "Chromecast Video Profile (H.264)"

        maxStreamingBitrate = 12000000
        maxStaticBitrate = 12000000

        codecProfile {
            type = CodecType.VIDEO
            codec = "h264"
            conditions {
                lowerThanOrEquals(ProfileConditionValue.VIDEO_LEVEL, 41)
                notEquals(ProfileConditionValue.VIDEO_PROFILE, "high 10")
                lowerThanOrEquals(ProfileConditionValue.WIDTH, 1920)
                lowerThanOrEquals(ProfileConditionValue.HEIGHT, 1080)
                lowerThanOrEquals(ProfileConditionValue.VIDEO_FRAMERATE, 60)
            }
        }

        codecProfile {
            type = CodecType.AUDIO
            codec = "aac,mp3,flac,opus,vorbis,ac3,eac3"
            conditions {
                lowerThanOrEquals(ProfileConditionValue.AUDIO_CHANNELS, 6)
            }
        }

        directPlayProfile {
            container("mp4", "m4v")
            type = DlnaProfileType.VIDEO
            videoCodec("h264")
            audioCodec("aac", "mp3", "opus", "vorbis", "flac", "ac3", "eac3")
        }

        transcodingProfile {
            container = "ts"
            type = DlnaProfileType.VIDEO
            videoCodec("h264")
            audioCodec("aac", "mp3")
            protocol = MediaStreamProtocol.HLS
            context = EncodingContext.STREAMING
            minSegments = 2
            breakOnNonKeyFrames = true
            maxAudioChannels = "2"
        }

        subtitleProfile("vtt", SubtitleDeliveryMethod.EXTERNAL)
        subtitleProfile("vtt", SubtitleDeliveryMethod.EMBED)
    }
}