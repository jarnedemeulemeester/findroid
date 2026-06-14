package dev.jdtech.jellyfin.offline.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectedPathResolverTest {
    private val resolver = ProjectedPathResolver()

    @Test
    fun preservesJellyfinLogicalPathWithoutInventingTemplates() {
        val result =
            resolver.resolve(
                ProjectedPathInput(
                    logicalDirectorySegments = listOf("Конференции", "AvitoTech", "26"),
                    displayName = "human_name.mp4",
                )
            )

        assertTrue(result is ProjectedPathResult.Success)
        val path = (result as ProjectedPathResult.Success).path
        assertEquals("Конференции/AvitoTech/26", path.relativeDirectory)
        assertEquals("Конференции/AvitoTech/26/human_name.mp4", path.relativeFilePath)
    }

    @Test
    fun failsWhenLogicalPathIsMissing() {
        val result =
            resolver.resolve(
                ProjectedPathInput(
                    logicalDirectorySegments = emptyList(),
                    displayName = "human_name.mp4",
                )
            )

        assertFailure(ProjectedPathFailureKind.MissingLogicalDirectory, result)
    }

    @Test
    fun failsInsteadOfSanitizingSegmentWithSeparator() {
        val result =
            resolver.resolve(
                ProjectedPathInput(
                    logicalDirectorySegments = listOf("Конференции/AvitoTech", "26"),
                    displayName = "human_name.mp4",
                )
            )

        assertFailure(ProjectedPathFailureKind.SegmentContainsSeparator, result)
    }

    @Test
    fun failsInsteadOfSanitizingDisplayNameWithSeparator() {
        val result =
            resolver.resolve(
                ProjectedPathInput(
                    logicalDirectorySegments = listOf("Конференции", "AvitoTech", "26"),
                    displayName = "nested/human_name.mp4",
                )
            )

        assertFailure(ProjectedPathFailureKind.DisplayNameContainsSeparator, result)
    }

    @Test
    fun failsOnReservedDotSegments() {
        val result =
            resolver.resolve(
                ProjectedPathInput(
                    logicalDirectorySegments = listOf("Конференции", "..", "26"),
                    displayName = "human_name.mp4",
                )
            )

        assertFailure(ProjectedPathFailureKind.ReservedSegment, result)
    }

    private fun assertFailure(expectedKind: ProjectedPathFailureKind, result: ProjectedPathResult) {
        assertTrue(result is ProjectedPathResult.Failure)
        assertEquals(expectedKind, (result as ProjectedPathResult.Failure).failure.kind)
    }
}
