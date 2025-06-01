package dev.jdtech.jellyfin.utils

import dev.jdtech.jellyfin.models.FindroidShow

fun getShowDateString(item: FindroidShow): String {
    val dateRange: MutableList<String> = mutableListOf()
    item.productionYear?.let { dateRange.add(it.toString()) }
    when (item.status) {
        "Continuing" -> {
            dateRange.add("Present")
        }

        "Ended" -> {
            item.endDate?.let { dateRange.add(it.year.toString()) }
        }
    }
    if (dateRange.count() > 1 && dateRange[0] == dateRange[1]) return dateRange[0]
    return dateRange.joinToString(separator = " - ")
}
