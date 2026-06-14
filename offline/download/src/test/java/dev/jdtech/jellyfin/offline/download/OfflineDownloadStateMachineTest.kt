package dev.jdtech.jellyfin.offline.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDownloadStateMachineTest {
    private val machine = OfflineDownloadStateMachine()

    @Test
    fun happyPathPublishesOnlyAfterVerifyThenScan() {
        val states = mutableListOf<OfflineDownloadStatus>()
        val effects = mutableListOf<OfflineDownloadEffect>()
        var state = OfflineDownloadState()

        fun apply(event: OfflineDownloadEvent) {
            val transition = machine.reduce(state, event)
            state = transition.state
            states += state.status
            effects += transition.effects
        }

        apply(OfflineDownloadEvent.StartRequested)
        apply(OfflineDownloadEvent.WorkerStarted)
        apply(OfflineDownloadEvent.Prepared)
        apply(OfflineDownloadEvent.BytesReceived(downloaded = 10, expected = 100))
        apply(OfflineDownloadEvent.StreamCompleted)
        apply(OfflineDownloadEvent.VerifySucceeded)
        apply(OfflineDownloadEvent.PublishSucceeded)
        apply(OfflineDownloadEvent.ScanSucceeded)

        assertEquals(OfflineDownloadStatus.READY, state.status)
        assertTrue(
            effects.indexOf(OfflineDownloadEffect.PublishVerifiedAsset) >
                effects.indexOf(OfflineDownloadEffect.VerifyStagedAsset)
        )
        assertTrue(
            effects.indexOf(OfflineDownloadEffect.MarkReady) >
                effects.indexOf(OfflineDownloadEffect.ScanPublishedAsset)
        )
        assertEquals(
            listOf(
                OfflineDownloadStatus.QUEUED,
                OfflineDownloadStatus.PREPARING,
                OfflineDownloadStatus.DOWNLOADING,
                OfflineDownloadStatus.DOWNLOADING,
                OfflineDownloadStatus.VERIFYING,
                OfflineDownloadStatus.PUBLISHING,
                OfflineDownloadStatus.SCANNING,
                OfflineDownloadStatus.READY,
            ),
            states,
        )
    }

    @Test
    fun permissionMissingBlocksBeforeQueue() {
        val transition =
            machine.reduce(OfflineDownloadState(), OfflineDownloadEvent.PermissionMissing)

        assertEquals(OfflineDownloadStatus.WAITING_PERMISSION, transition.state.status)
        assertEquals(listOf(OfflineDownloadEffect.RequestStoragePermission), transition.effects)
        assertEquals(OfflineDownloadFailureKind.PermissionRequired, transition.state.failure?.kind)
    }

    @Test
    fun startRequestDoesNotBypassMissingPermissionOrNetwork() {
        val permissionState =
            OfflineDownloadState(status = OfflineDownloadStatus.WAITING_PERMISSION)
        val networkState = OfflineDownloadState(status = OfflineDownloadStatus.WAITING_NETWORK)

        val permissionTransition =
            machine.reduce(permissionState, OfflineDownloadEvent.StartRequested)
        val networkTransition = machine.reduce(networkState, OfflineDownloadEvent.StartRequested)

        assertEquals(permissionState, permissionTransition.state)
        assertEquals(networkState, networkTransition.state)
        assertTrue(permissionTransition.effects.isEmpty())
        assertTrue(networkTransition.effects.isEmpty())
    }

    @Test
    fun resolvedMissingPermissionAndNetworkQueueDownload() {
        val permissionTransition =
            machine.reduce(
                OfflineDownloadState(status = OfflineDownloadStatus.WAITING_PERMISSION),
                OfflineDownloadEvent.PermissionGranted,
            )
        val networkTransition =
            machine.reduce(
                OfflineDownloadState(status = OfflineDownloadStatus.WAITING_NETWORK),
                OfflineDownloadEvent.NetworkAvailable,
            )

        assertEquals(OfflineDownloadStatus.QUEUED, permissionTransition.state.status)
        assertEquals(OfflineDownloadStatus.QUEUED, networkTransition.state.status)
        assertEquals(
            listOf(OfflineDownloadEffect.AcquireSerialDownloadSlot),
            permissionTransition.effects,
        )
        assertEquals(
            listOf(OfflineDownloadEffect.AcquireSerialDownloadSlot),
            networkTransition.effects,
        )
    }

    @Test
    fun networkLossStopsTransferAndWaitsForNetwork() {
        val state = OfflineDownloadState(status = OfflineDownloadStatus.DOWNLOADING)

        val transition = machine.reduce(state, OfflineDownloadEvent.NetworkLost)

        assertEquals(OfflineDownloadStatus.WAITING_NETWORK, transition.state.status)
        assertEquals(
            listOf(OfflineDownloadEffect.StopTransfer, OfflineDownloadEffect.WaitForNetwork),
            transition.effects,
        )
    }

    @Test
    fun retryableTransferFailureSchedulesRetry() {
        val state = OfflineDownloadState(status = OfflineDownloadStatus.DOWNLOADING)
        val failure = OfflineDownloadFailure(OfflineDownloadFailureKind.StreamInterrupted)

        val transition = machine.reduce(state, OfflineDownloadEvent.TransferFailed(failure))

        assertEquals(OfflineDownloadStatus.RETRY_WAIT, transition.state.status)
        assertEquals(1, transition.state.retryAttempt)
        assertEquals(listOf(OfflineDownloadEffect.ScheduleRetry(1)), transition.effects)
    }

    @Test
    fun missingSourceTransferFailureDoesNotAutoRetry() {
        val state = OfflineDownloadState(status = OfflineDownloadStatus.DOWNLOADING)
        val failure = OfflineDownloadFailure(OfflineDownloadFailureKind.SourceMissingOrChanged)

        val transition = machine.reduce(state, OfflineDownloadEvent.TransferFailed(failure))

        assertEquals(OfflineDownloadStatus.FAILED_TERMINAL, transition.state.status)
        assertEquals(0, transition.state.retryAttempt)
        assertEquals(
            listOf(OfflineDownloadEffect.ReportTerminalFailure(failure)),
            transition.effects,
        )
    }

    @Test
    fun authExpiredTransferFailureBlocksForUserActionWithoutRetry() {
        val state = OfflineDownloadState(status = OfflineDownloadStatus.DOWNLOADING)
        val failure = OfflineDownloadFailure(OfflineDownloadFailureKind.AuthExpired)

        val transition = machine.reduce(state, OfflineDownloadEvent.TransferFailed(failure))

        assertEquals(OfflineDownloadStatus.BLOCKED_USER_ACTION, transition.state.status)
        assertEquals(0, transition.state.retryAttempt)
        assertEquals(
            listOf(OfflineDownloadEffect.ReportBlockedUserAction(failure)),
            transition.effects,
        )
    }

    @Test
    fun retryLimitTurnsRetryableFailureIntoTerminalFailure() {
        val state =
            OfflineDownloadState(
                status = OfflineDownloadStatus.DOWNLOADING,
                retryAttempt = 5,
                maxRetryAttempts = 5,
            )
        val failure = OfflineDownloadFailure(OfflineDownloadFailureKind.StreamInterrupted)

        val transition = machine.reduce(state, OfflineDownloadEvent.TransferFailed(failure))

        assertEquals(OfflineDownloadStatus.FAILED_TERMINAL, transition.state.status)
        assertEquals(6, transition.state.retryAttempt)
        assertEquals(
            listOf(OfflineDownloadEffect.ReportTerminalFailure(failure)),
            transition.effects,
        )
    }

    @Test
    fun resumeRejectedDeletesStagingAndRestartsFromBeginning() {
        val state = OfflineDownloadState(status = OfflineDownloadStatus.DOWNLOADING)
        val failure = OfflineDownloadFailure(OfflineDownloadFailureKind.ResumeRejected)

        val transition = machine.reduce(state, OfflineDownloadEvent.TransferFailed(failure))

        assertEquals(OfflineDownloadStatus.RETRY_WAIT, transition.state.status)
        assertEquals(
            listOf(
                OfflineDownloadEffect.DeleteStagedAsset,
                OfflineDownloadEffect.StartFromBeginning,
                OfflineDownloadEffect.ScheduleRetry(1),
            ),
            transition.effects,
        )
    }

    @Test
    fun foreignFileCollisionRequiresUserAction() {
        val state = OfflineDownloadState(status = OfflineDownloadStatus.PUBLISHING)
        val failure = OfflineDownloadFailure(OfflineDownloadFailureKind.CollisionWithForeignFile)

        val transition = machine.reduce(state, OfflineDownloadEvent.PublishFailed(failure))

        assertEquals(OfflineDownloadStatus.BLOCKED_USER_ACTION, transition.state.status)
        assertEquals(
            listOf(OfflineDownloadEffect.ReportBlockedUserAction(failure)),
            transition.effects,
        )
    }

    @Test
    fun scanFailureStillMarksAssetReadyAndSchedulesScanRetry() {
        val state = OfflineDownloadState(status = OfflineDownloadStatus.SCANNING)
        val failure = OfflineDownloadFailure(OfflineDownloadFailureKind.ScanFailed)

        val transition = machine.reduce(state, OfflineDownloadEvent.ScanFailed(failure))

        assertEquals(OfflineDownloadStatus.READY, transition.state.status)
        assertEquals(
            listOf(OfflineDownloadEffect.MarkReady, OfflineDownloadEffect.RetryScanLater(failure)),
            transition.effects,
        )
    }

    @Test
    fun restartFromActiveStateRecoversStagingBeforePreparing() {
        val state = OfflineDownloadState(status = OfflineDownloadStatus.PUBLISHING)

        val transition = machine.reduce(state, OfflineDownloadEvent.AppRestarted)

        assertEquals(OfflineDownloadStatus.PREPARING, transition.state.status)
        assertEquals(
            listOf(OfflineDownloadEffect.RecoverStagedAsset, OfflineDownloadEffect.PrepareStaging),
            transition.effects,
        )
    }

    @Test
    fun outOfOrderPhaseEventDoesNotAdvanceState() {
        val state = OfflineDownloadState(status = OfflineDownloadStatus.PLANNED)

        val transition = machine.reduce(state, OfflineDownloadEvent.StreamCompleted)

        assertEquals(state, transition.state)
        assertTrue(transition.effects.isEmpty())
    }

    @Test
    fun terminalStateIgnoresNonRestartEvents() {
        val state = OfflineDownloadState(status = OfflineDownloadStatus.READY)

        val transition = machine.reduce(state, OfflineDownloadEvent.UserCanceled)

        assertEquals(state, transition.state)
        assertTrue(transition.effects.isEmpty())
    }

    @Test
    fun resolvedUserActionQueuesDownloadAgain() {
        val failure = OfflineDownloadFailure(OfflineDownloadFailureKind.CollisionWithForeignFile)
        val state =
            OfflineDownloadState(
                status = OfflineDownloadStatus.BLOCKED_USER_ACTION,
                failure = failure,
            )

        val transition = machine.reduce(state, OfflineDownloadEvent.UserActionResolved)

        assertEquals(OfflineDownloadStatus.QUEUED, transition.state.status)
        assertEquals(null, transition.state.failure)
        assertEquals(listOf(OfflineDownloadEffect.AcquireSerialDownloadSlot), transition.effects)
    }
}
