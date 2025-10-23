package com.example.algovisualizer.core.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.algovisualizer.core.data.FeatureCategory
import com.example.algovisualizer.features.avl.AvlScreen
import com.example.algovisualizer.features.bst.BstScreen
import com.example.algovisualizer.features.category.CategoryScreen
import com.example.algovisualizer.features.doublylinkedlist.DoublyLinkedListScreen
import com.example.algovisualizer.features.graph.GraphScreen
import com.example.algovisualizer.features.graph.TraversalAlgorithm
import com.example.algovisualizer.features.heap.HeapScreen
import com.example.algovisualizer.features.heap.HeapType
import com.example.algovisualizer.features.heap.HeapViewModel
import com.example.algovisualizer.features.heap.HeapViewModelFactory
import com.example.algovisualizer.features.home.HomeScreen
import com.example.algovisualizer.features.queue.QueueScreen
import com.example.algovisualizer.features.queue.QueueViewModel
import com.example.algovisualizer.features.singlylinkedlist.LinkedListScreen
import com.example.algovisualizer.features.subcategory.SubCategoryScreen
import com.example.algovisualizer.features.welcome.WelcomeScreen
import com.example.algovisualizer.features.stack.StackScreen

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
                onSubCategoryClick = { subCategory ->
                    navController.navigate(Screen.SubCategory.createRoute(subCategory.id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

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

        composable(Screen.SinglyLinkedList.route) {
            LinkedListScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.DoublyLinkedList.route) {
            DoublyLinkedListScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.CircularLinkedList.route) {
            PlaceholderScreen("Circular Linked List", onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Queue.route) {
            val viewModel: QueueViewModel = viewModel()
            QueueScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Stack.route) {
            StackScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.BinarySearchTree.route) {
            BstScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.AvlTree.route) {
            AvlScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.MinHeap.route) {
            val factory = HeapViewModelFactory(HeapType.MIN)
            val viewModel: HeapViewModel = viewModel(factory = factory)
            HeapScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.MaxHeap.route) {
            val factory = HeapViewModelFactory(HeapType.MAX)
            val viewModel: HeapViewModel = viewModel(factory = factory)
            HeapScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Bfs.route) {
            GraphScreen(
                algorithm = TraversalAlgorithm.BFS,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Dfs.route) {
            GraphScreen(
                algorithm = TraversalAlgorithm.DFS,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$title\nComing Soon!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}