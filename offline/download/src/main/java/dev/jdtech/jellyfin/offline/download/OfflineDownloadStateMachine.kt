package dev.jdtech.jellyfin.offline.download

enum class OfflineDownloadStatus {
    PLANNED,
    WAITING_PERMISSION,
    WAITING_NETWORK,
    QUEUED,
    PREPARING,
    DOWNLOADING,
    VERIFYING,
    PUBLISHING,
    SCANNING,
    READY,
    RETRY_WAIT,
    PAUSED_USER,
    BLOCKED_USER_ACTION,
    FAILED_TERMINAL,
}

data class OfflineDownloadState(
    val status: OfflineDownloadStatus = OfflineDownloadStatus.PLANNED,
    val bytesDownloaded: Long = 0,
    val bytesExpected: Long? = null,
    val retryAttempt: Int = 0,
    val maxRetryAttempts: Int = 5,
    val failure: OfflineDownloadFailure? = null,
) {
    val isTerminal: Boolean
        get() =
            status == OfflineDownloadStatus.READY || status == OfflineDownloadStatus.FAILED_TERMINAL
}

sealed interface OfflineDownloadEvent {
    data object StartRequested : OfflineDownloadEvent

    data object PermissionMissing : OfflineDownloadEvent

    data object PermissionGranted : OfflineDownloadEvent

    data object NetworkLost : OfflineDownloadEvent

    data object NetworkAvailable : OfflineDownloadEvent

    data object WorkerStarted : OfflineDownloadEvent

    data object Prepared : OfflineDownloadEvent

    data class BytesReceived(val downloaded: Long, val expected: Long?) : OfflineDownloadEvent

    data object StreamCompleted : OfflineDownloadEvent

    data object VerifySucceeded : OfflineDownloadEvent

    data class TransferFailed(val failure: OfflineDownloadFailure) : OfflineDownloadEvent

    data class VerifyFailed(val failure: OfflineDownloadFailure) : OfflineDownloadEvent

    data class PublishFailed(
        val failure: OfflineDownloadFailure,
        val finalFileIsValid: Boolean = false,
    ) : OfflineDownloadEvent

    data object PublishSucceeded : OfflineDownloadEvent

    data class ScanFailed(val failure: OfflineDownloadFailure) : OfflineDownloadEvent

    data object ScanSucceeded : OfflineDownloadEvent

    data object RetryTimerFired : OfflineDownloadEvent

    data object UserPaused : OfflineDownloadEvent

    data object UserCanceled : OfflineDownloadEvent

    data object UserActionResolved : OfflineDownloadEvent

    data object AppRestarted : OfflineDownloadEvent
}

sealed interface OfflineDownloadEffect {
    data object RequestStoragePermission : OfflineDownloadEffect

    data object WaitForNetwork : OfflineDownloadEffect

    data object AcquireSerialDownloadSlot : OfflineDownloadEffect

    data object PrepareStaging : OfflineDownloadEffect

    data object StartOrResumeTransfer : OfflineDownloadEffect

    data object VerifyStagedAsset : OfflineDownloadEffect

    data object PublishVerifiedAsset : OfflineDownloadEffect

    data object ScanPublishedAsset : OfflineDownloadEffect

    data object MarkReady : OfflineDownloadEffect

    data object StopTransfer : OfflineDownloadEffect

    data object DeleteStagedAsset : OfflineDownloadEffect

    data object RecoverStagedAsset : OfflineDownloadEffect

    data object StartFromBeginning : OfflineDownloadEffect

    data class ScheduleRetry(val attempt: Int) : OfflineDownloadEffect

    data class RetryScanLater(val failure: OfflineDownloadFailure) : OfflineDownloadEffect

    data class ReportBlockedUserAction(val failure: OfflineDownloadFailure) : OfflineDownloadEffect

    data class ReportTerminalFailure(val failure: OfflineDownloadFailure) : OfflineDownloadEffect
}

data class OfflineDownloadTransition(
    val state: OfflineDownloadState,
    val effects: List<OfflineDownloadEffect> = emptyList(),
)

class OfflineDownloadStateMachine {
    fun reduce(
        state: OfflineDownloadState,
        event: OfflineDownloadEvent,
    ): OfflineDownloadTransition {
        if (state.isTerminal && event !is OfflineDownloadEvent.AppRestarted) {
            return OfflineDownloadTransition(state)
        }

        return when (event) {
            OfflineDownloadEvent.StartRequested -> start(state)
            OfflineDownloadEvent.PermissionMissing ->
                state.transitionTo(
                    OfflineDownloadStatus.WAITING_PERMISSION,
                    failure = OfflineDownloadFailure(OfflineDownloadFailureKind.PermissionRequired),
                    effects = listOf(OfflineDownloadEffect.RequestStoragePermission),
                )
            OfflineDownloadEvent.PermissionGranted ->
                state.onlyFrom(OfflineDownloadStatus.WAITING_PERMISSION) { queue(clearFailure()) }
            OfflineDownloadEvent.NetworkLost ->
                if (state.status.canLoseNetwork()) {
                    state.transitionTo(
                        OfflineDownloadStatus.WAITING_NETWORK,
                        failure =
                            OfflineDownloadFailure(OfflineDownloadFailureKind.NetworkUnavailable),
                        effects =
                            listOf(
                                OfflineDownloadEffect.StopTransfer,
                                OfflineDownloadEffect.WaitForNetwork,
                            ),
                    )
                } else {
                    OfflineDownloadTransition(state)
                }
            OfflineDownloadEvent.NetworkAvailable ->
                state.onlyFrom(OfflineDownloadStatus.WAITING_NETWORK) { queue(clearFailure()) }
            OfflineDownloadEvent.WorkerStarted ->
                state.onlyFrom(OfflineDownloadStatus.QUEUED) {
                    transitionTo(
                        OfflineDownloadStatus.PREPARING,
                        effects = listOf(OfflineDownloadEffect.PrepareStaging),
                    )
                }
            OfflineDownloadEvent.Prepared ->
                state.onlyFrom(OfflineDownloadStatus.PREPARING) {
                    transitionTo(
                        OfflineDownloadStatus.DOWNLOADING,
                        effects = listOf(OfflineDownloadEffect.StartOrResumeTransfer),
                    )
                }
            is OfflineDownloadEvent.BytesReceived ->
                state.onlyFrom(OfflineDownloadStatus.DOWNLOADING) {
                    OfflineDownloadTransition(
                        copy(
                            bytesDownloaded = event.downloaded,
                            bytesExpected = event.expected,
                            failure = null,
                        )
                    )
                }
            OfflineDownloadEvent.StreamCompleted ->
                state.onlyFrom(OfflineDownloadStatus.DOWNLOADING) {
                    transitionTo(
                        OfflineDownloadStatus.VERIFYING,
                        effects = listOf(OfflineDownloadEffect.VerifyStagedAsset),
                    )
                }
            OfflineDownloadEvent.VerifySucceeded ->
                state.onlyFrom(OfflineDownloadStatus.VERIFYING) {
                    transitionTo(
                        OfflineDownloadStatus.PUBLISHING,
                        effects = listOf(OfflineDownloadEffect.PublishVerifiedAsset),
                    )
                }
            is OfflineDownloadEvent.TransferFailed ->
                state.onlyFrom(OfflineDownloadStatus.PREPARING, OfflineDownloadStatus.DOWNLOADING) {
                    handleFailure(this, event.failure)
                }
            is OfflineDownloadEvent.VerifyFailed ->
                state.onlyFrom(OfflineDownloadStatus.VERIFYING) {
                    handleFailure(
                        this,
                        event.failure,
                        cleanupEffects = listOf(OfflineDownloadEffect.DeleteStagedAsset),
                    )
                }
            is OfflineDownloadEvent.PublishFailed ->
                state.onlyFrom(OfflineDownloadStatus.PUBLISHING) {
                    if (event.finalFileIsValid) {
                        transitionTo(
                            OfflineDownloadStatus.READY,
                            failure = event.failure,
                            effects =
                                listOf(
                                    OfflineDownloadEffect.MarkReady,
                                    OfflineDownloadEffect.RetryScanLater(event.failure),
                                ),
                        )
                    } else {
                        handleFailure(
                            this,
                            event.failure,
                            cleanupEffects = listOf(OfflineDownloadEffect.DeleteStagedAsset),
                        )
                    }
                }
            OfflineDownloadEvent.PublishSucceeded ->
                state.onlyFrom(OfflineDownloadStatus.PUBLISHING) {
                    transitionTo(
                        OfflineDownloadStatus.SCANNING,
                        effects = listOf(OfflineDownloadEffect.ScanPublishedAsset),
                    )
                }
            is OfflineDownloadEvent.ScanFailed ->
                state.onlyFrom(OfflineDownloadStatus.SCANNING) {
                    transitionTo(
                        OfflineDownloadStatus.READY,
                        failure = event.failure,
                        effects =
                            listOf(
                                OfflineDownloadEffect.MarkReady,
                                OfflineDownloadEffect.RetryScanLater(event.failure),
                            ),
                    )
                }
            OfflineDownloadEvent.ScanSucceeded ->
                state.onlyFrom(OfflineDownloadStatus.SCANNING) {
                    transitionTo(
                        OfflineDownloadStatus.READY,
                        effects = listOf(OfflineDownloadEffect.MarkReady),
                    )
                }
            OfflineDownloadEvent.RetryTimerFired ->
                state.onlyFrom(OfflineDownloadStatus.RETRY_WAIT) {
                    transitionTo(
                        OfflineDownloadStatus.QUEUED,
                        failure = null,
                        effects = listOf(OfflineDownloadEffect.AcquireSerialDownloadSlot),
                    )
                }
            OfflineDownloadEvent.UserPaused ->
                state.transitionTo(
                    OfflineDownloadStatus.PAUSED_USER,
                    effects = listOf(OfflineDownloadEffect.StopTransfer),
                )
            OfflineDownloadEvent.UserCanceled -> {
                val failure = OfflineDownloadFailure(OfflineDownloadFailureKind.Canceled)
                state.transitionTo(
                    OfflineDownloadStatus.FAILED_TERMINAL,
                    failure = failure,
                    effects =
                        listOf(
                            OfflineDownloadEffect.StopTransfer,
                            OfflineDownloadEffect.DeleteStagedAsset,
                            OfflineDownloadEffect.ReportTerminalFailure(failure),
                        ),
                )
            }
            OfflineDownloadEvent.UserActionResolved ->
                state.onlyFrom(OfflineDownloadStatus.BLOCKED_USER_ACTION) {
                    transitionTo(
                        OfflineDownloadStatus.QUEUED,
                        failure = null,
                        effects = listOf(OfflineDownloadEffect.AcquireSerialDownloadSlot),
                    )
                }
            OfflineDownloadEvent.AppRestarted -> recoverAfterRestart(state)
        }
    }

    private fun start(state: OfflineDownloadState): OfflineDownloadTransition =
        when (state.status) {
            OfflineDownloadStatus.PLANNED,
            OfflineDownloadStatus.PAUSED_USER,
            OfflineDownloadStatus.RETRY_WAIT -> queue(state)
            else -> OfflineDownloadTransition(state)
        }

    private fun queue(state: OfflineDownloadState): OfflineDownloadTransition =
        state.transitionTo(
            OfflineDownloadStatus.QUEUED,
            failure = null,
            effects = listOf(OfflineDownloadEffect.AcquireSerialDownloadSlot),
        )

    private fun recoverAfterRestart(state: OfflineDownloadState): OfflineDownloadTransition =
        when (state.status) {
            OfflineDownloadStatus.READY,
            OfflineDownloadStatus.FAILED_TERMINAL,
            OfflineDownloadStatus.BLOCKED_USER_ACTION,
            OfflineDownloadStatus.PAUSED_USER -> OfflineDownloadTransition(state)
            OfflineDownloadStatus.RETRY_WAIT ->
                OfflineDownloadTransition(
                    state,
                    listOf(OfflineDownloadEffect.ScheduleRetry(state.retryAttempt)),
                )
            OfflineDownloadStatus.WAITING_PERMISSION ->
                OfflineDownloadTransition(
                    state,
                    listOf(OfflineDownloadEffect.RequestStoragePermission),
                )
            OfflineDownloadStatus.WAITING_NETWORK ->
                OfflineDownloadTransition(state, listOf(OfflineDownloadEffect.WaitForNetwork))
            else ->
                state.transitionTo(
                    OfflineDownloadStatus.PREPARING,
                    failure = OfflineDownloadFailure(OfflineDownloadFailureKind.AppInterrupted),
                    effects =
                        listOf(
                            OfflineDownloadEffect.RecoverStagedAsset,
                            OfflineDownloadEffect.PrepareStaging,
                        ),
                )
        }

    private fun handleFailure(
        state: OfflineDownloadState,
        failure: OfflineDownloadFailure,
        cleanupEffects: List<OfflineDownloadEffect> = emptyList(),
    ): OfflineDownloadTransition =
        when (failure.disposition) {
            OfflineDownloadFailureDisposition.Retryable -> {
                val nextAttempt = state.retryAttempt + 1
                if (nextAttempt > state.maxRetryAttempts) {
                    state.transitionTo(
                        OfflineDownloadStatus.FAILED_TERMINAL,
                        retryAttempt = nextAttempt,
                        failure = failure,
                        effects =
                            cleanupEffects + OfflineDownloadEffect.ReportTerminalFailure(failure),
                    )
                } else {
                    val restartEffects =
                        if (failure.kind == OfflineDownloadFailureKind.ResumeRejected) {
                            listOf(
                                OfflineDownloadEffect.DeleteStagedAsset,
                                OfflineDownloadEffect.StartFromBeginning,
                            )
                        } else {
                            emptyList()
                        }
                    state.transitionTo(
                        OfflineDownloadStatus.RETRY_WAIT,
                        retryAttempt = nextAttempt,
                        failure = failure,
                        effects =
                            cleanupEffects +
                                restartEffects +
                                OfflineDownloadEffect.ScheduleRetry(nextAttempt),
                    )
                }
            }
            OfflineDownloadFailureDisposition.UserAction ->
                if (failure.kind == OfflineDownloadFailureKind.PermissionRequired) {
                    state.transitionTo(
                        OfflineDownloadStatus.WAITING_PERMISSION,
                        failure = failure,
                        effects = listOf(OfflineDownloadEffect.RequestStoragePermission),
                    )
                } else {
                    state.transitionTo(
                        OfflineDownloadStatus.BLOCKED_USER_ACTION,
                        failure = failure,
                        effects = listOf(OfflineDownloadEffect.ReportBlockedUserAction(failure)),
                    )
                }
            OfflineDownloadFailureDisposition.Terminal ->
                state.transitionTo(
                    OfflineDownloadStatus.FAILED_TERMINAL,
                    failure = failure,
                    effects = cleanupEffects + OfflineDownloadEffect.ReportTerminalFailure(failure),
                )
        }

    private fun OfflineDownloadState.clearFailure(): OfflineDownloadState = copy(failure = null)

    private fun OfflineDownloadState.onlyFrom(
        vararg allowedStatuses: OfflineDownloadStatus,
        transition: OfflineDownloadState.() -> OfflineDownloadTransition,
    ): OfflineDownloadTransition =
        if (status in allowedStatuses) {
            transition()
        } else {
            OfflineDownloadTransition(this)
        }

    private fun OfflineDownloadStatus.canLoseNetwork(): Boolean =
        this in
            setOf(
                OfflineDownloadStatus.QUEUED,
                OfflineDownloadStatus.PREPARING,
                OfflineDownloadStatus.DOWNLOADING,
                OfflineDownloadStatus.VERIFYING,
            )

    private fun OfflineDownloadState.transitionTo(
        status: OfflineDownloadStatus,
        retryAttempt: Int = this.retryAttempt,
        failure: OfflineDownloadFailure? = this.failure,
        effects: List<OfflineDownloadEffect> = emptyList(),
    ): OfflineDownloadTransition =
        OfflineDownloadTransition(
            copy(status = status, retryAttempt = retryAttempt, failure = failure),
            effects,
        )
}
