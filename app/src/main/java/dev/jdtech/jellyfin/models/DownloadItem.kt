package dev.jdtech.jellyfin.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.*

@Parcelize
@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey
    val id: UUID,
    val type: BaseItemKind,
    val name: String,
    val played: Boolean,
    val overview: String? = null,
    val seriesId: UUID? = null,
    val seriesName: String? = null,
    val indexNumber: Int? = null,
    val parentIndexNumber: Int? = null,
    val playbackPosition: Long? = null,
    val playedPercentage: Double? = null,
    val downloadId: Long? = null,
) : Parcelable