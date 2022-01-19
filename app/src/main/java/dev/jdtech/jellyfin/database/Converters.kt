package dev.jdtech.jellyfin.database

import androidx.room.TypeConverter
import java.util.*

class Converters {
    @TypeConverter
    fun fromStringToUUID(value: String?): UUID? {
        return value?.let { UUID.fromString(it) }
    }

    @TypeConverter
    fun fromUUIDToString(value: UUID?): String? {
        return value?.toString()
    }
}