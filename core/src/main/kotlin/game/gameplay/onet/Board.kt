package game.gameplay.onet

import game.gameplay.shared.BOARD_COLS
import game.gameplay.shared.BOARD_ROWS
import game.gameplay.shared.COLS
import game.gameplay.shared.ROWS
import game.gameplay.shared.RandomGenerator
import game.gameplay.shared.shuffleList
import game.gameplay.shared.tileIdsFor
import kotlin.math.abs
import kotlin.math.max

data class Point(val r: Int, val c: Int)

data class GroupSize(val rows: Int, val cols: Int)

data class Tile(
    val id: String,
    val themeKey: String? = null,
    val groupRows: Int? = null,
    val groupCols: Int? = null,
    val isSplit: Boolean = false,
    val splitOrientation: String? = null,
    val openSide: String? = null,
    val splitDepth: Int = 0,
    val splitAxis: String? = null,
    val justSplit: Boolean = false,
    val splitAnimKey: String? = null,
    val splitFromX: Int = 0,
    val splitFromY: Int = 0,
    val isSlave: Boolean = false,
    val masterR: Int = 0,
    val masterC: Int = 0,
    val direction: String? = null,
    val broken: Boolean = false,
    val shape: String? = null
)

data class PathResult(
    val path: List<Point>? = null,
    val error: String? = null
)

data class Hint(
    val p1: Point,
    val p2: Point,
    val path: List<Point>
)

fun groupSize(tile: Tile?): GroupSize {
    val rows = tile?.groupRows ?: if (tile?.isSplit == true && tile.splitOrientation == "vertical") 2 else 1
    val cols = tile?.groupCols ?: if (tile?.isSplit == true && tile.splitOrientation == "horizontal") 2 else 1
    return GroupSize(rows, cols)
}

fun getTileCells(r: Int, c: Int, tile: Tile?): List<Point> {
    val (rows, cols) = groupSize(tile)
    val result = ArrayList<Point>(rows * cols)
    for (i in 0 until (rows * cols)) {
        result.add(Point(r + i / cols, c + i % cols))
    }
    return result
}

fun canMatch(a: Tile?, b: Tile?): Boolean {
    if (a == null || b == null) return false
    if (a.isSlave || b.isSlave) return false
    if (a.id != b.id) return false
    val gA = groupSize(a)
    val gB = groupSize(b)
    return gA.rows == gB.rows && gA.cols == gB.cols
}

private data class QueueNode(
    val r: Int,
    val c: Int,
    val dir: Int,
    val turns: Int,
    val points: List<Point>
)

private val DIRS = listOf(
    Dir(-1, 0, "up", "down"),
    Dir(0, 1, "right", "left"),
    Dir(1, 0, "down", "up"),
    Dir(0, -1, "left", "right")
)

private data class Dir(val dr: Int, val dc: Int, val name: String, val opposite: String)

fun getPath(
    board: List<List<Tile?>>,
    r1: Int,
    c1: Int,
    r2: Int,
    c2: Int,
    ignoredTiles: List<Point> = emptyList()
): PathResult? {
    val a = board.getOrNull(r1)?.getOrNull(c1)
    val b = board.getOrNull(r2)?.getOrNull(c2)
    if (!canMatch(a, b) || (r1 == r2 && c1 == c2) || a == null || b == null) return null

    val starts = getTileCells(r1, c1, a)
    val ends = getTileCells(r2, c2, b)
    val endpoint = ends.map { "${it.r},${it.c}" }.toSet()

    val allowed = mutableSetOf<String>()
    for (p in starts) allowed.add("${p.r},${p.c}")
    for (p in ends) allowed.add("${p.r},${p.c}")
    for (p in ignoredTiles) {
        val ignoredTile = board.getOrNull(p.r)?.getOrNull(p.c)
        for (cell in getTileCells(p.r, p.c, ignoredTile)) {
            allowed.add("${cell.r},${cell.c}")
        }
    }

    val boardRows = board.size
    val boardCols = if (board.isNotEmpty()) board[0].size else 0

    for (start in starts) {
        val queue = mutableListOf(QueueNode(start.r, start.c, -1, 0, listOf(start)))
        val seen = mutableMapOf<String, Int>()
        var index = 0
        while (index < queue.size) {
            val cur = queue[index++]
            for (dir in 0 until 4) {
                val d = DIRS[dir]
                if (cur.dir == -1 && a.openSide != null && a.openSide != d.name) continue
                val turns = cur.turns + if (cur.dir != -1 && cur.dir != dir) 1 else 0
                if (turns > 2) continue
                val r = cur.r + d.dr
                val c = cur.c + d.dc
                if (r < 0 || c < 0 || r >= boardRows || c >= boardCols) continue
                val cellTile = board[r][c]
                if (cellTile != null && !allowed.contains("$r,$c")) continue

                val points = if (cur.dir == dir) {
                    cur.points.dropLast(1) + Point(r, c)
                } else {
                    cur.points + Point(r, c)
                }

                if (endpoint.contains("$r,$c") && (b.openSide == null || b.openSide == d.opposite)) {
                    return PathResult(path = points)
                }

                val key = "$r,$c,$dir"
                val prevTurns = seen[key] ?: 3
                if (prevTurns <= turns) continue
                seen[key] = turns
                queue.add(QueueNode(r, c, dir, turns, points))
            }
        }
    }

    return PathResult(error = "NO_PATH")
}

fun findHint(board: List<List<Tile?>>): Hint? {
    val tiles = mutableListOf<Point>()
    for (r in 1..ROWS) {
        for (c in 1..COLS) {
            val tile = board.getOrNull(r)?.getOrNull(c)
            if (tile != null && !tile.isSlave) {
                tiles.add(Point(r, c))
            }
        }
    }
    for (i in 0 until tiles.size) {
        for (j in (i + 1) until tiles.size) {
            val a = tiles[i]
            val b = tiles[j]
            val tileA = board[a.r][a.c]
            val tileB = board[b.r][b.c]
            if (!canMatch(tileA, tileB)) continue
            val result = getPath(board, a.r, a.c, b.r, b.c)
            if (result?.path != null) {
                return Hint(p1 = a, p2 = b, path = result.path)
            }
        }
    }
    return null
}

fun countRemaining(board: List<List<Tile?>>): Int {
    var count = 0
    for (row in board) {
        for (cell in row) {
            if (cell != null) count++
        }
    }
    return count
}

fun getMatchAction(board: List<List<Tile?>>, a: Point, b: Point? = null): String {
    val tile = board.getOrNull(a.r)?.getOrNull(a.c)
    val (rows, cols) = groupSize(tile)
    val area = rows * cols
    return when (area) {
        4 -> "split_to_1x2"
        2 -> "split_to_1x1"
        else -> "clear"
    }
}

private fun placeGroup(board: Array<Array<Tile?>>, r: Int, c: Int, tile: Tile) {
    for (p in getTileCells(r, c, tile)) {
        board[p.r][p.c] = if (p.r == r && p.c == c) tile else Tile(
            id = tile.id,
            isSlave = true,
            masterR = r,
            masterC = c
        )
    }
}

private val THEME_REGEX = Regex("^(candy|ocean|forest|space)_")

fun clearMatchedGroups(
    board: List<List<Tile?>>,
    a: Point,
    b: Point,
    themeKey: String? = null
): List<List<Tile?>> {
    val next = Array(board.size) { r -> Array(board[r].size) { c -> board[r][c] } }
    val tile = board.getOrNull(a.r)?.getOrNull(a.c)
    val tileB = board.getOrNull(b.r)?.getOrNull(b.c)
    if (!canMatch(tile, tileB) || tile == null) {
        return next.map { it.toList() }
    }
    val (rows, cols) = groupSize(tile)
    val area = rows * cols
    if (area == 1) {
        next[a.r][a.c] = null
        next[b.r][b.c] = null
        return next.map { it.toList() }
    }

    val fallbackTheme = THEME_REGEX.find(tile.id)?.groupValues?.get(1)
    val theme = themeKey ?: tile.themeKey ?: fallbackTheme ?: "vanilla"
    val pool = tileIdsFor(theme)
    val parentIndex = max(0, pool.indexOf(tile.id))
    val depth = tile.splitDepth + 1
    val horizontal = if (area == 4) tile.splitAxis != "vertical" else cols > 1

    for (p in listOf(a, b)) {
        for (i in 0..1) {
            val childRows = if (area == 4) (if (horizontal) 1 else 2) else 1
            val childCols = if (area == 4) (if (horizontal) 2 else 1) else 1
            val r = p.r + (if ((horizontal && area == 4) || (!horizontal && area == 2)) i else 0)
            val c = p.c + (if ((!horizontal && area == 4) || (horizontal && area == 2)) i else 0)
            val id = pool[(parentIndex + 1 + depth * 2 + i * (depth + 2)) % pool.size]
            val splitFromX = if ((horizontal && area == 2) || (!horizontal && area == 4)) (if (i == 0) 1 else -1) else 0
            val splitFromY = if ((horizontal && area == 4) || (!horizontal && area == 2)) (if (i == 0) 1 else -1) else 0

            placeGroup(
                next, r, c, Tile(
                    id = id,
                    themeKey = theme,
                    groupRows = childRows,
                    groupCols = childCols,
                    isSplit = (area == 4),
                    openSide = null,
                    splitDepth = depth,
                    splitAxis = tile.splitAxis,
                    justSplit = true,
                    splitAnimKey = "${tile.id}-$depth-$r-$c",
                    splitFromX = splitFromX,
                    splitFromY = splitFromY
                )
            )
        }
    }

    return next.map { it.toList() }
}

fun isBoardSolvable(input: List<List<Tile?>>): Boolean {
    var board = input
    for (step in 0 until 150) {
        if (countRemaining(board) == 0) return true
        val hint = findHint(board) ?: return false
        board = clearMatchedGroups(board, hint.p1, hint.p2)
    }
    return false
}

private fun side(p: Point, q: Point): String = when {
    q.r < p.r -> "up"
    q.r > p.r -> "down"
    q.c < p.c -> "left"
    else -> "right"
}

private fun addDirectionTiles(
    candidateBoard: List<List<Tile?>>,
    arrowPairs: Int
): List<List<Tile?>> {
    val candidate = Array(candidateBoard.size) { r -> Array(candidateBoard[r].size) { c -> candidateBoard[r][c] } }
    if (arrowPairs > 0 && isBoardSolvable(candidate.map { it.toList() })) {
        var simulation = candidate.map { it.toList() }
        var added = 0
        for (step in 0 until 100) {
            if (added >= arrowPairs) break
            val h = findHint(simulation) ?: break
            val a = simulation[h.p1.r][h.p1.c]
            val b = simulation[h.p2.r][h.p2.c]
            val gA = groupSize(a)
            val isAUnit = gA.rows * gA.cols == 1
            val condCandidate = listOf(h.p1, h.p2).all { p ->
                val candTile = candidate[p.r][p.c]
                if (candTile != null && !candTile.isSlave) {
                    val g = groupSize(candTile)
                    g.rows * g.cols == 1
                } else false
            }
            if (isAUnit && condCandidate && candidate[h.p1.r][h.p1.c]?.id == a?.id && candidate[h.p2.r][h.p2.c]?.id == b?.id) {
                val first = h.path[0]
                val next = h.path[1]
                val last = h.path[h.path.size - 1]
                val prev = h.path[h.path.size - 2]

                candidate[h.p1.r][h.p1.c] = candidate[h.p1.r][h.p1.c]?.copy(openSide = side(first, next))
                candidate[h.p2.r][h.p2.c] = candidate[h.p2.r][h.p2.c]?.copy(openSide = side(last, prev))
                added++
            }
            simulation = clearMatchedGroups(simulation, h.p1, h.p2)
        }
    }
    return candidate.map { it.toList() }
}

private fun constructPairs(
    input: List<List<Tile?>>,
    positions: List<Point>,
    tiles: List<Tile>,
    rng: RandomGenerator,
    distance: Int
): List<List<Tile?>> {
    val result = Array(input.size) { r -> Array(input[r].size) { c -> input[r][c] } }
    val probe = Array(BOARD_ROWS) { Array<Tile?>(BOARD_COLS) { null } }
    positions.forEach { p ->
        probe[p.r][p.c] = Tile(id = "probe")
    }

    var remaining = positions.toList()
    var pair = 0

    while (remaining.size > 1) {
        val order = shuffleList(remaining, rng)
        var bestA: Point? = null
        var bestB: Point? = null
        var bestScore = -1

        val searchSlice = if (order.size > 8) order.subList(0, 8) else order
        searchLoop@ for (a in searchSlice) {
            for (b in order) {
                if (a == b) continue
                val d = abs(a.r - b.r) + abs(a.c - b.c)
                if (d <= bestScore) continue
                val probeList = probe.map { it.toList() }
                val path = getPath(probeList, a.r, a.c, b.r, b.c)
                if (path?.path != null) {
                    bestA = a
                    bestB = b
                    bestScore = d
                    if (d >= distance) break@searchLoop
                }
            }
        }

        if (bestA == null || bestB == null) {
            val probeList = probe.map { it.toList() }
            val h = findHint(probeList) ?: break
            bestA = h.p1
            bestB = h.p2
        }

        for (p in listOf(bestA, bestB)) {
            result[p.r][p.c] = tiles[pair * 2].copy(openSide = null, justSplit = false)
            probe[p.r][p.c] = null
        }

        remaining = remaining.filter { p -> probe[p.r][p.c] != null }
        pair++
    }

    return result.map { it.toList() }
}

fun generateBoard(
    themeKey: String,
    level: Int = 1,
    rng: RandomGenerator = RandomGenerator { Math.random() }
): List<List<Tile?>> {
    val ids = shuffleList(tileIdsFor(themeKey), rng)
    val difficulty = getOnetDifficulty(level)
    val board = Array(BOARD_ROWS) { Array<Tile?>(BOARD_COLS) { null } }

    for (n in 0 until difficulty.groups) {
        val rows = if (level >= 3 && n == 0) 2 else if (level % 2 == 0) 2 else 1
        val cols = if (rows == 2 && level >= 3 && n == 0) 2 else if (rows == 2) 1 else 2
        val r = if (n == 0) 1 else ROWS - rows + 1
        val c = 1
        val other = if (level >= 4) COLS - cols + 1 else c + cols

        for (col in listOf(c, other)) {
            val axis = if (rng.nextDouble() < 0.5) "horizontal" else "vertical"
            placeGroup(
                board, r, col, Tile(
                    id = ids[n],
                    themeKey = themeKey,
                    groupRows = rows,
                    groupCols = cols,
                    isSplit = true,
                    openSide = null,
                    splitAxis = axis
                )
            )
        }
        val masterCol = board[r][c]?.splitAxis
        board[r][other] = board[r][other]?.copy(splitAxis = masterCol)
    }

    val positions = mutableListOf<Point>()
    for (r in 1..ROWS) {
        for (c in 1..COLS) {
            if (board[r][c] == null) {
                positions.add(Point(r, c))
            }
        }
    }

    val tiles = mutableListOf<Tile>()
    for (i in 0 until (positions.size / 2)) {
        for (j in 0 until 2) {
            tiles.add(Tile(id = ids[(i + difficulty.groups) % ids.size], themeKey = themeKey, openSide = null))
        }
    }

    var candidate = board.map { it.toList() }
    for (attempt in 0 until 4) {
        candidate = constructPairs(board.map { it.toList() }, positions, tiles, rng, difficulty.separation)
        if (isBoardSolvable(candidate)) break
    }

    return addDirectionTiles(candidate, difficulty.arrowPairs)
}

fun guaranteedShuffle(
    input: List<List<Tile?>>,
    rng: RandomGenerator = RandomGenerator { Math.random() },
    level: Int = 1
): List<List<Tile?>> {
    val board = Array(input.size) { r ->
        Array(input[r].size) { c ->
            input[r][c]?.copy(openSide = null)
        }
    }

    val positions = mutableListOf<Point>()
    for (r in 1..ROWS) {
        for (c in 1..COLS) {
            val tile = board[r][c]
            if (tile != null && !tile.isSlave) {
                val g = groupSize(tile)
                if (g.rows * g.cols == 1) {
                    positions.add(Point(r, c))
                }
            }
        }
    }

    val tiles = positions.map { p -> board[p.r][p.c]!! }

    for (attempt in 0 until 5) {
        val shuffledTiles = shuffleList(tiles, rng)
        shuffledTiles.forEachIndexed { i, tile ->
            val p = positions[i]
            board[p.r][p.c] = tile
        }
        val boardList = board.map { it.toList() }
        if (isBoardSolvable(boardList)) {
            return addDirectionTiles(boardList, getOnetDifficulty(level).arrowPairs)
        }
    }

    val countsById = mutableMapOf<String, Int>()
    tiles.forEach { t -> countsById[t.id] = (countsById[t.id] ?: 0) + 1 }

    val ordered = shuffleList(countsById.keys.toList(), rng).flatMap { id ->
        val originalTile = tiles.first { it.id == id }
        List(countsById[id] ?: 0) { originalTile }
    }

    val rebuilt = constructPairs(
        board.map { it.toList() },
        positions,
        ordered,
        rng,
        getOnetDifficulty(level).separation
    )

    if (findHint(rebuilt) != null) {
        return addDirectionTiles(rebuilt, getOnetDifficulty(level).arrowPairs)
    }

    // Emergency recovery only: convert footprints to even unit pairs, then peel legal paths.
    val occupied = mutableListOf<Point>()
    val counts = mutableMapOf<String, Int>()
    for (r in 1..ROWS) {
        for (c in 1..COLS) {
            val tile = board[r][c]
            if (tile != null) {
                occupied.add(Point(r, c))
                counts[tile.id] = (counts[tile.id] ?: 0) + 1
            }
        }
    }

    val pairs = shuffleList(
        counts.entries.flatMap { (id, n) -> List(n / 2) { id } },
        rng
    )

    var probe = board.map { row -> row.map { if (it != null) Tile(id = "probe") else null } }
    val finalBoard = Array(board.size) { r -> Array(board[r].size) { c -> board[r][c] } }

    for (id in pairs) {
        val h = findHint(probe) ?: break
        for (p in listOf(h.p1, h.p2)) {
            finalBoard[p.r][p.c] = Tile(id = id, openSide = null)
        }
        probe = clearMatchedGroups(probe, h.p1, h.p2)
    }

    return finalBoard.map { it.toList() }
}

fun placeTilesSmartly(
    tiles: List<Tile>,
    currentBoard: List<List<Tile?>>? = null,
    positions: List<Point>? = null
): List<List<Tile?>> {
    val b = currentBoard?.let { cur ->
        Array(cur.size) { r -> Array(cur[r].size) { c -> cur[r][c] } }
    } ?: Array(BOARD_ROWS) { Array<Tile?>(BOARD_COLS) { null } }

    val pos = positions ?: List(ROWS * COLS) { i ->
        Point(r = i / COLS + 1, c = i % COLS + 1)
    }

    val shuffled = shuffleList(tiles, RandomGenerator { Math.random() })
    shuffled.forEachIndexed { i, t ->
        if (i < pos.size) {
            val p = pos[i]
            b[p.r][p.c] = t
        }
    }

    return guaranteedShuffle(b.map { it.toList() })
}
