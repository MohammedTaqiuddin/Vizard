package com.example.algovisualizer.core.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Home : Screen("home")
    object Category : Screen("category/{categoryId}") {
        fun createRoute(categoryId: String) = "category/$categoryId"
    }
    object SubCategory : Screen("subcategory/{subCategoryId}") {
        fun createRoute(subCategoryId: String) = "subcategory/$subCategoryId"
    }

    // Lists
    object SinglyLinkedList : Screen("singly_linked_list")
    object DoublyLinkedList : Screen("doubly_linked_list")
    object CircularLinkedList : Screen("circular_linked_list")
    object Queue : Screen("queue")

    // Trees
    object BinarySearchTree : Screen("bst")
    object AvlTree : Screen("avl_tree")

    // Heaps
    object MinHeap : Screen("min_heap")
    object MaxHeap : Screen("max_heap")

    // Sorting
    object BubbleSort : Screen("bubble_sort")
    object QuickSort : Screen("quick_sort")

    // Graphs
    object Bfs : Screen("bfs")
    object Dfs : Screen("dfs")

    // Legacy - keep for backward compatibility
    object LinkedList : Screen("linked_list")
    object Heap : Screen("heap")
}