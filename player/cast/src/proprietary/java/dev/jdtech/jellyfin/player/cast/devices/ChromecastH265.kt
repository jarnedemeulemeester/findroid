package dev.jdtech.jellyfin.player.cast.devices

import org.jellyfin.sdk.model.api.CodecType
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.deviceprofile.buildDeviceProfile

object ChromecastH265 {
    val deviceProfile = buildDeviceProfile {
        name = "Chromecast H.265 Video Profile"
        maxStreamingBitrate = 16000000
        maxStaticBitrate = 16000000
        musicStreamingTranscodingBitrate = 384000

        codecProfile {
            type = CodecType.VIDEO
            codec = "hevc,h264"
        }
        codecProfile {
            type = CodecType.AUDIO
            codec = "aac,mp3,flac,opus,vorbis,ac3,eac3"
        }

        directPlayProfile {
            container("mp4", "m4v")
            type = DlnaProfileType.VIDEO
            videoCodec("hevc", "h264")
            audioCodec("aac", "mp3", "opus", "vorbis", "flac", "ac3", "eac3")
        }
        directPlayProfile {
            container("mp3")
            type = DlnaProfileType.AUDIO
        }
        directPlayProfile {
            container("aac")
            type = DlnaProfileType.AUDIO
        }
        directPlayProfile {
            container("flac")
            type = DlnaProfileType.AUDIO
        }
        directPlayProfile {
            container("wav")
            type = DlnaProfileType.AUDIO
        }
        directPlayProfile {
            container("ogg")
            type = DlnaProfileType.AUDIO
        }

        transcodingProfile {
            container = "ts"
            type = DlnaProfileType.VIDEO
            videoCodec("hevc", "h264")
            audioCodec("aac", "mp3", "ac3", "eac3", "flac", "opus", "vorbis")
            protocol = MediaStreamProtocol.HLS
            context = EncodingContext.STREAMING
            minSegments = 2
            breakOnNonKeyFrames = true
        }
        transcodingProfile {
            container = "mp4"
            type = DlnaProfileType.VIDEO
            videoCodec("hevc", "h264")
            audioCodec("aac", "mp3", "ac3", "eac3", "flac", "opus", "vorbis")
            protocol = MediaStreamProtocol.HTTP
            context = EncodingContext.STREAMING
            minSegments = 2
        }
        transcodingProfile {
            container = "mp3"
            type = DlnaProfileType.AUDIO
            audioCodec("mp3")
            protocol = MediaStreamProtocol.HTTP
            context = EncodingContext.STREAMING
        }
        transcodingProfile {
            container = "aac"
            type = DlnaProfileType.AUDIO
            audioCodec("aac")
            protocol = MediaStreamProtocol.HTTP
            context = EncodingContext.STREAMING
        }

        subtitleProfile("vtt", SubtitleDeliveryMethod.EXTERNAL)
        subtitleProfile("subrip", SubtitleDeliveryMethod.EMBED)
        subtitleProfile("srt", SubtitleDeliveryMethod.EXTERNAL)
    }
}