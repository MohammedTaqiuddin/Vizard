package com.example.algovisualizer.core.navigation


/**
 * Sealed class representing all navigation routes
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Category : Screen("category/{categoryId}") {
        fun createRoute(categoryId: String) = "category/$categoryId"
    }
    object BinarySearchTree : Screen("bst")
    object LinkedList : Screen("linked_list")
    object BubbleSort : Screen("bubble_sort")
    // Add more as features are developed
}