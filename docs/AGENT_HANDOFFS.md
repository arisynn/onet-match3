# Actual specialist handoffs and integration evidence

This replaces earlier handoff tables that marked work “Verified” without retained execution evidence. Interfaces and ownership are defined in `SWARM_CONTRACTS.md`; active/resumed agents are described in `SWARM_PLAN.md`.

| Workstream | Delivered commit | Integrated commit | Functionality | Checks actually reported | Outstanding integration |
| --- | --- | --- | --- | --- | --- |
| Gameplay | `1a1669a` | `167bf0c` | Compatible original APIs; Onet tutorial 1–3; difficulty-aware certified Triple campaign generator; clock/restore/timer edge fixes | 21 focused tests passed before final two tests were added; 96 level/seed witnesses completed with tray occupancy at most 2 | Run all final 23 gameplay tests in integrated suite; difficulty still needs playtesting |
| Economy | `8551cbc` | `2f785d3` | Validated ledger restore, safe arithmetic, mission budgets/clock guard, catalog prices and inventory validation, event-driven simulator | `git diff --check`; new Kotlin tests not executed by specialist | Integrated compilation exposed Kotlin ConcurrentHashMap membership error; lead fixing before rerun; no new simulation result claimed |
| Platform/audio/billing | `22ee6ed` | `bf68bac` | 13 original PCM WAVs, audio/haptic abstraction and lifecycle, unavailable billing default; fake provider confined to tests; verified provider boundary | Python PCM/header/frame/hash/peak verification passed for 13 files; 5 new Kotlin tests not yet executed | Actual device audio/haptic and purchase configuration remain unverified; no real Google Play BillingClient integration |
| Session/progression | `2905104` | `1093892` | Versioned atomic save snapshots, active rounds, per-mode records/unlocks, replay-aware XP, transactional claim facade | 5 new Kotlin tests added, not executed before handoff | QA found semantic validation could fail after backup selection; `session_fix` owns correction and interruption tests |
| UI | Work in progress in isolated worktree | Not integrated at this audit | Ceramic renderer/typography/assets baseline; revised screens pending | No new integrated screenshot/input result | Prior agent hit usage limit; resumed `ui_finish` continues its preserved changes |
| Audit | This handoff commit | Pending lead cherry-pick | Evidence-based asset rights inventory, fail-closed public gate, gate tests, corrected architecture/roadmap/release docs | See `TEST_REPORT.md` and raw `qa-evidence/` | Lead must rerun gate against final bundled tree and append final build/test evidence |

## Session facade and invariants

The integrated application exposes `saveNow`, `completeOnet`, `completeTriple`, `captureOnet`, `captureTriple`, `resumedOnet`, `resumedTriple`, `startNewRound`, `unlockedLevel`, `bestScore`, `levelRecord`, `claimMission`, `claimAchievement`, `buyCosmetic`, `equipCosmetic`, and mutable settings through `GameSettings`. Gameplay mode arguments use `ScreenType.ONET_GAME` / `TRIPLE_MATCH_GAME`.

Screens must call these application operations rather than directly award Gems, arbitrary XP or unlock premium ownership. Durable claims must save reward status, ledger, inventory and records together and restore the previous in-memory state on storage failure. Every restored domain must be validated before the primary/backup candidate is accepted.

Gameplay remains independent from libGDX/Android and animation timing. The `core` module also contains rendering and platform abstractions; it is incorrect to claim the entire module has no graphics dependencies. UI action rectangles and projection must be shared with instrumentation input tests.

## Acceptance protocol

A handoff means code is available for review, not that a production feature passed. The lead cherry-picks sequentially, records compilation/test results and visual evidence, and fixes failures. The 79-test historical release result does not certify this implementation. Missing commercial rights or configured Play verification cannot be waived by a unit test or a signed artifact.

## Final integrated handoff to owner device testing

Lead integrated the six specialist streams and resumedUI/session/audit agents after the interrupted execution. Branch `feature/premium-native` contains actual17-screen ceramicUI, transactional versioned local state, independent campaigns, records/replay-awareXP, validated Gems claims/purchases, originalaudio/haptics, accessibility settings and unavailable-by-default real-money billing. Font generation is app-owned and reused between screens. Nativefixture review and input smoke harnesses are included. Latest executed results:98core +2headless pass,12Node pass; emulator input acceptance incomplete/failed as recorded in TEST_REPORT. Production licensing and Play verification gates remain closed. Owner explicitly requested APK handoff for testing rather than more emulator retries.
