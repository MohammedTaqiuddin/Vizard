package com.example.algovisualizer.features.linkedlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.algovisualizer.features.linkedlist.core.Board
import com.example.algovisualizer.features.linkedlist.core.Cell
import com.example.algovisualizer.features.linkedlist.core.Change
import com.example.algovisualizer.features.linkedlist.core.Frame
import com.example.algovisualizer.features.linkedlist.core.GridFullException
import com.example.algovisualizer.features.linkedlist.core.ListKind
import com.example.algovisualizer.features.linkedlist.core.ListState
import com.example.algovisualizer.features.linkedlist.core.Step
import com.example.algovisualizer.features.linkedlist.core.Timeline
import com.example.algovisualizer.features.linkedlist.core.clear
import com.example.algovisualizer.features.linkedlist.core.diffStates
import com.example.algovisualizer.features.linkedlist.core.fits
import com.example.algovisualizer.features.linkedlist.core.moveNode
import com.example.algovisualizer.features.linkedlist.core.perform
import com.example.algovisualizer.features.linkedlist.core.relayoutForKind
import com.example.algovisualizer.features.linkedlist.core.reverse
import com.example.algovisualizer.features.linkedlist.core.sampleList
import com.example.algovisualizer.features.linkedlist.core.toFrame
import com.example.algovisualizer.features.linkedlist.core.toListState
import com.example.algovisualizer.features.linkedlist.core.traceDeleteAt
import com.example.algovisualizer.features.linkedlist.core.traceInsertAt
import com.example.algovisualizer.features.linkedlist.core.traceSearch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Thin shell over the pure core. Operations that are worth watching run as step traces
 * (plan 12); the rest commit instantly. Either way the change list comes from one place.
 */
class LinkedListViewModel(initialKind: ListKind = ListKind.SINGLY) : ViewModel() {

    private val _state = MutableStateFlow(sampleList(initialKind))
    val state: StateFlow<ListState> = _state.asStateFlow()

    private val _timeline = MutableStateFlow(Timeline())
    val timeline: StateFlow<Timeline> = _timeline.asStateFlow()

    private val _changes = MutableStateFlow<List<Change>>(emptyList())
    val changes: StateFlow<List<Change>> = _changes.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var playJob: Job? = null

    /**
     * What the board draws: the trace's current frame while a trace is loaded, otherwise
     * the committed state. The board only ever sees a Frame, so it needs no changes to
     * render the half-linked moments inside an operation.
     */
    val frame: StateFlow<Frame> = combine(_state, _timeline) { s, t ->
        t.current?.frame ?: s.toFrame()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, sampleList(initialKind).toFrame())

    // ---- traced operations ------------------------------------------------

    private fun runTrace(label: String, build: (ListState) -> List<Step>) {
        playJob?.cancel()
        try {
            val before = _state.value
            val steps = build(before)
            if (steps.isEmpty()) return
            val after = steps.last().frame.toListState(seq = before.seq + 1, board = before.board)
            _changes.value = diffStates(before, after)
            _state.value = after
            _error.value = null
            writeLog(label, _changes.value)
            _timeline.value = Timeline.of(steps)
            play()
        } catch (e: Exception) {
            reportError(e)
        }
    }

    fun insertHead(value: Int) = runTrace("insert head $value") { it.traceInsertAt(0, value) }
    fun insertTail(value: Int) = runTrace("insert tail $value") { it.traceInsertAt(it.size, value) }
    fun deleteHead() = runTrace("delete head") { it.traceDeleteAt(0) }
    fun deleteTail() = runTrace("delete tail") { it.traceDeleteAt(it.size - 1) }

    /** Search changes nothing, so it only loads a trace to watch. */
    fun search(value: Int) {
        playJob?.cancel()
        try {
            val steps = _state.value.traceSearch(value)
            _error.value = null
            _log.value = (listOf("search $value") + _log.value).take(30)
            _timeline.value = Timeline.of(steps)
            play()
        } catch (e: Exception) {
            reportError(e)
        }
    }

    // ---- instant operations ----------------------------------------------

    private fun runInstant(label: String, op: (ListState) -> ListState) {
        playJob?.cancel()
        _timeline.value = Timeline()   // stale frames must not outlive the state they came from
        try {
            val applied = _state.value.perform(op)
            _state.value = applied.state
            _changes.value = applied.changes
            _error.value = null
            writeLog(label, applied.changes)
        } catch (e: Exception) {
            reportError(e)
        }
    }

    fun reverse() = runInstant("reverse") { it.reverse() }
    fun clear() = runInstant("clear") { it.clear() }
    fun loadSample() = runInstant("load sample") { sampleList(it.kind) }
    fun move(id: String, cell: Cell) = runInstant("move") { it.moveNode(id, cell) }

    /** The viewport decides how many cells there are; the model just records it. */
    fun setBoard(cols: Int, rows: Int) {
        val board = Board(cols, rows)
        if (board == _state.value.board) return
        _state.value = _state.value.copy(board = board)
    }

    fun setKind(kind: ListKind) {
        if (kind == _state.value.kind) return
        runInstant("switch to ${kind.label}") { it.relayoutForKind(kind) }
    }

    fun canPlace(id: String, cell: Cell): Boolean = _state.value.fits(cell, ignoreId = id)

    // ---- playback ---------------------------------------------------------

    fun play() {
        playJob?.cancel()
        _timeline.value = _timeline.value.play()
        playJob = viewModelScope.launch {
            while (_timeline.value.playing && !_timeline.value.atEnd) {
                delay(_timeline.value.holdMillis())
                if (!_timeline.value.playing) break
                _timeline.value = _timeline.value.next()
            }
            _timeline.value = _timeline.value.pause()
        }
    }

    fun pause() {
        playJob?.cancel()
        _timeline.value = _timeline.value.pause()
    }

    fun stepForward() { playJob?.cancel(); _timeline.value = _timeline.value.pause().next() }
    fun stepBack() { playJob?.cancel(); _timeline.value = _timeline.value.back() }
    fun restart() { playJob?.cancel(); _timeline.value = _timeline.value.restart() }
    fun finish() { playJob?.cancel(); _timeline.value = _timeline.value.finish() }
    fun goTo(index: Int) { playJob?.cancel(); _timeline.value = _timeline.value.goTo(index) }
    fun setSpeed(speed: Float) { _timeline.value = _timeline.value.copy(speed = speed) }

    /** Drop the trace and show the committed structure again. */
    fun dismissTrace() {
        playJob?.cancel()
        _timeline.value = Timeline()
    }

    // ---- helpers ----------------------------------------------------------

    private fun reportError(e: Exception) {
        _error.value = when (e) {
            is GridFullException -> "The board is full — delete a node or clear the list."
            is IllegalArgumentException, is IllegalStateException ->
                e.message ?: "That operation isn't valid here."
            else -> throw e
        }
    }

    private fun writeLog(label: String, changes: List<Change>) {
        val summary = describe(changes)
        _log.value = (listOf(if (summary.isEmpty()) label else "$label · $summary") + _log.value)
            .take(30)
    }

    /** One readable line per operation, built from the pointer diff. */
    private fun describe(changes: List<Change>): String {
        if (changes.isEmpty()) return ""
        // Reverse touches every pointer; summarise rather than printing ten lines (plan 8).
        if (changes.size > 6) return "${changes.size} pointer fields updated"
        val labels = _state.value.nodes
        fun name(id: String) = labels[id]?.value?.toString() ?: "node"
        return changes.joinToString(" · ") { c ->
            when (c) {
                is Change.Head -> "HEAD = ${c.to ?: "NULL"}"
                is Change.Tail -> "TAIL = ${c.to ?: "NULL"}"
                is Change.Next -> "${name(c.nodeId)}.next = ${c.to ?: "NULL"}"
                is Change.Prev -> "${name(c.nodeId)}.prev = ${c.to ?: "NULL"}"
                is Change.Addr -> "${name(c.nodeId)} moved ${c.from} → ${c.to}"
            }
        }
    }
}

val ListKind.label: String
    get() = when (this) {
        ListKind.SINGLY -> "Singly"
        ListKind.DOUBLY -> "Doubly"
        ListKind.CIRCULAR_SINGLY -> "Circular"
        ListKind.CIRCULAR_DOUBLY -> "Circular ×2"
    }

val ListKind.title: String
    get() = when (this) {
        ListKind.SINGLY -> "Singly Linked List"
        ListKind.DOUBLY -> "Doubly Linked List"
        ListKind.CIRCULAR_SINGLY -> "Circular Linked List"
        ListKind.CIRCULAR_DOUBLY -> "Circular Doubly Linked List"
    }

val ListKind.blurb: String
    get() = when (this) {
        ListKind.SINGLY -> "tail.next = NULL"
        ListKind.DOUBLY -> "pointers run both ways"
        ListKind.CIRCULAR_SINGLY -> "tail.next = head"
        ListKind.CIRCULAR_DOUBLY -> "wraps in both directions"
    }
