package dev.jdtech.jellyfin.offline.transfer

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import dev.jdtech.jellyfin.offline.storage.DirectFileAssetResult
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber

class OfflineVideoPostProcessor(
    private val markerScanner: Mp4MarkerScanner = Mp4MarkerScanner(),
) {
    suspend fun ensureSeekableMp4(file: File, label: String): DirectFileAssetResult<OfflineVideoPostProcessReport> =
        withContext(Dispatchers.IO) {
            try {
                val sourceMarkers = markerScanner.scan(file)
                if (!sourceMarkers.requiresMp4Remux) {
                    Timber.i(
                        "Offline video post-process skipped label=%s file=%s markers=%s",
                        label,
                        file.name,
                        sourceMarkers,
                    )
                    return@withContext DirectFileAssetResult.Success(
                        OfflineVideoPostProcessReport(remuxed = false, sourceMarkers = sourceMarkers)
                    )
                }

                val tempFile = File(file.parentFile, ".${file.name}.seekable-remux.tmp")
                if (tempFile.exists() && !tempFile.delete()) {
                    return@withContext DirectFileAssetResult.Failure(
                        OfflineDownloadFailure(
                            OfflineDownloadFailureKind.StorageRootUnavailable,
                            "Unable to delete stale remux temp file",
                        )
                    )
                }

                if (file.parentFile?.usableSpace?.let { it < file.length() + REMUX_FREE_SPACE_MARGIN_BYTES } == true) {
                    return@withContext DirectFileAssetResult.Failure(
                        OfflineDownloadFailure(
                            OfflineDownloadFailureKind.InsufficientSpace,
                            "Not enough free space to remux offline MP4",
                        )
                    )
                }

                val stats = remuxMp4ToSeekableFile(source = file, destination = tempFile)
                val remuxedMarkers = markerScanner.scan(tempFile)
                if (remuxedMarkers.hasMoof || !remuxedMarkers.hasSampleTable || !hasReadableDuration(tempFile)) {
                    tempFile.delete()
                    return@withContext DirectFileAssetResult.Failure(
                        OfflineDownloadFailure(
                            OfflineDownloadFailureKind.IntegrityFailed,
                            "Remuxed MP4 failed seekable marker verification",
                        )
                    )
                }

                replaceFile(source = tempFile, destination = file)
                Timber.i(
                    "Offline video post-process remuxed label=%s file=%s sourceBytes=%d outputBytes=%d tracks=%d samples=%d durationUs=%d sourceMarkers=%s outputMarkers=%s",
                    label,
                    file.name,
                    stats.sourceBytes,
                    file.length(),
                    stats.trackCount,
                    stats.sampleCount,
                    stats.durationUs,
                    sourceMarkers,
                    remuxedMarkers,
                )
                DirectFileAssetResult.Success(
                    OfflineVideoPostProcessReport(
                        remuxed = true,
                        sourceMarkers = sourceMarkers,
                        outputMarkers = remuxedMarkers,
                        stats = stats,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                DirectFileAssetResult.Failure(
                    OfflineDownloadFailure(
                        OfflineStorageOrIntegrityFailureMapper.fromIOException(e, file),
                        e.message,
                    )
                )
            } catch (e: RuntimeException) {
                DirectFileAssetResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.IntegrityFailed, e.message)
                )
            }
        }

    private suspend fun remuxMp4ToSeekableFile(
        source: File,
        destination: File,
    ): OfflineVideoRemuxStats {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        return try {
            extractor.setDataSource(source.absolutePath)
            val selectedTracks = extractor.selectedAvTracks()
            if (selectedTracks.isEmpty()) {
                throw IOException("No audio/video tracks available for remux")
            }

            muxer = MediaMuxer(destination.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            readRotationDegrees(source)?.let { muxer.setOrientationHint(it) }
            val outputTrackByInputTrack =
                selectedTracks.associateWith { trackIndex ->
                    muxer.addTrack(extractor.getTrackFormat(trackIndex))
                }
            selectedTracks.forEach { extractor.selectTrack(it) }
            muxer.start()
            muxerStarted = true

            val bufferSize = selectedTracks.maxOf { extractor.getTrackFormat(it).maxInputSize() }
            val buffer = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            var sampleCount = 0L
            var durationUs = 0L

            while (true) {
                currentCoroutineContext().ensureActive()
                val inputTrackIndex = extractor.sampleTrackIndex
                if (inputTrackIndex < 0) break
                val outputTrackIndex = outputTrackByInputTrack[inputTrackIndex]
                if (outputTrackIndex == null) {
                    extractor.advance()
                    continue
                }

                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs >= 0L) {
                    bufferInfo.set(0, sampleSize, sampleTimeUs, extractor.sampleFlags)
                    muxer.writeSampleData(outputTrackIndex, buffer, bufferInfo)
                    sampleCount++
                    if (sampleTimeUs > durationUs) durationUs = sampleTimeUs
                }
                extractor.advance()
            }

            if (sampleCount == 0L) {
                throw IOException("No media samples written during remux")
            }

            muxer.stop()
            muxerStarted = false
            OfflineVideoRemuxStats(
                sourceBytes = source.length(),
                outputBytes = destination.length(),
                trackCount = selectedTracks.size,
                sampleCount = sampleCount,
                durationUs = durationUs,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: RuntimeException) {
            throw IOException("Unable to remux offline MP4: ${e.message}", e)
        } finally {
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
            if (!destination.exists() || destination.length() == 0L) {
                destination.delete()
            }
        }
    }

    private fun MediaExtractor.selectedAvTracks(): List<Int> =
        (0 until trackCount).filter { trackIndex ->
            val mime = getTrackFormat(trackIndex).getString(MediaFormat.KEY_MIME)
            mime?.startsWith("video/") == true || mime?.startsWith("audio/") == true
        }

    private fun MediaFormat.maxInputSize(): Int {
        val declaredSize =
            if (containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                runCatching { getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) }.getOrDefault(0)
            } else {
                0
            }
        return declaredSize.coerceAtLeast(DEFAULT_REMUX_BUFFER_SIZE).coerceAtMost(MAX_REMUX_BUFFER_SIZE)
    }

    private fun readRotationDegrees(file: File): Int? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()
                ?.takeIf { it != 0 }
        } catch (_: RuntimeException) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun hasReadableDuration(file: File): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.let { it > 0L }
                ?: false
        } catch (_: RuntimeException) {
            false
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun replaceFile(source: File, destination: File) {
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    private companion object {
        const val DEFAULT_REMUX_BUFFER_SIZE = 4 * 1024 * 1024
        const val MAX_REMUX_BUFFER_SIZE = 64 * 1024 * 1024
        const val REMUX_FREE_SPACE_MARGIN_BYTES = 64L * 1024L * 1024L
    }
}

data class OfflineVideoPostProcessReport(
    val remuxed: Boolean,
    val sourceMarkers: Mp4MarkerScan,
    val outputMarkers: Mp4MarkerScan? = null,
    val stats: OfflineVideoRemuxStats? = null,
)

data class OfflineVideoRemuxStats(
    val sourceBytes: Long,
    val outputBytes: Long,
    val trackCount: Int,
    val sampleCount: Long,
    val durationUs: Long,
)

data class Mp4MarkerScan(
    val hasFtyp: Boolean,
    val hasMoof: Boolean,
    val hasSidx: Boolean,
    val hasSampleTable: Boolean,
    val bytesScanned: Long,
) {
    val requiresMp4Remux: Boolean
        get() = hasFtyp && hasMoof
}

class Mp4MarkerScanner(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
) {
    fun scan(file: File): Mp4MarkerScan {
        var hasFtyp = false
        var hasMoof = false
        var hasSidx = false
        var hasSampleTable = false
        var bytesScanned = 0L
        val buffer = ByteArray(bufferSize.coerceAtLeast(MARKER_OVERLAP + 1))
        var previousTail = ByteArray(0)

        FileInputStream(file).use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                val chunk = ByteArray(previousTail.size + read)
                previousTail.copyInto(chunk, destinationOffset = 0)
                buffer.copyInto(chunk, destinationOffset = previousTail.size, startIndex = 0, endIndex = read)

                hasFtyp = hasFtyp || chunk.containsAsciiMarker("ftyp")
                hasMoof = hasMoof || chunk.containsAsciiMarker("moof")
                hasSidx = hasSidx || chunk.containsAsciiMarker("sidx")
                hasSampleTable = hasSampleTable || chunk.containsAsciiMarker("stbl")
                bytesScanned += read

                val tailSize = minOf(MARKER_OVERLAP, chunk.size)
                previousTail = chunk.copyOfRange(chunk.size - tailSize, chunk.size)

                if (hasFtyp && hasMoof && hasSidx && hasSampleTable) break
            }
        }

        return Mp4MarkerScan(
            hasFtyp = hasFtyp,
            hasMoof = hasMoof,
            hasSidx = hasSidx,
            hasSampleTable = hasSampleTable,
            bytesScanned = bytesScanned,
        )
    }

    private fun ByteArray.containsAsciiMarker(marker: String): Boolean {
        val markerBytes = marker.toByteArray(StandardCharsets.US_ASCII)
        if (size < markerBytes.size) return false
        for (index in 0..size - markerBytes.size) {
            var matches = true
            for (markerIndex in markerBytes.indices) {
                if (this[index + markerIndex] != markerBytes[markerIndex]) {
                    matches = false
                    break
                }
            }
            if (matches) return true
        }
        return false
    }

    private companion object {
        const val DEFAULT_BUFFER_SIZE = 1024 * 1024
        const val MARKER_OVERLAP = 3
    }
}

private object OfflineStorageOrIntegrityFailureMapper {
    fun fromIOException(error: IOException, file: File): OfflineDownloadFailureKind =
        when {
            file.parentFile?.usableSpace?.let { it < 1L * 1024L * 1024L } == true ->
                OfflineDownloadFailureKind.InsufficientSpace
            error.message?.contains("No space left", ignoreCase = true) == true ->
                OfflineDownloadFailureKind.InsufficientSpace
            error.message?.contains("Permission denied", ignoreCase = true) == true ->
                OfflineDownloadFailureKind.PermissionRequired
            else -> OfflineDownloadFailureKind.IntegrityFailed
        }
}
