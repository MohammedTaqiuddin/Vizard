package com.example.algovisualizer.features.linkedlist.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.algovisualizer.features.linkedlist.core.NodeStatus

/**
 * How a node is drawn. Kept out of the board so the look can change in one place.
 *
 * A node is a small piece of glass hardware: a soft shadow, a gradient body, a bright
 * specular edge along the top, and recessed compartments for the pointer fields so the
 * DATA cell reads as the content and the pointers as sockets holding addresses.
 */
object NodeArt {

    private fun Color.lift(f: Float) = Color(
        red = (red + (1f - red) * f).coerceIn(0f, 1f),
        green = (green + (1f - green) * f).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * f).coerceIn(0f, 1f),
        alpha = alpha
    )

    private fun Color.sink(f: Float) = Color(
        red = (red * (1f - f)).coerceIn(0f, 1f),
        green = (green * (1f - f)).coerceIn(0f, 1f),
        blue = (blue * (1f - f)).coerceIn(0f, 1f),
        alpha = alpha
    )

    /** Status drives the body colour, so a new / detached / freed node is obvious. */
    fun bodyColor(
        status: NodeStatus,
        idle: Color,
        created: Color,
        detached: Color,
        freed: Color
    ): Color = when (status) {
        NodeStatus.NORMAL -> idle
        NodeStatus.NEW -> created
        NodeStatus.DETACHED -> detached
        NodeStatus.FREED -> freed
    }

    fun DrawScope.drawNodeBody(
        box: NodeBox,
        base: Color,
        status: NodeStatus,
        dragging: Boolean,
        cell: Float
    ) {
        val radius = CornerRadius(cell * 0.22f)
        val rect = box.rect
        val alpha = if (status == NodeStatus.FREED) 0.35f else 1f

        // soft shadow: two cheap offset layers instead of a blur
        if (status != NodeStatus.FREED) {
            drawRoundRect(
                color = Color.Black.copy(alpha = if (dragging) 0.30f else 0.20f),
                topLeft = rect.topLeft + Offset(0f, cell * 0.10f),
                size = rect.size,
                cornerRadius = radius
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.10f),
                topLeft = rect.topLeft + Offset(0f, cell * 0.20f),
                size = rect.size,
                cornerRadius = radius
            )
        }

        // body: lit from above
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    base.lift(0.22f).copy(alpha = alpha),
                    base.copy(alpha = alpha),
                    base.sink(0.18f).copy(alpha = alpha)
                ),
                startY = rect.top,
                endY = rect.bottom
            ),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = radius
        )

        // recessed pointer compartments
        val inset = cell * 0.07f
        fun socket(r: Rect) {
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.22f * alpha),
                topLeft = Offset(r.left + inset, r.top + inset),
                size = Size(r.width - inset * 2, r.height - inset * 2),
                cornerRadius = CornerRadius(cell * 0.14f)
            )
        }
        box.prevRect?.let { socket(it) }
        socket(box.nextRect)

        // specular edge along the top, the same cue the glass panels use
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.45f * alpha), Color.Transparent),
                startY = rect.top,
                endY = rect.top + rect.height * 0.55f
            ),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = radius,
            style = Stroke(width = cell * 0.035f)
        )

        // status outline
        val outline = when {
            dragging -> Color.White.copy(alpha = 0.85f)
            status == NodeStatus.NEW -> Color.White.copy(alpha = 0.75f)
            status == NodeStatus.DETACHED -> Color.White.copy(alpha = 0.55f)
            else -> null
        }
        if (outline != null) {
            drawRoundRect(
                color = outline,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = radius,
                style = Stroke(
                    width = cell * 0.045f,
                    pathEffect = if (status == NodeStatus.NEW) {
                        PathEffect.dashPathEffect(floatArrayOf(cell * 0.16f, cell * 0.12f))
                    } else {
                        null
                    }
                )
            )
        }
    }
}
