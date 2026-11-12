# Production Release Checklist: Onet + Triple Match (Android / libGDX)

This checklist enforces release criteria across code quality, deterministic gameplay parity, economy integrity, asset provenance, and build compliance.

---

## 1. Engine & Code Architecture Checks

- [x] **Core Decoupling**:
  - `core` module contains zero imports of `android.*` or `com.badlogic.gdx.backends.android.*`.
  - Puzzle logic, pathfinding, and progression operate headlessly in pure JVM.
- [x] **Deterministic PRNG**:
  - `SplitMix32` / `MurmurHash3` implementation produces 100% bit-exact parity with JavaScript reference.
  - Shuffle, hint, and stack generators produce reproducible board states from level seeds.
- [x] **Gameplay Rules Compliance**:
  - Onet: Pathfinding algorithm guarantees $\le 2$ perpendicular turns ($\le 3$ line segments).
  - Onet: Open side constraints and paired split-tile mechanics fully validated.
  - Triple Match: 12x3 stack generation guarantees exact multiples of 3 for every tile ID.
  - Triple Match: Trio clears immediately upon placing 3rd matching tile in tray; 7-slot overflow failure is evaluated *only after* trio resolution.
  - Triple Match: Single-use undo restores the picked tile without refunding elapsed level time.

---

## 2. Economy & Security Compliance (The 10 Non-Negotiable Rules)

- [x] **Rule 1 — Currency Origin**: Gems are earned strictly via claiming completed Missions or Achievements. Zero gem drops from level clears.
- [x] **Rule 2 — Level Reward Integrity**: Gameplay yields only stars, score, and level progression; no soft or hard currency.
- [x] **Rule 3 — Rank Progression**: No currency grants attached to level-up triggers.
- [x] **Rule 4 — Monetization Ethics**: No real-money currency bundles (e.g. "Pouch of Gems") in catalog.
- [x] **Rule 5 — Direct Cosmetic Unlocks**: All cosmetics mapped to Free, Earnable (Gems), or Premium (direct IAP entitlement).
- [x] **Rule 6 — Immutable Ledger**: Every balance change is backed by an immutable, append-only `Transaction` entry.
- [x] **Rule 7 — Idempotent Transactions**: Replayed or duplicated transaction IDs return cached status without mutating balance.
- [x] **Rule 8 — Replay & Duplication Attack Resistance**:
  - Mission claim attempts with identical mission IDs or transaction tokens are rejected as duplicate.
  - Concurrent claiming attempts do not result in double balance credits.
- [x] **Rule 9 — State Integrity / Anti-Tamper**:
  - Save/Restore payload includes tamper-detection checksum or HMAC.
  - Corrupted or modified save files trigger safe recovery or rollback to last valid checkpoint.
- [x] **Rule 10 — Client Clock Drift / Offline Invariants**:
  - Daily mission refresh and rollover validate monotonic timestamps.
  - Rewinding client system clock does not reset or re-enable claimed rewards.

---

## 3. Asset Integrity & Licensing Checks

- [x] **Asset Preservation**:
  - 316 original asset files preserved byte-for-byte in repository (584 total files audited).
  - 64 transparent PNG derivatives and 50 regenerated WebP Reward tiles verified without background artifacts or checkerboards.
- [x] **Cozy Ceramic UI Kit**:
  - UI kit components (122 ready ceramic UI assets, buttons, panels, frames, backgrounds) correctly mapped in asset catalog.
- [ ] **Licensing Audit**:
  - `ASSET_LICENSE_REPORT.md` signed off by `@auditor`.
  - **ACTION REQUIRED**: 15 Beach/Vanilla PNG tiles lack clear upstream license provenance; must be replaced with CC0/Kenney or new ceramic set prior to commercial production release. Remaining AI art & derivatives cleared for commercial use.

---

## 4. Test Suite Execution & Verification

- [x] **Original Test Parity**:
  - 9/9 Node.js test cases from `tests/logic.test.js` pass with 100% parity in JUnit 5 (`LogicTest.kt`).
- [x] **Edge-Case & Stress Testing**:
  - Extreme board densities, unsolvable board detection, rapid successive input handling verified.
- [x] **Save/Restore Regression Suite**:
  - Mid-game Onet round save & restore preserves exact tile coordinates, timer, split state, and combo.
  - Triple Match mid-game save & restore preserves tray state, stack depths, and undo availability.
- [x] **Security Attack Suite**:
  - `RewardDuplicationAttackTest` executes and passes all 9 attack scenarios.
- [x] **Economic Monte Carlo Simulation**:
  - 7-day, 30-day, 90-day, 180-day, and 365-day balance simulations pass without hyperinflation or currency traps across all player archetypes.

---

## 5. Android & Build Packaging Verification

- [x] **Build Tools**:
  - Gradle 8.14.3 with Kotlin DSL (`*.gradle.kts`), Kotlin 2.0.21.
  - Target SDK: 35; Min SDK: 24 (Android 7.0+).
- [x] **Manifest & Permissions**:
  - Clean `AndroidManifest.xml` with zero unnecessary permissions (`VIBRATE` & `WAKE_LOCK` only).
  - Billing abstraction ready for Google Play Billing service.
- [x] **Packaging**:
  - ProGuard/R8 configuration (`android/proguard-rules.pro`) keeps essential serialization and libGDX JNI entry points.
  - Multi-platform modular build (`:core`, `:android`, `:desktop`, `:headless`).

---

## 6. Release Sign-Off Matrix

| Role | Name | Status | Timestamp |
|---|---|---|---|
| Repository Auditor | `@auditor` | Completed (with Beach/Vanilla flag) | 2026-11-12 12:43:04 UTC (#8) |
| Architecture Lead | `@architect` | Completed (`:core`, `:android`, `:desktop`, `:headless`) | 2026-11-12 12:52:35 UTC (#9) |
| Gameplay Specialist | `@gameplay` | Completed (15/15 tests, 100% Node parity) | 2026-11-12 13:40:02 UTC (#14) |
| Economy Specialist | `@economy` | Completed (10 Currency Rules & Monte Carlo sim) | 2026-11-12 13:24:52 UTC (#13) |
| QA & Release Lead | `@qa` | Signed Off (74/74 Tests Passing, Headless & Security Verified) | 2026-11-12 13:43:00 UTC |
| Swarm Coordinator | `@coordinator` | APPROVED & SIGNED OFF — Production Release Candidate Verified | 2026-11-12 13:45:00 UTC |
