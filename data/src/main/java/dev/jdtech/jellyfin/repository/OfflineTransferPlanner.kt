package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import dev.jdtech.jellyfin.offline.download.OfflineProfile
import dev.jdtech.jellyfin.offline.download.OfflineTransferRequest
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod

interface OfflineTransferPlanner {
    suspend fun planVideoTransfer(
        itemId: String,
        mediaSourceId: String,
        profile: OfflineProfile,
    ): OfflineTransferPlanResult
}

data class OfflineTransferPlan(
    val request: OfflineTransferRequest,
    val expectedBytes: Long? = null,
)

sealed interface OfflineTransferPlanResult {
    data class Success(val plan: OfflineTransferPlan) : OfflineTransferPlanResult

    data class Failure(val failure: OfflineDownloadFailure) : OfflineTransferPlanResult
}

class JellyfinOfflineTransferPlanner(
    private val jellyfinApi: JellyfinApi,
) : OfflineTransferPlanner {
    override suspend fun planVideoTransfer(
        itemId: String,
        mediaSourceId: String,
        profile: OfflineProfile,
    ): OfflineTransferPlanResult =
        withContext(Dispatchers.IO) {
            try {
                val itemUuid = UUID.fromString(itemId)
                val url =
                    if (profile.preserveOriginal) {
                        jellyfinApi.videosApi.getVideoStreamUrl(
                            itemId = itemUuid,
                            static = true,
                            mediaSourceId = mediaSourceId,
                        )
                    } else {
                        jellyfinApi.videosApi.getVideoStreamUrl(
                            itemId = itemUuid,
                            container = profile.container,
                            static = false,
                            mediaSourceId = mediaSourceId,
                            audioCodec = profile.audioCodec,
                            enableAutoStreamCopy = false,
                            allowVideoStreamCopy = false,
                            allowAudioStreamCopy = false,
                            audioBitRate = profile.audioBitrateBitsPerSecond,
                            audioChannels = DEFAULT_AUDIO_CHANNELS,
                            maxAudioChannels = DEFAULT_AUDIO_CHANNELS,
                            maxHeight = profile.maxHeight,
                            videoBitRate = profile.videoBitrateBitsPerSecond,
                            subtitleMethod = SubtitleDeliveryMethod.DROP,
                            maxVideoBitDepth = DEFAULT_VIDEO_BIT_DEPTH,
                            requireAvc = profile.videoCodec == H264_CODEC,
                            deInterlace = true,
                            videoCodec = profile.videoCodec,
                            context = EncodingContext.STREAMING,
                            enableAudioVbrEncoding = false,
                        )
                    }

                if (url.isBlank()) {
                    OfflineTransferPlanResult.Failure(
                        OfflineDownloadFailure(OfflineDownloadFailureKind.ProfileUnsupported)
                    )
                } else {
                    OfflineTransferPlanResult.Success(
                        OfflineTransferPlan(request = OfflineTransferRequest(url = url))
                    )
                }
            } catch (e: IllegalArgumentException) {
                OfflineTransferPlanResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.ProfileUnsupported, e.message)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                OfflineTransferPlanResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.ServerUnavailable, e.message)
                )
            }
        }

    private companion object {
        const val H264_CODEC = "h264"
        const val DEFAULT_AUDIO_CHANNELS = 2
        const val DEFAULT_VIDEO_BIT_DEPTH = 8
    }
}
