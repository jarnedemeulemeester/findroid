package dev.jdtech.jellyfin.utils

import dev.jdtech.jellyfin.models.FindroidChapter
import dev.jdtech.jellyfin.models.FindroidImages
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.UiText
import java.util.UUID

/** Minimal item; the queue only ever reads its id and name. */
data class TestItem(
    override val id: UUID = UUID.randomUUID(),
    override val name: String = "Episode",
) : FindroidItem {
    override val originalTitle: String? = null
    override val overview: String = ""
    override val played: Boolean = false
    override val favorite: Boolean = false
    override val canPlay: Boolean = true
    override val canDownload: Boolean = true
    override val sources: List<FindroidSource> = emptyList()
    override val runtimeTicks: Long = 0
    override val playbackPositionTicks: Long = 0
    override val unplayedItemCount: Int? = null
    override val images: FindroidImages = FindroidImages()
    override val chapters: List<FindroidChapter> = emptyList()
}

fun request(item: TestItem) = DownloadRequest(item = item, sourceId = "source-${item.id}")

/** Records what the queue asked of it, and lets a test steer each download's outcome. */
class FakeDownloader : Downloader {
    /** Item ids in the order downloadItem was called for them. */
    val started = mutableListOf<UUID>()

    /** Item ids the queue asked to cancel. */
    val cancelled = mutableListOf<UUID>()

    /** How many downloads were in flight at once, at the high-water mark. */
    var peakConcurrent = 0
        private set

    private var inFlight = 0
    private var nextDownloadId = 1L

    /** Per item id: the sequence of statuses getDownloadInfo will report, then the last repeats. */
    val statuses = mutableMapOf<UUID, MutableList<DownloadInfo>>()

    /** Item ids whose downloadItem call should report a failure to start. */
    val failToStart = mutableSetOf<UUID>()

    private val idToItem = mutableMapOf<Long, UUID>()

    /** Downloads that exist without the queue having started them, as after a restart. */
    val externalStatuses = mutableMapOf<Long, DownloadInfo>()

    override suspend fun downloadItem(
        item: FindroidItem,
        sourceId: String,
        storageIndex: Int,
    ): Pair<Long, UiText?> {
        started += item.id
        inFlight++
        if (inFlight > peakConcurrent) peakConcurrent = inFlight
        if (item.id in failToStart) {
            inFlight--
            return Pair(-1L, null)
        }
        val id = nextDownloadId++
        idToItem[id] = item.id
        return Pair(id, null)
    }

    override suspend fun cancelDownload(item: FindroidItem, downloadId: Long) {
        cancelled += item.id
    }

    override suspend fun deleteItem(item: FindroidItem, source: FindroidSource) = Unit

    override suspend fun getProgress(downloadId: Long?): Pair<Int, Int> = Pair(0, 0)

    override suspend fun getDownloadInfo(downloadId: Long): DownloadInfo? {
        externalStatuses[downloadId]?.let {
            return it
        }
        val itemId = idToItem[downloadId] ?: return null
        val queued = statuses[itemId]
        val info =
            when {
                queued == null -> succeeded()
                queued.size > 1 -> queued.removeAt(0)
                else -> queued.first()
            }
        if (info.status == android.app.DownloadManager.STATUS_SUCCESSFUL || info.status == FAILED) {
            inFlight = (inFlight - 1).coerceAtLeast(0)
        }
        return info
    }

    companion object {
        const val FAILED = android.app.DownloadManager.STATUS_FAILED
        const val RUNNING = android.app.DownloadManager.STATUS_RUNNING

        fun succeeded() = DownloadInfo(android.app.DownloadManager.STATUS_SUCCESSFUL, 100)

        fun running(progress: Int = 10, bytes: Long = 1_000) =
            DownloadInfo(RUNNING, progress, bytesDownloaded = bytes)

        fun failed(reason: Int) = DownloadInfo(FAILED, -1, reason)
    }
}

/** In-memory stand-in for the two database facts the queue needs. */
class FakeDownloadedSources(
    private val downloaded: MutableSet<Pair<UUID, String>> = mutableSetOf(),
    private val downloadIds: MutableMap<Pair<UUID, String>, Long> = mutableMapOf(),
) : DownloadedSources {
    /** Which items had their recorded download id looked up, so tests can be specific. */
    val downloadIdLookups = mutableListOf<UUID>()

    fun markDownloaded(itemId: UUID, sourceId: String) {
        downloaded += itemId to sourceId
    }

    fun recordDownloadId(itemId: UUID, sourceId: String, id: Long) {
        downloadIds[itemId to sourceId] = id
    }

    override suspend fun isDownloaded(itemId: UUID, sourceId: String) =
        (itemId to sourceId) in downloaded

    override suspend fun downloadIdFor(itemId: UUID, sourceId: String): Long? {
        downloadIdLookups += itemId
        return downloadIds[itemId to sourceId]
    }
}

/** In-memory stand-in for the table the queue mirrors itself into. */
class FakeDownloadQueueStore(seed: List<PersistedQueueEntry> = emptyList()) : DownloadQueueStore {
    private val rows = seed.associateByTo(linkedMapOf()) { it.itemId }

    /** Everything currently persisted, in queue order. */
    val saved: List<PersistedQueueEntry>
        get() = rows.values.sortedBy { it.position }

    override suspend fun load(): List<PersistedQueueEntry> = saved

    override suspend fun save(entry: PersistedQueueEntry) {
        rows[entry.itemId] = entry
    }

    override suspend fun remove(itemId: UUID) {
        rows.remove(itemId)
    }
}

/** Hands back the items a test has registered, and null for anything else. */
class FakeItemLoader(items: List<TestItem> = emptyList()) : QueuedItemLoader {
    private val known = items.associateBy { it.id }.toMutableMap()

    fun add(item: TestItem) {
        known[item.id] = item
    }

    override suspend fun load(itemId: UUID, type: QueuedItemType) = known[itemId]
}

/** A persisted row for [item], as a previous run would have left behind. */
fun persisted(
    item: TestItem,
    batchId: UUID,
    status: DownloadEntryStatus = DownloadEntryStatus.QUEUED,
    downloadId: Long? = null,
    position: Long = 0,
    batchTotal: Int = 1,
) =
    PersistedQueueEntry(
        itemId = item.id,
        batchId = batchId,
        itemType = QueuedItemType.EPISODE,
        sourceId = "source-${item.id}",
        storageIndex = 0,
        status = status,
        downloadId = downloadId,
        retries = 0,
        position = position,
        batchTotal = batchTotal,
    )
