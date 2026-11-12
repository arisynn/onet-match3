# Onet + Triple Match: Implementation Roadmap

> [!IMPORTANT]
> This roadmap coordinates the development, integration, and release phases of **Onet + Triple Match** across all swarm agents and components.

---

## 1. Roadmap Summary & Swarm Ownership Matrix

| Phase | Focus Area | Responsible Agents | Primary Deliverables | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Phase 1** | Asset Audit & Provenance | AGENT 01 (`@auditor`) | `REPOSITORY_AUDIT.md`, `ASSET_INVENTORY.md`, `ASSET_LICENSE_REPORT.md`, `REUSABLE_COMPONENTS.md` | ✅ **Completed** |
| **Phase 2** | Architecture & Foundation | AGENT 02 (`@architect`) | Multi-project Kotlin Gradle setup (`:core`, `:android`, `:desktop`, `:headless`), Screen Management, `ARCHITECTURE.md`, `IMPLEMENTATION_ROADMAP.md` | 🚀 **Delivered** |
| **Phase 3** | Gameplay Logic Engine | AGENT 03 & 04 (`@gameplay`) | Pure Kotlin Onet engine, Triple Match engine, SeededRandom, 100% test parity with JS baseline | 🔄 **In Integration** |
| **Phase 4** | Economy & Progression | AGENT 06 & 07 (`@economy`) | 10 Currency Rules, Idempotent Transaction Ledger, Missions, Achievements, Monte Carlo Simulation (7–365d), Billing abstraction | 🔄 **In Integration** |
| **Phase 5** | libGDX UI & Presentation | Swarm Integration | Cozy Ceramic UI Kit layout, Stage2D widgets, animations, particle effects, SoundService integration | 📅 **Next Phase** |
| **Phase 6** | QA, Testing & Security | AGENT 09 & 10 (`@qa`) | `TEST_REPORT.md`, `RELEASE_CHECKLIST.md`, double-claim attack tests, regression verification, Android APK build | 🔄 **In Verification** |
| **Phase 7** | Release & Deployment | Coordinator & QA | Target SDK 34/35 compliance, Release signing, ProGuard optimization, Google Play bundle generation | 📅 **Final Release** |

---

## 2. Detailed Phase Breakdown

### Phase 1: Repository Audit & Asset Verification
- **Objectives**: Inspect all raw assets, verify cryptographic checksums, audit commercial licensing rights, and catalog the Cozy Ceramic UI Kit.
- **Key Milestones**:
  - Audit 584 assets (316 original, 114 transparent derivatives, 9 master atlases, 122 ceramic UI kit elements).
  - Verify 100% SHA-256 integrity and validate 273 tile identifiers.
  - Flag unlicensed Beach/Vanilla assets and formulate Kenney/CC0 replacement recommendation.
- **Sign-off**: Delivered by `@auditor` in message #8.

### Phase 2: Architecture & Android Foundation
- **Objectives**: Establish multi-module Gradle build using Kotlin DSL, build launchers for Android, Desktop, and Headless test environments, and implement screen navigation.
- **Key Milestones**:
  - Configure root `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, and Gradle wrapper.
  - Establish `:core` module targeting JVM 17 with libGDX 1.13.1 and Kotlin 2.0+.
  - Establish `:android` module targeting Android API 34/35 (`compileSdk = 35`, `targetSdk = 35`, `minSdk = 24`) with native JNI `.so` packaging.
  - Establish `:desktop` module with LWJGL3 backend (540x960 portrait emulation) and `:headless` module for CI/CD test automation.
  - Implement `ScreenManager`, `BaseScreen`, `ScreenTransition`, and `OnetMatch3Game` bootstrap.
  - Implement `AssetService`, `TileAssetResolver`, and `SoundService`.
  - Author `docs/ARCHITECTURE.md` and `docs/IMPLEMENTATION_ROADMAP.md`.
- **Sign-off**: Delivered by `@architect`.

### Phase 3: Core Domain Gameplay Porting
- **Objectives**: Port pure JavaScript game logic from `src/onet/`, `src/match3/`, and `src/shared/` to pure Kotlin domain engines under `core/src/main/kotlin/game/gameplay/`.
- **Key Milestones**:
  - Implement MurmurHash3 / SplitMix32 `SeededRandom`.
  - Implement `OnetEngine`: 10x6 board, $\le 2$-turn pathfinding, split groups, openSide checks, hint calculation, and automatic shuffle.
  - Implement `TripleMatchEngine`: 12x3 stack generator, 7-slot tray, trio clear resolution prior to tray-overflow checks, single-use undo.
  - Port all 9 original Node.js test cases to JUnit 5 tests + add edge-case and boundary tests.
- **Sign-off**: Delivered by `@gameplay`.

### Phase 4: Economy, Progression, Missions & Billing
- **Objectives**: Implement player progression, economy, and monetization domain layers in strict adherence to the 10 Non-Negotiable Currency Rules.
- **Key Milestones**:
  - Implement `Wallet`, `Transaction`, and append-only `TransactionLedger` with idempotency safeguards.
  - Implement `ProgressionEngine`: Player profile, XP curves, level scaling, and zero-gem level-up guarantees.
  - Implement `MissionRotationEngine` and `AchievementCatalog`: Daily/weekly rollover, tier tracking, and exclusive gem earning mechanism.
  - Implement `CosmeticCatalog` and `CosmeticInventory` (Free, Earnable, Premium).
  - Implement `BillingService` and `Entitlement` abstraction for non-consumable in-app purchases.
  - Run 7, 30, 90, 180, and 365-day Monte Carlo economic simulations to verify balance and prevent inflation.
- **Sign-off**: Delivered by `@economy`.

### Phase 5: Presentation Layer & UI Integration
- **Objectives**: Assemble the user-facing game experience in libGDX using the Cozy Ceramic UI Kit.
- **Key Milestones**:
  - Build `MainMenuScreen`: Play mode cards, player header (Coins, Gems, Level), and navigation footer.
  - Build `OnetGameScreen`: 10x6 ceramic tile rendering, line drawing for 1/2-turn connection routes, combo counter, timer bar, hint and shuffle power-up buttons.
  - Build `TripleMatchGameScreen`: 12-stack 3D layered rendering, 7-slot ceramic tray, tile selection animation, trio burst effects, undo button.
  - Build `RoundResultScreen`: Win/defeat banner, coin earnings tally, combo rating, retry and next buttons.
  - Build `SettingsScreen` & `ShopCosmeticsScreen`: Audio sliders, haptics toggle, skin selector.
  - Integrate sound effects and background music playback via `SoundService`.

### Phase 6: QA, Security & Build Verification
- **Objectives**: Execute full regression, fuzzing, attack resistance, and cross-platform verification.
- **Key Milestones**:
  - Execute 100% of unit tests (`:core:test`).
  - Execute headless screen lifecycle tests (`:headless:test`).
  - Perform reward duplication and replay attack tests against `TransactionLedger`.
  - Verify Android debug build (`:android:assembleDebug`).
  - Validate release checklist and sign off on test matrix in `docs/TEST_REPORT.md`.
- **Sign-off**: Delivered by `@qa`.

### Phase 7: Release Readiness & Play Store Compliance
- **Objectives**: Finalize production build artifacts for Google Play deployment.
- **Key Milestones**:
  - Verify Target SDK 34/35 compliance.
  - Configure ProGuard obfuscation rules (`android/proguard-rules.pro`).
  - Generate release Android App Bundle (AAB).
  - Prepare store listing metadata, screenshots, and privacy policy declaration.
