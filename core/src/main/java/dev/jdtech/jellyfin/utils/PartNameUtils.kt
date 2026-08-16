package dev.jdtech.jellyfin.utils

import android.content.Context
import dev.jdtech.jellyfin.core.R

fun String.getTranslatablePartName(context: Context): String {
    val regex = Regex("(?i)(cd|dvd|part|pt|disc|disk)[ .\\-_]?([0-9]+|[a-d])\\b")
    val matchResult = regex.find(this)
    
    if (matchResult != null) {
        val partType = matchResult.groupValues[1].lowercase()
        val partNumber = matchResult.groupValues[2].uppercase()
        
        return when (partType) {
            "cd" -> context.getString(R.string.cd_name, partNumber)
            "dvd" -> context.getString(R.string.dvd_name, partNumber)
            "disc", "disk" -> context.getString(R.string.disc_name, partNumber)
            "part", "pt" -> context.getString(R.string.part_name, partNumber)
            else -> this
        }
    }
    
    return this
}

fun String.isPartName(): Boolean {
    val regex = Regex("(?i)(cd|dvd|part|pt|disc|disk)[ .\\-_]?([0-9]+|[a-d])\\b")
    return regex.containsMatchIn(this)
}
