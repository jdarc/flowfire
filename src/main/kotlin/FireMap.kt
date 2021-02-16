import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.IndexColorModel
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.*

class FireMap(width: Int, height: Int) {
    val image = BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, generateColorMap())
    val pixels: ByteArray = (image.raster.dataBuffer as DataBufferByte).data
    private val reducer = ByteArray(pixels.size)
    private val temp = ByteArray(pixels.size)
    private val tasks = ArrayList<Callable<Void>>()
    private var offset = 0

    fun draw5PointedStar(x: Double, y: Double, imax: Int, scale: Double, ft1: Double, ft2: Double, ft3: Double) {
        val fr1Next = 588.0 / 0x10000
        val fr5Next = 2941.0 / 0x10000
        var fr1 = ft1
        var fr5 = ft1 * 5.0
        val sinft3 = sin(ft3)
        for (i in 0 until imax) {
            fr1 += fr1Next
            fr5 += fr5Next
            val scaler = scale * (2.0 + sin(fr5 - ft2) * sinft3)
            val x1 = (scaler * sin(fr1) + x).toInt()
            val y1 = (scaler * cos(fr1) + y).toInt()
            setPixel(x1, y1)
            setPixel(x1, y1 + 1)
            setPixel(x1, y1 - 1)
            setPixel(x1 - 1, y1)
            setPixel(x1 + 1, y1)
            setPixel(x1 - 1, y1 - 1)
            setPixel(x1 + 1, y1 - 1)
            setPixel(x1 + 1, y1 + 1)
            setPixel(x1 - 1, y1 + 1)
        }
    }

    private fun setPixel(x: Int, y: Int) {
        if (x >= 0 && y >= 0 && x < image.width && y < image.height) pixels[y * image.width + x] = 255.toByte()
    }

    fun reduce() {
        for (y in 0 until image.height) {
            val mo = y * image.width
            for (x in 0 until image.width) {
                pixels[mo + x] = max(0, (pixels[mo + x].toInt() and 255) - (reducer[offset++].toInt() and 255)).toByte()
            }
            offset = ++offset % (reducer.size - image.width)
        }
    }

    fun smooth() = smooth(pixels)

    fun scroll(amount: Int) =
        System.arraycopy(pixels, amount * image.width, pixels, 0, pixels.size - amount * image.width)

    private fun smooth(buffer: ByteArray) {
        if (tasks.isEmpty()) {
            var by = 0
            while (by < image.height) {
                val finalBy = by
                var bx = 0
                while (bx < image.width) {
                    val finalBx = bx
                    tasks.add(Callable {
                        val limitY = min(finalBy + 32, image.height)
                        val limitX = min(finalBx + 32, image.width)
                        for (y in finalBy until limitY) {
                            for (x in finalBx until limitX) {
                                var accumulator = 0
                                for (fy in 0..2) {
                                    for (fx in 0..2) {
                                        val clampX = (x + fx - 1).coerceIn(0, image.width - 1)
                                        val clampY = (y + fy - 1).coerceIn(0, image.height - 1)
                                        accumulator += FILTER[fy * 3 + fx] * (255 and buffer[clampY * image.width + clampX].toInt())
                                    }
                                }
                                temp[y * image.width + x] = (accumulator shr 4).toByte()
                            }
                        }
                        null
                    })
                    bx += 32
                }
                by += 32
            }
        }
        ForkJoinPool.commonPool().invokeAll(tasks)
        System.arraycopy(temp, 0, buffer, 0, buffer.size)
    }

    companion object {
        private val FILTER = intArrayOf(1, 2, 1, 2, 4, 2, 1, 2, 1)

        private fun generateColorMap(): IndexColorModel {
            val markers = intArrayOf(0x000000, 0xAA0000, 0xFF6600, 0xFFAA00, 0xFFFF00)
            val red = ByteArray(256)
            val grn = ByteArray(256)
            val blu = ByteArray(256)
            var eR = 0.0
            var eG = 0.0
            var eB = 0.0
            val step = 256.0 / (markers.size - 1)
            for (j in 1 until markers.size) {
                var sR = eR
                var sG = eG
                var sB = eB
                eR = 255.and(markers[j].shr(16)).toDouble()
                eG = 255.and(markers[j].shr(8)).toDouble()
                eB = 255.and(markers[j]).toDouble()
                val dr = (eR - sR) / step
                val dg = (eG - sG) / step
                val db = (eB - sB) / step
                var i = 0.0
                while (i < step) {
                    val index = floor((j - 1) * step + i).toInt()
                    red[index] = floor(sR).toInt().coerceIn(0, 255).toByte()
                    grn[index] = floor(sG).toInt().coerceIn(0, 255).toByte()
                    blu[index] = floor(sB).toInt().coerceIn(0, 255).toByte()
                    sR += dr
                    sG += dg
                    sB += db
                    i += 1.0
                }
            }
            return IndexColorModel(8, 256, red, grn, blu)
        }
    }

    init {
        val rnd = ThreadLocalRandom.current()
        for (ctr in reducer.indices) {
            reducer[ctr] = (rnd.nextDouble(1.0).pow(3.0) * 4.0).toInt().toByte()
        }
    }
}
