# Native architecture and integration contracts

Kotlin/libGDX application with `core`, `android`, `desktop`, and `headless` modules. The core module currently contains both presentation and pure-domain packages; package boundaries, not a fictional separate module graph, enforce separation. No WebView, JavaScript runtime, Compose game renderer or web framework is introduced.

## Boundaries

- **Domain:** gameplay rules, XP/records, economy, missions, achievements, inventory. No rendering or Android dependencies in gameplay engines.
- **Session/application:** orchestrates domain services, once-per-round completion, active-round capture, settings and save transactions. Screens call this facade rather than awarding fixed XP/Gems directly.
- **Persistence:** versioned snapshot with validated recovery and atomic replacement. Wallet ledger, reward status, inventory and records are a consistent snapshot; a hash checksum is corruption detection, not protection from an adversary editing an offline device.
- **Presentation:** SpriteBatch-based ceramic rendering and shared hit rectangles, managed textures, font pipeline, transitions and optional visual effects. Input/animation never determines whether a trio counts or a reward was granted.
- **Platform:** Android launcher, haptics, audio lifecycle and purchase-provider boundary. Purchase success is never faked in production; unavailable/unverified flows fail closed.

## Public integration surface

The session specialist owns exact signatures and enum types. The facade integrated in `1093892` includes: `saveNow`, `completeOnet`, `completeTriple`, `captureOnet`, `captureTriple`, `resumedOnet`, `resumedTriple`, `startNewRound`, `unlockedLevel`, `bestScore`, and `settings`. Additional transactional claim/shop/equip methods and records are listed in `AGENT_HANDOFFS.md`. Compilation and runtime verification are recorded separately; API presence is not a passed test.

Integrated gameplay preserves existing JS-compatible overloads, adds `createOnetLevelRound` tutorial levels and a level-aware Triple generator with a winning witness. UI owns `PuzzleLayout` so the same geometry is used for drawing and touch hit-testing on tall displays. Platform and session coordinate reduced-motion, reduced-effects and audio/haptic settings.

## Actual toolchain baseline

Kotlin 2.0.21, libGDX 1.13.1, Android Gradle Plugin 8.7.3, Gradle wrapper 8.14.3. Core/desktop/headless use JDK 21; Android targets JVM 17, compile/target SDK 35, min SDK 24. These are the repository values, not a claim that they are the newest versions. Dependency changes need compatibility/build verification.

## Release boundary

Commercial-use rights gate is independent of source checksum validation. Local debug builds are internal validation. Public release APK/AAB and Google Play publication require separate readiness evidence; Play publication additionally requires explicit authorization.
