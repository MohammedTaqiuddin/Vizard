package com.example.algovisualizer.core.navigation

import com.example.algovisualizer.core.data.FeatureCategory
import com.example.algovisualizer.core.data.FeatureInfo

object FeatureRegistry {

    private val features = listOf(
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
            description = "Visualize linked list operations",
            route = Screen.LinkedList.route
        ),
        FeatureInfo(
            id = "bubble_sort",
            name = "Bubble Sort",
            category = FeatureCategory.SORTING_ALGORITHMS,
            description = "Visualize bubble sort algorithm",
            route = Screen.BubbleSort.route
        )
    )

    fun getAllFeatures(): List<FeatureInfo> = features

    fun getFeaturesByCategory(category: FeatureCategory): List<FeatureInfo> {
        return features.filter { it.category == category }
    }

    fun getAvailableCategories(): List<FeatureCategory> {
        return features.map { it.category }.distinct()
    }

    fun getFeatureById(id: String): FeatureInfo? {
        return features.find { it.id == id }
    }
}