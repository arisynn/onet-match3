# Executed test evidence

## Audit execution resumed 2026-11-13

Executed in `/workspace/onet-audit` on `workstream/audit`, audit worktree based on `6720f88`, with asset inspection against the integrated `/workspace/onet-match3` tree `1093892`:

| Check | Actual result |
| --- | --- |
| `npm test` | 12 tests passed, 0 failed/skipped: 9 original gameplay reference tests and 3 new public-rights gate tests |
| `npm run check:assets` | Passed: 316 originals, 114 derivatives, 9 atlases, 273 IDs, 122 UI files, 4 animations checked against manifests |
| Pillow full image/frame decode | 802 image files / 830 frames decoded in integrated tree; zero failures |
| Native transcoding/hash comparison | Passed for all 219 UI/tile PNGs: exact source/output RGBA pixels and manifest hashes |
| Original audio verification | Passed for 13 PCM files: manifest hashes, frame count, sample rate, channels and 16-bit WAV format |
| SHA-256 duplicate scan | Zero exact duplicate groups among 824 asset files |
| `node scripts/check-commercial-rights.mjs` | **Expected failure (exit 1):** 824 bundled files checked, 19 confirmed font/audio/evidence files, 805 without commercial-use evidence |

The gate's expected rejection is a successful negative enforcement check, not a public release pass. Tests prove matching evidence is accepted and unknown/new/modified assets or missing/modified license evidence is rejected. CLI fails closed on malformed/unreadable manifests as well.

Raw Node/asset outputs and the rights check output are retained under `docs/qa-evidence/`. Asset decode inventory is in `docs/asset-inventory-runtime.json`.

## Earlier release evidence

The historical `v1.0.2-debug` work reported 67 core tests, 2 headless, 9 JS and 1 Android instrumentation test, with seven screenshots on Android API 29 x86_64 software rendering. That result belongs to the earlier renderer/application and is **not** asserted for the new integrated implementation.

## Pending integrated evidence

Lead must append actual specialist commit IDs and executed results after cherry-picking: full Kotlin suites, session/process-death/claim durability, new generator witness sweeps, billing pending/cancel/unavailable flows, audio/platform behavior, screenshot/input checks on 540×960 and 576×1280 plus cutout/safe-area cases. Physical ARM devices, release R8 execution, measured 60 FPS/low-end memory, real Play purchase/restore and public commercial rights are not passed by this audit.

## First consolidated Kotlin run — failed, not a pass

Observed `/tmp/premium-integrated-tests.log` on the integrated tree: `:core:compileKotlin FAILED` because ledger restore used Kotlin membership against `ConcurrentHashMap` (`TransactionLedger.kt:28`), which requires explicit `containsKey`. This result was reported to the lead for correction. Tests were not reached; no test counts or simulation numbers are attributed to this failed run. The lead must append the subsequent executed result after fixing and integrating session/UI follow-ups.

## Lead integration result — 2026-11-13

The integrated implementation built successfully with JDK21/AndroidSDK35. Latest full Gradle invocation passed98core tests and2headless tests with0failures/errors/skips, and generated both debugAPK and instrumentationAPK. The new shared font atlas lifetime is included in that build. Node12tests and original asset integrity checks passed. Actual25-cohort economy simulation output is retained in `qa-evidence/economy-simulation-integrated.md`.

Native end-to-end input acceptance is **not passed**: software-emulator attempts were interrupted by a SystemUI ANR overlay, subsequent ADB reconnection/install instability, and MAIN_MENU/MODE_SELECT deadlines. This is not counted as an application test pass. A separate17-screen native framebuffer-review test is included but has not been executed successfully. User requested stopping emulator work and supplying the APK for device testing. Screenshots from interrupted attempts are diagnostic only, not clean acceptance evidence.

The local asset policy now lists825files:20font/audio/evidence files confirmed and805artwork files unresolved. Both Node gate and actual Gradle verification task failed closed as expected. No public productionAPK/AAB is cleared. ARMphysical hardware, cutouts/gesture-navigation and60FPS/device-memory targets still require device testing. UIgeometry tests cover four aspect ratios, but those are not physical-device tests.


## Solo UI beta 1.0.4 — 2026-11-13

Owner limited this pass to visuals and requested no swarm. No gameplay/economy/save-domain files changed.

- Executed Gradle `:core:test :headless:test :android:assembleDebug`: **101 core + 2 headless tests passed**, zero failures, errors or skipped tests. Three new layout regressions cover result-section overlap, menu actions/navigation and mission claim-row clearance at four sizes.
- Executed `:desktop:previewUi` at **540×960, 540×1200, 540×1440 and 720×960** using the actual native libGDX/OpenGL renderer with Xorg dummy display and software GL. **72 PNGs generated**: 17 screen types plus a saved-game menu at each size. Reviewed result, gameplay, main menu, shop, missions, profile and settings compositions. This supersedes the earlier unexecuted desktop screenshot-review status; it does not pass Android input/lifecycle acceptance.
- Android debug APK generated with package `com.onetmatch3.game`, versionName `1.0.4`, versionCode `5`, minSDK24/targetSDK35 and arm64-v8a/armeabi-v7a/x86/x86_64 libraries. `apksigner verify` passed APK signature v2 and ZIP integrity passed.
- Existing JS/gameplay and original artwork files were not modified. No new Node result, Android device result, FPS target, commercial licensing clearance or Play purchase acceptance is asserted for this pass.

Reproduction and UI scope: `UI_POLISH_1_0_4.md`. Native captures and Gradle test/build logs accompany the internal testing artifact.
