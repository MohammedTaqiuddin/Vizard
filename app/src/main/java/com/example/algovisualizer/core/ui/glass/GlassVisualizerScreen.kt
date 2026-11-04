package com.example.algovisualizer.core.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.core.theme.LocalGlass

/**
 * Shared chrome for every visualizer: floating header, a canvas/code switch, and a glass
 * control panel docked in the thumb zone. Replaces the hand-rolled Scaffold + TopAppBar +
 * NavigationBar stack that each feature screen used to carry its own copy of.
 */
@Composable
fun GlassVisualizerScreen(
    title: String,
    onBack: () -> Unit,
    code: String,
    highlightedLines: Set<Int>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    canvasLabel: String = "Visual",
    canvasIcon: ImageVector = Icons.Default.PlayArrow,
    headerTrailing: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    controls: @Composable ColumnScope.() -> Unit,
    canvas: @Composable BoxScope.() -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        GlassHeader(
            title = title,
            subtitle = subtitle,
            onBack = onBack,
            trailing = headerTrailing
        )

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                canvas()
            } else {
                GlassCodePanel(code = code, highlightedLines = highlightedLines)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                strong = true
            ) {
                Column(modifier = Modifier.padding(16.dp), content = controls)
            }
            Spacer(Modifier.height(12.dp))
            GlassTabBar(
                tabs = listOf(
                    canvasLabel to canvasIcon,
                    "Code" to Icons.Default.Code
                ),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it }
            )
        }
    }
}

/**
 * One code panel for the whole app. Previously this exact composable existed six times,
 * differing only in the ViewModel type it reached into -- so it now takes plain data.
 */
@Composable
fun GlassCodePanel(
    code: String,
    highlightedLines: Set<Int>,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlass.current
    val codeLines = remember(code) { code.lines() }
    val listState = rememberLazyListState()
    val horizontalScroll = rememberScrollState()

    LaunchedEffect(highlightedLines) {
        val first = highlightedLines.minOrNull() ?: return@LaunchedEffect
        val target = (first - 1).coerceIn(0, (codeLines.size - 1).coerceAtLeast(0))
        val visible = listState.layoutInfo.visibleItemsInfo
        val onScreen = visible.any { it.index == target }
        if (!onScreen) listState.animateScrollToItem(target)
    }

    GlassSurface(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
                .horizontalScroll(horizontalScroll)
        ) {
            itemsIndexed(codeLines) { index, line ->
                val highlighted = (index + 1) in highlightedLines
                Text(
                    text = if (line.isEmpty()) " " else line,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (highlighted) glass.codeHighlight else androidx.compose.ui.graphics.Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = if (highlighted) glass.accent else glass.textSecondary,
                    softWrap = false
                )
            }
        }
    }
}
