# Android startup audit & production updates — 1.0.3

## Version 1.0.3 Updates

- **Companion PNG Tile Pipeline**: Generated companion PNG textures for all 194 WebP tiles across premium and reward themes while preserving all original WebP assets. Updated `ScreenUi.kt` to resolve companion PNGs dynamically, enabling native texture rendering across all themes.
- **Cross-Platform Persistence Layer**: Implemented `game.persistence.GameStorage` utilizing libGDX `Preferences` (Android `SharedPreferences`). Persists player profile, level, lifetime stats, the complete cryptographically-chained SHA-256 transaction ledger, unlocked cosmetics, equipped style, selected level, and audio mute/volume settings on app pause, round win, and mission/achievement claim.
- **Bundled Tactile Audio Assets**: Synthesized 9 tactile sound effects and cozy background music tracks in both `.wav` and `.ogg` formats (`assets/audio/`: `pick`, `click`, `match`, `invalid`, `undo`, `claim`, `win`, `lose`, `bgm`). Integrated with `SoundService` with fallback path normalization and volume controls.
- **Expanded Cosmetic Collection & Shop UI**: Enhanced `CosmeticCollectionScreen` and `CosmeticShopScreen` in `DefaultScreens.kt` to allow browsing, equipping owned themes (Candy, Ocean, Forest, Space, Beach, Bakery, Fruit, Galaxy, Meadow, Sakura), and purchasing earnable reward themes with gems earned through missions and milestone achievements.
- **Version Bump**: Updated Android `versionCode` to 4, `versionName` to `1.0.3`.

## Validation

- **Kotlin Unit & Security Suites**: 68 tests in `:core` passed 100% (including `GameStorageTest`, `CurrencyRulesTest`, `SaveRestoreRegressionTest`, `LogicTest`, etc.).
- **Headless Application Tests**: 2 tests in `:headless` passed 100%.
- **Node.js Baseline Tests**: 9 reference tests passed 100%.
- **Zero WebView / Zero Hybrid**: 100% native Kotlin + libGDX codebase.
