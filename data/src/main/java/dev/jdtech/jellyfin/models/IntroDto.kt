package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "intros")
data class IntroDto(
    @PrimaryKey
    val itemId: UUID,
    val start: Double,
    val end: Double,
    val showAt: Double,
    val hideAt: Double,
)

fun Intro.toIntroDto(itemId: UUID): IntroDto {
    return IntroDto(
        itemId = itemId,
        start = introStart,
        end = introEnd,
        showAt = showSkipPromptAt,
        hideAt = hideSkipPromptAt,
    )
}
