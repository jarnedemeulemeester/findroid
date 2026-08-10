package dev.jdtech.jellyfin.models

enum class SortOrder(val sortString: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending");

    companion object {
        val defaultValue = ASCENDING

        fun fromString(string: String): SortOrder {
            return try {
                valueOf(string)
            } catch (_: IllegalArgumentException) {
                defaultValue
            }
        }
    }
}
