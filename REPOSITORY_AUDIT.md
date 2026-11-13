# Repository implementation audit

Initial reviewed baseline: `6720f88` (2026-11-12); resumed integration reviewed through `1093892` (2026-11-13). This replaces the earlier report's unsupported completion claims. Specialist code is now partially integrated on `feature/premium-native`; API presence and unintegrated work are not counted as tested or shipped.

## What exists and works at the baseline

- Kotlin/libGDX `core`, `android`, `desktop`, and `headless` Gradle modules; portrait native Android launcher, four packaged ABIs, existing signing configuration.
- Pure Kotlin Onet pathfinding/board/round engines and Triple Match stack/tray engine, with JavaScript modules retained as behavioral references.
- Screen factories, lifecycle/navigation manager and 17 screen classes. `v1.0.2-debug` added rendering and transition fixes; its visual treatment was a functional prototype. The current baseline contains a new ceramic renderer that still requires integration and visual review.
- Domain classes for XP, Gems ledger, missions, achievements, cosmetics and billing; the presence of these classes does not establish integration, durability, fairness or production purchase verification.
- Existing source artwork and provenance/checksum manifests.

## Material gaps found before the current workstreams

| Area | Baseline evidence/gap | Owner |
| --- | --- | --- |
| Persistence | Runtime profile, services and active rounds live in memory; in-memory save/restore tests do not prove disk durability | Session |
| Progression | Screens award fixed XP; independent mode unlocks, records and replay-aware rewards are not wired | Session/UI |
| Economy | Ledger restoration appends records without validating all invariants; completion claims and storage atomicity require review | Economy/Session |
| Gameplay | Triple mode uses the same 12×3 generation at every level; level-aware solvable generation needs implementation | Gameplay |
| UI | Ceramic assets unused by old renderer; hardcoded coordinates/bitmap typography; new responsive renderer incomplete | UI |
| Purchases | Application instantiates FakeBillingProvider; success must never be simulated in production | Platform/Session |
| Audio | No bundled playable tracks; SoundService silently skips unloaded paths | Platform |
| Rights | Previous report inferred commercial permission from account ownership and generation metadata; no sufficient evidence for existing artwork | Audit/Release |
| Release | Existing signed workflow does not imply licensing clearance, real-device coverage, 60 FPS or billing verification | Lead/QA |

## Repository map

- `src/{onet,match3,shared}`: original pure ES modules, no application UI; `tests/logic.test.js` is the reference regression suite.
- `core/src/main/kotlin/game/gameplay`: pure rule engines. Rendering, animation and persistence belong outside this package.
- `core/.../{economy,missions,achievements,cosmetics,progression,billing}`: domain services and catalogs; audit consumers, not just unit behavior.
- `core/.../screen`: native rendering/input/navigation; contains 17 screens plus compatibility enum aliases.
- `android`: Android launcher, native library extraction, manifest, resources, instrumentation tests.
- `desktop`, `headless`: iteration/test launchers.
- `assets`: original artwork, derived tile art/UI and new licensed font files. Originals are preserved.
- `docs`: provenance, visual kit, implementation contracts, current audit and handoffs.

## Scope and evidence rules

The user's supplied master prompt ends during section 6. Sections referred to but not supplied are not invented. Explicit available currency/monetization restrictions remain binding. Google Play publication is unauthorized. Public APK/AAB publication is also blocked while commercial rights are unresolved. Local debug builds are internal engineering validation, not clearance to redistribute assets.

## Resumed integration status

Gameplay, economy, audio/platform and first session persistence commits are integrated; exact IDs and unresolved checks are in `docs/AGENT_HANDOFFS.md`. The first consolidated run failed at Kotlin compilation (`ConcurrentHashMap` membership in ledger restoration), so it supplies no integrated test pass. A QA review also found semantic save validation must run before backup selection, owned by the resumed session specialist. The UI specialist's interrupted changes remain in its worktree until reviewed and integrated.

The integrated asset scan now includes all native tile PNG conversions and original generated audio; see current inventory JSON and test evidence. Existing source art remains unchanged and commercial rights remain unconfirmed.
