package com.example.algovisualizer.core.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.core.theme.GlassColors
import com.example.algovisualizer.core.theme.LocalGlass

/** The specular edge: bright at the top, fading to nothing at the bottom. This sells the glass. */
private fun edgeBrush(glass: GlassColors) = Brush.verticalGradient(
    listOf(glass.edgeHighlight, glass.edgeShadow)
)

/**
 * Base glass surface. [strong] raises the fill opacity for surfaces that sit over busy content
 * (control panels over a visualizer canvas) rather than over the ambient background.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    strong: Boolean = false,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlass.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "press"
    )

    val fill = when {
        pressed && onClick != null -> glass.fillPressed
        strong -> glass.fillStrong
        else -> glass.fill
    }

    Box(
        modifier = modifier
            .scale(scale)
            // Android paints elevation shadows *under* the layer and assumes the view is
            // opaque to hide them. Glass is translucent, so any shadow here shows through
            // as a hard-edged patch. Depth comes from the edge highlight instead.
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(elevation, shape, clip = false, glass.shadowTint, glass.shadowTint)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, edgeBrush(glass)), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        content = content
    )
}

/** A tappable list row: icon, title, supporting line, chevron. The app's navigation unit. */
@Composable
fun GlassListCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    accent: Color? = null
) {
    val glass = LocalGlass.current
    GlassSurface(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                val tint = accent ?: glass.accent
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tint.copy(alpha = 0.16f))
                        .border(
                            BorderStroke(1.dp, tint.copy(alpha = 0.28f)),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        leading,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textTertiary
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineMedium,
                color = glass.textTertiary
            )
        }
    }
}

/** Primary action. [prominent] fills with the accent; otherwise it stays glass. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val glass = LocalGlass.current
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "btn"
    )
    val alpha = if (enabled) 1f else 0.4f

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 52.dp)
            .then(
                if (prominent) {
                    Modifier.shadow(16.dp, shape, clip = false, glass.accent, glass.accent)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(
                when {
                    prominent -> glass.accent.copy(alpha = alpha)
                    pressed -> glass.fillPressed
                    else -> glass.fillStrong
                }
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (prominent) {
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)
                        )
                    } else {
                        edgeBrush(glass)
                    }
                ),
                shape
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (prominent) glass.onAccent else glass.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = (if (prominent) glass.onAccent else glass.textPrimary).copy(alpha = alpha)
            )
        }
    }
}

/** Circular glass icon button -- back arrows, mode toggles. 44dp minimum tap target. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    val glass = LocalGlass.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "iconbtn"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) glass.accentSoft else glass.fillStrong)
            .border(
                BorderStroke(
                    1.dp,
                    if (active) glass.accent.copy(alpha = 0.5f) else glass.edgeHighlight
                ),
                CircleShape
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (active) glass.accent else glass.textPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Floating glass header. Not a TopAppBar -- it floats over the ambient layer. */
@Composable
fun GlassHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val glass = LocalGlass.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = glass.textPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textTertiary
                )
            }
        }
        trailing()
    }
}

/** Compact text-only segmented control -- traversal pickers, mode switches. */
@Composable
fun GlassSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlass.current
    val outer = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .clip(outer)
            .background(glass.fill)
            .border(BorderStroke(1.dp, edgeBrush(glass)), outer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) glass.accentSoft else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) glass.accent else glass.textTertiary
                )
            }
        }
    }
}

/** Glass input. Numeric by default -- every caller in this app enters integers. */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = true
) {
    val glass = LocalGlass.current
    val shape = RoundedCornerShape(18.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = glass.textPrimary),
        cursorBrush = SolidColor(glass.accent),
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(glass.fill)
            .border(BorderStroke(1.dp, edgeBrush(glass)), shape),
        decorationBox = { inner ->
            Box(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = glass.textTertiary
                    )
                }
                inner()
            }
        }
    )
}

/** Segmented glass tab bar, replacing NavigationBar on the visualizer screens. */
@Composable
fun GlassTabBar(
    tabs: List<Pair<String, ImageVector>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlass.current
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        strong = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (selected) glass.accentSoft else Color.Transparent)
                        .then(
                            if (selected) {
                                Modifier.border(
                                    BorderStroke(1.dp, glass.accent.copy(alpha = 0.4f)),
                                    RoundedCornerShape(17.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            tab.second,
                            contentDescription = tab.first,
                            tint = if (selected) glass.accent else glass.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = tab.first,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) glass.accent else glass.textTertiary
                        )
                    }
                }
            }
        }
    }
}
