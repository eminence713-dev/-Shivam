package com.example.util

import java.nio.charset.StandardCharsets

/**
 * Pure-Kotlin ISO/IEC 18004 QR Code (Byte mode, ECC Level L, Versions 1..10) matrix encoder
 * for rendering genuinely scannable verified-merchant UPI QR codes without external native dependencies.
 */
object QrCodeGenerator {

    private data class VersionSpec(
        val version: Int,
        val totalCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocks: Int,
        val alignmentCenters: IntArray
    )

    private val SPECS = listOf(
        VersionSpec(2, 44, 10, 1, intArrayOf(6, 18)),
        VersionSpec(3, 70, 15, 1, intArrayOf(6, 22)),
        VersionSpec(4, 100, 20, 1, intArrayOf(6, 26)),
        VersionSpec(5, 134, 26, 1, intArrayOf(6, 30)),
        VersionSpec(6, 172, 18, 2, intArrayOf(6, 34)),
        VersionSpec(7, 196, 20, 2, intArrayOf(6, 22, 38)),
        VersionSpec(8, 242, 24, 2, intArrayOf(6, 24, 42)),
        VersionSpec(9, 292, 30, 2, intArrayOf(6, 26, 46)),
        VersionSpec(10, 346, 18, 4, intArrayOf(6, 28, 50))
    )

    fun encode(text: String): Array<BooleanArray>? {
        val payload = text.toByteArray(StandardCharsets.UTF_8)
        val spec = SPECS.firstOrNull { s ->
            val dataCapacityBytes = (s.totalCodewords - s.ecCodewordsPerBlock * s.numBlocks) - 3
            payload.size <= dataCapacityBytes
        } ?: return null

        val dataCapacity = spec.totalCodewords - spec.ecCodewordsPerBlock * spec.numBlocks
        val bits = ArrayList<Int>(dataCapacity * 8)

        fun appendBits(value: Int, len: Int) {
            for (i in len - 1 downTo 0) bits.add((value ushr i) and 1)
        }

        // Byte mode indicator (0100) + character count (8 bits for V1..9, 16 bits for V10)
        appendBits(0b0100, 4)
        appendBits(payload.size, if (spec.version <= 9) 8 else 16)
        for (b in payload) appendBits(b.toInt() and 0xFF, 8)

        val terminator = minOf(4, dataCapacity * 8 - bits.size)
        repeat(terminator) { bits.add(0) }
        while (bits.size % 8 != 0) bits.add(0)

        val dataBytes = IntArray(dataCapacity)
        for (i in 0 until bits.size / 8) {
            var v = 0
            for (j in 0 until 8) v = (v shl 1) or bits[i * 8 + j]
            dataBytes[i] = v
        }
        var padIdx = bits.size / 8
        var toggle = true
        while (padIdx < dataCapacity) {
            dataBytes[padIdx++] = if (toggle) 0xEC else 0x11
            toggle = !toggle
        }

        // Split into blocks & compute Reed-Solomon EC codewords
        val shortBlockLen = dataCapacity / spec.numBlocks
        val numLongBlocks = dataCapacity % spec.numBlocks
        val numShortBlocks = spec.numBlocks - numLongBlocks
        val dataBlocks = ArrayList<IntArray>()
        val ecBlocks = ArrayList<IntArray>()
        var offset = 0
        val genPoly = reedSolomonDivisor(spec.ecCodewordsPerBlock)

        for (b in 0 until spec.numBlocks) {
            val len = shortBlockLen + if (b >= numShortBlocks) 1 else 0
            val block = dataBytes.copyOfRange(offset, offset + len)
            offset += len
            dataBlocks.add(block)
            ecBlocks.add(reedSolomonRemainder(block, genPoly))
        }

        val interleaved = ArrayList<Int>(spec.totalCodewords)
        val maxDataLen = shortBlockLen + if (numLongBlocks > 0) 1 else 0
        for (i in 0 until maxDataLen) {
            for (block in dataBlocks) if (i < block.size) interleaved.add(block[i])
        }
        for (i in 0 until spec.ecCodewordsPerBlock) {
            for (ec in ecBlocks) interleaved.add(ec[i])
        }

        val size = spec.version * 4 + 17
        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        fun setFunc(x: Int, y: Int, dark: Boolean) {
            if (x in 0 until size && y in 0 until size) {
                modules[y][x] = dark
                isFunction[y][x] = true
            }
        }

        // Timing patterns
        for (i in 0 until size) {
            setFunc(6, i, i % 2 == 0)
            setFunc(i, 6, i % 2 == 0)
        }

        // Finder patterns + separators
        fun drawFinder(cx: Int, cy: Int) {
            for (dy in -4..4) {
                for (dx in -4..4) {
                    val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                    setFunc(cx + dx, cy + dy, dist != 2 && dist != 4)
                }
            }
        }
        drawFinder(3, 3)
        drawFinder(size - 4, 3)
        drawFinder(3, size - 4)

        // Alignment patterns
        val centers = spec.alignmentCenters
        for (cy in centers) {
            for (cx in centers) {
                if ((cx == 6 && cy == 6) || (cx == 6 && cy == size - 7) || (cx == size - 7 && cy == 6)) continue
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        setFunc(cx + dx, cy + dy, maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != 1)
                    }
                }
            }
        }

        // Reserve format bits & dark module
        for (i in 0..8) {
            if (i != 6) {
                setFunc(8, i, false)
                setFunc(i, 8, false)
            }
        }
        for (i in 0..7) {
            setFunc(size - 1 - i, 8, false)
            setFunc(8, size - 1 - i, false)
        }
        setFunc(8, size - 8, true)

        // Version info for V >= 7
        if (spec.version >= 7) {
            var rem = spec.version
            repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25) }
            val vBits = (spec.version shl 12) or rem
            for (i in 0 until 18) {
                val bit = ((vBits ushr i) and 1) != 0
                val a = size - 11 + i % 3
                val b = i / 3
                setFunc(a, b, bit)
                setFunc(b, a, bit)
            }
        }

        // Zigzag data placement
        var bitIdx = 0
        val totalBits = interleaved.size * 8
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until size) {
                for (j in 0..1) {
                    val x = right - j
                    val upward = ((right + 1) and 2) == 0
                    val y = if (upward) size - 1 - vert else vert
                    if (!isFunction[y][x] && bitIdx < totalBits) {
                        val b = ((interleaved[bitIdx ushr 3] ushr (7 - (bitIdx and 7))) and 1) != 0
                        modules[y][x] = b
                        bitIdx++
                    }
                }
            }
            right -= 2
        }

        // Apply Mask 0: (x + y) % 2 == 0 and write ECC Level L (01) + Mask 0 (000) format bits
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!isFunction[y][x] && (x + y) % 2 == 0) {
                    modules[y][x] = !modules[y][x]
                }
            }
        }

        val dataFormat = (0b01 shl 3) or 0 // ECC L = 01, Mask = 000
        var rem = dataFormat
        repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }
        val formatBits = ((dataFormat shl 10) or rem) xor 0x5412

        fun fBit(i: Int) = ((formatBits ushr i) and 1) != 0
        for (i in 0..5) setFunc(8, i, fBit(i))
        setFunc(8, 7, fBit(6))
        setFunc(8, 8, fBit(7))
        setFunc(7, 8, fBit(8))
        for (i in 9..14) setFunc(14 - i, 8, fBit(i))

        for (i in 0..7) setFunc(size - 1 - i, 8, fBit(i))
        for (i in 8..14) setFunc(8, size - 15 + i, fBit(i))
        setFunc(8, size - 8, true)

        return modules
    }

    private fun gfMul(x: Int, y: Int): Int {
        var z = 0
        for (i in 7 downTo 0) {
            z = (z shl 1) xor ((z ushr 7) * 0x11D)
            z = z xor (((y ushr i) and 1) * x)
        }
        return z
    }

    private fun reedSolomonDivisor(degree: Int): IntArray {
        val result = IntArray(degree)
        result[degree - 1] = 1
        var root = 1
        repeat(degree) {
            for (j in 0 until degree) {
                result[j] = gfMul(result[j], root)
                if (j + 1 < degree) result[j] = result[j] xor result[j + 1]
            }
            root = gfMul(root, 0x02)
        }
        return result
    }

    private fun reedSolomonRemainder(data: IntArray, divisor: IntArray): IntArray {
        val result = IntArray(divisor.size)
        for (b in data) {
            val factor = b xor result[0]
            System.arraycopy(result, 1, result, 0, result.size - 1)
            result[result.size - 1] = 0
            for (i in divisor.indices) {
                result[i] = result[i] xor gfMul(divisor[i], factor)
            }
        }
        return result
    }
}
