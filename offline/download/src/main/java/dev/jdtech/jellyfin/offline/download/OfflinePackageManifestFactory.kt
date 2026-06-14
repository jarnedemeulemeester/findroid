package dev.jdtech.jellyfin.offline.download

data class OfflinePackageManifestInput(
    val itemId: String,
    val mediaSourceId: String,
    val logicalDirectorySegments: List<String>,
    val baseFileName: String,
    val sourceExtension: String?,
    val profile: OfflineProfile = OfflineProfile.Default480p,
)

sealed interface OfflinePackageManifestFactoryResult {
    data class Success(val manifest: OfflinePackageManifest) : OfflinePackageManifestFactoryResult

    data class Failure(val failure: ProjectedPathFailure) : OfflinePackageManifestFactoryResult
}

class OfflinePackageManifestFactory(
    private val projectedPathResolver: ProjectedPathResolver = ProjectedPathResolver()
) {
    fun create(input: OfflinePackageManifestInput): OfflinePackageManifestFactoryResult {
        val projectedPath =
            when (
                val pathResult =
                    projectedPathResolver.resolve(
                        ProjectedPathInput(
                            logicalDirectorySegments = input.logicalDirectorySegments,
                            displayName = input.outputDisplayName(),
                        )
                    )
            ) {
                is ProjectedPathResult.Success -> pathResult.path
                is ProjectedPathResult.Failure ->
                    return OfflinePackageManifestFactoryResult.Failure(pathResult.failure)
            }
        val packageId = input.packageId()
        val videoAssetId = "$packageId.video"

        return OfflinePackageManifestFactoryResult.Success(
            OfflinePackageManifest(
                packageId = packageId,
                itemId = input.itemId,
                mediaSourceId = input.mediaSourceId,
                profile = input.profile,
                projectedPath = projectedPath,
                assets =
                    listOf(
                        OfflineAsset(
                            assetId = videoAssetId,
                            packageId = packageId,
                            kind = OfflineAssetKind.VIDEO,
                            ownerItemId = input.itemId,
                            sourceId = input.mediaSourceId,
                            profileId = input.profile.id,
                            mimeType = input.profile.videoMimeType(),
                            storageScope = OfflineStorageScope.PUBLIC_MEDIA,
                            requiredness = OfflineAssetRequiredness.PLAYBACK_REQUIRED,
                        )
                    ),
            )
        )
    }

    private fun OfflinePackageManifestInput.packageId(): String =
        listOf(itemId, mediaSourceId, profile.id).joinToString(".")

    private fun OfflinePackageManifestInput.outputDisplayName(): String {
        val stem = baseFileName.substringBeforeLast('.', baseFileName)
        val extension =
            if (profile.preserveOriginal) {
                sourceExtension?.trimStart('.') ?: baseFileName.substringAfterLast('.', "")
            } else {
                profile.container
            }
        return if (extension.isBlank()) stem else "$stem.$extension"
    }

    private fun OfflineProfile.videoMimeType(): String? =
        if (preserveOriginal) {
            null
        } else {
            when (container) {
                "mkv" -> "video/x-matroska"
                else -> "video/$container"
            }
        }
}
