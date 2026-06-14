package dev.jdtech.jellyfin.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateUpdateTest {
    @Test
    fun stopBelowTenPercentClearsResumeWithoutMarkingPlayed() {
        val state = playbackStopState(positionTicks = 12_000L, playedPercentage = 9)

        assertFalse(state.played)
        assertEquals(0L, state.playbackPositionTicks)
    }

    @Test
    fun stopAboveNinetyPercentMarksPlayedAndClearsResume() {
        val state = playbackStopState(positionTicks = 120_000L, playedPercentage = 91)

        assertTrue(state.played)
        assertEquals(0L, state.playbackPositionTicks)
    }

    @Test
    fun stopBetweenTenAndNinetyKeepsResumeProgress() {
        val state = playbackStopState(positionTicks = 60_000L, playedPercentage = 50)

        assertFalse(state.played)
        assertEquals(60_000L, state.playbackPositionTicks)
    }

    @Test
    fun progressKeepsPositionWithoutMarkingPlayed() {
        val state = playbackProgressState(positionTicks = 42_000L)

        assertFalse(state.played)
        assertEquals(42_000L, state.playbackPositionTicks)
    }
}
