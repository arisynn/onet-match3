# Implementation and integration roadmap

Current coordinated work starts from `6720f88`. Old roadmap “complete” labels are superseded by this evidence-based sequence.

1. **Contracts and isolated branches — established.** Lead inspected implementation and assigned six actual specialists with exclusive file ownership; see SWARM_PLAN and SWARM_CONTRACTS.
2. **Pure gameplay/domain corrections — integrated, verification pending.** Preserve existing working APIs; add difficulty/solvability proof, validated ledger restore and mission/cosmetic rules. Integrate engine/domain commits first.
3. **Durable session/progression — first implementation integrated; QA follow-up in progress.** Atomic versioned recovery, active-round save, independent unlocks/records, bounded replay EXP and transactional claims. Integrate after domain contracts settle.
4. **Ceramic UI and platform game feel — platform integrated, UI in progress.** Actual art/fonts, responsive hit geometry, screens, animations, audio/haptics/accessibility and honest billing availability. Integrate with session facade; remove fixed UI rewards.
5. **Independent integrated QA — pending lead execution.** Run full Kotlin/JS tests, asset integrity, save/claim interruption regressions, instrumentation on multiple sizes, screenshot review, native ABI inspection and offline checks. Specialist unit results do not substitute for integration evidence.
6. **Public release readiness — blocked.** Resolve commercial-use permissions per hash, verify approved-only packaging, sign/verify actual outputs, physical-device/performance coverage and Play-console/backend purchase configuration. No Play publication without explicit authorization.

The user-supplied text stops mid-sentence in currency-ledger/offline limitations. This roadmap follows the supplied requirements and does not fabricate missing sections or silently weaken explicit restrictions.

## Lead integration update — 2026-11-13

Gameplay, economy, platform audio/haptics, atomic session persistence and all17 native ceramic screens are integrated on `feature/premium-native`. Core/headless tests have been executed successfully (98 core +2headless); Node12 tests and source asset integrity passed. Save semantic fallback and interruption tests are included. UI uses Scene2D touch focus, shared drawing/hit rectangles and Android safe-area offsets. Font weight600 static OFL derivative replaces the variable font's thin defaultweight200.

Android debug/instrumentation APKs have been generated. Native emulator acceptance is in progress: the first attempt was obstructed by an emulator SystemUI ANR, so it is not a pass. Final results belong in TEST_REPORT. Public rights, real Play products/server verification, physical ARM/device performance, production R8/store delivery remain gates; no commercial clearance is claimed.
