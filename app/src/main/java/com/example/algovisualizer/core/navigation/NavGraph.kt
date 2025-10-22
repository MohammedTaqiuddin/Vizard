package com.example.algovisualizer.core.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.algovisualizer.core.data.FeatureCategory
import com.example.algovisualizer.features.avl.AvlScreen
import com.example.algovisualizer.features.bst.BstScreen
import com.example.algovisualizer.features.singlylinkedlist.LinkedListScreen
import com.example.algovisualizer.features.singlylinkedlist.LinkedListViewModel
import com.example.algovisualizer.features.category.CategoryScreen
import com.example.algovisualizer.features.doublylinkedlist.DoublyLinkedListScreen
import com.example.algovisualizer.features.subcategory.SubCategoryScreen
import com.example.algovisualizer.features.home.HomeScreen
import com.example.algovisualizer.features.welcome.WelcomeScreen
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Welcome.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onStartClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onCategoryClick = { category ->
                    navController.navigate(Screen.Category.createRoute(category.name))
                }
            )
        }

        composable(
            route = Screen.Category.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")
            val category = try {
                FeatureCategory.valueOf(categoryId ?: "DATA_STRUCTURES")
            } catch (e: IllegalArgumentException) {
                FeatureCategory.DATA_STRUCTURES
            }

            CategoryScreen(
                category = category,
                onSubCategoryClick = { subCategory ->  // ← FIXED: Changed from onFeatureClick
                    navController.navigate(Screen.SubCategory.createRoute(subCategory.id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // SubCategory Screen
        composable(
            route = Screen.SubCategory.route,
            arguments = listOf(
                navArgument("subCategoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subCategoryId = backStackEntry.arguments?.getString("subCategoryId") ?: ""

            SubCategoryScreen(
                subCategoryId = subCategoryId,
                onFeatureClick = { feature ->
                    navController.navigate(feature.route)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // BST Screen
        composable(Screen.BinarySearchTree.route) {
            BstScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Singly Linked List
        composable(Screen.SinglyLinkedList.route) {
            LinkedListScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Placeholders for other features
        composable(Screen.DoublyLinkedList.route) {
            DoublyLinkedListScreen(
                onBackClick = { navController.popBackStack() })
        }

        composable(Screen.CircularLinkedList.route) {
            PlaceholderScreen("Circular Linked List", navController::popBackStack)
        }

        composable(Screen.AvlTree.route) {
            AvlScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Heap.route) {
            PlaceholderScreen("Doubly Linked List", navController::popBackStack)
        }
    }
}

// Placeholder screen for unimplemented features
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    onBackClick: () -> Unit
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text(title) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBackClick) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = "$title\nComing Soon!",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}