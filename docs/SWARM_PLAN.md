# Swarm Plan: Onet + Triple Match Native Kotlin/libGDX Game

## 1. Mission Statement & Scope
Transform the extracted clean assets, Cozy Ceramic UI kit, and deterministic puzzle mechanics from `arisynn/connectify-new` into an enterprise-grade, commercial-ready Native Android puzzle game using Kotlin 2.0+ and libGDX 1.13.1. The game features two distinct primary game modes:
1. **Onet Connect**: 10x6 board, pathfinding with $\le 2$ turns (perpendicular segments $\le 3$), split tile pairing, open-side constraints, hint, shuffle, and combo timers.
2. **Triple Match**: 12x3 stack generation, 7-slot tray, immediate trio matching, overflow failure condition, and single-use undo.

The ecosystem strictly implements ethical monetization under the **10 Non-Negotiable Currency Rules**, utilizing an immutable append-only transaction ledger and offline-first deterministic architecture.

---

## 2. Swarm Roster & Domain Ownership

| Agent ID | Role Name | Agent Handle | Key Deliverables & Code Ownership |
|---|---|---|---|
| **00** | Swarm Coordinator | `@coordinator` | Swarm dispatch, high-level alignment, milestone approvals |
| **Monitor** | Stall Monitor | `@monitor` | Stall detection, deadlock prevention |
| **01** | Asset & Repository Auditor | `@auditor` | `REPOSITORY_AUDIT.md`, `ASSET_INVENTORY.md`, `ASSET_LICENSE_REPORT.md`, `REUSABLE_COMPONENTS.md` |
| **02** | Kotlin Architecture & Android Foundation | `@architect` | Gradle Kotlin DSL (`core`, `android`, `desktop`/`headless`), libGDX bootstrap, `docs/ARCHITECTURE.md`, `docs/IMPLEMENTATION_ROADMAP.md` |
| **03 & 04** | Onet & Triple Match Gameplay Engine | `@gameplay` | `core/src/main/kotlin/game/gameplay/onet/`, `triplematch/`, `shared/`, Port from JS to pure Kotlin, 9 Node test parity in JUnit |
| **06 & 07** | Economy, Progression & Billing Specialist | `@economy` | `docs/ECONOMY_DESIGN.md`, `docs/GAME_DESIGN.md`, `core/.../economy/`, `progression/`, `missions/`, `achievements/`, `billing/`, 10 Currency Rules, Monte Carlo simulation |
| **09 & 10** | QA, Integration, Build & Release Specialist | `@qa` | `docs/SWARM_PLAN.md`, `docs/AGENT_HANDOFFS.md`, `docs/TEST_REPORT.md`, `docs/RELEASE_CHECKLIST.md`, test execution, save/restore regression, attack mitigation tests |

---

## 3. Execution Phases & Milestones

```mermaid
flowchart TD
    subgraph Phase 1: Foundation & Audit
        A1["Agent 01: Audit Assets & Repo"]
        A2["Agent 02: Kotlin Gradle Multi-Project Scaffold"]
        QA_P1["Agent 09/10: Swarm Plan & Handoff Matrix"]
    end

    subgraph Phase 2: Domain Logic & Economy
        A3["Agent 03/04: Onet & Triple Match Kotlin Engines"]
        A4["Agent 06/07: Immutable Ledger & Economy Sim"]
    end

    subgraph Phase 3: Integration & Security Verification
        QA_TEST["Agent 09/10: JUnit Parity, Edge-Case & Attack Tests"]
        SAVE_RESTORE["Save/Restore Serialization & Cryptographic Verification"]
    end

    subgraph Phase 4: UI, Lifecycles & Headless Harness
        A2_UI["Agent 02 / Integration: libGDX Screens & Asset Pipeline"]
    end

    subgraph Phase 5: Verification & Release Sign-Off
        QA_REPORT["Agent 09/10: Test Execution & TEST_REPORT.md"]
        QA_REL["Agent 09/10: Android Build Verification & RELEASE_CHECKLIST.md"]
    end

    Phase 1 --> Phase 2
    Phase 2 --> Phase 3
    Phase 3 --> Phase 4
    Phase 4 --> Phase 5
```

### Milestone Schedule
- **M1: Foundation Ready**
  - Asset inventory, transparency verification, and commercial licensing reports completed.
  - Multi-project Kotlin Gradle (`core`, `desktop`/`headless`, `android`) building cleanly with JVM 21.
- **M2: Gameplay & Economy Domain Complete**
  - Pure Kotlin Onet engine matching JS behavior with seed-reproducible RNG (`SplitMix32` / `MurmurHash3`).
  - Pure Kotlin Triple Match engine with tray resolution and trio clearing.
  - 100% parity across all 9 original Node.js logic tests in Kotlin JUnit 5.
  - Immutable transaction ledger, mission rotation, achievement tracking, and Monte Carlo simulation (7 to 365 days).
- **M3: Security Hardening & Integration Test Suite**
  - Save/Restore state regression test suite (validating zero state desync).
  - Reward duplication attack test suite (replay attack, concurrent claiming, clock manipulation).
  - Idempotent transaction integrity verification.
- **M4: Build & Release Readiness**
  - Headless test execution passing 100%.
  - Gradle task verification (`core:test`, `desktop:run` / headless).
  - `TEST_REPORT.md` and `RELEASE_CHECKLIST.md` finalized with zero critical defects.

---

## 4. Quality Gates & Acceptance Criteria

1. **Gate 1: Zero External Runtime Dependency in Core**
   - `core` module must have zero dependencies on Android SDK or rendering subsystem.
   - Core puzzle logic, pathfinding, RNG, and economy ledger must run in headless JVM unit tests.
2. **Gate 2: Deterministic Parity**
   - Onet pathfinding and shuffle algorithms must produce identical results given identical seed inputs.
   - All 9 test cases from `tests/logic.test.js` ported and passing under JUnit 5.
3. **Gate 3: Strict Currency & Monetization Invariants**
   - Invariant: Gems only obtained via mission/achievement claims.
   - Invariant: Zero gems awarded directly from board completion.
   - Invariant: Zero real-money currency packs (only direct non-consumable cosmetic entitlements).
   - Invariant: Double-claim requests must return idempotent cached result without ledger mutation.
4. **Gate 4: CI / Build Cleanliness**
   - Build compiles with 0 errors and zero uncontrolled deprecations.
   - All test suites execute deterministically in under 30 seconds.

---

## 5. Communications & Board Protocol

- All peer handoffs must explicitly tag the receiving agent (`@architect`, `@gameplay`, `@economy`, `@qa`).
- Tag `@coordinator` upon milestone completion, blocker escalation, or final sign-off.
- Progress updates are tracked across `docs/AGENT_HANDOFFS.md` and recorded in `docs/TEST_REPORT.md`.
