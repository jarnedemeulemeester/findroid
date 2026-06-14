package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import dev.jdtech.jellyfin.offline.download.OfflineAsset
import dev.jdtech.jellyfin.offline.download.OfflineAssetKind
import dev.jdtech.jellyfin.offline.download.OfflineAssetRequiredness
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflineStorageScope
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifest
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifestFactory
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifestFactoryResult
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifestInput
import dev.jdtech.jellyfin.offline.download.OfflineProfile
import dev.jdtech.jellyfin.offline.download.ProjectedPathFailureKind
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType

interface OfflinePackagePlanner {
    suspend fun planVideoPackage(
        item: FindroidItem,
        sourceId: String,
        profile: OfflineProfile = OfflineProfile.Default480p,
    ): OfflinePackagePlanningResult
}

sealed interface OfflinePackagePlanningResult {
    data class Success(val manifest: OfflinePackageManifest) : OfflinePackagePlanningResult

    data class Failure(val failure: OfflineDownloadFailure) : OfflinePackagePlanningResult
}

class JellyfinOfflinePackagePlanner(
    private val jellyfinApi: JellyfinApi,
    private val manifestFactory: OfflinePackageManifestFactory,
) : OfflinePackagePlanner {
    override suspend fun planVideoPackage(
        item: FindroidItem,
        sourceId: String,
        profile: OfflineProfile,
    ): OfflinePackagePlanningResult =
        withContext(Dispatchers.IO) {
            try {
                val source =
                    item.sources.firstOrNull { it.id == sourceId }
                        ?: return@withContext OfflinePackagePlanningResult.Failure(
                            OfflineDownloadFailure(OfflineDownloadFailureKind.MissingRequiredAsset)
                        )
                val ancestorItems =
                    jellyfinApi.libraryApi
                        .getAncestors(itemId = item.id, userId = jellyfinApi.userId!!)
                        .content
                val ancestors =
                    ancestorItems
                        .asReversed()
                        .mapNotNull { it.toLogicalDirectorySegment() }

                if (ancestors.isEmpty()) {
                    return@withContext OfflinePackagePlanningResult.Failure(
                        OfflineDownloadFailure(OfflineDownloadFailureKind.PathProjectionUnavailable)
                    )
                }

                when (
                    val result =
                        manifestFactory.create(
                            OfflinePackageManifestInput(
                                itemId = item.id.toString(),
                                mediaSourceId = source.id,
                                logicalDirectorySegments = ancestors,
                                baseFileName = item.offlineBaseFileName(),
                                sourceExtension = source.sourceExtension(),
                                profile = profile,
                            )
                        )
                ) {
                    is OfflinePackageManifestFactoryResult.Success ->
                        OfflinePackagePlanningResult.Success(
                            result.manifest.withArtworkAssets(
                                item = item,
                                ancestorItems = ancestorItems,
                            )
                        )
                    is OfflinePackageManifestFactoryResult.Failure ->
                        OfflinePackagePlanningResult.Failure(
                            OfflineDownloadFailure(result.failure.kind.toDownloadFailureKind())
                        )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                OfflinePackagePlanningResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.ProfileUnsupported, e.message)
                )
            } catch (e: Exception) {
                OfflinePackagePlanningResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.ServerUnavailable, e.message)
                )
            }
        }

    private fun dev.jdtech.jellyfin.models.FindroidSource.sourceExtension(): String? =
        path.substringAfterLast('.', missingDelimiterValue = "")
            .takeIf { it.isNotBlank() && it.length <= MAX_EXTENSION_LENGTH }

    private fun FindroidItem.offlineBaseFileName(): String =
        when (this) {
            is FindroidEpisode -> episodeBaseFileName()
            else -> name
        }

    private fun FindroidEpisode.episodeBaseFileName(): String {
        val episodePrefix =
            when {
                indexNumber <= 0 -> null
                indexNumberEnd != null && indexNumberEnd > indexNumber ->
                    "E%02d-E%02d".format(indexNumber, indexNumberEnd)
                else -> "E%02d".format(indexNumber)
            }
        if (episodePrefix == null || name.startsWithEpisodePrefix()) {
            return name
        }
        return "$episodePrefix - $name"
    }

    private fun String.startsWithEpisodePrefix(): Boolean =
        EPISODE_PREFIX_REGEX.matches(trimStart())

    private fun BaseItemDto.toLogicalDirectorySegment(): String? =
        when (type) {
            BaseItemKind.USER_ROOT_FOLDER -> null
            BaseItemKind.SEASON -> indexNumber?.toString()
            else -> name?.takeIf(String::isNotBlank)
        }

    private fun OfflinePackageManifest.withArtworkAssets(
        item: FindroidItem,
        ancestorItems: List<BaseItemDto>,
    ): OfflinePackageManifest {
        val assets = buildList {
            addAll(assets)
            addItemArtwork(item)
            addPublicFolderPoster(item, ancestorItems)
        }
        return copy(assets = assets.distinctBy { it.assetId })
    }

    private fun MutableList<OfflineAsset>.addItemArtwork(item: FindroidItem) {
        val itemPrimaryTag = item.images.primary?.getQueryParameter("tag")
        val itemBackdropTag = item.images.backdrop?.getQueryParameter("tag")
        val itemLogoTag = item.images.logo?.getQueryParameter("tag")

        if (item is FindroidEpisode) {
            item.images.showPrimary?.getQueryParameter("tag")?.let { tag ->
                addImageAsset(
                    kind = OfflineAssetKind.SERIES_PRIMARY,
                    ownerItemId = item.seriesId.toString(),
                    imageType = ImageType.PRIMARY,
                    imageTag = tag,
                    requiredness = OfflineAssetRequiredness.PACKAGE_REQUIRED,
                    storageScope = OfflineStorageScope.APP_PRIVATE,
                )
            }
            item.images.showBackdrop?.getQueryParameter("tag")?.let { tag ->
                addImageAsset(
                    kind = OfflineAssetKind.SERIES_BACKDROP,
                    ownerItemId = item.seriesId.toString(),
                    imageType = ImageType.BACKDROP,
                    imageIndex = 0,
                    imageTag = tag,
                    requiredness = OfflineAssetRequiredness.OPTIONAL,
                    storageScope = OfflineStorageScope.APP_PRIVATE,
                )
            }
            item.images.showLogo?.getQueryParameter("tag")?.let { tag ->
                addImageAsset(
                    kind = OfflineAssetKind.SERIES_LOGO,
                    ownerItemId = item.seriesId.toString(),
                    imageType = ImageType.LOGO,
                    imageTag = tag,
                    requiredness = OfflineAssetRequiredness.OPTIONAL,
                    storageScope = OfflineStorageScope.APP_PRIVATE,
                )
            }
        } else {
            itemPrimaryTag?.let { tag ->
                addImageAsset(
                    kind = OfflineAssetKind.POSTER_PRIMARY,
                    ownerItemId = item.id.toString(),
                    imageType = ImageType.PRIMARY,
                    imageTag = tag,
                    requiredness = OfflineAssetRequiredness.PACKAGE_REQUIRED,
                    storageScope = OfflineStorageScope.APP_PRIVATE,
                )
            }
        }
        itemLogoTag?.let { tag ->
            addImageAsset(
                kind = OfflineAssetKind.LOGO,
                ownerItemId = item.id.toString(),
                imageType = ImageType.LOGO,
                imageTag = tag,
                requiredness = OfflineAssetRequiredness.OPTIONAL,
                storageScope = OfflineStorageScope.APP_PRIVATE,
            )
        }

        itemPrimaryTag?.let { tag ->
            addImageAsset(
                kind = OfflineAssetKind.POSTER_PRIMARY,
                ownerItemId = item.id.toString(),
                imageType = ImageType.PRIMARY,
                imageTag = tag,
                requiredness = OfflineAssetRequiredness.OPTIONAL,
                storageScope = OfflineStorageScope.APP_PRIVATE,
            )
        }
        itemBackdropTag?.let { tag ->
            addImageAsset(
                kind = OfflineAssetKind.BACKDROP,
                ownerItemId = item.id.toString(),
                imageType = ImageType.BACKDROP,
                imageIndex = 0,
                imageTag = tag,
                requiredness = OfflineAssetRequiredness.OPTIONAL,
                storageScope = OfflineStorageScope.APP_PRIVATE,
            )
        }
    }

    private fun MutableList<OfflineAsset>.addPublicFolderPoster(
        item: FindroidItem,
        ancestorItems: List<BaseItemDto>,
    ) {
        val ancestorPoster =
            ancestorItems.firstNotNullOfOrNull { ancestor ->
                if (ancestor.type == BaseItemKind.USER_ROOT_FOLDER) {
                    null
                } else {
                    ancestor.imageTags?.get(ImageType.PRIMARY)?.let { tag ->
                    PlannedImage(ownerItemId = ancestor.id.toString(), imageTag = tag)
                }
                }
            }
        val fallbackPoster =
            when (item) {
                is FindroidEpisode ->
                    item.images.showPrimary?.getQueryParameter("tag")?.let { tag ->
                        PlannedImage(ownerItemId = item.seriesId.toString(), imageTag = tag)
                    }
                else -> null
            }
                ?: item.images.primary?.getQueryParameter("tag")?.let { tag ->
                    PlannedImage(ownerItemId = item.id.toString(), imageTag = tag)
                }
        val poster = ancestorPoster ?: fallbackPoster ?: return
        addImageAsset(
            kind = OfflineAssetKind.PUBLIC_FOLDER_POSTER,
            ownerItemId = poster.ownerItemId,
            imageType = ImageType.PRIMARY,
            imageTag = poster.imageTag,
            requiredness = OfflineAssetRequiredness.OPTIONAL,
            storageScope = OfflineStorageScope.PUBLIC_MEDIA,
        )
    }

    private fun MutableList<OfflineAsset>.addImageAsset(
        kind: OfflineAssetKind,
        ownerItemId: String,
        imageType: ImageType,
        imageTag: String,
        requiredness: OfflineAssetRequiredness,
        storageScope: OfflineStorageScope,
        imageIndex: Int? = null,
    ) {
        add(
            OfflineAsset(
                assetId =
                    listOf(
                            last().packageId,
                            kind.name.lowercase(),
                            ownerItemId,
                            imageType.name.lowercase(),
                            imageIndex?.toString(),
                        )
                        .filterNotNull()
                        .joinToString("."),
                packageId = last().packageId,
                kind = kind,
                ownerItemId = ownerItemId,
                sourceId = last().sourceId,
                profileId = last().profileId,
                imageType = imageType.name,
                imageIndex = imageIndex,
                imageTag = imageTag,
                mimeType = "image/jpeg",
                storageScope = storageScope,
                requiredness = requiredness,
                status = OfflineAssetStatus.PLANNED,
            )
        )
    }

    private data class PlannedImage(val ownerItemId: String, val imageTag: String)

    private fun ProjectedPathFailureKind.toDownloadFailureKind(): OfflineDownloadFailureKind =
        when (this) {
            ProjectedPathFailureKind.MissingLogicalDirectory ->
                OfflineDownloadFailureKind.PathProjectionUnavailable
            else -> OfflineDownloadFailureKind.InvalidProjectedPath
        }

    private companion object {
        const val MAX_EXTENSION_LENGTH = 8
        val EPISODE_PREFIX_REGEX = Regex("""^E\d{1,3}(?:-E\d{1,3})?\b.*""")
    }
}
