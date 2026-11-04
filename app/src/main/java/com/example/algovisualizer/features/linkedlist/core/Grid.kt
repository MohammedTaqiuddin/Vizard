package com.example.algovisualizer.features.linkedlist.core

/**
 * Board rules in cells. Pixel sizing lives in the render layer, not here.
 *
 * There is deliberately **no maximum board size** in this file. How many cells there are is
 * a property of the display, not of the data structure: a 10" tablet has room for far more
 * cells at the same readable node size than a phone, and a hard cap here would make it
 * refuse to use them. The caller passes the board it actually has; the floor below only
 * exists so a very small screen still has somewhere to put a node.
 */
object Grid {
    const val MIN_COLS = 8
    const val MIN_ROWS = 4
}

/** Empty columns between neighbours: a doubly-linked row has to fit two arrows in there. */
val ListKind.gap: Int get() = if (doubly) 2 else 1

/**
 * True if a node of this list's span can sit at [cell]: on the board and not horizontally
 * overlapping another node on the same row. [cols] and [rows] default to the state's own board because
 * that is what the viewport reported. See the note on [Grid].
 */
fun ListState.fits(
    cell: Cell,
    ignoreId: String? = null,
    cols: Int = board.cols,
    rows: Int = board.rows
): Boolean {
    val span = kind.span
    if (cell.c < 0 || cell.r < 0) return false
    if (cell.c + span > cols || cell.r >= rows) return false

    return nodes.values.none { other ->
        other.id != ignoreId &&
            other.cell.r == cell.r &&
            cell.c < other.cell.c + span &&
            other.cell.c < cell.c + span
    }
}

/**
 * Where to drop a new node: just past the tail on its row, else the next row down, else the
 * first free slot anywhere. Null when the board is full.
 */
fun ListState.freeSpot(cols: Int = board.cols, rows: Int = board.rows): Cell? {
    val span = kind.span
    val step = span + kind.gap

    // The last node that actually exists, not `order.last()`: relayoutForKind rebuilds
    // `nodes` one entry at a time while `order` is already complete, so the tail id can
    // name a node that has not been placed yet.
    val tail = order.lastOrNull { nodes.containsKey(it) }?.let { nodes.getValue(it).cell }

    if (tail != null) {
        val beside = Cell(tail.c + step, tail.r)
        if (fits(beside, cols = cols, rows = rows)) return beside

        var r = tail.r + 2
        while (r < rows) {
            val start = Cell(0, r)
            if (fits(start, cols = cols, rows = rows)) return start
            r += 2
        }
    }

    for (r in 0 until rows) {
        var c = 0
        while (c + span <= cols) {
            val cell = Cell(c, r)
            if (fits(cell, cols = cols, rows = rows)) return cell
            c += step
        }
    }
    return null
}

/**
 * After a kind switch, 2-wide nodes may become 3-wide and collide. Push the losers to a
 * free spot, keeping list order. Produces Addr changes so the user sees them move.
 */
fun ListState.relayoutForKind(
    newKind: ListKind,
    cols: Int = board.cols,
    rows: Int = board.rows
): ListState {
    if (newKind.span == kind.span) return copy(kind = newKind)

    var acc = copy(kind = newKind, nodes = emptyMap())
    order.forEach { id ->
        val node = nodes.getValue(id)
        val keep = acc.fits(node.cell, ignoreId = id, cols = cols, rows = rows)
        val cell = if (keep) node.cell else acc.freeSpot(cols, rows) ?: node.cell
        acc = acc.copy(nodes = acc.nodes + (id to node.copy(cell = cell)))
    }
    return acc
}
