package dev.jdtech.jellyfin.database

import androidx.room.TypeConverter
import dev.jdtech.jellyfin.models.FindroidChapter
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jellyfin.sdk.model.DateTime

class Converters {
    @TypeConverter
    fun fromStringToUUID(value: String?): UUID? {
        return value?.let { UUID.fromString(it) }
    }

    @TypeConverter
    fun fromUUIDToString(value: UUID?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun fromDateTimeToLong(value: DateTime?): Long? {
        return value?.toEpochSecond(ZoneOffset.UTC)
    }

    @TypeConverter
    fun fromLongToDatetime(value: Long?): DateTime? {
        return value?.let { DateTime.ofEpochSecond(it, 0, ZoneOffset.UTC) }
    }

    @TypeConverter
    fun fromFindroidChaptersToString(value: List<FindroidChapter>?): String? {
        return value?.let { Json.encodeToString(value) }
    }

    @TypeConverter
    fun fromStringToFindroidChapters(value: String?): List<FindroidChapter>? {
        return value?.let { Json.decodeFromString(value) }
    }
}
