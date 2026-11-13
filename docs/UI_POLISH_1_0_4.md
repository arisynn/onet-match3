# Native UI polish — beta 1.0.4

Implemented solo following the owner's request to focus on visuals. Gameplay engines, economy, progression, save schema and billing are unchanged. Android versionCode is 5, versionName is 1.0.4.

## Changes

- Main menu: compact level badge and candy wallet, grouped ceramic tile illustration, direct mode cards, distinct Play/Continue actions and compact bottom navigation.
- Victory/defeat: one vertical result layout for artwork, title, subtitle, stars, score/EXP and actions. Stars and EXP cannot sit beneath Next level. Replay/Home share a separate footer row.
- Shared style: quieter cream background, forest/mint contrast, consistent sans typography, rounded surfaces and clean button shadows. Rounded panels use a tessellated fill with a feathered edge instead of overlapping translucent circles and rectangles.
- Currency: reuse the original blue wrapped candy (`candy_3`) in balances, rewards and prices. Visible currency text is Permen; existing GEM save/ledger identifiers remain compatible.
- Triple: one grouped playfield, responsive stack rows, compact depth badges and an integrated seven-slot tray. Drawn tiles and input hit areas share the same geometry.
- Missions/achievements: responsive pagination and card density, distinct reward/claim/progress rows.
- Collection/shop: responsive two-column cards, consistent preview/name/price positions.
- Profile/settings: grouped records, EXP and candy wallet; five labeled switches with descriptions.

## Native screenshot review

`./gradlew :desktop:previewUi -PpreviewOutput=<directory> -PpreviewWidth=540 -PpreviewHeight=1200`

Runs the actual libGDX renderer with LWJGL3/OpenGL. Isolated temporary saves supply deterministic review fixtures; no player save is modified. Captures 17 screen types and an additional saved-game main menu. Reduced motion is enabled to capture settled states. Android production code does not invoke this launcher.

Reviewed portrait sizes: 540×960, 540×1200, 540×1440 and 720×960. This is desktop native rendering evidence, not Android device input, cutout, audio/haptic or measured FPS acceptance. The prior Android emulator acceptance limitation remains open.

## Regression protection

`ResultLayoutRegressionTest` checks result text/section gaps, saved-game menu actions against navigation, and reward-description clearance above claims at four viewport sizes. Existing responsive tests also check Onet outside-board routing and tile-to-input mapping.

Internal debug distribution retains the existing commercial-asset rights restriction. No original artwork was regenerated or changed in this pass; no production license clearance is implied.
