package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "shows")
data class FindroidShowDto(
    @PrimaryKey val id: UUID,
    val serverId: String?,
    val name: String,
    val originalTitle: String?,
    val overview: String,
    val runtimeTicks: Long,
    val communityRating: Float?,
    val officialRating: String?,
    val status: String,
    val productionYear: Int?,
    val endDate: LocalDateTime?,
    val primaryBlurHash: String?,
    val backdropBlurHash: String?,
    val logoBlurHash: String?,
    val showPrimaryBlurHash: String?,
    val showBackdropBlurHash: String?,
    val showLogoBlurHash: String?,
)

fun FindroidShow.toFindroidShowDto(serverId: String? = null): FindroidShowDto {
    return FindroidShowDto(
        id = id,
        serverId = serverId,
        name = name,
        originalTitle = originalTitle,
        overview = overview,
        runtimeTicks = runtimeTicks,
        communityRating = communityRating,
        officialRating = officialRating,
        status = status,
        productionYear = productionYear,
        endDate = endDate,
        primaryBlurHash = images.primary?.blurHash,
        backdropBlurHash = images.backdrop?.blurHash,
        logoBlurHash = images.logo?.blurHash,
        showPrimaryBlurHash = images.showPrimary?.blurHash,
        showBackdropBlurHash = images.showBackdrop?.blurHash,
        showLogoBlurHash = images.showLogo?.blurHash,
    )
}
