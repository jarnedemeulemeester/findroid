package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "credits")
data class CreditDto(
    @PrimaryKey
    val itemId: UUID,
    val start: Double,
    val end: Double,
    val showAt: Double,
    val hideAt: Double,
)

fun Credit.toCreditDto(itemId: UUID): CreditDto {
    return CreditDto(
        itemId = itemId,
        start = credit.introStart,
        end = credit.introEnd,
        showAt = credit.showSkipPromptAt,
        hideAt = credit.hideSkipPromptAt,
    )
}
