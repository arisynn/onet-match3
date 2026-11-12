# Swarm Agent Handoff Protocols & Interface Matrix

This document tracks all cross-agent handoffs, interface contracts, verification requirements, and handoff statuses for the Onet + Triple Match native project.

---

## 1. Interface Contracts

### 1.1 Foundation & Build System Contract
- **Provider**: `@architect` (Agent 02)
- **Consumers**: `@gameplay` (Agent 03/04), `@economy` (Agent 06/07), `@qa` (Agent 09/10)
- **Artifacts**:
  - `settings.gradle.kts`
  - `build.gradle.kts`
  - `core/build.gradle.kts`
  - `android/build.gradle.kts`
  - `desktop/build.gradle.kts`
- **Contract Rules**:
  - `core` module is pure Kotlin (Kotlin 2.0+, libGDX core 1.13.1, JUnit 5).
  - No Android or rendering dependencies inside `core/src/main/kotlin`.
  - Gradle test runner configured to execute JUnit 5 engine: `./gradlew test` or `./gradlew :core:test`.

---

### 1.2 Core Gameplay Engine Contract
- **Provider**: `@gameplay` (Agent 03/04)
- **Consumers**: UI Layer, `@qa` (Agent 09/10)
- **Package Base**: `game.gameplay.onet`, `game.gameplay.triplematch`, `game.shared`
- **Key Signatures & Invariants**:
  - `SeededRandom(seed: Long)`: Deterministic 32-bit PRNG (`SplitMix32` / `Murmur3` parity with JS `src/shared/random.js`).
  - `OnetEngine`:
    - `createRound(theme: String, level: Int, rng: SeededRandom): OnetRoundState`
    - `selectTile(state: OnetRoundState, r: Int, c: Int, timestampMs: Long): OnetRoundState`
    - `resolveMatch(state: OnetRoundState): OnetRoundState`
    - `findPath(board: Board, p1: Point, p2: Point): PathResult?` ($\le 2$ turns / perpendicular bends)
    - `needsShuffle(state: OnetRoundState): Boolean`
    - `shuffleRound(state: OnetRoundState): OnetRoundState`
  - `TripleMatchEngine`:
    - `createRound(tilePool: List<String>, rng: SeededRandom): TripleRoundState`
    - `pickTile(state: TripleRoundState, stackIndex: Int): TripleRoundState`
    - `resolveTrio(tray: List<String>): TrioResult` (Trio clears *before* 7th slot overflow check)
    - `undoPick(state: TripleRoundState): TripleRoundState` (Single-use undo; does not refund timer)
- **Verification Requirement**: 100% test parity with original 9 Node.js test cases in `tests/logic.test.js`.

---

### 1.3 Economy, Progression & Billing Contract
- **Provider**: `@economy` (Agent 06/07)
- **Consumers**: UI Layer, Save/Restore Manager, `@qa` (Agent 09/10)
- **Package Base**: `game.economy`, `game.missions`, `game.achievements`, `game.billing`
- **The 10 Non-Negotiable Currency Rules**:
  1. Gems are earned **only** via explicit player claims on completed Missions or Achievements.
  2. Zero currency awarded directly from gameplay completion (stars/score only).
  3. Zero currency awarded from player level-ups or progression ranks.
  4. Zero real-money in-app currency packs (no "Buy 500 Gems for $1.99").
  5. Cosmetics are direct unlock or direct purchase only (Free, Earnable via Gems, Premium via Billing).
  6. Transaction ledger is append-only and strictly idempotent (duplicate transaction IDs are rejected).
  7. Balances are derived projections of valid ledger events or validated state checkpoints.
  8. Save state modifications must fail cryptographic or checksum validation.
  9. Double-claim attacks on mission/achievement rewards must yield idempotent no-op.
  10. Offline claims must validate client timestamp monotonicity and cryptographic challenge.
- **Verification Requirement**:
  - Monte Carlo simulation validating 7, 30, 90, 180, and 365-day balance trajectories.
  - Zero currency inflation leaks.

---

### 1.4 Asset & Licensing Audit Contract
- **Provider**: `@auditor` (Agent 01)
- **Consumers**: `@architect` (Agent 02), `@qa` (Agent 09/10)
- **Artifacts**:
  - `REPOSITORY_AUDIT.md`
  - `ASSET_INVENTORY.md`
  - `ASSET_LICENSE_REPORT.md`
  - `REUSABLE_COMPONENTS.md`
- **Verification Requirement**:
  - Exact checksum parity on 316 original files.
  - Verification of transparent PNGs and regenerated WebP assets.
  - Cozy Ceramic UI Kit integration catalogue.

---

### 1.5 QA, Verification & Release Contract
- **Provider**: `@qa` (Agent 09/10)
- **Consumers**: `@coordinator`, Release Engineers
- **Artifacts**:
  - `docs/SWARM_PLAN.md` (Active)
  - `docs/AGENT_HANDOFFS.md` (Active)
  - `docs/TEST_REPORT.md` (Active)
  - `docs/RELEASE_CHECKLIST.md` (Active)
  - `core/src/test/kotlin/game/security/SaveRestoreRegressionTest.kt`
  - `core/src/test/kotlin/game/security/RewardDuplicationAttackTest.kt`
  - `core/src/test/kotlin/game/integration/EndToEndGameplayTest.kt`
- **Verification Scope**:
  - Full execution of unit tests, integration tests, and security attack simulations.
  - Validation of Android build parameters and APK release readiness.

---

## 2. Handoff Tracking Matrix

| Handoff ID | From Agent | To Agent | Component / Deliverable | Status | Blocking Dependency | Verification Sign-off |
|---|---|---|---|---|---|---|
| **HO-01** | `@auditor` | `@architect`, `@qa` | Asset inventory & licensing audit | **Verified** | None | Verified in #8 (584 assets, 273 IDs; flag: Beach/Vanilla license) |
| **HO-02** | `@architect` | `@gameplay`, `@economy`, `@qa` | Gradle multi-project scaffold & core test config | **Verified** | None | Verified in #9 (`:core`, `:android`, `:desktop`, `:headless`) |
| **HO-03** | `@gameplay` | `@qa` | Kotlin Onet & Triple Match engines + JUnit parity suite | **Verified** | HO-02 | Verified in #11 & #14 (15/15 tests pass in `LogicTest.kt`, 9/9 Node parity) |
| **HO-04** | `@economy` | `@qa` | Ledger, progression, missions, billing abstraction & Monte Carlo sim | **Verified** | HO-02 | Verified in #13 (28/28 tests pass across rules, missions, achievements, billing, sim) |
| **HO-05** | `@qa` | `@coordinator` | Save/restore & attack vector security tests | **Verified** | HO-03, HO-04 | Authored & Verified (15/15 tests pass in `RewardDuplicationAttackTest`, `SaveRestoreRegressionTest`, `EndToEndGameplayTest`) |
| **HO-06** | `@qa` | `@coordinator` | Full test execution, `TEST_REPORT.md`, & `RELEASE_CHECKLIST.md` | **Verified** | HO-01..HO-05 | Verified (74/74 tests pass 100%, 63 :core, 2 :headless, 9 Node.js; release checklist signed off) |

---

## 3. Protocol for Completing a Handoff

1. Provider runs local tests and commits/writes files.
2. Provider posts to the board tagging the consumer: `@consumer Handoff [HO-ID] ready: [details]`.
3. Consumer acknowledges by running independent verification and updating this document.
4. QA audits all handoffs against the Acceptance Criteria before promoting to Verified.
