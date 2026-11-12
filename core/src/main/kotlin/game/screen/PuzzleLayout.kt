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
    val boardWidth = min(width - 56f, 480f)
    val cellWidth = boardWidth / 6f
    val cellHeight = min(cellWidth, (height - 314f) / 10f)
    val boardHeight = cellHeight * 10f
    val boardLeft = (width - boardWidth) / 2f
    val boardTop = (height - 170f + 144f + boardHeight) / 2f
    val board = Rectangle(boardLeft, boardTop - boardHeight, boardWidth, boardHeight)
    val tray = Rectangle(24f, 155f, width - 48f, 102f)
    private val stackColumnWidth = (width - 56f) / 4f
    private val stackHeight = min(140f, (height - 465f) / 3f)
    private val stackTop = (height - 180f + 285f + stackHeight * 3f) / 2f

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
        val size = min(stackColumnWidth - 18f, stackHeight - 30f)
        return Rectangle(
            28f + (index % 4) * stackColumnWidth + (stackColumnWidth - size) / 2f,
            stackTop - (index / 4 + 1) * stackHeight + 18f,
            size, size
        )
    }

    fun stackHit(x: Float, y: Float): Int? = (0 until 12).firstOrNull { stack(it).contains(x, y) }
    fun traySlot(index: Int): Rectangle {
        val step = (tray.width - 24f) / 7f
        return Rectangle(tray.x + 12f + index * step + 3f, tray.y + 15f, step - 6f, 57f)
    }
}
