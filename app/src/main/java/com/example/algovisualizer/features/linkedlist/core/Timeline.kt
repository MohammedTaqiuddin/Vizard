package com.example.algovisualizer.features.linkedlist.core

/**
 * Pure playback state over a trace (plan 12.2). Holds no coroutines and no Compose types,
 * so the stepping rules are unit-testable on their own.
 *
 * Stepping back works because every [Step] carries a full [Frame]; going back is just
 * moving the index, not undoing anything.
 */
data class Timeline(
    val steps: List<Step> = emptyList(),
    val index: Int = 0,
    val playing: Boolean = false,
    val speed: Float = 1f
) {
    val isEmpty: Boolean get() = steps.isEmpty()
    val current: Step? get() = steps.getOrNull(index)
    val atStart: Boolean get() = index <= 0
    val atEnd: Boolean get() = index >= steps.size - 1
    val progress: Float get() = if (steps.size < 2) 1f else index.toFloat() / (steps.size - 1)

    fun next(): Timeline =
        if (atEnd) copy(playing = false) else copy(index = index + 1)

    fun back(): Timeline =
        if (atStart) this else copy(index = index - 1, playing = false)

    fun restart(): Timeline = copy(index = 0, playing = false)

    fun finish(): Timeline = copy(index = (steps.size - 1).coerceAtLeast(0), playing = false)

    fun goTo(target: Int): Timeline =
        copy(index = target.coerceIn(0, (steps.size - 1).coerceAtLeast(0)), playing = false)

    fun play(): Timeline = if (isEmpty || atEnd) restart().copy(playing = true) else copy(playing = true)

    fun pause(): Timeline = copy(playing = false)

    /** How long to hold on the current step before advancing, in ms. */
    fun holdMillis(): Long {
        val base = when {
            current?.effects?.any { it is Effect.Travel } == true -> 700L
            current?.effects?.any { it is Effect.Retarget } == true -> 650L
            current?.effects?.any { it is Effect.Create || it is Effect.Free } == true -> 600L
            else -> 500L
        }
        return (base / speed).toLong().coerceAtLeast(60L)
    }

    /**
     * What the operation has cost so far, counted from the effects of every step up to and
     * including the current one. This is the number the complexity lecture is about: a
     * student can watch "pointers walked" grow with n on a search and stay at 1 on an
     * insert at head, instead of being told O(n) and O(1).
     */
    fun cost(): Cost {
        var walked = 0
        var writes = 0
        var allocs = 0
        steps.take(index + 1).forEach { step ->
            step.effects.forEach { e ->
                when (e) {
                    is Effect.Visit, is Effect.Travel, is Effect.Compare -> walked++
                    is Effect.Retarget -> writes++
                    is Effect.Create -> allocs++
                    is Effect.Free -> allocs++
                }
            }
        }
        return Cost(walked, writes, allocs)
    }

    companion object {
        fun of(steps: List<Step>): Timeline = Timeline(steps = steps, index = 0, playing = false)
    }
}

/** Work done by an operation, in the units a complexity analysis counts. */
data class Cost(val walked: Int, val pointerWrites: Int, val nodes: Int) {
    val isEmpty: Boolean get() = walked == 0 && pointerWrites == 0 && nodes == 0
}

val SPEEDS = listOf(0.5f, 1f, 1.5f, 2f)
