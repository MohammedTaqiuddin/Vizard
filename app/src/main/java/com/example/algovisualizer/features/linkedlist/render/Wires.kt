package com.example.algovisualizer.features.linkedlist.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Arrow routing.
 *
 * The previous router drew a cubic between two *fixed sockets*: `next` always left the
 * right-hand side and always entered the target's left-hand side. Any target that wasn't
 * to the right forced the curve to double back on itself, which is where the loops and
 * curls in Memory view came from -- the arrow had to leave east and arrive from the west
 * no matter where the node actually was.
 *
 * This router makes neither assumption. The arrow starts at the socket *centre* and may
 * leave in any direction; it ends on whichever face of the target node is nearest, so it
 * points at the node rather than at one particular socket. Paths are orthogonal polylines
 * with rounded corners, chosen from a small candidate set: the first one that crosses no
 * other node wins.
 */

/** Which side a long edge detours through. `next` runs above/left, `prev` below/right. */
enum class Lane { UPPER_LEFT, LOWER_RIGHT }

/** An orthogonal polyline. Corner rounding happens at paint time, in [toPath]. */
data class Wire(val points: List<Offset>) {

    val tip: Offset get() = points.last()

    /** Direction the arrowhead points, taken from the final segment. */
    val tipFrom: Offset get() = points[points.size - 2]

    val segments: List<Pair<Offset, Offset>>
        get() = points.zipWithNext()

    fun toPath(radius: Float = 10f): Path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size - 1) {
            val prev = points[i - 1]
            val here = points[i]
            val next = points[i + 1]
            val r = minOf(radius, dist(prev, here) / 2f, dist(here, next) / 2f)
            if (r < 0.5f) {
                lineTo(here.x, here.y)
                continue
            }
            val into = towards(here, prev, r)
            val out = towards(here, next, r)
            lineTo(into.x, into.y)
            quadraticTo(here.x, here.y, out.x, out.y)
        }
        val end = points.last()
        lineTo(end.x, end.y)
    }

    private fun dist(a: Offset, b: Offset) = abs(a.x - b.x) + abs(a.y - b.y)

    private fun towards(from: Offset, to: Offset, by: Float): Offset {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val len = max(0.0001f, sqrt(dx * dx + dy * dy))
        return Offset(from.x + dx / len * by, from.y + dy / len * by)
    }
}

object Wires {

    private const val EPS = 1.5f

    /**
     * Route from [start] (a socket centre, inside [source]) to [target], avoiding every rect
     * in [obstacles] that is neither endpoint.
     */
    fun route(
        start: Offset,
        source: Rect,
        target: Rect,
        obstacles: List<Rect>,
        lane: Lane,
        cell: Float,
        /**
         * The point the target's face is chosen from. Defaults to [start]; the board passes
         * `start` nudged into its lane so that a `next` and a `prev` crossing the same gap
         * land on different heights instead of on top of each other.
         */
        entryFrom: Offset = start
    ): Wire {
        val entry = nearestFace(target, entryFrom)
        val blockers = obstacles.filter { it !== source && it !== target }
        val options = candidates(start, entry, source, target, lane, cell)

        // First clear candidate wins; otherwise the least-bad one, so a wire always draws.
        return options.firstOrNull { pts -> crossings(pts, blockers) == 0 }
            ?.let { Wire(it) }
            ?: Wire(options.minByOrNull { crossings(it, blockers) } ?: options.first())
    }

    /**
     * The point on [target]'s boundary an arrow coming from [from] should land on: the
     * nearest face, with the landing point slid along that face to line up with the source.
     */
    fun nearestFace(target: Rect, from: Offset): Offset {
        val inset = min(target.width, target.height) * 0.16f
        val outLeft = target.left - from.x
        val outRight = from.x - target.right
        val outTop = target.top - from.y
        val outBottom = from.y - target.bottom

        return when (maxOf(outLeft, outRight, outTop, outBottom)) {
            outLeft -> Offset(target.left, from.y.coerceIn(target.top + inset, target.bottom - inset))
            outRight -> Offset(target.right, from.y.coerceIn(target.top + inset, target.bottom - inset))
            outTop -> Offset(from.x.coerceIn(target.left + inset, target.right - inset), target.top)
            else -> Offset(from.x.coerceIn(target.left + inset, target.right - inset), target.bottom)
        }
    }

    /**
     * Straight, then both L shapes, then both Z shapes through a lane. `next` detours above
     * and to the left, `prev` below and to the right, so the two never share a channel.
     */
    private fun candidates(
        start: Offset,
        entry: Offset,
        source: Rect,
        target: Rect,
        lane: Lane,
        cell: Float
    ): List<List<Offset>> {
        val pad = cell * 0.55f
        val laneY = if (lane == Lane.UPPER_LEFT) {
            min(source.top, target.top) - pad
        } else {
            max(source.bottom, target.bottom) + pad
        }
        val laneX = if (lane == Lane.UPPER_LEFT) {
            min(source.left, target.left) - pad
        } else {
            max(source.right, target.right) + pad
        }

        val out = mutableListOf<List<Offset>>()

        // Straight, but only when it really is axis-aligned -- a diagonal is not orthogonal.
        if (abs(start.y - entry.y) < EPS) out += listOf(Offset(start.x, entry.y), entry)
        if (abs(start.x - entry.x) < EPS) out += listOf(Offset(entry.x, start.y), entry)

        // Vertical first: this is what gets the wire onto its lane immediately, so a next
        // and a prev crossing the same gap run as two parallel rails rather than one line.
        out += listOf(start, Offset(start.x, entry.y), entry)
        out += listOf(start, Offset(entry.x, start.y), entry)

        // Z detours. The lane the caller asked for comes first, then the other channels
        // around the two boxes, so a blocked preferred lane degrades to a clear one rather
        // than to a wire through somebody's node.
        val ys = listOf(
            laneY,
            min(source.top, target.top) - pad,
            max(source.bottom, target.bottom) + pad,
            (max(min(source.bottom, target.bottom), 0f) + min(source.top, target.top)) / 2f,
            source.top - pad,
            source.bottom + pad,
            target.top - pad,
            target.bottom + pad
        )
        val xs = listOf(
            laneX,
            min(source.left, target.left) - pad,
            max(source.right, target.right) + pad,
            source.left - pad,
            source.right + pad,
            target.left - pad,
            target.right + pad
        )
        ys.distinct().forEach { y ->
            out += listOf(start, Offset(start.x, y), Offset(entry.x, y), entry)
        }
        xs.distinct().forEach { x ->
            out += listOf(start, Offset(x, start.y), Offset(x, entry.y), entry)
        }

        // Last resort: a four-turn path that leaves on the lane, crosses on a free column
        // and comes back. Enough freedom to get around a node directly in the way.
        ys.take(3).forEach { y ->
            xs.take(3).forEach { x ->
                out += listOf(
                    start, Offset(start.x, y), Offset(x, y), Offset(x, entry.y), entry
                )
            }
        }
        return out
    }

    /** How many node rects this polyline cuts through. */
    fun crossings(points: List<Offset>, obstacles: List<Rect>): Int =
        points.zipWithNext().sumOf { (a, b) ->
            obstacles.count { segmentHits(a, b, it) }
        }

    /**
     * Axis-aligned segment against a rect. Strictly inside only: a segment that merely
     * grazes an edge (every wire does, at its endpoints) is not a crossing.
     */
    fun segmentHits(a: Offset, b: Offset, rect: Rect): Boolean {
        val shrunk = Rect(rect.left + EPS, rect.top + EPS, rect.right - EPS, rect.bottom - EPS)
        if (shrunk.width <= 0f || shrunk.height <= 0f) return false

        return if (abs(a.y - b.y) < EPS) {
            val lo = min(a.x, b.x)
            val hi = max(a.x, b.x)
            a.y > shrunk.top && a.y < shrunk.bottom && hi > shrunk.left && lo < shrunk.right
        } else {
            val lo = min(a.y, b.y)
            val hi = max(a.y, b.y)
            a.x > shrunk.left && a.x < shrunk.right && hi > shrunk.top && lo < shrunk.bottom
        }
    }

    /** The only case a loop is correct: a one-node circular list pointing at itself. */
    fun selfLoop(box: Rect, cell: Float): Wire {
        val up = box.top - cell * 0.55f
        return Wire(
            listOf(
                Offset(box.right - cell * 0.4f, box.top),
                Offset(box.right - cell * 0.4f, up),
                Offset(box.left + cell * 0.4f, up),
                Offset(box.left + cell * 0.4f, box.top)
            )
        )
    }

    /** Arrowhead triangle at [tip], pointing along the direction from [tail] to [tip]. */
    fun arrowHead(tip: Offset, tail: Offset, size: Float = 9f): Path {
        val dx = tip.x - tail.x
        val dy = tip.y - tail.y
        val len = max(0.0001f, sqrt(dx * dx + dy * dy))
        val ux = dx / len
        val uy = dy / len
        val px = -uy
        val py = ux
        val baseX = tip.x - ux * size
        val baseY = tip.y - uy * size
        return Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(baseX + px * size * 0.55f, baseY + py * size * 0.55f)
            lineTo(baseX - px * size * 0.55f, baseY - py * size * 0.55f)
            close()
        }
    }
}
