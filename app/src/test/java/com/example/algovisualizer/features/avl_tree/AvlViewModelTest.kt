package com.example.algovisualizer.features.avl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

/** Regression tests for the two AVL bugs found in review. */
@OptIn(ExperimentalCoroutinesApi::class)
class AvlViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun heightOf(node: AvlNode?): Int =
        if (node == null) 0 else 1 + max(heightOf(node.left), heightOf(node.right))

    /** Every node's balance factor must be within [-1, 1], and BST order must hold. */
    private fun assertBalancedBst(node: AvlNode?, low: Int = Int.MIN_VALUE, high: Int = Int.MAX_VALUE) {
        if (node == null) return
        assertTrue("value ${node.value} violates BST order", node.value in (low + 1)..(high - 1) ||
            (low == Int.MIN_VALUE && high == Int.MAX_VALUE) || (node.value > low && node.value < high))
        val balance = heightOf(node.left) - heightOf(node.right)
        assertTrue(
            "node ${node.value} has balance $balance, outside [-1,1]",
            abs(balance) <= 1
        )
        assertBalancedBst(node.left, low, node.value)
        assertBalancedBst(node.right, node.value, high)
    }

    private fun inOrder(node: AvlNode?, acc: MutableList<Int> = mutableListOf()): List<Int> {
        if (node == null) return acc
        inOrder(node.left, acc); acc.add(node.value); inOrder(node.right, acc)
        return acc
    }

    @Test
    fun `inserting still works after Clear Tree`() = runTest(dispatcher) {
        val vm = AvlViewModel()

        vm.insertValue(10); advanceUntilIdle()
        vm.insertValue(20); advanceUntilIdle()
        assertNotNull("setup: tree should exist before clearing", vm.root.value)

        vm.clearTree(); advanceUntilIdle()
        assertNull("clear should empty the tree", vm.root.value)

        // The bug: clearTree() cancelled the only consumer of the insertion channel, so
        // every later insert was posted and silently dropped.
        vm.insertValue(42); advanceUntilIdle()
        assertNotNull("insert after clear must still reach the tree", vm.root.value)
        assertEquals(listOf(42), inOrder(vm.root.value))
    }

    @Test
    fun `deleting a node with two children keeps the tree balanced`() = runTest(dispatcher) {
        val vm = AvlViewModel()
        // A shape where removing a two-child node leaves the subtree unbalanced unless
        // the delete path rebalances on the way back up.
        listOf(50, 30, 70, 20, 40, 60, 80, 10, 25).forEach {
            vm.insertValue(it); advanceUntilIdle()
        }
        assertBalancedBst(vm.root.value)
        val before = inOrder(vm.root.value)

        vm.deleteValue(70); advanceUntilIdle()

        val after = inOrder(vm.root.value)
        assertEquals("delete should remove exactly one value", before - 70, after)
        assertEquals("in-order must stay sorted", after.sorted(), after)
        assertBalancedBst(vm.root.value)
    }

    @Test
    fun `stored height matches the real height after deletes`() = runTest(dispatcher) {
        val vm = AvlViewModel()
        listOf(50, 30, 70, 20, 40, 60, 80).forEach { vm.insertValue(it); advanceUntilIdle() }
        vm.deleteValue(30); advanceUntilIdle()

        // The displayed balance factors come from these stored fields, so stale values are
        // a visible bug in a tree whose whole point is showing balance.
        fun check(node: AvlNode?) {
            if (node == null) return
            assertEquals("stored height wrong at ${node.value}", heightOf(node), node.height)
            check(node.left); check(node.right)
        }
        check(vm.root.value)
    }
}
