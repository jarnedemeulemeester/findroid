package dev.jdtech.jellyfin.offline.transfer

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Mp4MarkerScannerTest {
    @Test
    fun detectsFragmentedMp4MarkersAcrossBufferBoundaries() {
        val file =
            temporaryFile(
                "aaftypisomxxmoo",
                "fyyzzzsid",
                "xqqqst",
                "bl",
            )

        val scan = Mp4MarkerScanner(bufferSize = 5).scan(file)

        assertTrue(scan.hasFtyp)
        assertTrue(scan.hasMoof)
        assertTrue(scan.hasSidx)
        assertTrue(scan.hasSampleTable)
        assertTrue(scan.requiresMp4Remux)
    }

    @Test
    fun doesNotRequestRemuxForNonFragmentedMp4() {
        val file = temporaryFile("aaftypisom", "moov", "trak", "mdia", "minf", "stbl")

        val scan = Mp4MarkerScanner(bufferSize = 4).scan(file)

        assertTrue(scan.hasFtyp)
        assertFalse(scan.hasMoof)
        assertTrue(scan.hasSampleTable)
        assertFalse(scan.requiresMp4Remux)
    }

    @Test
    fun fragmentedMp4WithoutSampleTableRequestsRemux() {
        val file = temporaryFile("ftyp", "isom", "moof", "mdat")

        val scan = Mp4MarkerScanner(bufferSize = 4).scan(file)

        assertTrue(scan.hasFtyp)
        assertTrue(scan.hasMoof)
        assertFalse(scan.hasSampleTable)
        assertTrue(scan.requiresMp4Remux)
    }

    @Test
    fun sampleTableWithoutFragmentedMarkerDoesNotRequestRemux() {
        val file = temporaryFile("ftyp", "isom", "moov", "trak", "stbl", "mdat")

        val scan = Mp4MarkerScanner(bufferSize = 4).scan(file)

        assertTrue(scan.hasFtyp)
        assertFalse(scan.hasMoof)
        assertTrue(scan.hasSampleTable)
        assertFalse(scan.requiresMp4Remux)
    }

    @Test
    fun fragmentedMarkerWithoutMp4HeaderDoesNotRequestRemux() {
        val file = temporaryFile("random", "moof", "payload")

        val scan = Mp4MarkerScanner(bufferSize = 4).scan(file)

        assertFalse(scan.hasFtyp)
        assertTrue(scan.hasMoof)
        assertFalse(scan.requiresMp4Remux)
    }

    private fun temporaryFile(vararg chunks: String): File {
        val file = kotlin.io.path.createTempFile(prefix = "mp4-marker", suffix = ".bin").toFile()
        file.writeBytes(chunks.joinToString(separator = "").encodeToByteArray())
        file.deleteOnExit()
        return file
    }
}
