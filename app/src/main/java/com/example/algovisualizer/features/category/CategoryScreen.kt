package com.example.algovisualizer.features.category

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
import com.example.algovisualizer.core.data.FeatureCategory
import com.example.algovisualizer.core.data.SubCategory
import com.example.algovisualizer.core.navigation.FeatureRegistry
import com.example.algovisualizer.core.ui.accentForSubCategory
import com.example.algovisualizer.core.ui.glass.GlassHeader
import com.example.algovisualizer.core.ui.glass.GlassListCard
import com.example.algovisualizer.core.ui.iconForSubCategory
import com.example.algovisualizer.features.home.displayName

@Composable
fun CategoryScreen(
    category: FeatureCategory,
    onSubCategoryClick: (SubCategory) -> Unit,
    onBackClick: () -> Unit
) {
    val subCategories = FeatureRegistry.getSubCategoriesByCategory(category)

    Column(modifier = Modifier.fillMaxSize()) {
        GlassHeader(
            title = category.displayName,
            subtitle = "Pick a family",
            onBack = onBackClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(subCategories) { subCategory ->
                GlassListCard(
                    title = subCategory.name,
                    subtitle = subCategory.description,
                    leading = iconForSubCategory(subCategory.id),
                    accent = accentForSubCategory(subCategory.id),
                    onClick = { onSubCategoryClick(subCategory) }
                )
            }
        }
    }
}
