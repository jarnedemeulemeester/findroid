package dev.jdtech.jellyfin.database

import androidx.room.TypeConverter
import org.jellyfin.sdk.model.DateTime
import java.time.ZoneOffset
import java.util.UUID

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
}
