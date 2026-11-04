package com.example.algovisualizer.features.linkedlist.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plan 9: invariants hold after every operation for every kind, and moving a node
 * produces exactly the change set listed in plan 2.3.
 */
class LinkedListCoreTest {


    private val allKinds = ListKind.entries

    // ---- invariants -------------------------------------------------------

    private fun ListState.assertInvariants(label: String) {
        assertEquals("$label: duplicate ids in order", order.size, order.toSet().size)
        order.forEach { assertTrue("$label: order holds unknown id $it", nodes.containsKey(it)) }
        assertEquals("$label: orphan nodes", order.size, nodes.size)

        // no two nodes overlap
        nodes.values.forEach { a ->
            nodes.values.forEach { b ->
                if (a.id < b.id && a.cell.r == b.cell.r) {
                    val overlap = a.cell.c < b.cell.c + kind.span && b.cell.c < a.cell.c + kind.span
                    assertTrue("$label: ${a.id}@${a.cell} overlaps ${b.id}@${b.cell}", !overlap)
                }
            }
        }

        val p = pointersOf()

        // doubly: next and prev must agree
        if (kind.doubly) {
            val byCell = nodes.values.associateBy { it.cell }
            order.forEach { id ->
                val forward = p.next[id] ?: return@forEach
                val target = byCell.getValue(forward).id
                assertEquals("$label: $id.next -> $target but $target.prev disagrees",
                    nodes.getValue(id).cell, p.prev[target])
            }
        }

        // reachability: exactly n steps, terminating per kind
        if (isEmpty) {
            assertNull("$label: head of empty list", p.head)
            assertNull("$label: tail of empty list", p.tail)
        } else {
            assertEquals("$label: walk did not visit every node", order.size, walk().size)
            assertEquals("$label: walk order", order, walk())
            val lastNext = p.next.getValue(order.last())
            if (kind.circular) {
                assertEquals("$label: tail must wrap to head", cellAt(0), lastNext)
            } else {
                assertNull("$label: tail.next must be NULL", lastNext)
            }
            if (kind.doubly) {
                val headPrev = p.prev.getValue(order.first())
                if (kind.circular) {
                    assertEquals("$label: head.prev must wrap to tail", cellAt(order.size - 1), headPrev)
                } else {
                    assertNull("$label: head.prev must be NULL", headPrev)
                }
            }
        }
    }

    @Test
    fun `invariants survive a mixed operation sequence on every kind`() {
        allKinds.forEach { kind ->
            var s = ListState(kind = kind)
            s.assertInvariants("$kind empty")

            s = s.insertTail(1); s.assertInvariants("$kind one node")
            s = s.insertTail(2).insertTail(3).insertHead(0)
            s.assertInvariants("$kind four nodes")
            assertEquals(listOf(0, 1, 2, 3), s.order.map { s.nodes.getValue(it).value })

            s = s.insertAt(2, 99); s.assertInvariants("$kind insert middle")
            assertEquals(listOf(0, 1, 99, 2, 3), s.order.map { s.nodes.getValue(it).value })

            s = s.deleteAt(2); s.assertInvariants("$kind delete middle")
            s = s.reverse(); s.assertInvariants("$kind reversed")
            assertEquals(listOf(3, 2, 1, 0), s.order.map { s.nodes.getValue(it).value })

            s = s.deleteHead(); s.assertInvariants("$kind delete head")
            s = s.deleteTail(); s.assertInvariants("$kind delete tail")
            s = s.clear(); s.assertInvariants("$kind cleared")
        }
    }

    @Test
    fun `single node in a circular list points at itself`() {
        listOf(ListKind.CIRCULAR_SINGLY, ListKind.CIRCULAR_DOUBLY).forEach { kind ->
            val s = ListState(kind = kind).insertTail(7)
            val p = s.pointersOf()
            val own = s.cellAt(0)
            assertEquals("$kind self next", own, p.next.getValue(s.order[0]))
            if (kind.doubly) assertEquals("$kind self prev", own, p.prev.getValue(s.order[0]))
            assertEquals(1, s.walk().size)
        }
    }

    // ---- change diff (plan 2.3) -------------------------------------------

    /** Four nodes, move the one at [index] to a known-free cell, return the changes. */
    private fun movedChanges(kind: ListKind, index: Int): Pair<ListState, List<Change>> {
        val s = sampleList(kind).copy(board = Board(14, 12))
        val target = Cell(0, 8)
        assertTrue("test setup: target must be free", s.fits(target, ignoreId = s.order[index]))
        val applied = s.perform { it.moveNode(s.order[index], target) }
        applied.state.assertInvariants("$kind after move")
        return s to applied.changes
    }

    @Test
    fun `singly - moving a middle node updates its address and the previous node's next`() {
        val (s, changes) = movedChanges(ListKind.SINGLY, 2)
        assertEquals(
            setOf(
                Change.Addr(s.order[2], s.cellAt(2), Cell(0, 8)),
                Change.Next(s.order[1], s.cellAt(2), Cell(0, 8))
            ),
            changes.toSet()
        )
    }

    @Test
    fun `singly - moving the head updates HEAD, not any next`() {
        val (s, changes) = movedChanges(ListKind.SINGLY, 0)
        assertEquals(
            setOf(
                Change.Head(s.cellAt(0), Cell(0, 8)),
                Change.Addr(s.order[0], s.cellAt(0), Cell(0, 8))
            ),
            changes.toSet()
        )
    }

    @Test
    fun `doubly - moving a middle node updates both neighbours`() {
        val (s, changes) = movedChanges(ListKind.DOUBLY, 2)
        assertEquals(
            setOf(
                Change.Addr(s.order[2], s.cellAt(2), Cell(0, 8)),
                Change.Next(s.order[1], s.cellAt(2), Cell(0, 8)),
                Change.Prev(s.order[3], s.cellAt(2), Cell(0, 8))
            ),
            changes.toSet()
        )
    }

    @Test
    fun `circular singly - moving the head also retargets tail's next`() {
        val (s, changes) = movedChanges(ListKind.CIRCULAR_SINGLY, 0)
        assertEquals(
            setOf(
                Change.Head(s.cellAt(0), Cell(0, 8)),
                Change.Addr(s.order[0], s.cellAt(0), Cell(0, 8)),
                Change.Next(s.order[3], s.cellAt(0), Cell(0, 8))
            ),
            changes.toSet()
        )
    }

    @Test
    fun `circular doubly - moving the head wraps both ways`() {
        val (s, changes) = movedChanges(ListKind.CIRCULAR_DOUBLY, 0)
        assertEquals(
            setOf(
                Change.Head(s.cellAt(0), Cell(0, 8)),
                Change.Addr(s.order[0], s.cellAt(0), Cell(0, 8)),
                Change.Next(s.order[3], s.cellAt(0), Cell(0, 8)),
                Change.Prev(s.order[1], s.cellAt(0), Cell(0, 8))
            ),
            changes.toSet()
        )
    }

    // ---- grid -------------------------------------------------------------

    @Test
    fun `fits rejects overlap, the board edge, and accepts the ignored node itself`() {
        val s = sampleList(ListKind.SINGLY, listOf(1, 2))
        val first = s.cellAt(0)
        assertTrue("own cell must fit when ignored", s.fits(first, ignoreId = s.order[0]))
        assertTrue("occupied cell must not fit", !s.fits(first))
        assertTrue("overlapping by one column must not fit", !s.fits(Cell(first.c + 1, first.r)))
        assertTrue("off the right edge", !s.fits(Cell(s.board.cols - 1, 0)))
        assertTrue("off the bottom", !s.fits(Cell(0, s.board.rows)))
        assertTrue("negative", !s.fits(Cell(-1, 0)))
    }

    @Test
    fun `switching to a wider kind relayouts without overlap`() {
        val narrow = sampleList(ListKind.SINGLY, listOf(1, 2, 3, 4, 5)).copy(board = Board(14, 12))
        val wide = narrow.relayoutForKind(ListKind.DOUBLY)
        assertEquals(narrow.order, wide.order)
        wide.assertInvariants("relayout to doubly")
    }

    @Test
    fun `duplicate values are allowed because identity is the id`() {
        val s = ListState(kind = ListKind.SINGLY).insertTail(5).insertTail(5).insertTail(5)
        s.assertInvariants("duplicates")
        assertEquals(3, s.size)
        assertEquals(0, s.indexOfValue(5))
    }

    // ---- traces (plan 12.8) ------------------------------------------------

    /** Strip the id counter: it is bookkeeping, not structure. */
    private fun ListState.shape() = copy(seq = 0)

    @Test
    fun `insert trace ends exactly where the instant insert lands`() {
        allKinds.forEach { kind ->
            val base = sampleList(kind)
            for (i in 0..base.size) {
                val instant = base.insertAt(i, 99)
                val traced = base.traceInsertAt(i, 99).last().frame.toListState(seq = base.seq + 1)
                assertEquals("$kind insert at $i", instant.shape(), traced.shape())
                traced.assertInvariants("$kind traced insert at $i")
            }
        }
    }

    @Test
    fun `delete trace ends exactly where the instant delete lands`() {
        allKinds.forEach { kind ->
            val base = sampleList(kind)
            for (i in 0 until base.size) {
                val instant = base.deleteAt(i)
                val traced = base.traceDeleteAt(i).last().frame.toListState(seq = base.seq)
                assertEquals("$kind delete at $i", instant.shape(), traced.shape())
                traced.assertInvariants("$kind traced delete at $i")
            }
        }
    }

    @Test
    fun `search trace visits nodes in order and reports the right index`() {
        val base = sampleList(ListKind.SINGLY, listOf(12, 7, 30, 4))
        val found = base.traceSearch(30)
        assertEquals("found", found.last().line)
        assertEquals("2", found.last().vars["idx"])

        val missing = base.traceSearch(999)
        assertEquals("not-found", missing.last().line)
        // every node compared exactly once before giving up
        assertEquals(4, missing.count { it.line == "compare" })
    }

    @Test
    fun `every step carries a frame, so stepping back never has to undo anything`() {
        allKinds.forEach { kind ->
            val steps = sampleList(kind).traceInsertAt(2, 55)
            assertTrue("$kind: trace should have several steps", steps.size >= 4)
            steps.forEachIndexed { i, step ->
                assertTrue("$kind step $i has no line id", step.line.isNotBlank())
                assertTrue("$kind step $i has no narration", step.note.isNotBlank())
            }
        }
    }

    @Test
    fun `timeline stepping is reversible`() {
        val steps = sampleList(ListKind.DOUBLY).traceInsertAt(1, 42)
        var t = Timeline.of(steps)
        val startFrame = t.current!!.frame
        t = t.next().next()
        val mid = t.current!!.frame
        t = t.back().back()
        assertEquals("back to the start must restore the first frame", startFrame, t.current!!.frame)
        t = t.next().next()
        assertEquals("forward again must restore the same frame", mid, t.current!!.frame)
    }

    @Test
    fun `timeline clamps at both ends`() {
        val t = Timeline.of(sampleList(ListKind.SINGLY).traceInsertAt(0, 1))
        assertEquals(0, t.back().back().index)
        assertEquals(t.steps.size - 1, t.finish().next().index)
        assertTrue("finishing should stop playback", !t.play().finish().playing)
    }
}
