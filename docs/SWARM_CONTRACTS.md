# Implementation contracts — current execution

Baseline: native Kotlin/libGDX; preserve working engine API and original assets. Existing 79-test result applies only to v1.0.2-debug, not new work.

Ownership and integration order:
1. UI: screen package only; renderer, responsive layout, all 17 screens, animations and native input. Call session facade after handoff; never grant Gems or hardcode XP in screens.
2. Persistence/runtime: new persistence/session packages, OnetMatch3Game, progression package. Own atomic versioned snapshot, restore, records, independent mode unlocks, balanced XP and settings. Expose saveNow(), completeOnet(state,hints,shuffles), completeTriple(state), capture/resume APIs; coordinate exact signatures before UI integration.
3. Economy: economy/missions/achievements/cosmetics packages and tests. Preserve public APIs where possible; validate restore, atomic snapshot-friendly reward claims, monotonic mission cadence, catalog-validated purchases. Billing owned by platform specialist.
4. Gameplay: gameplay packages and tests. Preserve JS semantics and existing overloads; add level-aware solvable Triple generation and documented witness tests. Never touch UI/economy.
5. Platform/game feel: assets/SoundService, new platform abstractions, billing package, Android launcher/platform files. No Gradle modifications without lead integration. Never fake purchase success; use unavailable provider by default. Audio must have explicit provenance.
6. Audit/QA: audit inventory/license docs, QA integration tests and release readiness. No claims of legal clearance without evidence. Public release is gated on confirmed rights; debug is internal validation.
Lead owns Gradle, manifests, integration, git operations, build/screenshot verification, final docs consistency.

Every specialist uses an isolated worktree and branch, commits changes, and reports modified files, executed tests/results, dependencies, limitations and integration instructions. Lead cherry-picks commits sequentially. No force push, destructive reset, deletion of originals, or public release while rights remain unresolved.
