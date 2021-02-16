import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.floor
import kotlin.math.sqrt

class WarpGrid(private val width: Int, private val height: Int, private val rows: Int, private val cols: Int) {
    private val gridPoints = Array(rows) { Array(cols) { GridPoint() } }
    private val buffer = ByteArray(width * height)

    fun relax() {
        for (y in 1 until rows - 1) {
            for (x in 1 until cols - 1) {
                gridPoints[y][x].integrate()
            }
        }
    }

    fun warp(destination: ByteArray) {
        System.arraycopy(destination, 0, buffer, 0, buffer.size)
        val latch = CountDownLatch(rows - 1)
        for (row in 0 until rows - 1) {
            ForkJoinPool.commonPool().execute {
                for (col in 0 until cols - 1) deform(col, row, destination)
                latch.countDown()
            }
        }
        latch.await()
    }

    private fun deform(col: Int, row: Int, dest: ByteArray) {
        val dx = width / (cols - 1.0)
        val dy = height / (rows - 1.0)
        val xi = floor(col * dx).toInt()
        val yi = floor(row * dy).toInt()
        val a = gridPoints[row][col]
        val b = gridPoints[row][col + 1]
        val c = gridPoints[row + 1][col + 1]
        val d = gridPoints[row + 1][col]
        val dxl = (d.px - a.px) / dx
        val dyl = (dy + d.py - a.py) / dy
        val dxr = (dx + c.px - dx + b.px) / dx
        val dyr = (dy + c.py - b.py) / dy
        var tx1 = xi + a.px
        var ty1 = yi + a.py
        var tx2 = xi + b.px + dx
        var ty2 = yi + b.py
        var y = 0
        while (y < dy) {
            var offset = (yi + y) * width + xi
            var tx = tx1
            var ty = ty1
            val hdx = (tx2 - tx1) / dx
            val hdy = (ty2 - ty1) / dy
            var x = 0
            while (x < dx) {
                dest[offset++] = buffer[(ty + 0.5).toInt() * width + (tx + 0.5).toInt()]
                tx += hdx
                ty += hdy
                ++x
            }
            tx1 += dxl
            ty1 += dyl
            tx2 += dxr
            ty2 += dyr
            ++y
        }
    }

    private class GridPoint {
        var px = 0.0
        var py = 0.0
        var vx = 0.0
        var vy = 0.0
        val rnd = ThreadLocalRandom.current()
        fun integrate() {
            val length = 0.009998 / sqrt(px * px + py * py)
            vx -= px * length + rnd.nextDouble(-0.001, 0.001)
            vy -= py * length + rnd.nextDouble(-0.001, 0.001)
            px += vx
            py += vy
        }
    }

    init {
        val rnd = ThreadLocalRandom.current()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val point = gridPoints[row][col]
                if (col > 0 && row > 0 && col < cols - 1 && row < rows - 1) {
                    point.px = rnd.nextDouble(-2.5, 2.5)
                    point.py = rnd.nextDouble(-2.5, 2.5)
                    point.vx = rnd.nextDouble(-0.1, 0.2)
                    point.vy = rnd.nextDouble(-0.1, 0.2)
                }
            }
        }
    }
}
