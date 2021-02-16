import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import kotlin.math.sin

class FirePanel : JPanel() {
    private var warpGrid: WarpGrid
    private var surface: FireMap
    private var isBurning = true
    private var ft1 = .0032
    private var ft3 = 0.002
    private var arc = 0
    private var mx = 0.0
    private var my = 0.0

    fun initialise() {
        mx = width / 2.0
        my = height / 2.0
        Toolkit.getDefaultToolkit().systemEventQueue.push(object : EventQueue() {
            override fun dispatchEvent(event: AWTEvent) {
                super.dispatchEvent(event)
                if (peekEvent() == null) {
                    warpGrid.warp(surface.pixels)
                    warpGrid.relax()
                    if (isBurning) {
                        ft1 += 0.012
                        ft3 += 0.002
                        if (arc < 700) {
                            arc += 5
                        }
                        surface.draw5PointedStar(mx, my, arc, 50.0, ft1, sin(ft1) * 8.0, ft3)
                    }
                    surface.reduce()
                    repeat((0..3).count()) {
                        surface.smooth()
                        surface.scroll(1)
                    }
                    repaint()
                }
            }
        })
    }

    override fun paint(g: Graphics) {
        g.drawImage(surface.image, 0, 0, this)
    }

    init {
        size = Dimension(800, 900)
        preferredSize = size

        background = Color.BLACK
        ignoreRepaint = true

        surface = FireMap(width, height)
        warpGrid = WarpGrid(width, height, 48, 48)

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                isBurning = !isBurning
            }
        })

        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                super.mouseMoved(e)
                mx = e.x.toDouble()
                my = e.y.toDouble()
            }
        })
    }
}
