package com.example.algovisualizer.core.navigation

import com.example.algovisualizer.core.data.FeatureCategory
import com.example.algovisualizer.core.data.FeatureInfo
import com.example.algovisualizer.core.data.SubCategory

object FeatureRegistry {

    private val subCategories = listOf(
        SubCategory(
            id = "lists",
            name = "Lists",
            parentCategory = FeatureCategory.DATA_STRUCTURES,
            description = "Linear data structures"
        ),
        SubCategory(
            id = "trees",
            name = "Trees",
            parentCategory = FeatureCategory.DATA_STRUCTURES,
            description = "Hierarchical data structures"
        )
    )

    private val features = listOf(
        // LISTS
        FeatureInfo(
            id = "singly_linked_list",
            name = "Singly Linked List",
            category = FeatureCategory.DATA_STRUCTURES,
            subCategory = "lists",
            description = "Each node points to the next node",
            route = Screen.SinglyLinkedList.route
        ),
        FeatureInfo(
            id = "doubly_linked_list",
            name = "Doubly Linked List",
            category = FeatureCategory.DATA_STRUCTURES,
            subCategory = "lists",
            description = "Nodes with bidirectional pointers",
            route = Screen.DoublyLinkedList.route
        ),
        FeatureInfo(
            id = "circular_linked_list",
            name = "Circular Linked List",
            category = FeatureCategory.DATA_STRUCTURES,
            subCategory = "lists",
            description = "Last node points back to first",
            route = Screen.CircularLinkedList.route
        ),

        // TREES
        FeatureInfo(
            id = "bst",
            name = "Binary Search Tree",
            category = FeatureCategory.DATA_STRUCTURES,
            subCategory = "trees",
            description = "Sorted binary tree structure",
            route = Screen.BinarySearchTree.route
        ),
        FeatureInfo(
            id = "avl_tree",
            name = "AVL Tree",
            category = FeatureCategory.DATA_STRUCTURES,
            subCategory = "trees",
            description = "Self-balancing BST",
            route = Screen.AvlTree.route
        )
    )

    fun getAllFeatures(): List<FeatureInfo> = features

    fun getFeaturesByCategory(category: FeatureCategory): List<FeatureInfo> {
        return features.filter { it.category == category }
    }

    fun getSubCategoriesByCategory(category: FeatureCategory): List<SubCategory> {
        return subCategories.filter { it.parentCategory == category }
    }

    fun getFeaturesBySubCategory(subCategoryId: String): List<FeatureInfo> {
        return features.filter { it.subCategory == subCategoryId }
    }

    fun getAvailableCategories(): List<FeatureCategory> {
        return features.map { it.category }.distinct()
    }

    fun getFeatureById(id: String): FeatureInfo? {
        return features.find { it.id == id }
    }

    fun getSubCategoryById(id: String): SubCategory? {
        return subCategories.find { it.id == id }
    }
}