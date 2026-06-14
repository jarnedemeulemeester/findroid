package dev.jdtech.jellyfin.core.presentation.downloader

import android.app.DownloadManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.models.toOfflineItemSnapshot
import dev.jdtech.jellyfin.offline.OfflineDownloadEnqueueResult
import dev.jdtech.jellyfin.offline.OfflineDownloadManager
import dev.jdtech.jellyfin.offline.download.OfflineAssetKind
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifest
import dev.jdtech.jellyfin.offline.download.OfflineProfile
import dev.jdtech.jellyfin.offline.download.OfflineStorageScope
import dev.jdtech.jellyfin.offline.storage.AllFilesAccessHelper
import dev.jdtech.jellyfin.repository.OfflinePackagePlanner
import dev.jdtech.jellyfin.repository.OfflinePackagePlanningResult
import dev.jdtech.jellyfin.repository.OfflinePackageRepository
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.Downloader
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DownloaderViewModel
@Inject
constructor(
    private val downloader: Downloader,
    private val jellyfinRepository: JellyfinRepository,
    private val offlinePackagePlanner: OfflinePackagePlanner,
    private val offlineDownloadManager: OfflineDownloadManager,
    private val offlinePackageRepository: OfflinePackageRepository,
    private val allFilesAccessHelper: AllFilesAccessHelper,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(DownloaderState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<DownloaderEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    var downloadId: Long? = null
    private var packageId: String? = null
    private var packageObserveJob: Job? = null
    private var successfulPackageId: String? = null
    private var batchPackageIds: List<String> = emptyList()
    private val pendingPermissionDownload = PendingPermissionAction<DownloaderAction.Download>()

    private val handler = Handler(Looper.getMainLooper())

    fun update(item: FindroidItem) {
        viewModelScope.launch {
            if (item is FindroidShow || item is FindroidSeason) {
                updateBatchItem(item)
                return@launch
            }

            val packageManifest =
                offlinePackageRepository.getPackagesByItemId(item.id.toString()).firstOrNull()
            if (packageManifest != null) {
                packageId = packageManifest.packageId
                observeOfflinePackage(packageManifest.packageId)
                return@launch
            }

            if (item.isDownloading()) {
                val source =
                    item.sources.firstOrNull { it.type == FindroidSourceType.LOCAL }
                        ?: return@launch
                this@DownloaderViewModel.downloadId = source.downloadId
                pollDownloadProgress(source.downloadId)
            }
        }
    }

    private fun download(item: FindroidItem, profile: OfflineProfile = OfflineProfile.Default480p) {
        viewModelScope.launch {
            if (item is FindroidShow || item is FindroidSeason) {
                downloadBatch(item, profile)
                return@launch
            }

            _state.emit(DownloaderState(status = DownloadManager.STATUS_PENDING))
            if (!allFilesAccessHelper.hasAccess()) {
                requestStoragePermission(item = item, profile = profile)
                return@launch
            }

            val source =
                item.sources.firstOrNull { it.type == FindroidSourceType.REMOTE }
                    ?: item.sources.firstOrNull()
                    ?: return@launch fail(OfflineDownloadFailure(OfflineDownloadFailureKind.MissingRequiredAsset))
            val serverId =
                appPreferences.getValue(appPreferences.currentServer)
                    ?: return@launch fail(OfflineDownloadFailure(OfflineDownloadFailureKind.MissingRequiredAsset))
            val nowMillis = System.currentTimeMillis()
            val manifest =
                when (
                    val planResult =
                        offlinePackagePlanner.planVideoPackage(
                            item = item,
                            sourceId = source.id,
                            profile = profile,
                        )
                ) {
                    is OfflinePackagePlanningResult.Success -> planResult.manifest
                    is OfflinePackagePlanningResult.Failure -> return@launch fail(planResult.failure)
                }

            when (
                val enqueueResult =
                    offlineDownloadManager.enqueueVideoPackage(
                        serverId = serverId,
                        manifest = manifest,
                        itemSnapshot =
                            item.toOfflineItemSnapshot(
                                packageId = manifest.packageId,
                                serverId = serverId,
                                nowMillis = nowMillis,
                            ),
                        nowMillis = nowMillis,
                    )
            ) {
                is OfflineDownloadEnqueueResult.Enqueued -> {
                    packageId = manifest.packageId
                    observeOfflinePackage(manifest.packageId)
                }
                is OfflineDownloadEnqueueResult.Failed -> fail(enqueueResult.failure)
            }
        }
    }

    private suspend fun downloadBatch(item: FindroidItem, profile: OfflineProfile) {
        _state.emit(DownloaderState(status = DownloadManager.STATUS_PENDING))
        if (!allFilesAccessHelper.hasAccess()) {
            requestStoragePermission(item = item, profile = profile)
            return
        }

        val serverId =
            appPreferences.getValue(appPreferences.currentServer)
                ?: return fail(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.MissingRequiredAsset)
                )
        val episodes = resolveDownloadEpisodes(item)
        if (episodes.isEmpty()) {
            return fail(OfflineDownloadFailure(OfflineDownloadFailureKind.MissingRequiredAsset))
        }

        val nowMillis = System.currentTimeMillis()
        val plannedPackages = mutableListOf<Pair<FindroidEpisode, OfflinePackageManifest>>()
        for (episode in episodes) {
            val downloadableEpisode = episode.withRemoteSources()
            val source =
                downloadableEpisode.sources.firstOrNull { it.type == FindroidSourceType.REMOTE }
                    ?: downloadableEpisode.sources.firstOrNull()
                    ?: return fail(
                        OfflineDownloadFailure(OfflineDownloadFailureKind.MissingRequiredAsset)
                    )
            val manifest =
                when (
                    val planResult =
                        offlinePackagePlanner.planVideoPackage(
                            item = downloadableEpisode,
                            sourceId = source.id,
                            profile = profile,
                        )
                ) {
                    is OfflinePackagePlanningResult.Success -> planResult.manifest
                    is OfflinePackagePlanningResult.Failure -> return fail(planResult.failure)
                }
            plannedPackages += downloadableEpisode to manifest
        }

        val enqueuedPackageIds = mutableListOf<String>()
        for ((episode, manifest) in plannedPackages) {
            when (
                val enqueueResult =
                    offlineDownloadManager.enqueueVideoPackage(
                        serverId = serverId,
                        manifest = manifest,
                        itemSnapshot =
                            episode.toOfflineItemSnapshot(
                                packageId = manifest.packageId,
                                serverId = serverId,
                                nowMillis = nowMillis,
                            ),
                        nowMillis = nowMillis,
                    )
            ) {
                is OfflineDownloadEnqueueResult.Enqueued -> enqueuedPackageIds += manifest.packageId
                is OfflineDownloadEnqueueResult.Failed -> {
                    enqueuedPackageIds.forEach { offlineDownloadManager.cancelVideoPackage(it) }
                    return fail(enqueueResult.failure)
                }
            }
        }

        batchPackageIds = enqueuedPackageIds
        packageId = enqueuedPackageIds.firstOrNull()
        observeBatchPackages(
            packageGroups = enqueuedPackageIds.map { BatchPackageGroup(listOf(it)) },
        )
        eventsChannel.trySend(DownloaderEvent.BatchQueued(enqueuedPackageIds.size))
    }

    private fun cancelDownload(item: FindroidItem) {
        viewModelScope.launch {
            // Stop progress polling
            handler.removeCallbacksAndMessages(null)
            packageObserveJob?.cancel()
            packageObserveJob = null

            if (item is FindroidShow || item is FindroidSeason) {
                val packageIds = resolveBatchPackageIds(item)
                packageIds.forEach { offlineDownloadManager.cancelVideoPackage(it) }
                batchPackageIds = emptyList()
                packageId = null
            } else {
                val packageIds = resolveItemPackageIds(item)
                if (packageIds.isNotEmpty()) {
                    packageIds.forEach { offlineDownloadManager.cancelVideoPackage(it) }
                    packageId = null
                } else {
                    downloadId?.let { downloader.cancelDownload(item = item, downloadId = it) }
                }
            }

            // Emit empty DownloadState
            _state.emit(DownloaderState())
        }
    }

    private suspend fun requestStoragePermission(item: FindroidItem, profile: OfflineProfile) {
        pendingPermissionDownload.remember(DownloaderAction.Download(item, profile))
        eventsChannel.send(
            DownloaderEvent.StoragePermissionRequired(
                intent = allFilesAccessHelper.settingsIntent(),
                fallbackIntent = allFilesAccessHelper.fallbackSettingsIntent(),
            )
        )
        fail(OfflineDownloadFailure(OfflineDownloadFailureKind.PermissionRequired))
    }

    fun onStoragePermissionResult() {
        viewModelScope.launch {
            when (val result = pendingPermissionDownload.consume(allFilesAccessHelper.hasAccess())) {
                PendingPermissionResult.None -> return@launch
                PendingPermissionResult.Denied ->
                    fail(OfflineDownloadFailure(OfflineDownloadFailureKind.PermissionRequired))
                is PendingPermissionResult.Resume -> onAction(result.action)
            }
        }
    }

    private fun deleteDownload(item: FindroidItem) {
        viewModelScope.launch {
            packageObserveJob?.cancel()
            packageObserveJob = null
            if (item is FindroidShow || item is FindroidSeason) {
                val packageIds = resolveBatchPackageIds(item)
                packageIds.forEach { offlineDownloadManager.deleteVideoPackage(it) }
                batchPackageIds = emptyList()
                packageId = null
            } else {
                val packageIds = resolveItemPackageIds(item)
                if (packageIds.isNotEmpty()) {
                    packageIds.forEach { offlineDownloadManager.deleteVideoPackage(it) }
                    packageId = null
                } else {
                    downloader.deleteItem(
                        item = item,
                        source = item.sources.first { it.type == FindroidSourceType.LOCAL },
                    )
                }
            }
            _state.emit(DownloaderState())
            eventsChannel.send(DownloaderEvent.Deleted)
        }
    }

    private suspend fun updateBatchItem(item: FindroidItem) {
        packageObserveJob?.cancel()
        packageObserveJob = null

        val episodes = resolveDownloadEpisodes(item)
        if (episodes.isEmpty()) {
            _state.emit(DownloaderState())
            return
        }

        val packageGroups =
            episodes.map { episode ->
                BatchPackageGroup(
                    packageIds =
                        offlinePackageRepository
                            .getPackagesByItemId(episode.id.toString())
                            .map { it.packageId },
                )
            }
        val resolvedPackageIds = packageGroups.flatMap { it.packageIds }.distinct()
        if (resolvedPackageIds.isEmpty()) {
            _state.emit(DownloaderState())
            return
        }

        batchPackageIds = resolvedPackageIds
        observeBatchPackages(packageGroups)
    }

    private fun observeBatchPackages(packageGroups: List<BatchPackageGroup>) {
        val packageIds = packageGroups.flatMap { it.packageIds }.distinct()
        if (packageIds.isEmpty()) return
        packageObserveJob?.cancel()
        packageObserveJob =
            viewModelScope.launch {
                val packageFlows = packageIds.map { offlinePackageRepository.observePackage(it) }
                combine(packageFlows) { manifests: Array<OfflinePackageManifest?> ->
                        manifests.filterNotNull()
                    }
                    .collect { manifests ->
                        val manifestById = manifests.associateBy { it.packageId }
                        val summary =
                            DownloaderBatchSummary(
                                readyEpisodes =
                                    packageGroups.count { group ->
                                        group.packageIds.any {
                                            manifestById[it]?.videoAssetStatus() ==
                                                OfflineAssetStatus.READY
                                        }
                                    },
                                totalEpisodes = packageGroups.size,
                                failedEpisodes =
                                    packageGroups.count { group ->
                                        group.packageIds.none {
                                            manifestById[it]?.videoAssetStatus() ==
                                                OfflineAssetStatus.READY
                                        } &&
                                            group.packageIds.any {
                                                manifestById[it]?.videoAssetStatus() ==
                                                    OfflineAssetStatus.FAILED_REQUIRED
                                            }
                                    },
                            )
                        val detailsText = summary.toDetailsText()
                        val activeManifest =
                            manifests.firstOrNull { it.videoAssetStatus() in BATCH_ACTIVE_STATUSES }
                        val failedManifest =
                            manifests.firstOrNull {
                                it.videoAssetStatus() == OfflineAssetStatus.FAILED_REQUIRED
                            }
                        val nextState =
                            when {
                                summary.isComplete ->
                                    DownloaderState(
                                        status = DownloadManager.STATUS_SUCCESSFUL,
                                        progress = 1f,
                                        detailsText = detailsText,
                                    )
                                activeManifest != null -> activeManifest.toDownloaderState(detailsText)
                                failedManifest != null -> failedManifest.toDownloaderState(detailsText)
                                else ->
                                    DownloaderState(
                                        status = DownloadManager.STATUS_PENDING,
                                        detailsText = detailsText,
                                    )
                            }

                        _state.emit(nextState)
                        if (summary.isComplete) {
                            val batchSuccessId = "batch:${packageIds.joinToString("|")}"
                            if (successfulPackageId != batchSuccessId) {
                                successfulPackageId = batchSuccessId
                                eventsChannel.trySend(DownloaderEvent.Successful)
                            }
                        }
                    }
            }
    }

    private suspend fun resolveBatchPackageIds(item: FindroidItem): List<String> {
        val resolvedPackageIds =
            batchPackageIds.ifEmpty {
                resolveDownloadEpisodes(item).flatMap { episode ->
                    offlinePackageRepository
                        .getPackagesByItemId(episode.id.toString())
                        .map { it.packageId }
                }
            }
        return resolvedPackageIds.distinct()
    }

    private suspend fun resolveItemPackageIds(item: FindroidItem): List<String> {
        packageId?.let { return listOf(it) }
        return offlinePackageRepository
            .getPackagesByItemId(item.id.toString())
            .map { it.packageId }
            .distinct()
    }

    private suspend fun resolveDownloadEpisodes(item: FindroidItem): List<FindroidEpisode> =
        when (item) {
            is FindroidEpisode -> listOf(item)
            is FindroidSeason ->
                jellyfinRepository.getEpisodes(
                    seriesId = item.seriesId,
                    seasonId = item.id,
                )
            is FindroidShow ->
                jellyfinRepository.getSeasons(item.id).flatMap { season ->
                    jellyfinRepository.getEpisodes(
                        seriesId = item.id,
                        seasonId = season.id,
                    )
                }
            else -> emptyList()
        }.filterNot { it.missing }

    private suspend fun FindroidEpisode.withRemoteSources(): FindroidEpisode =
        if (sources.any { it.type == FindroidSourceType.REMOTE }) {
            this
        } else {
            jellyfinRepository.getEpisode(id)
        }

    private fun pollDownloadProgress(downloadId: Long?) {
        handler.removeCallbacksAndMessages(null)
        val downloadProgressRunnable =
            object : Runnable {
                override fun run() {
                    viewModelScope.launch {
                        val (status, progress) = downloader.getProgress(downloadId)
                        _state.emit(
                            DownloaderState(
                                status = status,
                                progress = progress.coerceAtLeast(0) / 100f,
                            )
                        )
                    }

                    if (_state.value.status == DownloadManager.STATUS_SUCCESSFUL) {
                        eventsChannel.trySend(DownloaderEvent.Successful)
                    }

                    if (_state.value.isDownloading) {
                        handler.postDelayed(this, 1000L)
                    }
                }
            }
        handler.post(downloadProgressRunnable)
    }

    private fun observeOfflinePackage(packageId: String) {
        packageObserveJob?.cancel()
        packageObserveJob =
            viewModelScope.launch {
                offlinePackageRepository.observePackage(packageId).collect { manifest ->
                    if (manifest == null) {
                        _state.emit(DownloaderState())
                        return@collect
                    }
                    _state.emit(manifest.toDownloaderState())
                    if (
                        manifest.videoAssetStatus() == OfflineAssetStatus.READY &&
                            successfulPackageId != manifest.packageId
                    ) {
                        successfulPackageId = manifest.packageId
                        eventsChannel.trySend(DownloaderEvent.Successful)
                    }
                }
            }
    }

    private suspend fun fail(failure: OfflineDownloadFailure) {
        _state.emit(
            DownloaderState(
                status = DownloadManager.STATUS_FAILED,
                errorText = failure.toDownloaderErrorText(),
            )
        )
    }

    private fun OfflinePackageManifest.toDownloaderState(detailsText: UiText? = null): DownloaderState {
        val videoAsset =
            assets.firstOrNull {
                it.kind == OfflineAssetKind.VIDEO && it.storageScope == OfflineStorageScope.PUBLIC_MEDIA
            }
        return when (videoAsset?.status) {
            OfflineAssetStatus.PLANNED,
            OfflineAssetStatus.QUEUED ->
                DownloaderState(
                    status = DownloadManager.STATUS_PENDING,
                    detailsText = detailsText,
                )
            OfflineAssetStatus.DOWNLOADING,
            OfflineAssetStatus.VERIFYING -> {
                DownloaderState(
                    status = DownloadManager.STATUS_RUNNING,
                    progress = 0f,
                    detailsText = detailsText,
                )
            }
            OfflineAssetStatus.RETRY_WAIT ->
                DownloaderState(
                    status = DownloadManager.STATUS_PAUSED,
                    detailsText = detailsText,
                    errorText = videoAsset.failure?.toDownloaderErrorText(),
                )
            OfflineAssetStatus.READY ->
                DownloaderState(
                    status = DownloadManager.STATUS_SUCCESSFUL,
                    progress = 1f,
                    detailsText = detailsText,
                )
            OfflineAssetStatus.FAILED_OPTIONAL,
            OfflineAssetStatus.FAILED_REQUIRED ->
                DownloaderState(
                    status = DownloadManager.STATUS_FAILED,
                    detailsText = detailsText,
                    errorText =
                        videoAsset.failure?.toDownloaderErrorText()
                            ?: UiText.DynamicString("Download failed"),
                )
            OfflineAssetStatus.SKIPPED_NOT_AVAILABLE ->
                DownloaderState(detailsText = detailsText)
            null -> DownloaderState()
        }
    }

    private fun OfflinePackageManifest.videoAssetStatus(): OfflineAssetStatus? =
        assets
            .firstOrNull {
                it.kind == OfflineAssetKind.VIDEO && it.storageScope == OfflineStorageScope.PUBLIC_MEDIA
            }
            ?.status

    fun onAction(action: DownloaderAction) {
        when (action) {
            is DownloaderAction.Download -> download(action.item, action.profile)
            is DownloaderAction.DeleteDownload -> deleteDownload(action.item)
            is DownloaderAction.CancelDownload -> cancelDownload(action.item)
        }
    }

    private companion object {
        val BATCH_ACTIVE_STATUSES =
            setOf(
                OfflineAssetStatus.PLANNED,
                OfflineAssetStatus.QUEUED,
                OfflineAssetStatus.DOWNLOADING,
                OfflineAssetStatus.VERIFYING,
                OfflineAssetStatus.RETRY_WAIT,
            )
    }

    private data class BatchPackageGroup(val packageIds: List<String>)

    override fun onCleared() {
        super.onCleared()
        packageObserveJob?.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}
