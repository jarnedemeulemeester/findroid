package dev.jdtech.jellyfin.utils.bif

import java.nio.ByteOrder

class Indexed8Array(private val array: ByteArray) {

    private var readIndex = 0

    fun read(): Byte {
        return array[readIndex++]
    }

    fun addPosition(amount: Int) {
        readIndex += amount
    }

    fun readInt32(): Int {
        val b1 = read().toInt().and(0xFF)
        val b2 = read().toInt().and(0xFF)
        val b3 = read().toInt().and(0xFF)
        val b4 = read().toInt().and(0xFF)

        return b1 or (b2 shl 8) or (b3 shl 16) or (b4 shl 24)
    }

    fun array(): ByteArray {
        return array
    }

    fun limit(): Int {
        return array.size
    }

    fun order(): ByteOrder {
        return ByteOrder.BIG_ENDIAN
    }
}
