package dev.jdtech.jellyfin.database

import androidx.room.TypeConverter
import dev.jdtech.jellyfin.models.ContentType
import java.util.*

class Converters {
    @TypeConverter
    fun fromStringToUUID(value: String): UUID {
        return UUID.fromString(value)
    }

    @TypeConverter
    fun fromUUIDToString(value: UUID): String {
        return value.toString()
    }

    @TypeConverter
    fun fromStringToContentType(value: String): ContentType {
        return ContentType.valueOf(value)
    }

    @TypeConverter
    fun fromContentTypeToString(value: ContentType): String {
        return value.name
    }
}