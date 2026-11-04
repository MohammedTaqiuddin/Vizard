package com.example.algovisualizer.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.core.data.FeatureCategory
import com.example.algovisualizer.core.navigation.FeatureRegistry
import com.example.algovisualizer.core.theme.LocalGlass
import com.example.algovisualizer.core.ui.glass.GlassHeader
import com.example.algovisualizer.core.ui.glass.GlassListCard
import com.example.algovisualizer.core.ui.iconForCategory

@Composable
fun HomeScreen(
    onCategoryClick: (FeatureCategory) -> Unit
) {
    val glass = LocalGlass.current
    val categories = FeatureRegistry.getAvailableCategories()

    Column(modifier = Modifier.fillMaxSize()) {
        GlassHeader(
            title = "Vizard",
            subtitle = "${FeatureRegistry.getAllFeatures().size} visualizations"
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Choose a category",
            style = MaterialTheme.typography.bodyLarge,
            color = glass.textSecondary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { category ->
                GlassListCard(
                    title = category.displayName,
                    subtitle = "${FeatureRegistry.getFeaturesByCategory(category).size} visualizations",
                    leading = iconForCategory(category),
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

val FeatureCategory.displayName: String
    get() = when (this) {
        FeatureCategory.DATA_STRUCTURES -> "Data Structures"
        FeatureCategory.SORTING_ALGORITHMS -> "Sorting Algorithms"
        FeatureCategory.SEARCHING_ALGORITHMS -> "Searching Algorithms"
        FeatureCategory.GRAPH_ALGORITHMS -> "Graph Algorithms"
    }
