package dev.jdtech.jellyfin.player.cast.devices

import org.jellyfin.sdk.model.api.CodecType
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.deviceprofile.buildDeviceProfile

object ChromecastH265 {
    val deviceProfile = buildDeviceProfile {
        name = "Chromecast Video Profile (HEVC)"

        maxStreamingBitrate = 120000000
        maxStaticBitrate = 120000000

        codecProfile {
            type = CodecType.VIDEO
            codec = "hevc,h264"
            conditions {
                lowerThanOrEquals(ProfileConditionValue.VIDEO_LEVEL, 153)
                notEquals(ProfileConditionValue.VIDEO_PROFILE, "high 10")
                lowerThanOrEquals(ProfileConditionValue.WIDTH, 3840)
                lowerThanOrEquals(ProfileConditionValue.HEIGHT, 2160)
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
            videoCodec("hevc", "h264")
            audioCodec("aac", "mp3", "opus", "vorbis", "flac", "ac3", "eac3")
        }

        transcodingProfile {
            container = "ts"
            type = DlnaProfileType.VIDEO
            videoCodec("hevc", "h264")
            audioCodec("aac", "mp3", "ac3", "eac3")
            protocol = MediaStreamProtocol.HLS
            context = EncodingContext.STREAMING
            minSegments = 2
            breakOnNonKeyFrames = true
        }

        subtitleProfile("vtt", SubtitleDeliveryMethod.EXTERNAL)
        subtitleProfile("vtt", SubtitleDeliveryMethod.EMBED)
    }
}
