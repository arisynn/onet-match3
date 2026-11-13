package game.screen

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import kotlin.math.min

/** Shared drawing and hit areas in world units, including tall portrait displays. */
class PuzzleLayout(val width: Float, val height: Float) {
    val back = Rectangle(16f, height - 90f, 64f, 64f)
    val pause = Rectangle(width - 80f, height - 90f, 64f, 64f)
    private val toolWidth = (width - 72f) / 2f
    val hint = Rectangle(28f, 52f, toolWidth, 68f)
    val shuffle = Rectangle(width - 28f - toolWidth, 52f, toolWidth, 68f)
    val undo = hint
    val boardWidth = min(width - 96f, 480f)
    val cellWidth = boardWidth / 6f
    val cellHeight = min(cellWidth, (height - 366f) / 10f)
    val boardHeight = cellHeight * 10f
    val boardLeft = (width - boardWidth) / 2f
    val boardTop = (height - 190f + 176f + boardHeight) / 2f
    val board = Rectangle(boardLeft, boardTop - boardHeight, boardWidth, boardHeight)
    val playfield = Rectangle(20f,144f,width-40f,height-326f)
    val tray = Rectangle(playfield.x+12f,playfield.y+16f,playfield.width-24f,102f)
    private val stackColumnWidth = (playfield.width - 24f) / 4f
    private val stackAreaTop = playfield.y+playfield.height-28f
    private val stackAreaBottom = tray.y+tray.height+24f
    private val stackHeight = (stackAreaTop-stackAreaBottom)/3f

    fun onetCell(row: Int, col: Int, rows: Int = 1, cols: Int = 1) = Rectangle(
        boardLeft + (col - 1) * cellWidth + 3f,
        boardTop - (row - 1 + rows) * cellHeight + 3f,
        cols * cellWidth - 6f, rows * cellHeight - 6f
    )

    fun onetPoint(row: Int, col: Int) = Vector2(
        boardLeft + (col - 0.5f) * cellWidth, boardTop - (row - 0.5f) * cellHeight
    )

    fun onetHit(x: Float, y: Float): Pair<Int, Int>? {
        if (x < board.x || x >= board.x + board.width || y <= board.y || y > boardTop) return null
        val col = ((x - boardLeft) / cellWidth).toInt() + 1
        val row = ((boardTop - y) / cellHeight).toInt() + 1
        return if (row in 1..10 && col in 1..6) row to col else null
    }

    fun stack(index: Int): Rectangle {
        val tileWidth = stackColumnWidth-14f
        val tileHeight = min(tileWidth*1.22f,stackHeight-24f)
        return Rectangle(playfield.x+12f+(index%4)*stackColumnWidth+7f,
            stackAreaTop-(index/4+.5f)*stackHeight-tileHeight/2f,tileWidth,tileHeight)
    }

    fun stackHit(x: Float, y: Float): Int? = (0 until 12).firstOrNull { stack(it).contains(x, y) }
    fun traySlot(index: Int): Rectangle {
        val step = (tray.width - 24f) / 7f
        return Rectangle(tray.x + 12f + index * step + 3f, tray.y + 15f, step - 6f, 57f)
    }
}
