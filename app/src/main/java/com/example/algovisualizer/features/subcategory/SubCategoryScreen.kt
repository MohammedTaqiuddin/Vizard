package com.example.algovisualizer.features.subcategory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.core.data.FeatureInfo
import com.example.algovisualizer.core.navigation.FeatureRegistry
import com.example.algovisualizer.core.ui.accentForFeature
import com.example.algovisualizer.core.ui.glass.GlassHeader
import com.example.algovisualizer.core.ui.glass.GlassListCard
import com.example.algovisualizer.core.ui.iconForFeature

@Composable
fun SubCategoryScreen(
    subCategoryId: String,
    onFeatureClick: (FeatureInfo) -> Unit,
    onBackClick: () -> Unit
) {
    val subCategory = FeatureRegistry.getSubCategoryById(subCategoryId)
    val features = FeatureRegistry.getFeaturesBySubCategory(subCategoryId)

    Column(modifier = Modifier.fillMaxSize()) {
        GlassHeader(
            title = subCategory?.name ?: "Features",
            subtitle = subCategory?.description,
            onBack = onBackClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(features) { feature ->
                GlassListCard(
                    title = feature.name,
                    subtitle = feature.description,
                    leading = iconForFeature(feature.id),
                    accent = accentForFeature(feature.id),
                    onClick = { onFeatureClick(feature) }
                )
            }
        }
    }
}
