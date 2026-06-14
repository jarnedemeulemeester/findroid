package dev.jdtech.jellyfin.offline.storage

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import dev.jdtech.jellyfin.offline.download.ProjectedPath
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

interface OfflineTempPackageCleaner {
    fun cleanupTempPackage(packageId: String): Boolean
}

class DirectFileAssetStore(
    private val context: Context,
) : OfflineTempPackageCleaner {
    fun hasRequiredStorageAccess(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

    fun publicRoot(): File =
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            PUBLIC_ROOT_NAME,
        )

    fun tempRoot(): File = File(publicRoot(), TEMP_ROOT_NAME)

    fun existingPublishedPublicAsset(projectedPath: ProjectedPath): File? {
        if (!hasRequiredStorageAccess()) return null
        val finalFile = publicAssetFile(projectedPath) ?: return null
        return finalFile.takeIf { it.isFile && it.length() > 0L }
    }

    fun existingPublicSiblingAsset(projectedPath: ProjectedPath, displayName: String): File? {
        if (!hasRequiredStorageAccess()) return null
        val finalFile = publicSiblingAssetFile(projectedPath, displayName) ?: return null
        return finalFile.takeIf { it.isFile && it.length() > 0L }
    }

    fun preparePublicAsset(
        packageId: String,
        assetId: String,
        projectedPath: ProjectedPath,
        expectedBytes: Long? = null,
    ): DirectFileAssetResult<PreparedPublicAsset> {
        if (!hasRequiredStorageAccess()) {
            return DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineDownloadFailureKind.PermissionRequired)
            )
        }
        validatePathToken(packageId)?.let { return DirectFileAssetResult.Failure(it) }
        validatePathToken(assetId)?.let { return DirectFileAssetResult.Failure(it) }

        return try {
            val tempFile =
                containedFile(
                    root = tempRoot(),
                    candidate = File(File(tempRoot(), packageId), "$assetId.part"),
                )
                    ?: return DirectFileAssetResult.Failure(
                        OfflineDownloadFailure(OfflineDownloadFailureKind.InvalidProjectedPath)
                    )
            val finalFile =
                publicAssetFile(projectedPath)
                    ?: return DirectFileAssetResult.Failure(
                        OfflineDownloadFailure(OfflineDownloadFailureKind.InvalidProjectedPath)
                    )
            if (finalFile.exists()) {
                return DirectFileAssetResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.CollisionWithForeignFile)
                )
            }
            ensureDirectory(tempFile.parentFile)
            ensureDirectory(finalFile.parentFile)
            if (expectedBytes != null && finalFile.parentFile?.usableSpace?.let { it < expectedBytes } == true) {
                return DirectFileAssetResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.InsufficientSpace)
                )
            }
            DirectFileAssetResult.Success(
                PreparedPublicAsset(
                    packageId = packageId,
                    assetId = assetId,
                    tempFile = tempFile,
                    finalFile = finalFile,
                )
            )
        } catch (e: SecurityException) {
            DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineDownloadFailureKind.PermissionRequired, e.message)
            )
        } catch (e: IOException) {
            DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineStorageFailureMapper.fromIOException(e), e.message)
            )
        }
    }

    fun preparePublicSiblingAsset(
        packageId: String,
        assetId: String,
        projectedPath: ProjectedPath,
        displayName: String,
        expectedBytes: Long? = null,
    ): DirectFileAssetResult<PreparedPublicAsset> {
        if (!hasRequiredStorageAccess()) {
            return DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineDownloadFailureKind.PermissionRequired)
            )
        }
        validatePathToken(packageId)?.let { return DirectFileAssetResult.Failure(it) }
        validatePathToken(assetId)?.let { return DirectFileAssetResult.Failure(it) }
        validatePathToken(displayName)?.let { return DirectFileAssetResult.Failure(it) }

        return try {
            val tempFile =
                containedFile(
                    root = tempRoot(),
                    candidate = File(File(tempRoot(), packageId), "$assetId.part"),
                )
                    ?: return DirectFileAssetResult.Failure(
                        OfflineDownloadFailure(OfflineDownloadFailureKind.InvalidProjectedPath)
                    )
            val finalFile =
                publicSiblingAssetFile(projectedPath, displayName)
                    ?: return DirectFileAssetResult.Failure(
                        OfflineDownloadFailure(OfflineDownloadFailureKind.InvalidProjectedPath)
                    )
            if (finalFile.exists()) {
                return DirectFileAssetResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.CollisionWithForeignFile)
                )
            }
            ensureDirectory(tempFile.parentFile)
            ensureDirectory(finalFile.parentFile)
            if (expectedBytes != null && finalFile.parentFile?.usableSpace?.let { it < expectedBytes } == true) {
                return DirectFileAssetResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.InsufficientSpace)
                )
            }
            DirectFileAssetResult.Success(
                PreparedPublicAsset(
                    packageId = packageId,
                    assetId = assetId,
                    tempFile = tempFile,
                    finalFile = finalFile,
                )
            )
        } catch (e: SecurityException) {
            DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineDownloadFailureKind.PermissionRequired, e.message)
            )
        } catch (e: IOException) {
            DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineStorageFailureMapper.fromIOException(e), e.message)
            )
        }
    }

    fun publishPublicAsset(asset: PreparedPublicAsset): DirectFileAssetResult<File> {
        if (!asset.tempFile.exists() || asset.tempFile.length() == 0L) {
            return DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineDownloadFailureKind.IntegrityFailed)
            )
        }
        if (asset.finalFile.exists()) {
            return DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineDownloadFailureKind.CollisionWithForeignFile)
            )
        }

        return try {
            ensureDirectory(asset.finalFile.parentFile)
            Files.move(
                asset.tempFile.toPath(),
                asset.finalFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
            )
            DirectFileAssetResult.Success(asset.finalFile)
        } catch (e: AtomicMoveNotSupportedException) {
            DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineDownloadFailureKind.PublishFailed, e.message)
            )
        } catch (e: IOException) {
            DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineDownloadFailureKind.PublishFailed, e.message)
            )
        } catch (e: SecurityException) {
            DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineDownloadFailureKind.PermissionRequired, e.message)
            )
        }
    }

    suspend fun scanPublicAsset(file: File): DirectFileAssetResult<File> {
        if (!file.exists() || file.length() == 0L) {
            return DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineDownloadFailureKind.IntegrityFailed)
            )
        }

        return suspendCancellableCoroutine { continuation ->
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null) { _, uri ->
                val result =
                    if (uri != null) {
                        DirectFileAssetResult.Success(file)
                    } else {
                        DirectFileAssetResult.Failure(
                            OfflineDownloadFailure(OfflineDownloadFailureKind.ScanFailed)
                        )
                    }
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }
        }
    }

    private fun ensureDirectory(directory: File?) {
        if (directory == null) {
            throw IOException("Missing directory")
        }
        if (directory.isDirectory) return
        if (!directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Unable to create directory: ${directory.absolutePath}")
        }
    }

    override fun cleanupTempPackage(packageId: String): Boolean {
        validatePathToken(packageId)?.let { return false }
        val packageTempRoot =
            containedFile(root = tempRoot(), candidate = File(tempRoot(), packageId)) ?: return false
        return packageTempRoot.deleteRecursively()
    }

    fun deletePublicAsset(path: String?): DirectFileAssetResult<Unit> {
        if (path.isNullOrBlank()) return DirectFileAssetResult.Success(Unit)
        return try {
            val file =
                containedFile(root = publicRoot(), candidate = File(path))
                    ?: return DirectFileAssetResult.Failure(
                        OfflineDownloadFailure(OfflineDownloadFailureKind.InvalidProjectedPath)
                    )
            if (!file.exists() || file.delete()) {
                DirectFileAssetResult.Success(Unit)
            } else {
                DirectFileAssetResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.StorageRootUnavailable)
                )
            }
        } catch (e: SecurityException) {
            DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineDownloadFailureKind.PermissionRequired, e.message)
            )
        }
    }

    fun deletePrivateAsset(path: String?): DirectFileAssetResult<Unit> {
        if (path.isNullOrBlank()) return DirectFileAssetResult.Success(Unit)
        return try {
            val file =
                containedFile(root = context.filesDir, candidate = File(path))
                    ?: return DirectFileAssetResult.Failure(
                        OfflineDownloadFailure(OfflineDownloadFailureKind.InvalidProjectedPath)
                    )
            if (!file.exists() || file.delete()) {
                DirectFileAssetResult.Success(Unit)
            } else {
                DirectFileAssetResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.StorageRootUnavailable)
                )
            }
        } catch (e: SecurityException) {
            DirectFileAssetResult.Failure(
                OfflineDownloadFailure(OfflineDownloadFailureKind.PermissionRequired, e.message)
            )
        }
    }

    private fun publicAssetFile(projectedPath: ProjectedPath): File? =
        containedFile(root = publicRoot(), candidate = File(publicRoot(), projectedPath.relativeFilePath))

    private fun publicSiblingAssetFile(projectedPath: ProjectedPath, displayName: String): File? =
        containedFile(
            root = publicRoot(),
            candidate = File(File(publicRoot(), projectedPath.relativeDirectory), displayName),
        )

    private fun containedFile(root: File, candidate: File): File? =
        try {
            val canonicalRoot = root.canonicalFile
            val canonicalCandidate = candidate.canonicalFile
            if (canonicalCandidate == canonicalRoot || !canonicalCandidate.isInside(canonicalRoot)) {
                null
            } else {
                canonicalCandidate
            }
        } catch (_: IOException) {
            null
        }

    private fun File.isInside(root: File): Boolean =
        path.startsWith(root.path + File.separator)

    private fun validatePathToken(token: String): OfflineDownloadFailure? =
        if (token.isBlank() || token.contains('/') || token.contains('\\')) {
            OfflineDownloadFailure(OfflineDownloadFailureKind.InvalidProjectedPath)
        } else {
            null
        }

    companion object {
        const val PUBLIC_ROOT_NAME = "Findroid"
        const val TEMP_ROOT_NAME = ".findroid_tmp"
    }
}

data class PreparedPublicAsset(
    val packageId: String,
    val assetId: String,
    val tempFile: File,
    val finalFile: File,
)

sealed interface DirectFileAssetResult<out T> {
    data class Success<T>(val value: T) : DirectFileAssetResult<T>

    data class Failure(val failure: OfflineDownloadFailure) : DirectFileAssetResult<Nothing>
}

object OfflineStorageFailureMapper {
    fun fromIOException(
        error: IOException,
        hasNoUsableSpace: Boolean = false,
    ): OfflineDownloadFailureKind {
        val messages = error.causeChainMessages()
        return when {
            hasNoUsableSpace ||
                messages.any { message ->
                    message.contains("ENOSPC", ignoreCase = true) ||
                        message.contains("No space left", ignoreCase = true)
                } ->
                OfflineDownloadFailureKind.InsufficientSpace
            messages.any { it.contains("Permission denied", ignoreCase = true) } ->
                OfflineDownloadFailureKind.PermissionRequired
            else -> OfflineDownloadFailureKind.StorageRootUnavailable
        }
    }

    private fun Throwable.causeChainMessages(): Sequence<String> =
        generateSequence(this as Throwable?) { it.cause }.map { it.message.orEmpty() }
}
