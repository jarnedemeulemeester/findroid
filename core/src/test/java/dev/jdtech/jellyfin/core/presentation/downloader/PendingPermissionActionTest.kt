package dev.jdtech.jellyfin.core.presentation.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingPermissionActionTest {
    @Test
    fun consumeWithoutPendingActionReturnsNone() {
        val pending = PendingPermissionAction<String>()

        assertSame(PendingPermissionResult.None, pending.consume(hasPermission = true))
    }

    @Test
    fun deniedPermissionConsumesPendingAction() {
        val pending = PendingPermissionAction<String>()
        pending.remember("download")

        assertSame(PendingPermissionResult.Denied, pending.consume(hasPermission = false))
        assertSame(PendingPermissionResult.None, pending.consume(hasPermission = true))
    }

    @Test
    fun grantedPermissionResumesAndConsumesPendingAction() {
        val pending = PendingPermissionAction<String>()
        pending.remember("download")

        val result = pending.consume(hasPermission = true)

        assertTrue(result is PendingPermissionResult.Resume)
        assertEquals("download", (result as PendingPermissionResult.Resume).action)
        assertSame(PendingPermissionResult.None, pending.consume(hasPermission = true))
    }

    @Test
    fun newerActionReplacesOlderPendingAction() {
        val pending = PendingPermissionAction<String>()
        pending.remember("old")
        pending.remember("new")

        val result = pending.consume(hasPermission = true)

        assertEquals("new", (result as PendingPermissionResult.Resume).action)
    }
}
