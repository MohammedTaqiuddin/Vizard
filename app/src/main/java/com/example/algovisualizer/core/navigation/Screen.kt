package com.example.algovisualizer.core.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Category : Screen("category/{categoryId}") {
        fun createRoute(categoryId: String) = "category/$categoryId"
    }
    object BinarySearchTree : Screen("bst")
    object LinkedList : Screen("linked_list")
    object BubbleSort : Screen("bubble_sort")
}