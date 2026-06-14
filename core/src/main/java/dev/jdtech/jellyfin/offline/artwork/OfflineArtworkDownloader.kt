package dev.jdtech.jellyfin.offline.artwork

import android.content.Context
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.offline.download.OfflineAsset
import dev.jdtech.jellyfin.offline.download.OfflineAssetKind
import dev.jdtech.jellyfin.offline.download.OfflineAssetRequiredness
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureDisposition
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import dev.jdtech.jellyfin.offline.download.OfflineStorageScope
import dev.jdtech.jellyfin.offline.storage.DirectFileAssetResult
import dev.jdtech.jellyfin.offline.storage.DirectFileAssetStore
import dev.jdtech.jellyfin.repository.OfflinePackageRepository
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jellyfin.sdk.model.api.ImageFormat
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.request.GetItemImageByIndexRequest
import org.jellyfin.sdk.model.api.request.GetItemImageRequest

class OfflineArtworkDownloader
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val jellyfinApi: JellyfinApi,
    private val offlinePackageRepository: OfflinePackageRepository,
    private val directFileAssetStore: DirectFileAssetStore,
) {
    suspend fun downloadPackageArtwork(packageId: String) {
        val manifest = offlinePackageRepository.getPackage(packageId) ?: return
        manifest.assets
            .filter { it.isArtwork && it.status != OfflineAssetStatus.READY }
            .sortedBy { it.artworkDownloadPriority }
            .forEach { asset -> downloadAsset(packageId, asset) }
        refreshReadiness(packageId)
    }

    private suspend fun downloadAsset(packageId: String, asset: OfflineAsset) {
        val nowMillis = System.currentTimeMillis()
        offlinePackageRepository.setAssetState(
            asset = asset,
            status = OfflineAssetStatus.DOWNLOADING,
            failure = null,
            bytes = null,
            tempPath = asset.tempPath,
            finalPath = asset.finalPath,
            retryCount = asset.retryCount,
            nowMillis = nowMillis,
        )

        val result =
            try {
                val imageBytes =
                    withTimeoutOrNull(IMAGE_DOWNLOAD_TIMEOUT_MS) { downloadImageBytes(asset) }
                if (imageBytes == null) {
                    AssetWriteResult.Failure(
                        OfflineDownloadFailure(
                            OfflineDownloadFailureKind.ServerUnavailable,
                            "Timed out downloading artwork",
                        )
                    )
                } else if (!imageBytes.hasReadableImage()) {
                    AssetWriteResult.Failure(
                        OfflineDownloadFailure(OfflineDownloadFailureKind.IntegrityFailed)
                    )
                } else {
                    writeImageAsset(packageId, asset, imageBytes)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                AssetWriteResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.ProfileUnsupported, e.message)
                )
            } catch (e: Exception) {
                AssetWriteResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.ServerUnavailable, e.message)
                )
            }

        val completedAtMillis = System.currentTimeMillis()
        when (result) {
            is AssetWriteResult.Success ->
                offlinePackageRepository.setAssetState(
                    asset = asset,
                    status = OfflineAssetStatus.READY,
                    failure = null,
                    bytes = result.bytes,
                    tempPath = null,
                    finalPath = result.finalPath,
                    retryCount = asset.retryCount,
                    nowMillis = completedAtMillis,
                )
            is AssetWriteResult.Failure ->
                offlinePackageRepository.setAssetState(
                    asset = asset,
                    status = result.failure.toAssetFailureStatus(asset.requiredness),
                    failure = result.failure,
                    bytes = null,
                    tempPath = null,
                    finalPath = null,
                    retryCount = asset.retryCount + 1,
                    nowMillis = completedAtMillis,
                )
        }
    }

    private suspend fun downloadImageBytes(asset: OfflineAsset): ByteArray =
        withContext(Dispatchers.IO) {
            val ownerItemId = UUID.fromString(asset.ownerItemId)
            val imageType = ImageType.valueOf(asset.imageType ?: ImageType.PRIMARY.name)
            val imageIndex = asset.imageIndex
            val maxWidth = asset.maxImageWidth()
            val maxHeight = asset.maxImageHeight()
            if (imageIndex != null) {
                jellyfinApi.imageApi
                    .getItemImageByIndex(
                        GetItemImageByIndexRequest(
                            itemId = ownerItemId,
                            imageType = imageType,
                            imageIndex = imageIndex,
                            maxWidth = maxWidth,
                            maxHeight = maxHeight,
                            quality = IMAGE_QUALITY,
                            tag = asset.imageTag,
                            format = ImageFormat.JPG,
                        )
                    )
                    .content
            } else {
                jellyfinApi.imageApi
                    .getItemImage(
                        GetItemImageRequest(
                            itemId = ownerItemId,
                            imageType = imageType,
                            maxWidth = maxWidth,
                            maxHeight = maxHeight,
                            quality = IMAGE_QUALITY,
                            tag = asset.imageTag,
                            format = ImageFormat.JPG,
                        )
                    )
                    .content
            }
        }

    private suspend fun writeImageAsset(
        packageId: String,
        asset: OfflineAsset,
        imageBytes: ByteArray,
    ): AssetWriteResult =
        when (asset.storageScope) {
            OfflineStorageScope.APP_PRIVATE -> writePrivateImageAsset(asset, imageBytes)
            OfflineStorageScope.PUBLIC_MEDIA -> writePublicFolderPoster(packageId, asset, imageBytes)
            OfflineStorageScope.HIDDEN_WORK ->
                AssetWriteResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.MissingRequiredAsset)
                )
        }

    private suspend fun writePrivateImageAsset(
        asset: OfflineAsset,
        imageBytes: ByteArray,
    ): AssetWriteResult =
        withContext(Dispatchers.IO) {
            try {
                val finalFile = File(File(context.filesDir, "images/${asset.ownerItemId}"), asset.privateImageName())
                val tempFile = File(finalFile.parentFile, "${asset.privateImageName()}.${asset.assetId}.part")
                ensureDirectory(finalFile.parentFile)
                writeBytesAtomically(tempFile, finalFile, imageBytes)
                AssetWriteResult.Success(finalPath = finalFile.absolutePath, bytes = imageBytes.size.toLong())
            } catch (e: IOException) {
                AssetWriteResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.StorageRootUnavailable, e.message)
                )
            } catch (e: SecurityException) {
                AssetWriteResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.PermissionRequired, e.message)
                )
            }
        }

    private suspend fun writePublicFolderPoster(
        packageId: String,
        asset: OfflineAsset,
        imageBytes: ByteArray,
    ): AssetWriteResult =
        withContext(Dispatchers.IO) {
            val manifest =
                offlinePackageRepository.getPackage(packageId)
                    ?: return@withContext AssetWriteResult.Failure(
                        OfflineDownloadFailure(OfflineDownloadFailureKind.MissingRequiredAsset)
                    )
            directFileAssetStore.existingPublicSiblingAsset(manifest.projectedPath, FERMATA_COVER_NAME)?.let {
                return@withContext AssetWriteResult.Success(
                    finalPath = it.absolutePath,
                    bytes = it.length(),
                )
            }
            val prepared =
                when (
                    val prepareResult =
                        directFileAssetStore.preparePublicSiblingAsset(
                            packageId = packageId,
                            assetId = asset.assetId,
                            projectedPath = manifest.projectedPath,
                            displayName = FERMATA_COVER_NAME,
                            expectedBytes = imageBytes.size.toLong(),
                        )
                ) {
                    is DirectFileAssetResult.Success -> prepareResult.value
                    is DirectFileAssetResult.Failure -> {
                        return@withContext AssetWriteResult.Failure(prepareResult.failure)
                    }
                }
            try {
                writeBytes(prepared.tempFile, imageBytes)
            } catch (e: IOException) {
                return@withContext AssetWriteResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.StorageRootUnavailable, e.message)
                )
            }
            when (val publishResult = directFileAssetStore.publishPublicAsset(prepared)) {
                is DirectFileAssetResult.Success ->
                    AssetWriteResult.Success(
                        finalPath = publishResult.value.absolutePath,
                        bytes = imageBytes.size.toLong(),
                    )
                is DirectFileAssetResult.Failure -> AssetWriteResult.Failure(publishResult.failure)
            }
        }

    private suspend fun refreshReadiness(packageId: String) {
        val nowMillis = System.currentTimeMillis()
        val readiness = offlinePackageRepository.getPackage(packageId)?.readiness ?: return
        offlinePackageRepository.setPackageReadiness(packageId, readiness, nowMillis)
    }

    private fun ByteArray.hasReadableImage(): Boolean =
        isNotEmpty() &&
            BitmapFactory.Options()
                .also { options ->
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeByteArray(this, 0, size, options)
                }
                .let { it.outWidth > 0 && it.outHeight > 0 }

    private fun OfflineAsset.privateImageName(): String =
        when (imageType?.let(ImageType::valueOf)) {
            ImageType.BACKDROP -> "backdrop"
            ImageType.LOGO -> "logo"
            else -> "primary"
        }

    private fun OfflineAsset.maxImageWidth(): Int =
        when (kind) {
            OfflineAssetKind.BACKDROP -> BACKDROP_MAX_WIDTH
            else -> POSTER_MAX_WIDTH
        }

    private fun OfflineAsset.maxImageHeight(): Int? =
        when (kind) {
            OfflineAssetKind.BACKDROP -> BACKDROP_MAX_HEIGHT
            else -> null
        }

    private fun OfflineDownloadFailure.toAssetFailureStatus(
        requiredness: OfflineAssetRequiredness
    ): OfflineAssetStatus =
        if (requiredness == OfflineAssetRequiredness.OPTIONAL) {
            OfflineAssetStatus.FAILED_OPTIONAL
        } else if (disposition == OfflineDownloadFailureDisposition.Retryable) {
            OfflineAssetStatus.RETRY_WAIT
        } else {
            OfflineAssetStatus.FAILED_REQUIRED
        }

    private fun ensureDirectory(directory: File?) {
        if (directory == null) throw IOException("Missing directory")
        if (directory.isDirectory) return
        if (!directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Unable to create directory: ${directory.absolutePath}")
        }
    }

    private fun writeBytesAtomically(tempFile: File, finalFile: File, imageBytes: ByteArray) {
        writeBytes(tempFile, imageBytes)
        Files.move(
            tempFile.toPath(),
            finalFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    private fun writeBytes(file: File, bytes: ByteArray) {
        ensureDirectory(file.parentFile)
        FileOutputStream(file, false).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
    }

    private val OfflineAsset.isArtwork: Boolean
        get() =
            kind == OfflineAssetKind.POSTER_PRIMARY ||
            kind == OfflineAssetKind.BACKDROP ||
                kind == OfflineAssetKind.LOGO ||
                kind == OfflineAssetKind.SERIES_PRIMARY ||
                kind == OfflineAssetKind.SERIES_BACKDROP ||
                kind == OfflineAssetKind.SERIES_LOGO ||
                kind == OfflineAssetKind.SEASON_PRIMARY ||
                kind == OfflineAssetKind.PUBLIC_FOLDER_POSTER

    private val OfflineAsset.artworkDownloadPriority: Int
        get() =
            when {
                kind == OfflineAssetKind.PUBLIC_FOLDER_POSTER -> 0
                requiredness == OfflineAssetRequiredness.PACKAGE_REQUIRED -> 1
                else -> 2
            }

    private sealed interface AssetWriteResult {
        data class Success(val finalPath: String, val bytes: Long) : AssetWriteResult

        data class Failure(val failure: OfflineDownloadFailure) : AssetWriteResult
    }

    private companion object {
        const val FERMATA_COVER_NAME = "cover.jpg"
        const val IMAGE_QUALITY = 88
        const val POSTER_MAX_WIDTH = 600
        const val BACKDROP_MAX_WIDTH = 1280
        const val BACKDROP_MAX_HEIGHT = 720
        const val IMAGE_DOWNLOAD_TIMEOUT_MS = 30_000L
    }
}
