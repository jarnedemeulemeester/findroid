package dev.jdtech.jellyfin.database

import androidx.room3.ColumnTypeConverter
import dev.jdtech.jellyfin.models.FindroidChapter
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.serialization.json.Json
import org.jellyfin.sdk.model.DateTime

class Converters {
    @ColumnTypeConverter
    fun fromStringToUUID(value: String?): UUID? {
        return value?.let { UUID.fromString(it) }
    }

    @ColumnTypeConverter
    fun fromUUIDToString(value: UUID?): String? {
        return value?.toString()
    }

    @ColumnTypeConverter
    fun fromDateTimeToLong(value: DateTime?): Long? {
        return value?.toEpochSecond(ZoneOffset.UTC)
    }

    @ColumnTypeConverter
    fun fromLongToDatetime(value: Long?): DateTime? {
        return value?.let { DateTime.ofEpochSecond(it, 0, ZoneOffset.UTC) }
    }

    @ColumnTypeConverter
    fun fromFindroidChaptersToString(value: List<FindroidChapter>?): String? {
        return value?.let { Json.encodeToString(value) }
    }

    @ColumnTypeConverter
    fun fromStringToFindroidChapters(value: String?): List<FindroidChapter>? {
        return value?.let { Json.decodeFromString(value) }
    }
}
