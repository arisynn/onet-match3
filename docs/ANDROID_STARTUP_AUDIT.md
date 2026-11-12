# Android startup audit — 1.0.2

## Confirmed causes

- Every `draw()` implementation in `DefaultScreens.kt` was empty, including boot, loading, main menu and both games. The APK could advance normally while displaying only dark clear colors. This is directly visible in the source and was not covered by existing lifecycle tests.
- Android and desktop package the contents of `assets/` as the internal asset root, but the native tile resolver returned `assets/tiles/...`, adding an extra directory. Native paths now use `tiles/...`; the original JavaScript repository-relative API is unchanged.
- Animated modal navigation ignored `disposePrevious = false` and disposed the game retained on the stack. Pop also constructed every registered screen to guess its type. Stack entries now retain their exact type and instance.
- Outgoing screens updated during transitions, allowing repeated boot transitions, repeated round completion/XP, and input that could mutate navigation mid-transition. A transition now blocks input and renders without advancing game logic.
- Pause set the round flag but resume never cleared it. Replay/restart from Triple Match always launched Onet.

## Changes

- Added a real libGDX renderer with readable text, visible buttons, PNG tile art, selection/hint highlighting, Onet paths/directional markers, Triple Match stacks/tray, loading feedback, and result screens. Screen resources are allocated only with GL and disposed with their owner.
- Preserved paused screen instances through nested animated modals, restored gameplay on resume, kept retries in the selected mode, and fixed fade overlay projection and viewport.
- Replaced unvalidated placeholder reward credits with completion-checked service calls. Disabled the placeholder shop and fake purchase path; no real billing is configured. Free Candy/Ocean selection no longer changes to an unowned WebP theme.
- Moved generated native libraries into `android/build/`, ignored Kotlin build state, and increased Android version to `1.0.2` / code `3`.
- Added navigation regression tests and an Android instrumentation test that checks actual screenshot content, asset packaging, menu navigation, Onet/Triple rendering, nested pause/settings/resume, Triple restart, and background/foreground.
- Fixed the headless test so assertions on the application thread propagate to the test runner.

## Validation

Locally: 66 core Kotlin tests and 2 headless tests passed; 9 JavaScript tests passed. Asset integrity verified 316 originals, 114 transparent derivatives, 9 atlases, 273 tile IDs, 122 UI assets and 4 animations. Debug APK and instrumentation APK build successfully with JDK 21 and Android SDK 35.

The included example `docs/android-smoke-workflow.yml` can retain screenshots and reports as `android-startup-smoke-evidence` when installed. The supplied token lacks workflow write permission, so the active build workflow is unchanged. Consult the release notes for device test results for the published build; compiling and headless tests alone do not establish that an Android surface rendered correctly.

## Remaining scope

This is a playable native prototype. Player state, missions, wallet and settings still live in memory and reset after process death. Audio assets are not bundled. Real purchases are disabled. Premium WebP art needs a supported texture conversion/loading pipeline before those themes can be offered in gameplay. The release is not a claim of a complete store-ready economy or device compatibility audit.
