package com.example.algovisualizer.core.navigation



import com.example.algovisualizer.core.data.FeatureCategory
import com.example.algovisualizer.core.data.FeatureInfo

/**
 * Central registry of all available features in the app.
 * New features are simply added to this list.
 */
object FeatureRegistry {

    private val features = listOf(
        // Data Structures
        FeatureInfo(
            id = "bst",
            name = "Binary Search Tree",
            category = FeatureCategory.DATA_STRUCTURES,
            description = "Visualize BST operations: insert, search, delete",
            route = Screen.BinarySearchTree.route
        ),
        FeatureInfo(
            id = "linked_list",
            name = "Linked List",
            category = FeatureCategory.DATA_STRUCTURES,
            description = "Visualize linked list operations: append, delete, traverse",
            route = Screen.LinkedList.route
        ),

        // Sorting Algorithms
        FeatureInfo(
            id = "bubble_sort",
            name = "Bubble Sort",
            category = FeatureCategory.SORTING_ALGORITHMS,
            description = "Visualize the bubble sort algorithm step by step",
            route = Screen.BubbleSort.route
        )

        // Add more features here as they're developed
    )

    /**
     * Get all features
     */
    fun getAllFeatures(): List<FeatureInfo> = features

    /**
     * Get features by category
     */
    fun getFeaturesByCategory(category: FeatureCategory): List<FeatureInfo> {
        return features.filter { it.category == category }
    }

    /**
     * Get all categories that have at least one feature
     */
    fun getAvailableCategories(): List<FeatureCategory> {
        return features.map { it.category }.distinct()
    }

    /**
     * Get a feature by its ID
     */
    fun getFeatureById(id: String): FeatureInfo? {
        return features.find { it.id == id }
    }
}