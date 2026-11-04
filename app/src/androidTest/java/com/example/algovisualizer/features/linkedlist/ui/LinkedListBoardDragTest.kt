package com.example.algovisualizer.features.linkedlist.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.features.linkedlist.core.Board
import com.example.algovisualizer.features.linkedlist.core.Cell
import com.example.algovisualizer.features.linkedlist.core.ListKind
import com.example.algovisualizer.features.linkedlist.core.ListState
import com.example.algovisualizer.features.linkedlist.core.fits
import com.example.algovisualizer.features.linkedlist.core.moveNode
import com.example.algovisualizer.features.linkedlist.core.sampleList
import com.example.algovisualizer.features.linkedlist.core.toFrame
import com.example.algovisualizer.features.linkedlist.render.Metrics
import com.example.algovisualizer.features.linkedlist.render.MemoryLayout
import com.example.algovisualizer.features.linkedlist.render.occupiedBounds
import com.example.algovisualizer.features.linkedlist.render.fitTransform
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The drag regression that kept coming back: `pointerInput` keeps its lambda alive until a
 * key changes, and a move only touches `Frame.nodes`. Keyed on the node list, the gesture
 * went on hit-testing the positions from before the first drop.
 *
 * One swipe from a fresh start is exactly the case that always worked, so these tests drag
 * the SAME node twice -- the second time from where it now is, and once from where it was.
 */
class LinkedListBoardDragTest {

    @get:Rule
    val rule = createComposeRule()

    private val boardSize = 420.dp

    /** Mirrors the board's own metrics so the test aims at the same pixels it draws. */
    private val metrics: Metrics
        get() = with(rule.density) {
            Metrics(cell = 34.dp.toPx(), padLeft = 22.dp.toPx(), padTop = 30.dp.toPx())
        }

    private fun setUp(initial: ListState): () -> ListState {
        var state by mutableStateOf(initial)
        rule.setContent {
            Box(Modifier.size(boardSize)) {
                LinkedListBoard(
                    frame = state.toFrame(),
                    memoryView = true,
                    fits = { id, cell -> state.fits(cell, ignoreId = id) },
                    onMove = { id, cell -> state = state.moveNode(id, cell) },
                    onBoardChanged = { c, r -> state = state.copy(board = Board(c, r)) },
                    pan = Offset.Zero,
                    onPan = {},
                    modifier = Modifier.testTag(BOARD)
                )
            }
        }
        rule.waitForIdle()
        return { state }
    }

    /**
     * Centre of [cell] in canvas pixels. The view frames the *nodes*, not the whole board,
     * so this converts through the same `occupiedBounds` + `fitTransform` the board lays
     * out with rather than carrying its own copy of that arithmetic.
     */
    private fun pointAt(state: ListState, cell: Cell): Offset {
        val m = metrics
        val px = with(rule.density) { boardSize.toPx() }
        val canvas = Size(px, px)
        val frame = state.toFrame()
        val bounds = occupiedBounds(
            MemoryLayout.positions(frame, m).values, frame.kind.span, m
        )
        val centre = m.originOf(cell) +
            Offset(state.kind.span * m.cell / 2f, m.cell / 2f)
        return fitTransform(bounds, canvas).toCanvas(centre)
    }

    private fun drag(from: Offset, to: Offset) {
        rule.onNodeWithTag(BOARD).performTouchInput { swipe(from, to, durationMillis = 300) }
        rule.waitForIdle()
    }

    @Test
    fun dragging_the_same_node_twice_uses_its_new_position() {
        // Two nodes, laid out at (0,0) and (3,0).
        val read = setUp(sampleList(ListKind.SINGLY, listOf(1, 2)))
        val id = read().order.first()
        assertEquals(Cell(0, 0), read().nodes.getValue(id).cell)

        // First drag: the case a single synthetic swipe already covered.
        var s = read()
        drag(pointAt(s, Cell(0, 0)), pointAt(s, Cell(0, 3)))
        assertEquals("first drag must commit", Cell(0, 3), read().nodes.getValue(id).cell)

        // Second drag, starting where the node IS now. With a stale frame this hit test
        // finds nothing and the swipe is silently discarded.
        s = read()
        drag(pointAt(s, Cell(0, 3)), pointAt(s, Cell(6, 3)))
        assertEquals("second drag must commit", Cell(6, 3), read().nodes.getValue(id).cell)
    }

    @Test
    fun the_cell_a_node_used_to_occupy_is_not_draggable() {
        val read = setUp(sampleList(ListKind.SINGLY, listOf(1, 2)))
        val id = read().order.first()

        var s = read()
        drag(pointAt(s, Cell(0, 0)), pointAt(s, Cell(0, 3)))
        val afterFirst = read().nodes.getValue(id).cell
        assertEquals(Cell(0, 3), afterFirst)

        // (0,0) is empty now. A stale gesture still thinks the node lives there.
        s = read()
        drag(pointAt(s, Cell(0, 0)), pointAt(s, Cell(6, 0)))
        assertEquals("empty space must not drag anything", afterFirst, read().nodes.getValue(id).cell)
    }

    private companion object {
        const val BOARD = "linked-list-board"
    }
}
