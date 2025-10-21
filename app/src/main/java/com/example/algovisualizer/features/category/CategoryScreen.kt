package com.example.algovisualizer.features.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.algovisualizer.core.data.FeatureCategory
import com.example.algovisualizer.core.data.FeatureInfo
import com.example.algovisualizer.core.navigation.FeatureRegistry
import com.example.algovisualizer.features.home.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    category: FeatureCategory,
    onFeatureClick: (FeatureInfo) -> Unit,
    onBackClick: () -> Unit
) {
    val features = FeatureRegistry.getFeaturesByCategory(category)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Select a visualization",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(features) { feature ->
                    FeatureCard(
                        feature = feature,
                        onClick = { onFeatureClick(feature) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureCard(
    feature: FeatureInfo,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = feature.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}