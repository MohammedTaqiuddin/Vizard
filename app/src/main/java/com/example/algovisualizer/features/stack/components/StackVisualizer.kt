package com.example.algovisualizer.features.stack.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.core.data.NodeInfo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StackVisualizer(
    stack: List<NodeInfo>,
    peekedNodeId: Int?,
    poppedNodeIds: Set<Int>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(stack, key = { it.id }) { node ->
                AnimatedVisibility(
                    visible = !poppedNodeIds.contains(node.id),
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(500)
                    ) + fadeIn(animationSpec = tween(500)),
                    exit = fadeOut(animationSpec = tween(500))
                ) {
                    StackNode(
                        value = node.value,
                        peeked = node.id == peekedNodeId,
                    )
                }
            }
        }
    }
}

@Composable
fun StackNode(value: String, peeked: Boolean, modifier: Modifier = Modifier) {
    val cornerRadius by animateDpAsState(targetValue = 8.dp, label = "")
    val borderColor = if (peeked) Color.Yellow else MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .height(50.dp)
            .fillMaxWidth(0.7f)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.primary)
            .border(2.dp, borderColor, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}
