package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the behaviours that were actually got wrong while this queue was being built: cancelling
 * deleting finished downloads, an entry reaching the cancel path that had never been started, a
 * batch of duplicates reported as new, downloads overlapping, and a retry that never happened.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DownloadQueueImplTest {

    /**
     * The queue's own scope. It shares the test's scheduler and its dispatcher loop never
     * completes, so it has to be cancelled before the test body ends: runTest drains the scheduler
     * on the way out and would otherwise never finish.
     */
    private var queueScope: TestScope? = null

    private fun TestScope.queue(
        downloader: FakeDownloader = FakeDownloader(),
        sources: FakeDownloadedSources = FakeDownloadedSources(),
        store: FakeDownloadQueueStore = FakeDownloadQueueStore(),
        loadItem: FakeItemLoader = FakeItemLoader(),
        sequential: Boolean = true,
        onWorkAccepted: () -> Unit = {},
    ): DownloadQueueImpl {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val ownScope = TestScope(dispatcher)
        queueScope = ownScope
        return DownloadQueueImpl(
            downloader = downloader,
            sources = sources,
            store = store,
            loadItem = loadItem,
            isSequentialEnabled = { sequential },
            onWorkAccepted = onWorkAccepted,
            scope = ownScope,
            ioDispatcher = dispatcher,
            pollIntervalMs = 1,
        )
    }

    /**
     * Advances a bounded amount rather than to idle. Two reasons: some tests deliberately keep a
     * download running forever, so there is no idle to reach, and advancing to idle would also run
     * the queue's 30s prune, which clears the very entries being asserted on.
     */
    private fun TestScope.settle() {
        advanceTimeBy(1_000)
        runCurrent()
    }

    /**
     * runTest, with the queue's scope cancelled inside it and in a finally.
     *
     * Both details are load bearing. Inside, because runTest drains the shared scheduler on its way
     * out and the queue's dispatcher loop never completes, so a cancel after the body has returned
     * is already too late — @After cannot do this job, it runs after the drain has wedged. And in a
     * finally, because an assertion throwing part way through would otherwise skip the cancel and
     * turn a plain test failure into the whole run spinning at full CPU until something kills it.
     */
    private fun queueTest(body: suspend TestScope.() -> Unit) = runTest {
        try {
            body()
        } finally {
            queueScope?.cancel()
            queueScope = null
        }
    }

    @Test
    fun `submitting with sequential off leaves the queue untouched`() = queueTest {
        val downloader = FakeDownloader()
        val queue = queue(downloader = downloader, sequential = false)

        val outcome = queue.submit(listOf(request(TestItem())))
        settle()

        assertEquals(SubmitOutcome.Bypassed, outcome)
        assertTrue("nothing should have been downloaded", downloader.started.isEmpty())
        assertTrue("no state should be published", queue.state.value.entries.isEmpty())
    }

    @Test
    fun `work accepted raises the foreground service`() = queueTest {
        var raised = 0
        val queue = queue(onWorkAccepted = { raised++ })

        queue.submit(listOf(request(TestItem())))
        settle()

        assertEquals(1, raised)
    }

    @Test
    fun `sequential off never raises the foreground service`() = queueTest {
        var raised = 0
        val queue = queue(sequential = false, onWorkAccepted = { raised++ })

        queue.submit(listOf(request(TestItem())))
        settle()

        assertEquals(0, raised)
    }

    @Test
    fun `items download one at a time, in the order submitted`() = queueTest {
        val downloader = FakeDownloader()
        val items = List(4) { TestItem(name = "Episode $it") }
        // Each reports running once before finishing, so an overlap would be observable.
        items.forEach {
            downloader.statuses[it.id] =
                mutableListOf(FakeDownloader.running(), FakeDownloader.succeeded())
        }
        val queue = queue(downloader = downloader)

        queue.submit(items.map { request(it) })
        settle()

        assertEquals(items.map { it.id }, downloader.started)
        assertEquals("downloads must not overlap", 1, downloader.peakConcurrent)
    }

    @Test
    fun `an item already on disk is not downloaded again`() = queueTest {
        val downloader = FakeDownloader()
        val sources = FakeDownloadedSources()
        val item = TestItem()
        sources.markDownloaded(item.id, "source-${item.id}")
        val queue = queue(downloader = downloader, sources = sources)

        queue.submit(listOf(request(item)))
        settle()

        assertTrue(downloader.started.isEmpty())
        assertEquals(DownloadEntryStatus.SUCCEEDED, queue.state.value.entries.single().status)
    }

    @Test
    fun `resubmitting live items reports the batch that already owns them`() = queueTest {
        val downloader = FakeDownloader()
        val items = List(2) { TestItem() }
        items.forEach { downloader.statuses[it.id] = mutableListOf(FakeDownloader.running()) }
        val queue = queue(downloader = downloader)

        val first = queue.submit(items.map { request(it) }) as SubmitOutcome.Queued
        settle()
        val second = queue.submit(items.map { request(it) }) as SubmitOutcome.Queued
        settle()

        assertTrue("the first submit creates the batch", first.isNewBatch)
        assertFalse("the second must not claim a new batch", second.isNewBatch)
        assertEquals("and must point at the owning batch", first.batchId, second.batchId)
    }

    @Test
    fun `cancelling a batch leaves finished downloads alone`() = queueTest {
        val downloader = FakeDownloader()
        val done = TestItem(name = "already finished")
        val running = TestItem(name = "still going")
        downloader.statuses[done.id] = mutableListOf(FakeDownloader.succeeded())
        downloader.statuses[running.id] = mutableListOf(FakeDownloader.running())
        val queue = queue(downloader = downloader)

        val outcome = queue.submit(listOf(request(done), request(running))) as SubmitOutcome.Queued
        settle()
        queue.cancelBatch(outcome.batchId)
        settle()

        assertFalse(
            "a succeeded download must never reach cancelDownload, which deletes it",
            done.id in downloader.cancelled,
        )
        assertTrue("the one in flight should be cancelled", running.id in downloader.cancelled)
    }

    @Test
    fun `cancelling never touches an item that was only queued`() = queueTest {
        val downloader = FakeDownloader()
        val sources = FakeDownloadedSources()
        val running = TestItem(name = "in flight")
        val waiting = TestItem(name = "still queued")
        downloader.statuses[running.id] = mutableListOf(FakeDownloader.running())
        val queue = queue(downloader = downloader, sources = sources)

        val outcome =
            queue.submit(listOf(request(running), request(waiting))) as SubmitOutcome.Queued
        settle()
        queue.cancelBatch(outcome.batchId)
        settle()

        assertFalse(
            "a queued item was never enqueued, so there is nothing of ours to cancel",
            waiting.id in downloader.cancelled,
        )
        assertFalse(
            "and looking its id up could match an unrelated finished download",
            waiting.id in sources.downloadIdLookups,
        )
    }

    @Test
    fun `an interrupted download is retried, because a transcode cannot resume`() = queueTest {
        val downloader = FakeDownloader()
        val item = TestItem()
        downloader.statuses[item.id] =
            mutableListOf(
                FakeDownloader.failed(DownloadManager.ERROR_CANNOT_RESUME),
                FakeDownloader.succeeded(),
            )
        val queue = queue(downloader = downloader)

        queue.submit(listOf(request(item)))
        settle()

        assertEquals("it should have been started twice", 2, downloader.started.size)
        assertEquals(DownloadEntryStatus.SUCCEEDED, queue.state.value.entries.single().status)
    }

    @Test
    fun `running out of room is not retried`() = queueTest {
        val downloader = FakeDownloader()
        val item = TestItem()
        downloader.statuses[item.id] =
            mutableListOf(FakeDownloader.failed(DownloadManager.ERROR_INSUFFICIENT_SPACE))
        val queue = queue(downloader = downloader)

        queue.submit(listOf(request(item)))
        settle()

        assertEquals("retrying would fail the same way", 1, downloader.started.size)
        assertEquals(DownloadEntryStatus.FAILED, queue.state.value.entries.single().status)
    }

    @Test
    fun `a failure to start does not stall the rest of the batch`() = queueTest {
        val downloader = FakeDownloader()
        val bad = TestItem(name = "cannot start")
        val good = TestItem(name = "fine")
        downloader.failToStart += bad.id
        downloader.statuses[good.id] = mutableListOf(FakeDownloader.succeeded())
        val queue = queue(downloader = downloader)

        queue.submit(listOf(request(bad), request(good)))
        settle()

        assertTrue("the queue must move past the one that failed", good.id in downloader.started)
        val entries = queue.state.value.entries.associateBy { it.itemId }
        assertEquals(DownloadEntryStatus.FAILED, entries.getValue(bad.id).status)
        assertEquals(DownloadEntryStatus.SUCCEEDED, entries.getValue(good.id).status)
    }

    @Test
    fun `a queue left by a previous run is picked back up`() = queueTest {
        val downloader = FakeDownloader()
        val batch = UUID.randomUUID()
        val waiting = List(3) { TestItem(name = "Episode $it") }
        val store =
            FakeDownloadQueueStore(
                waiting.mapIndexed { index, item ->
                    persisted(item, batch, position = index.toLong(), batchTotal = 3)
                }
            )
        val queue =
            queue(downloader = downloader, store = store, loadItem = FakeItemLoader(waiting))

        queue.resume()
        settle()

        assertEquals(
            "the whole queue should come back, in its original order",
            waiting.map { it.id },
            downloader.started,
        )
    }

    @Test
    fun `a download still in flight from a previous run is adopted, not started again`() = queueTest {
        val downloader = FakeDownloader()
        val batch = UUID.randomUUID()
        val item = TestItem(name = "already downloading")
        // A previous run got as far as handing this to DownloadManager, which is still working.
        downloader.externalStatuses[77L] = FakeDownloader.running(bytes = 5_000)
        val store =
            FakeDownloadQueueStore(
                listOf(
                    persisted(item, batch, status = DownloadEntryStatus.RUNNING, downloadId = 77L)
                )
            )
        val queue =
            queue(downloader = downloader, store = store, loadItem = FakeItemLoader(listOf(item)))

        queue.resume()
        settle()

        assertTrue(
            "re-downloading it would leave two copies of the same file on disk",
            downloader.started.isEmpty(),
        )
        val entry = queue.state.value.entries.single()
        assertEquals(DownloadEntryStatus.RUNNING, entry.status)
        assertEquals(77L, entry.downloadId)
    }

    @Test
    fun `a batch that already finished does not come back`() = queueTest {
        val batch = UUID.randomUUID()
        val done = TestItem(name = "finished last time")
        val store =
            FakeDownloadQueueStore(
                listOf(persisted(done, batch, status = DownloadEntryStatus.SUCCEEDED))
            )
        val queue = queue(store = store, loadItem = FakeItemLoader(listOf(done)))

        queue.resume()
        settle()

        assertTrue(
            "a completed season must not put its card back on screen at every launch",
            queue.state.value.entries.isEmpty(),
        )
        assertTrue("and its rows should be gone", store.saved.isEmpty())
    }

    @Test
    fun `an entry whose item can no longer be loaded is dropped`() = queueTest {
        val batch = UUID.randomUUID()
        val gone = TestItem(name = "removed from the server")
        val store = FakeDownloadQueueStore(listOf(persisted(gone, batch)))
        // Nothing registered in the loader: the item cannot be rebuilt.
        val queue = queue(store = store, loadItem = FakeItemLoader())

        queue.resume()
        settle()

        assertTrue(queue.state.value.entries.isEmpty())
        assertTrue("it would otherwise be retried forever", store.saved.isEmpty())
    }

    @Test
    fun `a queued item is written down so a restart can find it`() = queueTest {
        val store = FakeDownloadQueueStore()
        val downloader = FakeDownloader()
        val items = List(2) { TestItem() }
        items.forEach { downloader.statuses[it.id] = mutableListOf(FakeDownloader.running()) }
        val queue = queue(downloader = downloader, store = store)

        queue.submit(items.map { request(it) })
        settle()

        assertEquals("both items belong in the store", 2, store.saved.size)
        assertEquals(
            "and in the order they were submitted",
            items.map { it.id },
            store.saved.map { it.itemId },
        )
    }

    @Test
    fun `cancelling a batch clears what was written down`() = queueTest {
        val store = FakeDownloadQueueStore()
        val downloader = FakeDownloader()
        val items = List(2) { TestItem() }
        items.forEach { downloader.statuses[it.id] = mutableListOf(FakeDownloader.running()) }
        val queue = queue(downloader = downloader, store = store)

        val outcome = queue.submit(items.map { request(it) }) as SubmitOutcome.Queued
        settle()
        queue.cancelBatch(outcome.batchId)
        settle()

        assertTrue(
            "a cancelled batch must not come back on the next launch",
            store.saved.isEmpty(),
        )
    }

    @Test
    fun `a download already in flight is adopted instead of fetched twice`() = queueTest {
        val downloader = FakeDownloader()
        val sources = FakeDownloadedSources()
        val item = TestItem(name = "already in flight")
        // A previous run handed this to DownloadManager and recorded the id, then died before the
        // queue could be written down.
        sources.recordDownloadId(item.id, "source-${item.id}", 99L)
        downloader.externalStatuses[99L] = FakeDownloader.running(bytes = 2_000)
        val queue = queue(downloader = downloader, sources = sources)

        queue.submit(listOf(request(item)))
        settle()

        assertTrue(
            "starting it again would leave two copies of the same file on disk",
            downloader.started.isEmpty(),
        )
        val entry = queue.state.value.entries.single()
        assertEquals(DownloadEntryStatus.RUNNING, entry.status)
        assertEquals(99L, entry.downloadId)
    }

    @Test
    fun `cancelling a single item clears what was written down`() = queueTest {
        val store = FakeDownloadQueueStore()
        val downloader = FakeDownloader()
        val item = TestItem()
        downloader.statuses[item.id] = mutableListOf(FakeDownloader.running())
        val queue = queue(downloader = downloader, store = store)

        queue.submit(listOf(request(item)))
        settle()
        queue.cancel(setOf(item.id))
        settle()

        assertTrue(
            "a cancelled item must not come back on the next launch",
            store.saved.isEmpty(),
        )
    }
}
