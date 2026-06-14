package dev.jdtech.jellyfin.car

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

internal object FindroidCarPlaybackHistory {
    private const val PREFS_NAME = "findroid_car_playback_history"
    private const val KEY_ITEMS = "items"
    private const val MAX_ITEMS = 10

    fun record(context: Context, item: FindroidCarCatalogItem, playbackPositionTicks: Long) {
        if (playbackPositionTicks <= 0L) return
        runCatching {
                val updatedItem =
                    item.copy(playbackPositionTicks = playbackPositionTicks, streamUrl = null)
                val entries =
                    readEntries(context)
                        .filterNot { it.item.itemId == item.itemId }
                        .toMutableList()
                entries.add(0, PlaybackHistoryEntry(System.currentTimeMillis(), updatedItem))
                writeEntries(context, entries.take(MAX_ITEMS))
            }
            .onFailure { Timber.w(it, "Failed to record Android Auto playback history") }
    }

    fun load(context: Context): List<FindroidCarCatalogItem> =
        loadEntries(context).map { it.item }

    fun loadEntries(context: Context): List<Entry> =
        runCatching { readEntries(context).map { Entry(it.updatedAtMillis, it.item) } }
            .onFailure { Timber.w(it, "Failed to load Android Auto playback history") }
            .getOrDefault(emptyList())

    fun clear(context: Context, itemId: String) {
        runCatching {
                val entries = readEntries(context).filterNot { it.item.itemId == itemId }
                writeEntries(context, entries)
            }
            .onFailure { Timber.w(it, "Failed to clear Android Auto playback history") }
    }

    private fun readEntries(context: Context): List<PlaybackHistoryEntry> {
        val raw = prefs(context).getString(KEY_ITEMS, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length())
            .mapNotNull { index ->
                runCatching {
                        val json = array.getJSONObject(index)
                        PlaybackHistoryEntry(
                            updatedAtMillis = json.optLong("updatedAtMillis"),
                            item = json.getJSONObject("item").toCatalogItem(),
                        )
                    }
                    .getOrNull()
            }
            .sortedByDescending { it.updatedAtMillis }
            .take(MAX_ITEMS)
    }

    private fun writeEntries(context: Context, entries: List<PlaybackHistoryEntry>) {
        val array =
            JSONArray().apply {
                entries.take(MAX_ITEMS).forEach { entry ->
                    put(
                        JSONObject()
                            .put("updatedAtMillis", entry.updatedAtMillis)
                            .put("item", entry.item.toJson())
                    )
                }
            }
        prefs(context).edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private data class PlaybackHistoryEntry(
        val updatedAtMillis: Long,
        val item: FindroidCarCatalogItem,
    )

    data class Entry(
        val updatedAtMillis: Long,
        val item: FindroidCarCatalogItem,
    )

    private fun FindroidCarCatalogItem.toJson(): JSONObject =
        JSONObject()
            .put("packageId", packageId)
            .put("itemId", itemId)
            .put("itemKind", itemKind.name)
            .put("playerItemKind", playerItemKind)
            .putNullable("seriesId", seriesId)
            .putNullable("seriesName", seriesName)
            .putNullable("seasonId", seasonId)
            .putNullable("seasonName", seasonName)
            .putNullable("indexNumber", indexNumber)
            .putNullable("parentIndexNumber", parentIndexNumber)
            .put("title", title)
            .put("subtitle", subtitle)
            .put("runtimeText", runtimeText)
            .put("runtimeTicks", runtimeTicks)
            .put("artworkPaths", JSONArray(artworkPaths))
            .putNullable("videoPath", videoPath)
            .put("played", played)
            .put("favorite", favorite)
            .put("playbackPositionTicks", playbackPositionTicks)
            .putNullable("unplayedItemCount", unplayedItemCount)

    private fun JSONObject.toCatalogItem(): FindroidCarCatalogItem =
        FindroidCarCatalogItem(
            packageId = getString("packageId"),
            itemId = getString("itemId"),
            itemKind = FindroidCarCatalogItemKind.valueOf(getString("itemKind")),
            playerItemKind = getString("playerItemKind"),
            seriesId = optNullableString("seriesId"),
            seriesName = optNullableString("seriesName"),
            seasonId = optNullableString("seasonId"),
            seasonName = optNullableString("seasonName"),
            indexNumber = optNullableInt("indexNumber"),
            parentIndexNumber = optNullableInt("parentIndexNumber"),
            title = getString("title"),
            subtitle = optString("subtitle"),
            runtimeText = optString("runtimeText"),
            runtimeTicks = optLong("runtimeTicks"),
            artworkPaths = getJSONArray("artworkPaths").toStringList(),
            videoPath = optNullableString("videoPath"),
            streamUrl = null,
            played = optBoolean("played"),
            favorite = optBoolean("favorite"),
            playbackPositionTicks = optLong("playbackPositionTicks"),
            unplayedItemCount = optNullableInt("unplayedItemCount"),
        )

    private fun JSONObject.putNullable(name: String, value: String?): JSONObject =
        put(name, value ?: JSONObject.NULL)

    private fun JSONObject.putNullable(name: String, value: Int?): JSONObject =
        put(name, value ?: JSONObject.NULL)

    private fun JSONObject.optNullableString(name: String): String? =
        if (isNull(name)) null else optString(name)

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (isNull(name)) null else optInt(name)

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).mapNotNull { index -> optString(index).takeIf { it.isNotBlank() } }
}
