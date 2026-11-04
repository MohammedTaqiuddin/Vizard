package com.example.algovisualizer.features.linkedlist.render

import androidx.compose.ui.geometry.Offset
import com.example.algovisualizer.features.linkedlist.core.Board
import com.example.algovisualizer.features.linkedlist.core.Cell
import com.example.algovisualizer.features.linkedlist.core.ListKind
import com.example.algovisualizer.features.linkedlist.core.ListState
import com.example.algovisualizer.features.linkedlist.core.fits
import com.example.algovisualizer.features.linkedlist.core.moveNode
import com.example.algovisualizer.features.linkedlist.core.pointersOf
import com.example.algovisualizer.features.linkedlist.core.sampleList
import com.example.algovisualizer.features.linkedlist.core.toFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LayoutAndWireTest {

    private val m = Metrics(cell = 90f, padLeft = 58f, padTop = 79f)
    private val cols = 12
    private val perLine = 4

    // ---- morph (Chain <-> Memory) -----------------------------------------

    @Test
    fun `every node has a position in both layouts, for every kind`() {
        ListKind.entries.forEach { kind ->
            val frame = sampleList(kind).toFrame()
            val chain = ChainLayout(perLine).positions(frame, m)
            val memory = MemoryLayout.positions(frame, m)

            assertEquals("$kind chain is missing nodes", frame.nodes.keys, chain.keys)
            assertEquals("$kind memory is missing nodes", frame.nodes.keys, memory.keys)

            // and the blend covers them at every t, including the ends
            listOf(0f, 0.3f, 0.5f, 1f).forEach { t ->
                val blended = morphedPositions(frame, m, perLine, t)
                assertEquals("$kind blend at t=$t", frame.nodes.keys, blended.keys)
            }
        }
    }

    @Test
    fun `the morph moves nodes but never changes the list`() {
        ListKind.entries.forEach { kind ->
            val state = sampleList(kind)
            val frame = state.toFrame()
            val before = state.pointersOf()

            // Layout is a pure function of the frame; it cannot write back to it.
            morphedPositions(frame, m, perLine, 0f)
            morphedPositions(frame, m, perLine, 1f)

            assertEquals("$kind: layout must not touch pointers", before, state.pointersOf())
            assertEquals("$kind: layout must not touch order", state.order, frame.order)
        }
    }

    @Test
    fun `chain layout puts nodes in list order and wraps in reading order`() {
        val state = sampleList(ListKind.SINGLY, (1..9).toList())
        val frame = state.toFrame()
        val pos = ChainLayout(perLine).positions(frame, m)

        // 2-wide nodes with a 1-cell gap: 4 fit a 12-column line.
        val lines = frame.order.map { pos.getValue(it).y }.distinct()
        assertEquals("expected 3 wrapped lines", 3, lines.size)
        assertTrue("lines must descend", lines == lines.sorted())

        frame.order.take(4).zipWithNext().forEach { (a, b) ->
            assertTrue(
                "within a line, later nodes sit further right",
                pos.getValue(b).x > pos.getValue(a).x
            )
            assertEquals("within a line, y is shared", pos.getValue(a).y, pos.getValue(b).y)
        }
    }

    // ---- wire routing ------------------------------------------------------

    /** Every node rect at its memory position. */
    private fun rectsOf(state: ListState): Map<String, androidx.compose.ui.geometry.Rect> =
        state.nodes.mapValues { (_, n) -> NodeBox(n.cell, state.kind, m).rect }

    /**
     * Route every pointer of [state] and return the wires with the rects they must avoid.
     */
    private fun routeAll(state: ListState): List<Pair<Wire, List<androidx.compose.ui.geometry.Rect>>> {
        val rects = rectsOf(state)
        val all = rects.values.toList()
        val p = state.pointersOf()
        val byCell = state.nodes.values.associateBy { it.cell }
        val out = mutableListOf<Pair<Wire, List<androidx.compose.ui.geometry.Rect>>>()

        state.order.forEach { id ->
            val box = NodeBox(state.nodes.getValue(id).cell, state.kind, m)
            listOf(
                Triple(p.next[id], box.nextRect, Lane.UPPER_LEFT),
                Triple(p.prev[id], box.prevRect, Lane.LOWER_RIGHT)
            ).forEach { (targetCell, socket, lane) ->
                if (targetCell == null || socket == null) return@forEach
                val targetId = byCell.getValue(targetCell).id
                if (targetId == id) return@forEach            // self loop, handled separately
                val source = rects.getValue(id)
                val target = rects.getValue(targetId)
                val start = socket.center
                val lift = m.cell * 0.27f
                val wire = Wires.route(
                    start = start,
                    source = source,
                    target = target,
                    obstacles = all,
                    lane = lane,
                    cell = m.cell,
                    entryFrom = Offset(
                        start.x,
                        start.y + if (lane == Lane.UPPER_LEFT) -lift else lift
                    )
                )
                out += wire to all.filter { it != source && it != target }
            }
        }
        return out
    }

    @Test
    fun `no wire crosses a node it does not connect, over 200 seeded scrambles`() {
        val rng = Random(20261015)
        var checked = 0

        repeat(200) { iteration ->
            val kind = ListKind.entries[iteration % ListKind.entries.size]
            var state = sampleList(kind).copy(board = Board(cols, 10))

            // scatter the nodes to arbitrary addresses
            state.order.forEach { id ->
                repeat(12) {
                    val cell = Cell(rng.nextInt(0, cols - kind.span + 1), rng.nextInt(0, 10))
                    if (state.fits(cell, ignoreId = id)) {
                        state = state.moveNode(id, cell)
                        return@forEach
                    }
                }
            }

            routeAll(state).forEach { (wire, obstacles) ->
                checked++
                assertEquals(
                    "iteration $iteration ($kind): wire cuts through a node it does not connect\n" +
                        "  points = ${wire.points}",
                    0,
                    Wires.crossings(wire.points, obstacles)
                )
            }
        }

        assertTrue("the property test routed nothing", checked > 500)
    }

    @Test
    fun `wires are orthogonal and never double back on themselves`() {
        val rng = Random(7)
        repeat(60) {
            var state = sampleList(ListKind.DOUBLY).copy(board = Board(cols, 10))
            state.order.forEach { id ->
                repeat(12) {
                    val cell = Cell(rng.nextInt(0, cols - 3 + 1), rng.nextInt(0, 10))
                    if (state.fits(cell, ignoreId = id)) {
                        state = state.moveNode(id, cell)
                        return@forEach
                    }
                }
            }

            routeAll(state).forEach { (wire, _) ->
                wire.segments.forEach { (a, b) ->
                    val horizontal = kotlin.math.abs(a.y - b.y) < 1.5f
                    val vertical = kotlin.math.abs(a.x - b.x) < 1.5f
                    assertTrue("segment $a -> $b is diagonal", horizontal || vertical)
                }
                // consecutive segments must turn, never reverse along the same axis
                wire.segments.zipWithNext().forEach { (s1, s2) ->
                    val d1 = Offset(s1.second.x - s1.first.x, s1.second.y - s1.first.y)
                    val d2 = Offset(s2.second.x - s2.first.x, s2.second.y - s2.first.y)
                    assertTrue(
                        "a segment reverses toward its origin: $d1 then $d2",
                        d1.x * d2.x >= -0.01f && d1.y * d2.y >= -0.01f
                    )
                }
            }
        }
    }

    @Test
    fun `a next and a prev crossing the same gap do not share a line`() {
        val state = sampleList(ListKind.DOUBLY, listOf(1, 2))
        val rects = rectsOf(state)
        val a = state.order[0]
        val b = state.order[1]
        val boxA = NodeBox(state.nodes.getValue(a).cell, state.kind, m)
        val boxB = NodeBox(state.nodes.getValue(b).cell, state.kind, m)
        val lift = m.cell * 0.27f

        val next = Wires.route(
            start = boxA.nextRect.center, source = rects.getValue(a), target = rects.getValue(b),
            obstacles = rects.values.toList(), lane = Lane.UPPER_LEFT, cell = m.cell,
            entryFrom = boxA.nextRect.center - Offset(0f, lift)
        )
        val prev = Wires.route(
            start = boxB.prevRect!!.center, source = rects.getValue(b), target = rects.getValue(a),
            obstacles = rects.values.toList(), lane = Lane.LOWER_RIGHT, cell = m.cell,
            entryFrom = boxB.prevRect!!.center + Offset(0f, lift)
        )

        assertTrue(
            "next must land above centre, prev below: next=${next.tip.y} prev=${prev.tip.y}",
            next.tip.y < prev.tip.y - lift
        )
    }
}
