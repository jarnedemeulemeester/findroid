package dev.jdtech.jellyfin.offline.download

data class OfflinePackageManifest(
    val packageId: String,
    val itemId: String,
    val mediaSourceId: String,
    val profile: OfflineProfile,
    val projectedPath: ProjectedPath,
    val assets: List<OfflineAsset>,
) {
    val readiness: OfflinePackageReadiness
        get() = OfflinePackageReadinessEvaluator.evaluate(assets)

    fun assets(kind: OfflineAssetKind): List<OfflineAsset> = assets.filter { it.kind == kind }

    fun failedAssets(): List<OfflineAsset> = assets.filter { it.isFailed }
}

enum class OfflinePackageReadiness {
    NOT_READY,
    PLAYABLE_READY,
    PACKAGE_READY,
    FULLY_ENRICHED,
}

object OfflinePackageReadinessEvaluator {
    fun evaluate(assets: List<OfflineAsset>): OfflinePackageReadiness {
        val playbackRequired = assets.filterRequired(OfflineAssetRequiredness.PLAYBACK_REQUIRED)
        if (playbackRequired.isEmpty() || playbackRequired.any { !it.isSatisfied }) {
            return OfflinePackageReadiness.NOT_READY
        }

        val packageRequired = assets.filterRequired(OfflineAssetRequiredness.PACKAGE_REQUIRED)
        if (packageRequired.any { !it.isSatisfied }) {
            return OfflinePackageReadiness.PLAYABLE_READY
        }

        val optionalAssets = assets.filterRequired(OfflineAssetRequiredness.OPTIONAL)
        if (optionalAssets.all { it.isSatisfied }) {
            return OfflinePackageReadiness.FULLY_ENRICHED
        }

        return OfflinePackageReadiness.PACKAGE_READY
    }

    private fun List<OfflineAsset>.filterRequired(
        requiredness: OfflineAssetRequiredness
    ): List<OfflineAsset> = filter { it.requiredness == requiredness }
}
