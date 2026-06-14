package dev.jdtech.jellyfin.offline.download

data class ProjectedPath(val directorySegments: List<String>, val displayName: String) {
    val relativeDirectory: String
        get() = directorySegments.joinToString("/")

    val relativeFilePath: String
        get() =
            if (directorySegments.isEmpty()) {
                displayName
            } else {
                "${relativeDirectory}/${displayName}"
            }
}

data class ProjectedPathInput(val logicalDirectorySegments: List<String>, val displayName: String)

data class ProjectedPathFailure(val kind: ProjectedPathFailureKind, val value: String? = null)

enum class ProjectedPathFailureKind {
    MissingLogicalDirectory,
    BlankSegment,
    ReservedSegment,
    SegmentContainsSeparator,
    SegmentContainsControlCharacter,
    SegmentTooLong,
    BlankDisplayName,
    DisplayNameContainsSeparator,
    DisplayNameContainsControlCharacter,
    DisplayNameTooLong,
}

sealed interface ProjectedPathResult {
    data class Success(val path: ProjectedPath) : ProjectedPathResult

    data class Failure(val failure: ProjectedPathFailure) : ProjectedPathResult
}

class ProjectedPathResolver(
    private val maxSegmentLength: Int = 180,
    private val maxDisplayNameLength: Int = 220,
) {
    fun resolve(input: ProjectedPathInput): ProjectedPathResult {
        if (input.logicalDirectorySegments.isEmpty()) {
            return ProjectedPathResult.Failure(
                ProjectedPathFailure(ProjectedPathFailureKind.MissingLogicalDirectory)
            )
        }

        input.logicalDirectorySegments.forEach { segment ->
            validateSegment(segment)?.let {
                return ProjectedPathResult.Failure(it)
            }
        }

        validateDisplayName(input.displayName)?.let {
            return ProjectedPathResult.Failure(it)
        }

        return ProjectedPathResult.Success(
            ProjectedPath(
                directorySegments = input.logicalDirectorySegments,
                displayName = input.displayName,
            )
        )
    }

    private fun validateSegment(segment: String): ProjectedPathFailure? =
        when {
            segment.isBlank() ->
                ProjectedPathFailure(ProjectedPathFailureKind.BlankSegment, segment)
            segment == "." || segment == ".." ->
                ProjectedPathFailure(ProjectedPathFailureKind.ReservedSegment, segment)
            segment.contains('/') || segment.contains('\\') ->
                ProjectedPathFailure(ProjectedPathFailureKind.SegmentContainsSeparator, segment)
            segment.any(Char::isISOControl) ->
                ProjectedPathFailure(
                    ProjectedPathFailureKind.SegmentContainsControlCharacter,
                    segment,
                )
            segment.length > maxSegmentLength ->
                ProjectedPathFailure(ProjectedPathFailureKind.SegmentTooLong, segment)
            else -> null
        }

    private fun validateDisplayName(displayName: String): ProjectedPathFailure? =
        when {
            displayName.isBlank() ->
                ProjectedPathFailure(ProjectedPathFailureKind.BlankDisplayName, displayName)
            displayName.contains('/') || displayName.contains('\\') ->
                ProjectedPathFailure(
                    ProjectedPathFailureKind.DisplayNameContainsSeparator,
                    displayName,
                )
            displayName.any(Char::isISOControl) ->
                ProjectedPathFailure(
                    ProjectedPathFailureKind.DisplayNameContainsControlCharacter,
                    displayName,
                )
            displayName.length > maxDisplayNameLength ->
                ProjectedPathFailure(ProjectedPathFailureKind.DisplayNameTooLong, displayName)
            else -> null
        }
}
