package dev.jdtech.jellyfin.car

internal object FindroidCarContinueWatchingResolver {
    fun resolve(
        serverResumeItems: List<FindroidCarCatalogItem>,
        offlineItems: List<FindroidCarCatalogItem>,
        historyEntries: List<FindroidCarPlaybackHistory.Entry>,
        userDataByItemId: Map<String, FindroidCarUserDataOverlay>,
        maxItems: Int,
    ): List<FindroidCarCatalogItem> {
        val offlineByItemId = offlineItems.associateBy { it.itemId }
        val serverByItemId = serverResumeItems.associateBy { it.itemId }
        val historyByItemId = historyEntries.associateBy { it.item.itemId }

        val orderedIds =
            buildList {
                    addAll(historyEntries.map { it.item.itemId })
                    addAll(serverResumeItems.map { it.itemId })
                    addAll(offlineItems.map { it.itemId })
                }
                .distinct()

        return orderedIds
            .mapNotNull { itemId ->
                val serverItem = serverByItemId[itemId]
                val offlineItem = offlineByItemId[itemId]
                val historyItem = historyByItemId[itemId]?.item
                val baseItem = mergePlaybackSources(serverItem, offlineItem, historyItem)
                    ?: return@mapNotNull null
                val overlay = userDataByItemId[itemId]
                baseItem.applyCanonicalPlayback(overlay, hasOfflineCopy = offlineItem != null)
            }
            .filter { it.shouldShowInContinueWatching() }
            .take(maxItems)
    }

    private fun mergePlaybackSources(
        serverItem: FindroidCarCatalogItem?,
        offlineItem: FindroidCarCatalogItem?,
        historyItem: FindroidCarCatalogItem?,
    ): FindroidCarCatalogItem? {
        val base = serverItem ?: offlineItem ?: historyItem ?: return null
        val localVideoPath = offlineItem?.videoPath ?: historyItem?.videoPath
        return base.copy(
            packageId = offlineItem?.packageId ?: base.packageId,
            artworkPaths =
                base.artworkPaths.ifEmpty {
                    offlineItem?.artworkPaths?.ifEmpty { historyItem?.artworkPaths.orEmpty() }
                        .orEmpty()
                },
            videoPath = localVideoPath ?: base.videoPath,
            streamUrl = if (!localVideoPath.isNullOrBlank()) null else base.streamUrl,
        )
    }

    private fun FindroidCarCatalogItem.applyCanonicalPlayback(
        overlay: FindroidCarUserDataOverlay?,
        hasOfflineCopy: Boolean,
    ): FindroidCarCatalogItem {
        if (overlay == null) return this
        if (overlay.played) {
            return copy(played = true, playbackPositionTicks = 0)
        }

        val localUserDataWins = hasOfflineCopy || overlay.toBeSynced || playbackPositionTicks <= 0L
        if (!localUserDataWins) return this

        return copy(
            played = overlay.played,
            playbackPositionTicks = overlay.playbackPositionTicks,
        )
    }

    private fun FindroidCarCatalogItem.shouldShowInContinueWatching(): Boolean =
        !played && playbackPositionTicks > 0L
}

internal data class FindroidCarUserDataOverlay(
    val played: Boolean,
    val playbackPositionTicks: Long,
    val toBeSynced: Boolean,
)
