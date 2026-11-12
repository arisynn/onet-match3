# Onet + Triple Match: Reusable Components & Architecture Specification

**Specification Version:** 1.0.0  
**Audit Date:** 2026-11-12  
**Author:** Agent 01 — Repository Audit & Asset Specialist (`@auditor`)  
**Target Repository:** `/root/.gemini/antigravity/scratch/onet-match3`  
**Downstream Consumers:** Kotlin/libGDX Architect (`@architect`), Gameplay Engine (`@gameplay`), Economy (`@economy`), and QA (`@qa`)

---

## 1. Architectural Philosophy & Component Overview

The codebase is engineered around **strict separation between pure domain logic and host presentation**. The game logic contains zero dependencies on browser DOM, React, Canvas, audio subsystems, or hardware clocks. This functional purity makes every component immediately reusable across different target platforms (libGDX JVM, Android Native, iOS RoboVM, WebAssembly, Flutter, or Node.js).

```mermaid
flowchart TD
    subgraph Host Presentation / Engine Layer (Kotlin / libGDX)
        Screen["GameScreen / Stage (Scene2D)"]
        Renderer["SpriteBatch / TextureAtlas"]
        Input["GestureListener / InputAdapter"]
        Audio["Sound / Music Manager"]
        Timer["Host Delta Clock / Ticker"]
    end

    subgraph Pure Domain Logic (Portable JVM / Kotlin)
        OnetEngine["Onet Connect Engine<br>(board.js / round.js)"]
        TripleEngine["Triple Match Engine<br>(match3/index.js)"]
        PRNG["Deterministic PRNG<br>(random.js)"]
        TileRegistry["Tile & Theme Catalog<br>(tiles.js)"]
    end

    subgraph Asset Design System
        UI_Kit["Cozy Ceramic UI Kit<br>(visuals.js / manifest.json)"]
        TileArt["273 Ready Tile Sprites"]
    end

    Input -->|Coordinates| OnetEngine
    Input -->|Stack Index| TripleEngine
    Timer -->|Delta Seconds| OnetEngine
    Timer -->|Delta Seconds| TripleEngine
    PRNG -->|Pseudo-Random Stream| OnetEngine
    PRNG -->|Pseudo-Random Stream| TripleEngine

    OnetEngine -->|State Snapshot| Screen
    TripleEngine -->|State Snapshot| Screen
    TileRegistry -->|Asset Paths| Renderer
    UI_Kit -->|Nine-Patch / Atlas Regions| Renderer
```

---

## 2. Onet Connect Component Engine

### 2.1 Board Topology & Data Model
- **Playfield Matrix (`src/onet/board.js`):**
  - Dimensions: `(ROWS + 2) x (COLS + 2)` = `12 x 8` grid (`ROWS = 10`, `COLS = 6`).
  - Rows `1..10` and Columns `1..6` represent active tile positions.
  - Row 0, Row 11, Column 0, and Column 7 are empty perimeter border cells (`0`). These border cells are mandatory to permit valid Onet routes that travel around the outside edge of the board.
- **Tile Object Representation:**
  ```typescript
  interface Tile {
    id: string;               // e.g., 'candy_0', 'sakura_3'
    themeKey: string;         // e.g., 'candy', 'sakura'
    groupRows?: number;       // 1 or 2
    groupCols?: number;       // 1 or 2
    isSplit?: boolean;        // true if multi-cell footprint
    splitOrientation?: 'horizontal' | 'vertical';
    openSide: 'up' | 'down' | 'left' | 'right' | null; // Arrow constraint
    isSlave?: boolean;        // true if part of multi-cell group
    masterR?: number;         // pointer to master cell
    masterC?: number;         // pointer to master cell
  }
  ```
- **Master/Slave Footprint System (`groupSize`, `getTileCells`):**
  - Multi-cell tiles ($2\times 2$, $1\times 2$, $2\times 1$) occupy multiple grid cells.
  - The top-left cell stores the primary tile object; other cells store a slave record `{id, isSlave: true, masterR, masterC}`.
  - Selecting any slave cell transparently delegates to the master cell.

---

### 2.2 Constrained Orthogonal Pathfinding (`getPath`)
- **Mathematical Rule:** Two matching tiles can be connected if and only if there exists an orthogonal path between them with **at most two turns** ($\le 2$ right-angle bends / $\le 3$ perpendicular segments).
- **Algorithm Implementation:** Breadth-First Search (BFS) over grid states:
  - State tuple: `(r, c, current_direction, turn_count, path_points)`.
  - Cardinal directions: Up `(-1, 0)`, Right `(0, 1)`, Down `(1, 0)`, Left `(0, -1)`.
  - Turns increment when `current_direction != next_direction`. If `turns > 2`, branch exploration terminates immediately.
  - **Open-Side Arrow Restriction:**
    - If Tile A has an `openSide` (e.g. `'right'`), the first step must exit in that direction.
    - If Tile B has an `openSide` (e.g. `'down'`), the final step entering Tile B must approach from the opposite direction (`'up'`).
  - Target coordinate match: If `(r, c)` reaches an allowed endpoint cell with matching ID and matching footprint dimensions, the route is returned as `{ path: Point[] }`.

---

### 2.3 Progressive Footprint Splitting System
Tiles progressively divide into smaller sub-units upon matching:

```mermaid
stateDiagram-v2
    [*] --> Area4: Level >= 3 (2x2 Giant Tile)
    Area4 --> Area2: Match (Awards 200 pts) -> Splits to 1x2 or 2x1
    Area2 --> Area1: Match (Awards 150 pts) -> Splits to 1x1
    Area1 --> Cleared: Match (Awards 100 pts) -> Removed from Board
    Cleared --> [*]
```

- **Deterministic Counterpart Pairing (`clearMatchedGroups`):**
  - When Pair A and Pair B ($2\times 2$) match, each splits into two children along the same split axis (`horizontal` or `vertical`).
  - Child 0 of Parent A and Child 0 of Parent B receive the **identical new tile ID**:
    $$\text{ID}_0 = \text{pool}[(\text{parentIndex} + 1 + 2\cdot\text{depth}) \pmod{\text{length}}]$$
  - Child 1 of Parent A and Child 1 of Parent B receive the **identical new tile ID**:
    $$\text{ID}_1 = \text{pool}[(\text{parentIndex} + 1 + 2\cdot\text{depth} + (\text{depth} + 2)) \pmod{\text{length}}]$$
  - **Sibling Distinction Invariant:** Sibling tiles occupying adjacent spaces within the same parent footprint are guaranteed to have different IDs ($\text{ID}_0 \ne \text{ID}_1$), preventing instant auto-clearing within a single footprint.

---

### 2.4 Solvability Heuristic & Shuffling Engine
- **Solvability Verification (`isBoardSolvable`):** Simulates greedy resolution up to 150 iterations using `findHint()`. If all active tiles are cleared, the board is solvable.
- **Level Generation (`generateBoard`):**
  - Levels 1–25 scale group counts ($0 \to 2$), timer ($150\text{s} \to 75\text{s}$), pair separation distance ($2 \to 8$), and arrow pairs ($0 \to 6$).
  - Places paired footprints on exposed perimeter edges.
  - Fills remaining positions using `constructPairs()`: searches from the outside inward, favoring longer path distances ($\ge \text{separation}$) to produce engaging level layouts.
  - Overlays arrow pairs along verified legal simulation paths via `addDirectionTiles()`.
- **Guaranteed Solvable Shuffle (`guaranteedShuffle`):**
  1. **Phase 1 (Fast Shuffle):** Shuffles remaining unit tiles up to 5 times. Tests `isBoardSolvable(board)`.
  2. **Phase 2 (Pair Reconstruction):** If Phase 1 fails, groups tiles by ID and executes `constructPairs()` along known open routes.
  3. **Phase 3 (Emergency Recovery):** If Phase 2 fails, flattens any remaining footprints into unit tiles and peels guaranteed legal paths from outside inward.

---

### 2.5 State Transition Machine (`src/onet/round.js`)
All gameplay state transitions are pure functions:

| Function | Signature | Responsibility |
|---|---|---|
| `createOnetRound` | `({theme, level, rng, board, seconds}) -> State` | Initializes state, duration, and board. |
| `selectTile` | `(state, r, c, timestampMs) -> State` | Handles selection, route finding, combo increment, and animation locking (`pending`). |
| `resolveMatch` | `(state) -> State` | Commits pending match, removes tiles / splits footprints, awards points. |
| `tickOnetRound` | `(state, deltaSeconds) -> State` | Decrements timer; transitions to `'lost'` on timeout. |
| `pauseOnetRound`| `(state, paused) -> State` | Pauses timer and locks tile selection. |
| `shuffleRound` | `(state, rng) -> State` | Reshuffles board with guaranteed solvable hint. |
| `needsShuffle` | `(state) -> Boolean` | Returns `true` if active board has remaining tiles but 0 valid hints. |
| `onetResult` | `(state) -> ResultSummary` | Computes final score, combo, flawless status, and elapsed time. |

---

## 3. Triple Match Tray Engine (`src/match3/index.js`)

### 3.1 Stack Generation & Invariants (`createStacks`)
- **Layout:** Exactly 12 stacks, each containing a depth of 3 tiles = **36 tiles total**.
- **Tile ID Selection:** Randomly selects exactly 6 distinct tile IDs from the provided theme pool.
- **Multiple-of-3 Guarantee:** Distributes tiles across 3 layers such that each of the 6 tile IDs appears an exact multiple of 3 times ($\sum = 36$).

---

### 3.2 Tray Resolution Mechanics (`pickTile`)
- **Capacity:** The tray accommodates up to `TRAY_CAPACITY = 7` tiles.
- **Trio Clearing Rule:**
  $$\text{tray} = [\dots \text{tray}, \text{pickedTile}]$$
  $$\text{if } \text{count}(\text{pickedTile}) == 3 \implies \text{remove all 3}, \text{score} += 150$$
- **The 7th Slot Invariant:**
  If the tray contains 6 tiles and the player picks a 7th tile:
  1. If the 7th tile matches 2 tiles already in the tray, the trio clears immediately, reducing tray occupancy to 4. **The player does not lose.**
  2. If the 7th tile does not form a trio, tray occupancy reaches 7. **The game transitions to `'lost'`.**

---

### 3.3 Single-Use Undo System (`undoPick`)
- Prior to every pick, a deep snapshot `{stacks, tray, cleared}` is cached in `state.undo`.
- Invoking `undoPick()` restores the previous snapshot and sets `undoUsed = true`.
- **Invariants:**
  - Can only be executed once per game round.
  - Does not refund elapsed time.
  - Cannot be used once round has ended (`'won'` or `'lost'`).

---

## 4. Deterministic Seeded PRNG (`src/shared/random.js`)

### 4.1 Algorithm Specification
Uses a 32-bit FNV-1a string hasher to produce an initial unsigned 32-bit integer state, followed by an imul-based bit permutation (SplitMix32/Murmur3 variant):

```javascript
export const seededRandom = (seed) => {
  let state = 2166136261;
  for (const char of String(seed)) {
    state = Math.imul(state ^ char.charCodeAt(0), 16777619);
  }
  return () => {
    state += 0x6D2B79F5;
    let t = Math.imul(state ^ state >>> 15, state | 1);
    t ^= t + Math.imul(t ^ t >>> 7, t | 61);
    return ((t ^ t >>> 14) >>> 0) / 4294967296;
  };
};
```

### 4.2 Kotlin Parity Porting Blueprint
To achieve 100% bit-exact parity in Kotlin:

```kotlin
package game.shared

class SeededRandom(seed: String) {
    private var state: Int = -2128831035 // 2166136261 as signed 32-bit Int

    init {
        for (ch in seed) {
            state = (state xor ch.code) * 16777619
        }
    }

    fun nextFloat(): Float = nextDouble().toFloat()

    fun nextDouble(): Double {
        state += 0x6D2B79F5.toInt()
        var t = (state xor (state ushr 15)) * (state or 1)
        t = t xor (t + ((t xor (t ushr 7)) * (t or 61)))
        val unsigned = (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
        return unsigned.toDouble() / 4294967296.0
    }
}
```

---

## 5. Cozy Ceramic UI Kit Component Integration

### 5.1 Layered Visual Compositing Pipeline
Tiles in the game do not have hardcoded background frames. Instead, they are composited via four decoupled render passes:

```
[Pass 4] Particle Dust / Burst Effect (assets/ui-kit/effects/ or animations/)
   ▲
[Pass 3] Highlight / Selection Ring / Directional Arrow
   ▲
[Pass 2] Tile Symbol Icon (assets/tiles/{theme}/{id}.webp, scaled to iconSafeRect)
   ▲
[Pass 1] Ceramic Tile Body (assets/ui-kit/tile-bodies/{shape}-{palette}.webp)
```

### 5.2 Nine-Patch & Inset Guidelines (`manifest.json`)
- **Tile Bodies:**
  - `square-*`: 512×512, normalized safe icon rect `[0.18, 0.16, 0.64, 0.62]`
  - `large-*`: 768×768, normalized safe icon rect `[0.22, 0.18, 0.56, 0.54]`
  - `horizontal-*`: 768×384, normalized safe icon rect `[0.16, 0.18, 0.68, 0.58]`
  - `vertical-*`: 384×768, normalized safe icon rect `[0.18, 0.16, 0.64, 0.62]`
- **Tray & Slot Composition:**
  - Render the empty ceramic tray (`assets/ui-kit/trays/{palette}.webp`, 1024×320).
  - Overlay 7 discrete ceramic slot cavities (`assets/ui-kit/slots/{palette}.webp`, 384×384) spaced evenly across the horizontal tray bounds.

### 5.3 Animation Strategies
Three interchangeable formats are available for the 4 animated visual effects (`match-burst`, `hint-pulse`, `tile-pop-dust`, `victory-confetti`):
1. **Animated WebP (`assets/ui-kit/animations/*.webp`):** Single-file format for engines with native WebP animation decoders.
2. **Discrete PNG Frames (`assets/ui-kit/effects/{effect}/00..07.png`):** 8 separate PNGs per animation. Recommended for libGDX `com.badlogic.gdx.graphics.g2d.Animation<TextureRegion>`.
3. **Sprite Strips (`assets/ui-kit/effects/*-strip.png`):** Single horizontal 2048×256 strip (8 frames of 256×256). Sliced via `TextureRegion.split(256, 256)`.

---

## 6. Kotlin / libGDX Multi-Project Translation Blueprint

### 6.1 Recommended Package Structure
```
core/src/main/kotlin/
└── game/
    ├── shared/
    │   ├── Config.kt                # ROWS = 10, COLS = 6
    │   ├── SeededRandom.kt          # PRNG matching JS implementation
    │   ├── TileCatalog.kt           # Themes, IDs, and asset path resolver
    │   └── VisualCatalog.kt         # UI Kit palettes, slots, trays, animations
    ├── onet/
    │   ├── OnetBoard.kt             # BFS pathfinder, group splitter, hint, shuffle
    │   ├── OnetDifficulty.kt        # Level progression & difficulty curve
    │   ├── OnetRoundState.kt        # Immutable state data class
    │   └── OnetEngine.kt            # Functional transition functions
    └── triplematch/
        ├── TripleRoundState.kt      # Immutable stack/tray state data class
        └── TripleEngine.kt          # Tray picking, trio matching, undo
```

### 6.2 Test Suite Parity Matrix (JUnit 5)

| Node.js Test (`tests/logic.test.js`) | Equivalent JUnit 5 Test Case |
|---|---|
| `Onet generation is deterministic, paired, solvable` | `testOnetDeterministicGeneration()` |
| `Onet routes use at most two turns; wrong IDs fail` | `testOnetPathfindingTurnLimitAndObstacles()` |
| `Onet split preserves footprints, siblings, counterparts` | `testOnetProgressiveSplitFootprints()` |
| `Onet matching locks during animation and resolves` | `testOnetAnimationLockAndResolve()` |
| `Onet pause stops timer; timeout is terminal; shuffle retains legal`| `testOnetPauseTimeoutAndShuffle()` |
| `Triple generator preserves 12x3 layout & multiples of 3` | `testTripleStackLayoutAndMultiplesOfThree()` |
| `Triple third tile clears immediately, scores 150, wins` | `testTripleTrioClearAndScore()` |
| `Triple seventh unmatched loses; trio on seventh frees tray first`| `testTripleSeventhSlotTrioPriority()` |
| `Triple undo is single-use and does not refund time` | `testTripleSingleUseUndoAndPause()` |

---

## 7. Reusability Certification

The components documented herein provide an enterprise-grade, deterministic foundation for production puzzle game engines. All algorithms, state contracts, and coordinate systems are certified ready for direct implementation by downstream swarm specialists.

*Certified by AGENT 01 (`@auditor`)*
