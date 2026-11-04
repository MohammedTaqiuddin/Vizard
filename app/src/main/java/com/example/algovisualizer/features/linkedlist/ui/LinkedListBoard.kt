package com.example.algovisualizer.features.linkedlist.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.core.theme.GlassColors
import com.example.algovisualizer.core.theme.LocalGlass
import com.example.algovisualizer.features.linkedlist.core.Cell
import com.example.algovisualizer.features.linkedlist.core.Frame
import com.example.algovisualizer.features.linkedlist.core.gap
import com.example.algovisualizer.features.linkedlist.render.CHAIN_ROW_STRIDE
import com.example.algovisualizer.features.linkedlist.render.ChainLayout
import com.example.algovisualizer.features.linkedlist.render.chainPerLine
import com.example.algovisualizer.features.linkedlist.render.Lane
import com.example.algovisualizer.features.linkedlist.render.Metrics
import com.example.algovisualizer.features.linkedlist.render.NodeArt
import com.example.algovisualizer.features.linkedlist.render.NodeBox
import com.example.algovisualizer.features.linkedlist.render.Wires
import com.example.algovisualizer.features.linkedlist.render.address
import com.example.algovisualizer.features.linkedlist.render.boardBounds
import com.example.algovisualizer.features.linkedlist.render.BoardFit
import com.example.algovisualizer.features.linkedlist.render.MemoryLayout
import com.example.algovisualizer.features.linkedlist.render.boardCells
import com.example.algovisualizer.features.linkedlist.render.occupiedBounds
import com.example.algovisualizer.features.linkedlist.render.fitTransform
import com.example.algovisualizer.features.linkedlist.render.morphedPositions
import kotlin.math.max

/**
 * One board, two lessons, morphing between them.
 *
 *  - **Chain** (default): position means list order. What a linked list IS.
 *  - **Memory**: position means address, nodes are draggable. WHY it isn't an array.
 *
 * Drawing both from one composable -- rather than building two screens -- is deliberate:
 * the transition between them is itself the teaching moment. The nodes fly from tidy order
 * to scattered addresses while every pointer stays attached, which is exactly the fact a
 * static diagram cannot show.
 *
 * Renders a [Frame], never a ListState, so the step engine can hand it half-linked
 * intermediate states without this file changing.
 */
@Composable
fun LinkedListBoard(
    frame: Frame,
    memoryView: Boolean,
    fits: (id: String, cell: Cell) -> Boolean,
    onMove: (id: String, cell: Cell) -> Unit,
    onBoardChanged: (cols: Int, rows: Int) -> Unit,
    pan: Offset,
    onPan: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false
) {
    val glass = LocalGlass.current
    val density = LocalDensity.current
    val metrics = remember(density) {
        with(density) { Metrics(cell = 34.dp.toPx(), padLeft = 22.dp.toPx(), padTop = 30.dp.toPx()) }
    }

    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragBaseCell by remember { mutableStateOf<Cell?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var focusedId by remember { mutableStateOf<String?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    /** 0 = chain, 1 = memory. */
    val t by animateFloatAsState(
        targetValue = if (memoryView) 1f else 0f,
        animationSpec = tween(durationMillis = 620),
        label = "viewMorph"
    )

    val canvas = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat())

    // The board must be tall enough for whichever layout needs more rows, so neither view
    // can push a node onto a cell that is off the board.
    val needRows = remember(frame.nodes, frame.order) {
        val deepest = frame.nodes.values.maxOfOrNull { it.cell.r + 1 } ?: 0
        max(deepest + 1, ((frame.order.size - 1).coerceAtLeast(0) * CHAIN_ROW_STRIDE) + 1)
    }
    val board = remember(canvasSize, needRows) { boardCells(metrics, canvas, needRows) }
    val cols = board.width
    val rows = board.height

    LaunchedEffect(cols, rows) { onBoardChanged(cols, rows) }

    val perLine = chainPerLine(frame, metrics, canvas)
    val positions = morphedPositions(frame, metrics, perLine, t)

    // The view frames YOUR NODES, in both layouts -- never the whole board. The board is an
    // address space several screens across and the viewport pans over it; fitting all of it
    // would shrink four nodes to specks, which is what capping the board was papering over.
    val span = frame.kind.span
    val memoryBounds = remember(frame.nodes, frame.kind) {
        occupiedBounds(MemoryLayout.positions(frame, metrics).values, span, metrics)
    }
    val chainBounds = remember(frame.order, frame.kind, perLine) {
        occupiedBounds(ChainLayout(perLine).positions(frame, metrics).values, span, metrics)
    }
    val bounds = lerp(chainBounds, memoryBounds, t)
    val fit = remember(bounds, canvasSize) { fitTransform(bounds, canvas) }

    val scale by animateFloatAsState(
        targetValue = fit.scale,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 180f),
        label = "boardScale"
    )

    // The pan is in CANVAS pixels and the gesture producing it sits OUTSIDE the layer.
    // Panning from inside cannot work: translating the layer changes the very mapping that
    // produces the gesture's coordinates, so each frame's pan cancels the previous one.
    val boardRect = remember(cols, rows, frame.kind) {
        boardBounds(metrics, cols, rows, frame.kind.circular)
    }
    val livePan = if (t >= 1f) clampPan(pan, boardRect, canvas, scale, fit.offset) else Offset.Zero
    val view = BoardFit(scale, fit.offset + livePan)
    val offset = view.offset

    // A drop is legal anywhere on the board -- which is far larger than the viewport. You
    // pan to reach the rest of it; you are not confined to what happens to be on screen.
    val onBoard: (String, Cell) -> Boolean = { id, cell ->
        cell.c >= 0 && cell.r >= 0 &&
            cell.c + frame.kind.span <= cols && cell.r < rows &&
            fits(id, cell)
    }

    // Everything the long-lived gesture reads goes through State: a move only mutates
    // Frame.nodes, so any value captured directly would be frozen at the first drag.
    val currentFrame by rememberUpdatedState(frame)
    val currentFits by rememberUpdatedState(onBoard)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentPositions by rememberUpdatedState(positions)
    val currentView by rememberUpdatedState(view)
    val currentPan by rememberUpdatedState(livePan)
    val onPanned by rememberUpdatedState(onPan)
    // Dragging edits addresses, which only mean anything in memory view.
    val canDrag by rememberUpdatedState(t >= 1f && !isPlaying)

    val previewCell: Cell? = draggingId?.let { id ->
        val base = dragBaseCell ?: return@let null
        metrics.cellAt(metrics.originOf(base) + dragDelta).takeIf { onBoard(id, it) }
    }

    fun originOf(id: String): Offset {
        val base = positions[id] ?: Offset.Zero
        return if (id == draggingId) base + dragDelta else base
    }

    fun boxOf(id: String): NodeBox =
        NodeBox(frame.nodes.getValue(id).cell, frame.kind, metrics, originOf(id))

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { canvasSize = it }
            // Both gestures sit OUTSIDE the layer, in canvas pixels, and convert through
            // `currentView` when they need board coordinates. That one conversion is read
            // from State, so it can never be the stale copy that broke dragging before.
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onPanned(Offset.Zero) },
                    onTap = { at ->
                        val f = currentFrame
                        val pos = currentPositions
                        val board = currentView.toBoard(at)
                        focusedId = f.order.lastOrNull { id ->
                            NodeBox(f.nodes.getValue(id).cell, f.kind, metrics, pos[id] ?: Offset.Zero)
                                .contains(board)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                var panning = false
                fun reset() {
                    draggingId = null
                    dragBaseCell = null
                    dragDelta = Offset.Zero
                    panning = false
                }
                detectDragGestures(
                    onDragStart = { start ->
                        val f = currentFrame
                        val pos = currentPositions
                        val board = currentView.toBoard(start)
                        val hit = if (!canDrag) null else f.order.lastOrNull { id ->
                            NodeBox(f.nodes.getValue(id).cell, f.kind, metrics, pos[id] ?: Offset.Zero)
                                .contains(board)
                        }
                        draggingId = hit
                        dragBaseCell = hit?.let { f.nodes.getValue(it).cell }
                        dragDelta = Offset.Zero
                        // Started on empty board, in memory view: that is a pan.
                        panning = hit == null && canDrag
                    },
                    onDrag = { change, amount ->
                        when {
                            draggingId != null -> {
                                change.consume()
                                // amount is canvas pixels; the node lives in board units.
                                dragDelta += amount / currentView.scale
                            }
                            panning -> {
                                change.consume()
                                onPanned(currentPan + amount)
                            }
                        }
                    },
                    onDragEnd = {
                        val id = draggingId
                        val base = dragBaseCell
                        if (id != null && base != null) {
                            val target = metrics.cellAt(metrics.originOf(base) + dragDelta)
                            if (target != base && currentFits(id, target)) currentOnMove(id, target)
                        }
                        reset()
                    },
                    onDragCancel = { reset() }
                )
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y,
                transformOrigin = TransformOrigin(0f, 0f)
            )
    ) {
        if (t > 0.01f) drawGrid(glass, metrics, cols, rows, t)

        if (draggingId != null && previewCell != null) {
            val ghost = NodeBox(previewCell, frame.kind, metrics)
            drawRect(
                color = glass.accent.copy(alpha = 0.18f),
                topLeft = ghost.rect.topLeft,
                size = ghost.rect.size
            )
        }

        val rects = frame.order.map { boxOf(it).rect }

        // ---- wires -------------------------------------------------------
        frame.order.forEachIndexed { index, id ->
            val from = boxOf(id)
            val dim = focusedId != null && focusedId != id && focusedId != frame.next[id]
            val alpha = if (dim) 0.18f else 1f

            drawPointer(
                glass = glass,
                m = metrics,
                socket = from.nextRect,
                source = from.rect,
                targetId = frame.next[id],
                boxes = { boxOf(it).rect },
                obstacles = rects,
                lane = Lane.UPPER_LEFT,
                colour = glass.accent.copy(alpha = alpha),
                dashed = frame.order.indexOf(frame.next[id]).let { it in 0 until index },
                selfRing = frame.order.size == 1
            )

            if (frame.kind.doubly) {
                val prevId = frame.prev[id]
                val dimPrev = focusedId != null && focusedId != id && focusedId != prevId
                drawPointer(
                    glass = glass,
                    m = metrics,
                    socket = from.prevRect ?: from.nextRect,
                    source = from.rect,
                    targetId = prevId,
                    boxes = { boxOf(it).rect },
                    obstacles = rects,
                    lane = Lane.LOWER_RIGHT,
                    colour = glass.nodeVisited.copy(alpha = if (dimPrev) 0.18f else 1f),
                    dashed = frame.order.indexOf(prevId) > index,
                    selfRing = frame.order.size == 1
                )
            }
        }

        // ---- nodes -------------------------------------------------------
        frame.order.forEach { id ->
            val node = frame.nodes.getValue(id)
            val box = boxOf(id)
            val isDragging = id == draggingId

            val base = NodeArt.bodyColor(
                status = node.status,
                idle = glass.nodeIdle,
                created = glass.nodeFound,
                detached = glass.nodeComparing,
                freed = glass.textTertiary
            )

            with(NodeArt) {
                drawNodeBody(
                    box = box,
                    base = base,
                    status = node.status,
                    dragging = isDragging,
                    cell = metrics.cell
                )
            }

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                paint.color = glass.onNode.toArgb()
                paint.textSize = metrics.cell * 0.42f
                paint.isFakeBoldText = true
                canvas.nativeCanvas.drawText(
                    node.value.toString(),
                    box.dataRect.center.x,
                    box.dataRect.center.y + paint.textSize / 3f,
                    paint
                )

                // Sockets. A null pointer reads as the empty set, in place, rather than an
                // arrow flying off to a floating "NULL" label.
                paint.isFakeBoldText = false
                paint.textSize = metrics.cell * 0.26f
                drawSocketText(canvas, paint, box.nextRect, frame.next[id], glass, t, cols, frame)
                box.prevRect?.let {
                    drawSocketText(canvas, paint, it, frame.prev[id], glass, t, cols, frame)
                }

                // Address and list index only mean something in memory view; in chain view
                // the position already says the index, so both fade out.
                if (t > 0.02f) {
                    paint.textSize = metrics.cell * 0.26f
                    paint.color = (if (isDragging) glass.accent else glass.textSecondary)
                        .copy(alpha = t).toArgb()
                    paint.isFakeBoldText = isDragging
                    canvas.nativeCanvas.drawText(
                        node.cell.address(cols), box.addressAnchor.x, box.addressAnchor.y, paint
                    )

                    paint.textSize = metrics.cell * 0.22f
                    paint.isFakeBoldText = false
                    paint.color = glass.textTertiary.copy(alpha = t).toArgb()
                    paint.textAlign = android.graphics.Paint.Align.LEFT
                    canvas.nativeCanvas.drawText(
                        "#${frame.order.indexOf(id)}",
                        box.rect.left + 4f,
                        box.rect.bottom + metrics.cell * 0.26f,
                        paint
                    )
                    paint.textAlign = android.graphics.Paint.Align.CENTER
                }
            }
        }

        frame.head?.let { drawChip(glass, metrics, "HEAD", boxOf(it), above = true) }
        if (frame.kind.doubly || frame.kind.circular) {
            frame.tail?.let { drawChip(glass, metrics, "TAIL", boxOf(it), above = false) }
        }
    }
}

/** The rect the chain layout occupies, plus the chrome around it. */
private fun chainBounds(frame: Frame, m: Metrics, perLine: Int): Rect {
    val pos = ChainLayout(perLine).positions(frame, m)
    if (pos.isEmpty()) return Rect(0f, 0f, m.cell, m.cell)
    val left = pos.values.minOf { it.x }
    val top = pos.values.minOf { it.y }
    val right = pos.values.maxOf { it.x } + frame.kind.span * m.cell
    val bottom = pos.values.maxOf { it.y } + m.cell
    return Rect(
        left = left - m.cell * 0.9f,
        top = top - m.cell * 1.2f,
        right = right + m.cell * 0.9f,
        bottom = bottom + m.cell * 1.1f
    )
}

/**
 * One pointer field: either the empty set drawn in its socket, or a wire routed to the
 * node it names. The wire starts at a dot in the socket centre and lands on whichever face
 * of the target is nearest, so it points *at the node* instead of doubling back to reach
 * one fixed side -- which is what made the old router curl.
 */
private fun DrawScope.drawPointer(
    glass: GlassColors,
    m: Metrics,
    socket: Rect,
    source: Rect,
    targetId: String?,
    boxes: (String) -> Rect,
    obstacles: List<Rect>,
    lane: Lane,
    colour: Color,
    dashed: Boolean,
    selfRing: Boolean
) {
    if (targetId == null) return          // drawn as the empty-set glyph with the node

    val start = socket.center
    drawCircle(colour, radius = m.cell * 0.055f, center = start)

    val target = boxes(targetId)
    val wire = if (target == source && selfRing) {
        Wires.selfLoop(source, m.cell)
    } else {
        val lift = m.cell * 0.27f
        val probe = Offset(start.x, start.y + if (lane == Lane.UPPER_LEFT) -lift else lift)
        Wires.route(
            start = start,
            source = source,
            target = target,
            obstacles = obstacles,
            lane = lane,
            cell = m.cell,
            entryFrom = probe
        )
    }

    drawPath(
        wire.toPath(m.cell * 0.28f),
        colour,
        style = Stroke(
            width = 3f,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null
        )
    )
    drawPath(Wires.arrowHead(wire.tip, wire.tipFrom), colour)
}

private fun drawSocketText(
    canvas: androidx.compose.ui.graphics.Canvas,
    paint: android.graphics.Paint,
    socket: Rect,
    targetId: String?,
    glass: GlassColors,
    t: Float,
    cols: Int,
    frame: Frame
) {
    if (targetId == null) {
        paint.color = glass.onNode.copy(alpha = 0.85f).toArgb()
        paint.textSize = socket.height * 0.42f
        canvas.nativeCanvas.drawText(
            "∅", socket.center.x, socket.center.y + paint.textSize / 3f, paint
        )
        return
    }
    if (t <= 0.02f) return
    paint.color = glass.onNode.copy(alpha = 0.92f * t).toArgb()
    paint.textSize = socket.height * 0.26f
    val addr = frame.nodes[targetId]?.cell?.address(cols) ?: return
    canvas.nativeCanvas.drawText(
        addr, socket.center.x, socket.center.y + paint.textSize / 3f, paint
    )
}

private fun DrawScope.drawGrid(glass: GlassColors, m: Metrics, cols: Int, rows: Int, alpha: Float) {
    val dot = glass.textTertiary.copy(alpha = 0.22f * alpha)
    for (r in 0..rows) {
        for (c in 0..cols) {
            drawCircle(dot, radius = 1.6f, center = Offset(m.x(c), m.y(r)))
        }
    }
}

private fun DrawScope.drawChip(
    glass: GlassColors,
    m: Metrics,
    label: String,
    box: NodeBox,
    above: Boolean
) {
    val y = if (above) box.rect.top - m.cell * 0.72f else box.rect.bottom + m.cell * 0.52f
    val x = box.rect.center.x
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = m.cell * 0.26f
            isFakeBoldText = true
            color = (if (above) glass.accent else glass.nodeComparing).toArgb()
        }
        canvas.nativeCanvas.drawText(label, x, y, paint)
    }
    val from = Offset(x, if (above) y + 6f else y - m.cell * 0.3f)
    val to = Offset(x, if (above) box.rect.top - 2f else box.rect.bottom + 2f)
    val color = if (above) glass.accent else glass.nodeComparing
    drawLine(color, from, to, 3f)
    drawPath(Wires.arrowHead(to, from), color)
}

/**
 * Keep the pan honest: the board may be several screens across, but at least a quarter of
 * the viewport must still be showing some of it, so you cannot flick the whole thing away
 * and be left staring at nothing.
 */
private fun clampPan(
    pan: Offset,
    board: Rect,
    canvas: Size,
    scale: Float,
    base: Offset
): Offset {
    if (canvas.width <= 0f || canvas.height <= 0f) return Offset.Zero
    val marginX = canvas.width * 0.25f
    val marginY = canvas.height * 0.25f
    val left = board.left * scale + base.x
    val right = board.right * scale + base.x
    val top = board.top * scale + base.y
    val bottom = board.bottom * scale + base.y
    return Offset(
        x = pan.x.coerceIn(
            minOf(marginX - right, 0f),
            maxOf(canvas.width - marginX - left, 0f)
        ),
        y = pan.y.coerceIn(
            minOf(marginY - bottom, 0f),
            maxOf(canvas.height - marginY - top, 0f)
        )
    )
}
