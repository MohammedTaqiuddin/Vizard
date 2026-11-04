# Vizard — Watch data structures think

An Android app that teaches data structures by showing them working, one step at a time,
with the narration and the picture always in agreement.

> **Status note (2026-10-15).** The linked-list module is the only one rebuilt on the new
> architecture. It now has **two views** — Chain and Memory — because a linked list has two
> things to teach and they want opposite layouts. See [§7.10](#710-the-two-views).

---

## 1. What this is, and who it is for

Vizard is a teaching tool, not a reference implementation. The user is a student meeting a
data structure for the first time, usually on a phone, usually the night before it matters.

Three commitments follow from that, and every design decision in this document is
downstream of them:

1. **The picture is the explanation.** If a student has to read a paragraph to understand
   the drawing, the drawing failed.
2. **Nothing happens instantly.** Every operation that changes structure plays as a
   sequence of steps the student can pause, step back through and scrub.
3. **The student can be wrong safely.** Any state reachable by poking at the app must be
   recoverable, and must still be a *correct* data structure.

The thing Vizard is competing with is a textbook diagram. A textbook diagram is static but
it is *clean*. Vizard only earns its place by being clearer than that, not merely
more animated. A screen a student would rather skip in favour of the textbook is a bug,
regardless of whether the code is correct.

---

## 2. Current state at a glance

| | |
|---|---|
| Package | `com.example.algovisualizer` |
| App name | Vizard |
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose, Material 3, single Activity |
| Gradle / AGP | 9.6.1 / 9.4.1 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 24 |
| Java | 17 source/target, built with Android Studio's bundled JBR (Java 25) |
| Compose BOM | 2026.09.00 |
| Source | ~7,900 lines of Kotlin across 50 files |
| Tests | 28 JVM unit tests, 2 Compose instrumentation tests |
| Modules shipped | 10 visualizers across 4 families |

Build commands live in [§11](#11-building-and-running).

---

## 3. Navigation map

A single `MainActivity` hosts one `NavHost`. Every destination is an object in
`core/navigation/Screen.kt`; the catalogue that drives the browse screens is
`core/navigation/FeatureRegistry.kt`.

```
Welcome  ("Watch data structures think")
   │
   └─ Home ─────────────── Choose a category
          │
          └─ Category ──── Data Structures → pick a family
                 │
                 └─ SubCategory ─── Lists | Trees | Heaps | Graphs
                        │
                        └─ Visualizer screen
```

### The ten visualizers

| Family | Screen | Route | Implementation |
|---|---|---|---|
| Lists | Singly Linked List | `singly_linked_list` | unified `features/linkedlist` |
| Lists | Doubly Linked List | `doubly_linked_list` | unified `features/linkedlist` |
| Lists | Circular Linked List | `circular_linked_list` | unified `features/linkedlist` |
| Lists | Queue | `queue` | `features/queue` (legacy) |
| Trees | Binary Search Tree | `bst` | `features/bst` (legacy) |
| Trees | AVL Tree | `avl_tree` | `features/avl_tree` (legacy) |
| Heaps | Min Heap | `min_heap` | `features/heap` (legacy) |
| Heaps | Max Heap | `max_heap` | `features/heap` (legacy) |
| Graphs | Breadth-First Search | `bfs` | `features/graph` (legacy) |
| Graphs | Depth-First Search | `dfs` | `features/graph` (legacy) |

"Legacy" means: written before the current architecture, still working, not yet ported.
`Screen.kt` also declares `BubbleSort`, `QuickSort`, `LinkedList` and `Heap` routes that
nothing navigates to — dead entries kept from an earlier structure.

---

## 4. Architecture

### 4.1 The layering rule

The linked-list module is the reference for how every module should eventually be built.
It has three layers and the dependency arrows only point one way:

```
core/     pure Kotlin. No Compose, no Android, no coroutines.
            The data structure, the operations, the step traces, playback state.
            Fully unit-testable on the JVM.
  ↑
render/   pure geometry and painting. Compose types (Offset, Rect, Path) but no state
            and no composables. Converts a Frame into shapes.
  ↑
ui/       composables and the ViewModel. Holds state, runs coroutines, draws.
```

`core/` never imports `render/` or `ui/`. `render/` never imports `ui/`. This is what makes
the hard parts testable: the invariants of a circular doubly linked list are checked by
plain JUnit with no device, and the playback rules are checked without a coroutine.

### 4.2 The two ideas that make it work

**Pointers are derived, never stored.** A node carries `(id, value, cell)` and nothing
else. `next` and `prev` are computed from `(kind, order, nodes)` by `pointersOf()`. The old
doubly-linked-list module stored `prev` on each node and had a long-standing bug where
reverse traversal walked a stale chain. In the new model that bug is *unrepresentable* —
there is no second copy of the truth to fall out of sync.

**Everything the screen draws is a `Frame`.** A `ListState` is always a valid list, which
means it cannot express the interesting moments: during an insert there is an instant where
`newNode.next` already points at its successor while the predecessor's `next` has not moved
yet. That half-linked moment is the entire lesson. A `Frame` therefore stores pointers
*explicitly*, by id, and the render layer consumes only `Frame`. Wiring up the step engine
required zero changes to the drawing code, because the drawing code already spoke `Frame`.

### 4.3 State flow

```
user action
    ↓
LinkedListViewModel
    ├── traced op   → build List<Step> → commit final state → load Timeline → play
    └── instant op  → perform(op) → commit state → clear Timeline
    ↓
frame: StateFlow<Frame> = timeline.current?.frame ?: state.toFrame()
    ↓
LinkedListBoard draws it
```

The final state is committed **immediately**, and the timeline then replays how it got
there. So navigating away mid-animation can never leave a half-finished structure, and a
test can assert that the last frame of a trace equals the instant result.

---

## 5. The design system

### 5.1 Glass

The visual language is Apple-style "liquid glass": translucent surfaces over a slow-moving
aurora background, hairline highlights, almost no hard edges.

`core/theme/Glass.kt` defines **28 tokens**, with a full light set and a full dark set
switched by the system setting (dynamic colour is deliberately **off** — the palette carries
meaning, so it cannot be hijacked by the wallpaper).

| Group | Tokens |
|---|---|
| Background | `base`, `baseDeep`, `orbPrimary`, `orbSecondary`, `orbTertiary`, `orbAlpha` |
| Surfaces | `fill`, `fillStrong`, `fillPressed`, `edgeHighlight`, `edgeShadow`, `shadowTint` |
| Text | `textPrimary`, `textSecondary`, `textTertiary` |
| Accent | `accent`, `accentSoft`, `onAccent` |
| Algorithm semantics | `nodeIdle`, `nodeComparing`, `nodeFound`, `nodeVisited`, `nodeQueued`, `edge`, `onNode` |
| Code | `codeHighlight` |

Reached through `LocalGlass.current`. The algorithm-semantic tokens are the important
group: "comparing" is one colour across the whole app, so a student learns the vocabulary
once.

### 5.2 Typography

`core/theme/Type.kt`: **four sizes** (32 / 22 / 16 / 13) and **two weights**. Values and
addresses are monospace so digits line up.

### 5.3 Components

`core/ui/glass/`:

- `GlassBackground` — three aurora orbs animated on a single shared 26-second phase
- `GlassComponents` — `GlassSurface`, `GlassListCard`, `GlassButton`, `GlassIconButton`,
  `GlassHeader`, `GlassTabBar`, `GlassSegmented`, `GlassTextField`
- `GlassVisualizerScreen` — the shared chrome every visualizer sits in: floating header,
  canvas/code tab switch, and a glass control panel docked in the thumb zone
- `GlassCodePanel` — one code panel for the whole app (this composable previously existed
  six times, differing only in which ViewModel it reached into)

`core/ui/FeatureVisuals.kt` gives each family an icon and an accent: lists = cyan,
trees = mint, heaps = amber, graphs = violet.

### 5.4 Rules the design follows

- Primary actions in the bottom third (thumb zone)
- 8-point spacing grid
- Tap targets ≥ 44dp
- `GlassSurface` defaults to **elevation 0**. Android paints elevation shadows beneath a
  layer assuming it is opaque, so a 58%-opaque glass surface rendered its own spot shadow
  as a hard dark patch bleeding through. Only the opaque prominent button casts a shadow.
- `imePadding()` plus `windowSoftInputMode=adjustResize` — the keyboard must never cover
  the controls

---

## 6. Module reference

### 6.1 `core/`

| File | Purpose |
|---|---|
| `navigation/Screen.kt` | Route constants |
| `navigation/NavGraph.kt` | The `NavHost` |
| `navigation/FeatureRegistry.kt` | Catalogue of families and features driving browse screens |
| `data/NodeInfo.kt`, `data/VisualizationStep.kt` | Shared value types |
| `theme/` | Glass tokens, theme, typography |
| `ui/glass/` | The component library |
| `ui/FeatureVisuals.kt` | Per-family icon and accent |
| `ui/BaseVisualizerViewModel.kt` | **Dead code.** A step-player that nothing extends |
| `ui/components/CodeViewPanel.kt` | **Superseded** by `GlassCodePanel` |

### 6.2 `features/linkedlist/` — the rebuilt module

```
core/     Types.kt  Pointers.kt  Operations.kt  Grid.kt
          Frame.kt  Steps.kt  Traces.kt  Timeline.kt
render/   Geometry.kt  Layout.kt  Wires.kt  NodeArt.kt
ui/       LinkedListBoard.kt  LinkedListScreen.kt
          LinkedListViewModel.kt  PlaybackBar.kt
```

One module serves all four kinds via `ListKind`:

| Kind | `span` | `doubly` | `circular` |
|---|---|---|---|
| `SINGLY` | 2 cells | no | no |
| `DOUBLY` | 3 cells | yes | no |
| `CIRCULAR_SINGLY` | 2 cells | no | yes |
| `CIRCULAR_DOUBLY` | 3 cells | yes | yes |

`span` is how many grid cells a node occupies: `DATA|NEXT`, or `PREV|DATA|NEXT`.

Detail in [§7](#7-the-linked-list-engine).

### 6.3 `features/queue/`

Legacy enqueue/dequeue visualizer. Kept deliberately — the plan only folds Queue into the
unified list engine at a later milestone.

### 6.4 `features/bst/` and `features/avl_tree/`

Binary search tree and self-balancing AVL tree with rotation animations. Both were
bug-fixed but not re-architected:

- AVL `clearTree()` used to leave a poisoned insertion channel so that **every insert after
  a clear silently failed**. Now drains the queue and restarts the processor.
- AVL two-child delete skipped rebalancing on one path. Rebalancing is now a single
  `rebalance(node)` both paths call.
- `depths[node.value]!!` could throw on a node missing from the depth map in both modules;
  now a safe skip.

Covered by `AvlViewModelTest` (3 tests).

### 6.5 `features/heap/`

Min and max heap sharing a `HeapType`. Known issue: `_nodeStates` is keyed by node *value*,
so duplicate values collide and highlight together.

### 6.6 `features/graph/`

BFS and DFS over an editable graph. Known issues: switching to the Code tab aborts a
running traversal; `GraphVisualizer.kt:62` hand-writes an inverse view transform
(`(tapOffset - offset) / scale`) instead of using `Modifier.graphicsLayer`, which is the
same fragile pattern that caused four separate drag failures in the list module.

### 6.7 `features/welcome|home|category|subcategory/`

The browse flow. All rebuilt on glass components.

---

## 7. The linked-list engine

### 7.1 Model (`core/Types.kt`)

```kotlin
data class Cell(val c: Int, val r: Int)              // grid address
data class LLNode(val id: String, val value: Int, val cell: Cell)
data class ListState(kind, order: List<String>, nodes: Map<String, LLNode>, seq: Int)
```

Identity is the **id**, not the value, so duplicate values are legal and behave correctly.
`seq` is a monotonic id source held in state so that operations stay pure functions.

### 7.2 Derived pointers (`core/Pointers.kt`)

`pointersOf()` computes `head`, `tail`, `next`, `prev` from `(kind, order, nodes)`.
Wrap-around for circular kinds and the `null` terminators for linear kinds live in exactly
one place. `walk()` traverses and terminates correctly for all four kinds.

`diffStates(before, after)` produces the change list that drives the log:

```kotlin
sealed interface Change { Head; Tail; Next; Prev; Addr }
```

### 7.3 Operations (`core/Operations.kt`)

`insertAt`, `insertHead`, `insertTail`, `deleteAt`, `deleteHead`, `deleteTail`,
`deleteById`, `indexOfValue`, `reverse`, `moveNode`, `clear`, `sampleList`.

All pure `ListState -> ListState`. `perform { }` wraps one and returns
`Applied(state, changes)`.

> `apply` was renamed **`perform`** because Kotlin's stdlib already owns `apply`.

`reverse()` reverses `order` only — **addresses stay put, only pointers flip**, which is
precisely the lesson.

### 7.4 The board (`core/Grid.kt`)

Nodes live at grid addresses. There is deliberately **no maximum board size in the model**:
how many cells exist is a property of the display, not of the data structure. `boardCells`
picks a cell size that reads well and then takes as many whole cells as fit the viewport, so
a tablet or a landscape window gets a genuinely larger board at the *same* node size. Only
a floor (`MIN_COLS = 8`, `MIN_ROWS = 4`) survives, for very small screens.

The size the viewport reports is stored on `ListState.board`, so pure operations can place a
node without every signature growing two parameters — and because "the board is full" really
is a fact about this list on this screen.

The view fits the **whole board**, not the nodes' bounding box. Fitting the nodes made the
visible area equal the occupied area: there was nowhere to drag *to*, and columns past ~11
never came on screen at all.

`fits(cell, ignoreId)` rejects anything off the board or horizontally overlapping a
neighbour on the same row. `freeSpot()` places a new node after the tail, wrapping to the
next row. `relayoutForKind()` handles 2-wide nodes becoming 3-wide on a kind switch.

### 7.5 Frames and steps (`core/Frame.kt`, `core/Steps.kt`)

```kotlin
enum class NodeStatus { NORMAL, NEW, DETACHED, FREED }

data class Step(
    val line: String,        // id of the pseudocode line to highlight
    val frame: Frame,        // the complete picture at this instant
    val vars: Map<String, String?>,   // variable chips: curr, newNode, idx
    val effects: List<Effect>,        // Create | Visit | Travel | Retarget | Compare | Free
    val note: String                  // the narration
)
```

Every step carries a **full frame**. Stepping backwards is therefore just moving an index —
nothing is ever undone, so there is no inverse operation to get wrong.

### 7.6 Traces (`core/Traces.kt`)

`traceInsertAt`, `traceDeleteAt`, `traceSearch` produce `List<Step>` following storyboards:
walk to the position comparing as you go, create the node, retarget pointers in the correct
order, free the old node.

### 7.7 Playback (`core/Timeline.kt`, `ui/PlaybackBar.kt`)

`Timeline` is pure: `next`, `back`, `restart`, `finish`, `goTo`, `play`, `pause`,
`holdMillis()` (per-step duration scaled by speed), `SPEEDS = [0.5, 1, 1.5, 2]`.

`PlaybackBar` renders narration, variable chips, a scrubber with one tick per step,
transport controls, a step counter and a speed toggle.

### 7.8 Rendering (`render/`)

- `Geometry.kt` — `Metrics` (pixel sizing), `NodeBox` (where every sub-rect and socket
  sits), `boardBounds`, `boardCells`, `fitTransform`, `Cell.address()`
- `Layout.kt` — `ListLayout`, `ChainLayout`, `MemoryLayout`, `morphedPositions`
- `Wires.kt` — the orthogonal router: `route`, `nearestFace`, `crossings`, `selfLoop`,
  `arrowHead`
- `NodeArt.kt` — gradient body, recessed pointer sockets, specular top edge, two-layer soft
  shadow, status outlines (NEW dashed, DETACHED, FREED faded)

### 7.9 Wire routing

The first router drew a cubic between two **fixed sockets**: `next` always left the
right-hand side and always entered the target's left-hand side. Any target that wasn't to
the right forced the curve to double back on itself — which is exactly where the loops and
curls in Memory view came from. The arrow had to leave east and arrive from the west no
matter where the node actually was.

The current router assumes neither:

1. The arrow starts at a **dot in the socket centre** and may leave in any direction.
2. It ends on whichever **face of the target is nearest**, so it points at the node rather
   than at one particular socket.
3. Paths are **orthogonal polylines with rounded corners**, chosen from a candidate set —
   straight, both L shapes, then Z detours through a series of channels. The first candidate
   that crosses no other node wins; if none is clear, the least-bad one is used so a wire
   always draws.
4. `next` prefers the upper/left channel, `prev` the lower/right, so the two never share a
   lane. The caller nudges the entry probe into its lane, which is what makes a `next` and a
   `prev` crossing the same gap run as two parallel rails instead of one line.
5. A **self loop** is drawn only for the one case it is correct: a single-node circular list.
6. A null pointer is **`∅` drawn inside its socket** — no arrow flying off to a floating
   `NULL` label.

### 7.10 The two views

A linked list has two lessons and they want opposite layouts, so the module has two, with a
`Chain | Memory` segmented control above the canvas. (The tab bar below still switches
canvas vs. source; these are two views of the same picture, so they do not belong there.)

| | **Chain** (default) | **Memory** |
|---|---|---|
| Position means | list order | address |
| Teaches | what a linked list **is** | **why** it isn't an array |
| Layout | `ChainLayout`, auto, wraps in reading order | `MemoryLayout`, the node's own cell |
| Sockets show | `∅` or nothing | `∅` or a hex address |
| Dragging | no | yes |
| Grid dots | hidden | shown |

They are **not two screens**. `LinkedListBoard` draws at `lerp(chain, memory, t)` and
animates `t` on the toggle, because the transition is itself the teaching moment: the nodes
fly from tidy order to scattered addresses while every pointer stays attached, which is
precisely the fact a static diagram cannot show. Dragging is enabled only at `t == 1`.

`chainPerLine` decides how many nodes go on a line, and it is not a constant: it tries every
line length and keeps the one that draws the biggest node, breaking ties toward fewer lines.
A phone in portrait has height going spare and takes a wrap; a tablet or landscape window
keeps the chain on one line at the same node size. Chain view also uses a tighter gap
(`CHAIN_GAP = 1`) than memory view, because its arrows are separated by lane rather than by
width.

### 7.9 The view transform, and why it is written this way

The board has no panning and no pinch — both were removed at the user's request. The view
scale and offset are derived from content only.

The transform is applied with **`Modifier.graphicsLayer(...)` placed before
`Modifier.pointerInput`** in the chain. Compose routes pointer input through layer
transforms automatically, so the gesture receives coordinates already in board space. The
alternative — `withTransform {}` in the draw scope — transforms *drawing only*, leaving you
to hand-write an inverse for hit testing. That hand-written inverse was the cause of
multiple drag bugs here and still exists in `GraphVisualizer.kt:62`.

The gesture is keyed on `Unit` and reads `frame`, `fits`, `onMove` and `isPlaying`
through `rememberUpdatedState`.

> **Why this matters, in one sentence:** `pointerInput` keeps its lambda alive until a key
> changes, a move only mutates `Frame.nodes`, and `nodes` is not a key — so keying the
> gesture on `frame.order` left it hit-testing *pre-move positions forever*. First drag
> worked, every drag after it did not. Reading live state through `State` removes the
> entire bug class, and `LinkedListBoardDragTest` drags the same node twice to prove it.

---

## 8. Testing

| Suite | Where | Count | Covers |
|---|---|---|---|
| `LinkedListCoreTest` | `test/` | 16 | invariants for all 4 kinds, change-diff rows, grid rules, trace↔instant equivalence at every index, search, timeline |
| `LayoutAndWireTest` | `test/` | 8 | both layouts cover every node at every `t`; the morph never touches pointers or order; chain wraps in reading order; **200 seeded scrambles with no wire crossing a node it does not connect**; wires orthogonal and never doubling back; `next` and `prev` not sharing a lane |
| `AvlViewModelTest` | `test/` | 3 | clear-then-insert, rebalance on both delete paths |
| `ExampleUnitTest` | `test/` | 1 | scaffolding |
| `LinkedListBoardDragTest` | `androidTest/` | 2 | second drag from the node's new position; old cell is not draggable |

Two habits worth keeping:

- **Mutation testing.** Every non-obvious suite here has been run against a deliberately
  broken version first. Breaking the circular wrap in `pointersOf` failed 4 of 10 core
  tests; reverting the drag fix failed both drag tests with the predicted values; making the
  router return a direct line — what the old cubic effectively drew — failed both wire
  tests. A test that passes against the broken version is worthless, and for stale-capture
  bugs it *will* pass, because correct and broken code behave identically on the first
  interaction.
- **Shared geometry.** The drag test aims its touches via the same `fitTransform` /
  `boardCells` the board lays out with. A test carrying its own copy of production maths can
  agree with a bug.

> `adb shell input swipe` is a smoke test, not a verification. It synthesises one clean
> pointer and does not reproduce real finger input.

---

## 9. What is wrong right now

### 9.0 Fixed, and worth remembering why

Three problems were fixed on 2026-10-15. They are recorded because the *reasoning* is
reusable, not because the code still needs it.

| Was | Root cause | Now |
|---|---|---|
| Doubly view unreadable; a student was better off with the textbook | only the Memory view existed, and it is the *second* lesson | Chain view, and it is the default — §7.10 |
| Nothing placeable past column 11; board capped 12×12 | the viewport was fitted to the nodes, so the free area equalled the occupied area; then a cap measured off one phone | board derived from the viewport, no maximum — §7.4 |
| Arrows looping and curling | fixed socket exit/entry sides forced a cubic to double back | orthogonal router, nearest-face entry — §7.9 |

<details>
<summary>The original diagnosis of the first one, kept because the distinction still governs the module</summary>

### 9.1 The default view taught the wrong lesson first

Two genuinely different views have been collapsed into one, and the advanced one was made
the default:

| | **Structural view** | **Memory view** |
|---|---|---|
| Shows | the chain, in list order | nodes at arbitrary addresses |
| Teaches | what a linked list *is* — traversal, head/tail, how insert rewires | *why* it differs from an array — non-contiguous storage, pointer chasing |
| Position means | list order | a memory address; order is carried by the pointers |
| Node placement | automatic, always tidy | arbitrary, draggable |
| Right for | first contact | after the chain is understood |

The memory view is the better *insight* — drag a node to a random address, watch every
pointer that references it update, and see that **the list did not change, only the
addresses did**. That is a genuinely excellent lesson and it is already implemented.

It was the wrong *first* lesson. A student meeting a doubly linked list saw four nodes on a
2×2 grid with arrows looping around the outside and learned nothing. The structural view had
never been built, so there was nothing clean to fall back to.

</details>

### 9.1 Open items

- **Code tab is static.** `pseudocodeFor(kind)` is a fixed string and `highlightedLines` is
  hardcoded empty. Every `Step` already carries a `line` id — the mapping from id to listing
  line is all that is missing.
- **Change log is one line.** The multi-line change panel is not built.
- **Cost meter counts but does not compare.** It shows walked / pointer writes / nodes for
  the current operation; it does not yet plot that against n.
- **Speed chip clips** at the right edge of the playback bar.
- **Tree and heap screens** still carry ~38 hardcoded colours, because they build per-depth
  palettes with `darker()`/`lighter()` rather than tokens.
- `BaseVisualizerViewModel` and `CodeViewPanel` are dead.
- **Nothing in this session is committed to git.**

---

## 10. Direction

In priority order.

1. **Circular wrap routing** as its own pass — the wrap edges work but deserve dedicated
   channels rather than the general candidate search.
2. **Walk mode.** Step a `curr` pointer along the chain by hand, so traversal is something
   the student does rather than watches.
3. **Rewire mode.** Edit a `Frame`'s pointers directly and let the student build a broken
   list on purpose. `core/reachableFrom(head)` greys out unreachable nodes; the edit commits
   only if the frame still walks to a valid list. This is where the classic wrong-order
   insert (`curr.next = newNode` before `newNode.next = curr.next`) becomes visible as two
   nodes going grey, instead of a sentence in a lecture.
4. **Compare-with-array mode** — the same operation on an array beside the list, with both
   cost meters running.
5. **Scramble addresses** button in Memory view, so the "the list didn't change, only the
   addresses did" lesson is demonstrated on purpose rather than discovered by accident.
6. **Wire up the code tab** — `step.line` → highlighted listing line, auto-scrolled.
7. **Port the remaining modules** to the `core`/`render`/`ui` split with step traces,
   starting with the trees, where the step engine pays off most.
8. **Fix `GraphVisualizer`'s manual inverse** with `graphicsLayer`.

---

## 11. Building and running

The only JDK on the development machine is Android Studio's bundled JBR, so `JAVA_HOME`
must be set explicitly:

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:installDebug
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:testDebugUnitTest
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:connectedDebugAndroidTest
```

With more than one device attached, `installDebug` installs to all of them. To target one:

```bash
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

Instrumentation tests need a connected device and take a filter:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=<fully.qualified.TestClass>
```
