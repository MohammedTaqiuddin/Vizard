# Linked List Visualizer — Implementation Plan

Interactive, draggable visualizer for **all linked list types** on a coordinate grid. Every node shows its own address `(x, y)` above it. Pointer boxes (NEXT / PREV) hold the addresses of their targets. When a node is dragged, its address changes and **every pointer that references it updates live**, with arrows redrawn.

Reference prototype (singly linked): the "Linked List Grid" artifact built earlier. This plan generalizes it.

---

## 1. Scope

### List types

| Type | Node anatomy | Entry points | Terminal rule |
|---|---|---|---|
| Singly linked (SLL) | `DATA \| NEXT` | HEAD | `tail.next = NULL` |
| Doubly linked (DLL) | `PREV \| DATA \| NEXT` | HEAD, TAIL | `head.prev = NULL`, `tail.next = NULL` |
| Circular singly (CSLL) | `DATA \| NEXT` | HEAD (optionally TAIL/LAST) | `tail.next = head` |
| Circular doubly (CDLL) | `PREV \| DATA \| NEXT` | HEAD | `tail.next = head`, `head.prev = tail` |

Optional later: header/sentinel-node variants (a dummy node at the head) and skip lists (multi-level NEXT). The architecture below supports these without changes to the core.

### Operations (all types)
- Insert at head, insert at tail, insert at index
- Delete head, delete tail, delete by index / by click
- Search by value (step-by-step traversal animation)
- Reverse
- Drag a node to a new cell (address change)
- Clear / load sample

### Out of scope for v1
- Persisting lists to a backend (add later through a `serialize()` / `load()` API)
- Multi-user editing

---

## 2. Core design decisions

### 2.1 Store order + positions; derive pointers
Do **not** store `next` / `prev` fields on nodes. Store:

```ts
type Kind = 'singly' | 'doubly' | 'circular-singly' | 'circular-doubly';

interface Cell { c: number; r: number }          // grid address (x = column, y = row)

interface LLNode {
  id: string;          // stable identity (never the address)
  value: number;
  cell: Cell;
}

interface ListState {
  kind: Kind;
  order: string[];                 // node ids in list order; order[0] is head
  nodes: Record<string, LLNode>;
}
```

Pointers are a pure function of `(kind, order, nodes)`:

```ts
type Addr = Cell | null;           // null renders as NULL

interface Pointers {
  head: Addr;
  tail: Addr;                      // shown for DLL / CSLL as a convenience
  next: Record<string, Addr>;      // per node id
  prev: Record<string, Addr>;      // empty object for singly variants
}

function pointersOf(s: ListState): Pointers {
  const ids = s.order, n = ids.length, at = (i: number) => s.nodes[ids[i]].cell;
  const circular = s.kind.startsWith('circular');
  const doubly = s.kind.endsWith('doubly');
  const p: Pointers = { head: n ? at(0) : null, tail: n ? at(n - 1) : null, next: {}, prev: {} };
  ids.forEach((id, i) => {
    p.next[id] = i < n - 1 ? at(i + 1) : circular ? at(0) : null;
    if (doubly) p.prev[id] = i > 0 ? at(i - 1) : circular ? at(n - 1) : null;
  });
  return p;
}
```

Why:
- The structure can never become inconsistent: a DLL can't have `a.next = b` without `b.prev = a`.
- All four types share the same operations code; only `pointersOf` knows about the type.
- Switching type (e.g. SLL → CDLL) is just changing `kind`.

### 2.2 Pointer diff drives all feedback
Every mutation goes through one function:

```ts
function apply(state: ListState, op: (s: ListState) => ListState): { state: ListState; changes: Change[] } {
  const before = pointersOf(state);
  const next = op(structuredClone(state));
  const after = pointersOf(next);
  return { state: next, changes: diffPointers(before, after, next) };
}

type Change =
  | { field: 'head' | 'tail'; from: Addr; to: Addr }
  | { field: 'next' | 'prev'; nodeId: string; from: Addr; to: Addr }
  | { field: 'addr'; nodeId: string; from: Cell; to: Cell };
```

`changes` feeds three things, the same way for every list type:
1. **Flash**: highlight each changed box (NEXT, PREV, address label, HEAD/TAIL chip).
2. **Log**: `node 3 moved (1,7) → (4,2) · node 2.next = (4,2) · node 4.prev = (4,2)`.
3. **Memory table** re-render.

### 2.3 Who changes when a node moves

The diff computes this automatically. The table below is for **writing tests** and for the learner-facing explanation panel:

| Type | Moving node *k* updates… |
|---|---|
| SLL | its address; `node(k-1).next` (or HEAD if k is head) |
| DLL | its address; `node(k-1).next` (or HEAD); `node(k+1).prev` (or TAIL) |
| CSLL | its address; `node(k-1).next`; if k is head → also HEAD and `tail.next` |
| CDLL | its address; `node(k-1).next` and `node(k+1).prev`, both wrapping around the ends; HEAD if k is head |

Edge cases: a single node in a circular list points to itself (`next = prev = own address`), so moving it updates its own pointer boxes too.

---

## 3. Module layout

Assumes React + TypeScript (adapt the `ui/` folder to your stack; everything in `core/` is framework-free).

```
src/features/linked-list/
  core/
    types.ts            # Kind, Cell, LLNode, ListState, Pointers, Change
    pointers.ts         # pointersOf(), diffPointers()
    operations.ts       # insertHead/Tail/At, deleteAt, reverse, moveNode, clear
    grid.ts             # GRID config, nodeSpan(kind), fits(), freeSpot()
    traversal.ts        # search generator yielding visited ids (for animation)
    samples.ts          # seed lists per kind
  render/
    geometry.ts         # cell → px, node box, port positions (prev/next anchors)
    wires.ts            # path builders: forward, back-edge, wrap-around, self-loop
  ui/
    LinkedListVisualizer.tsx   # top-level: state, reducer, toolbar, panels
    Board.tsx                  # grid background, axes, ghost, node layer, SVG wire layer
    NodeView.tsx               # address label, DATA/NEXT/PREV boxes, delete button
    Wires.tsx                  # renders paths from wires.ts
    MemoryTable.tsx            # columns adapt to kind
    ChangeLog.tsx
    useDrag.ts                 # pointer-events drag + keyboard move
    useFlash.ts                # turns Change[] into short-lived highlight classes
  __tests__/
    pointers.test.ts
    operations.test.ts
    grid.test.ts
```

State: one `useReducer` with actions `{ type: 'insert' | 'delete' | 'move' | 'reverse' | 'setKind' | 'clear' | 'load' }`. The reducer calls `apply()` and stores `changes` alongside state so the UI can flash and log them.

---

## 4. Grid and layout

```ts
export const GRID = { cell: 56, cols: 20, rows: 12, padLeft: 34, padTop: 34 };
export const nodeSpan = (k: Kind) => (k.endsWith('doubly') ? 3 : 2);   // cells wide
```

- Node address = its **top-left cell** `(c, r)`.
- `fits(cell, span, ignoreId)`: inside bounds and no horizontal overlap with another node in the same row.
- `freeSpot(state)`: try `tail.c + span + 1` on the tail's row, then next even row, then scan. Leave one empty cell between nodes so arrows have room.
- Changing `kind` changes `span`. When switching from 2-wide to 3-wide nodes, re-check every node with `fits`; push any that now overlap to `freeSpot`, and produce `addr` changes so the user sees them move.
- Make `cols/rows` responsive later (grow the board when a node is dropped near the edge).

---

## 5. Rendering wires

Ports on each node box:
- `nextOut`: center of NEXT box → leaves from right edge
- `prevOut`: center of PREV box → leaves from left edge
- `in`: left-center (for next arrows), right-center (for prev arrows)

Wire styles:
| Wire | Style | Route |
|---|---|---|
| next (forward) | solid, accent color, offset slightly **above** node center | cubic Bézier, control points `max(40, |dx|/2)` horizontally |
| prev (backward) | solid, second color, offset slightly **below** node center | mirror of next |
| circular back-edge (tail → head) | dashed, same color as next | route **around**: down below the lower of the two nodes, across, back up — avoids slicing through the middle of the list |
| self-loop (1-node circular) | small arc from NEXT box looping back into own left edge | |
| HEAD / TAIL | dotted, muted, from a fixed chip at the board's top-left | |

Offsetting next and prev vertically (±6px) keeps DLL arrow pairs from overlapping. Arrowheads use SVG `<marker>` with `fill` from a theme token.

Wires read **live** positions from node boxes during drag (not the committed cells), so arrows follow the pointer smoothly.

---

## 6. Interaction

### Drag (useDrag)
1. `pointerdown` on node → `setPointerCapture`, store offset.
2. `pointermove` → node follows pointer freely; compute snapped target cell; `valid = fits(target)`. Show a ghost cell (blue = valid, red outline = taken).
3. During drag, **preview** the address change: render the dragged node's label and all referencing boxes with the target address (compute via `pointersOf` on a temp state). No log entries yet.
4. `pointerup` → if valid and changed, dispatch `move`; otherwise snap back and show an inline message.

### Keyboard
- Nodes are focusable (`tabIndex=0`).
- Arrow keys move one cell (dispatch `move` if `fits`).
- Delete/Backspace removes the node.
- Every node gets an `aria-label` with index, address, data, next, prev.

### Toolbar
- Kind switcher (segmented control: Singly / Doubly / Circular / Circular doubly)
- Value input + Insert head / Insert tail / Insert at index
- Delete head / Delete tail
- Search value (animated traversal) · Reverse · Clear · Load sample

### Traversal animation
`traversal.ts` exports a generator yielding the id currently visited. The UI steps through it every ~500ms, highlighting the node and the wire just followed. For circular lists, stop when you return to head (guard against infinite loops by also capping at `order.length` steps).

---

## 7. Panels

**Memory table** — columns adapt to `kind`:

| # | Address | PREV (doubly only) | DATA | NEXT |
|---|---|---|---|---|

Show HEAD (and TAIL for DLL/circular) above the table. Highlight rows whose fields changed in the last operation.

**Change log** — newest first, capped at ~30 entries, one line per operation built from `Change[]`.

**Explain panel (optional)** — for the last operation, show which pointers had to change and why, using the table in §2.3. Useful for teaching.

---

## 8. Edge cases checklist

- [ ] Empty list: HEAD = NULL, TAIL = NULL, no wires, empty-state message
- [ ] Single node SLL/DLL: next/prev NULL
- [ ] Single node circular: self-loop; moving it updates its own NEXT/PREV
- [ ] Two-node CDLL: `a.next = a.prev = b` and vice versa (two wires each way, keep offsets so they don't overlap)
- [ ] Delete head in circular: tail.next (and head.prev for CDLL) now point at new head
- [ ] Reverse: every pointer changes; log should summarize ("reversed 5 nodes · HEAD = (x,y)") rather than list 10+ lines
- [ ] Drop on occupied cell / outside grid: revert
- [ ] Kind switch causing overlaps (2-wide → 3-wide)
- [ ] Grid full on insert: clear error message
- [ ] Duplicate values are allowed (identity is `id`, not value)
- [ ] Touch devices: `touch-action: none` on board and nodes; delete button always visible on `hover: none`

---

## 9. Testing

Unit tests on `core/` only (Vitest or Jest). The UI stays thin.

**Invariants to assert after every operation, for every kind:**
- `order` has no duplicates and every id exists in `nodes`.
- No two nodes overlap (`fits` holds for all).
- DLL / CDLL: for every node `a`, if `next[a] = cell(b)` then `prev[b] = cell(a)`.
- Circular: following `next` from head visits all `n` nodes and returns to head in exactly `n` steps.
- Non-circular: following `next` from head reaches NULL in exactly `n` steps.

**Change-diff tests** (one per row of §2.3): move node *k*, assert the exact set of `Change` entries.

**Property test (optional):** random sequences of insert/delete/move/reverse against all four kinds, checking invariants (fast-check).

---

## 10. Milestones

| # | Milestone | Done when |
|---|---|---|
| 1 | `core/` types, `pointersOf`, `diffPointers`, operations for SLL | Unit tests pass for SLL |
| 2 | Board + NodeView + wires + drag for SLL | Matches the prototype: drag updates address, prev.next, arrows, table, log |
| 3 | Doubly linked | 3-cell nodes, PREV box, prev wires below center, invariant tests pass |
| 4 | Circular singly | Back-edge routing, self-loop, head move updates tail.next |
| 5 | Circular doubly | Both wrap-around wires, 2-node case renders cleanly |
| 6 | Operations UI | Insert at index, delete by index, reverse, search animation for all kinds |
| 7 | Kind switcher | Switching relayouts without overlaps and logs moves |
| 8 | Polish | Keyboard, aria labels, reduced motion, dark mode, mobile layout |
| 9 | (Optional) Sentinel / skip list | Extra node kinds through `pointersOf` only |

Build milestones 1–2 first and treat SLL as the reference implementation. Each later type should be mostly a change in `pointersOf` and `wires.ts`.
