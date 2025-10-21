package com.example.algovisualizer.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.algovisualizer.core.data.FeatureCategory
import com.example.algovisualizer.features.category.CategoryScreen
import com.example.algovisualizer.features.home.HomeScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
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
                onFeatureClick = { feature ->
                    navController.navigate(feature.route)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}