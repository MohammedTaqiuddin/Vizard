package com.example.algovisualizer.features.linkedlist.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.example.algovisualizer.features.linkedlist.core.Cell
import com.example.algovisualizer.features.linkedlist.core.Grid
import com.example.algovisualizer.features.linkedlist.core.Frame
import com.example.algovisualizer.features.linkedlist.core.ListKind

/** How far the board may be zoomed to fit the canvas. */
const val MIN_SCALE = 0.35f
const val MAX_SCALE = 1.30f

/**
 * How many screenfuls across the board is. Memory is big; a memory view that fits on one
 * phone screen is not showing memory. The viewport pans over this.
 */
const val BOARD_SCREENS = 3

/** Pixel metrics for the board (plan 4). Cell size is in px, scaled by the caller's density. */
data class Metrics(
    val cell: Float,
    val padLeft: Float,
    val padTop: Float
) {
    fun x(column: Int) = padLeft + column * cell
    fun y(row: Int) = padTop + row * cell
    fun originOf(c: Cell) = Offset(x(c.c), y(c.r))

    /** Nearest grid cell for a pixel point -- used to snap a drag. */
    fun cellAt(point: Offset): Cell = Cell(
        c = Math.round((point.x - padLeft) / cell),
        r = Math.round((point.y - padTop) / cell)
    )
}

/**
 * The rectangle of the whole board -- every cell a node is allowed to occupy -- plus the
 * room the labels need: the address above each node, the index badge below, the HEAD/TAIL
 * chips, and the tail's NULL marker off to the right.
 *
 * The view fits THIS, not the nodes' bounding box. Fitting the nodes made the visible area
 * equal the occupied area, so there was no empty board to drag onto and the right-hand
 * columns never came on screen. The board itself grows and shrinks with the list, so the
 * nodes still get bigger as the list gets shorter.
 */
fun boardBounds(m: Metrics, cols: Int, rows: Int, circular: Boolean): Rect = Rect(
    left = m.x(0) - m.cell * 0.4f,
    top = m.y(0) - m.cell * 1.15f,                  // address label + HEAD chip
    right = m.x(cols) + m.cell * 1.1f,              // NULL marker past the last column
    // index badge + TAIL chip, and the loop a circular list drops below its last row
    bottom = m.y(rows) + m.cell * (if (circular) 0.9f else 0.3f)
)

/**
 * The board size in cells for a canvas of [canvas] pixels.
 *
 * Derived from the viewport, never from a constant: pick a cell size that reads well, then
 * take as many whole cells as fit. A phone gets a small board, a tablet or a landscape
 * window gets a genuinely bigger one at the *same* node size -- which is the whole point.
 * The earlier 12x12 cap was measured off one phone and would have made a 10" tablet refuse
 * the room it has.
 *
 * [needRows] keeps the board at least as tall as the content already is, so a list that has
 * outgrown the screen still has every node on a legal cell.
 *
 * Shared by the board and its instrumentation test, so a test touch is aimed with the same
 * arithmetic the board lays out with.
 */
fun boardCells(m: Metrics, canvas: Size, needRows: Int = 0): IntSize {
    if (canvas.width <= 0f || canvas.height <= 0f) {
        return IntSize(Grid.MIN_COLS, maxOf(Grid.MIN_ROWS, needRows))
    }
    val screenCols = (canvas.width / m.cell).toInt().coerceAtLeast(1)
    val screenRows = (canvas.height / m.cell).toInt().coerceAtLeast(1)
    return IntSize(
        maxOf(Grid.MIN_COLS, screenCols * BOARD_SCREENS),
        maxOf(Grid.MIN_ROWS, screenRows * BOARD_SCREENS, needRows)
    )
}

/**
 * The bounding box of a set of node positions, plus the room the labels need: the address
 * above, the index badge below, HEAD/TAIL chips, and the NULL marker off to the right.
 *
 * This is what the view frames -- *your nodes*, not the whole board. The board is an address
 * space several screens across; the viewport shows the part of it you are working in and
 * pans to the rest.
 */
fun occupiedBounds(positions: Collection<Offset>, span: Int, m: Metrics): Rect {
    if (positions.isEmpty()) return Rect(0f, 0f, m.cell * 4, m.cell * 2)
    return Rect(
        left = positions.minOf { it.x } - m.cell * 0.9f,
        top = positions.minOf { it.y } - m.cell * 1.2f,
        right = positions.maxOf { it.x } + span * m.cell + m.cell * 1.2f,
        bottom = positions.maxOf { it.y } + m.cell * 1.4f
    )
}

/** A plausible memory address for a cell, so the sockets read like real pointers. */
fun Cell.address(cols: Int): String = "0x%04X".format(0x2000 + (r * cols + c) * 16)

/** How the board is scaled and shifted to fit its content. */
data class BoardFit(val scale: Float, val offset: Offset) {
    /** Board space -> canvas space. */
    fun toCanvas(point: Offset) = point * scale + offset

    /** Canvas space -> board space. The gesture sits outside the layer, so it needs this. */
    fun toBoard(point: Offset) = (point - offset) / scale

    /** The same view shifted by a pan, in canvas pixels. */
    fun pannedBy(pan: Offset) = copy(offset = offset + pan)
}

/**
 * The transform that fits [bounds] centred inside a canvas of [size]. Pure, so the board
 * and its instrumentation test convert coordinates with the same arithmetic instead of two
 * copies that can drift apart.
 */
fun fitTransform(bounds: Rect, size: Size): BoardFit {
    if (size.width <= 0f || size.height <= 0f || bounds.isEmpty) return BoardFit(1f, Offset.Zero)
    val scale = minOf(size.width / bounds.width, size.height / bounds.height)
        .coerceIn(MIN_SCALE, MAX_SCALE)
    return BoardFit(
        scale = scale,
        offset = Offset(
            (size.width - bounds.width * scale) / 2f - bounds.left * scale,
            (size.height - bounds.height * scale) / 2f - bounds.top * scale
        )
    )
}

/**
 * Where every part of one node sits. Next-arrows ride slightly above centre and
 * prev-arrows slightly below (plan 5), so a doubly-linked pair never overlaps.
 */
class NodeBox(val cell: Cell, val kind: ListKind, val m: Metrics, val origin: Offset) {

    constructor(cell: Cell, kind: ListKind, m: Metrics) : this(cell, kind, m, m.originOf(cell))

    val span: Int = kind.span
    val width: Float = span * m.cell
    val height: Float = m.cell

    val rect: Rect get() = Rect(origin.x, origin.y, origin.x + width, origin.y + height)

    private val centerY: Float get() = origin.y + height / 2f
    private val lift: Float get() = height * 0.14f      // next above, prev below

    /** Sub-box rects, left to right: [PREV] DATA NEXT. */
    val prevRect: Rect? get() =
        if (kind.doubly) Rect(origin.x, origin.y, origin.x + m.cell, origin.y + height) else null

    val dataRect: Rect get() {
        val left = origin.x + if (kind.doubly) m.cell else 0f
        return Rect(left, origin.y, left + m.cell, origin.y + height)
    }

    val nextRect: Rect get() {
        val left = origin.x + width - m.cell
        return Rect(left, origin.y, left + m.cell, origin.y + height)
    }

    /** Arrow leaves the NEXT box at its right edge. */
    val nextOut: Offset get() = Offset(origin.x + width, centerY - lift)

    /** Arrow leaves the PREV box at its left edge. */
    val prevOut: Offset get() = Offset(origin.x, centerY + lift)

    /** Incoming next-arrows land on the left edge; incoming prev-arrows on the right. */
    val inFromNext: Offset get() = Offset(origin.x, centerY - lift)
    val inFromPrev: Offset get() = Offset(origin.x + width, centerY + lift)

    /** Address label sits above the node. */
    val addressAnchor: Offset get() = Offset(origin.x + width / 2f, origin.y - m.cell * 0.18f)

    fun contains(point: Offset): Boolean = rect.contains(point)
}
