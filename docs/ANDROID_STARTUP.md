# Android startup diagnosis

The initial implementation had two deterministic startup defects:

* BootScreen.update() requested a new 0.3 second fade on every frame after
  0.5 seconds. ScreenManager.setScreen() reset transitionElapsed to zero on
  every request. At normal frame rates the fade never reached its midpoint,
  so LOADING was never shown. LoadingScreen had the same repeated request.
* BootScreen, LoadingScreen and MainMenuScreen had empty draw() methods.
  BaseScreen only cleared the framebuffer. There was no Stage, SpriteBatch,
  font or visible startup UI in those screens.

ScreenManager now allows its active transition to finish before another request.
The regression test reproduces the repeated request from update(). Startup UI
uses a Stage (with its own SpriteBatch), bundled libGDX default BitmapFont, the
screen FitViewport and explicit act()/draw()/dispose(). Loading now checks the
real AssetManager rather than a timer. No external startup textures are required.
The previous asset source directory and native packaging are preserved. No
R8-specific cause was established; the original broad keep rules remain intact.
The ES2 configuration and production signing environment variables are unchanged.

Android debug uses com.onetmatch3.game.debug and debug signing. The production
application remains com.onetmatch3.game. Only the debug source set contains the
native console, FileProvider, crash persistence and native memory diagnostics.
It uses Android views above initializeForView(), without a GL dependency.
The console's 250-entry / 200,000-character buffer includes periodic render,
viewport, asset, lifecycle, GL and device diagnostics. Export shares a text file
through a restricted cache FileProvider. It does not execute logcat or capture
credentials. Audio failures are warnings; fatal game failures are recorded and
rethrown, and the previous crash is displayed on the next debug launch.

An uncaught failure may terminate the process before the UI can paint. The
console cannot recover native crashes, process termination or an unresponsive
UI thread/ANR. Use adb logcat for those cases. Launch debug again to access a
persisted Java crash. A missing render heartbeat opens the native panel after
15 seconds if the UI thread is responsive.

## Runtime verification

The proposed Actions workflow runs core tests, builds debug and signed release,
verifies release signing, checks manifests/assets/native library entries, then
installs both packages on an API 35 x86_64 emulator. It checks a MAIN_MENU render
heartbeat for debug and captures screenshots and Logcat for both variants.
Compilation and headless tests alone do not establish visible rendering.
The screenshots must also be reviewed. Emulator evidence does not replace
Android 16 ARM64 testing on the reported POCO F7 Pro.

On that device install both variants, launch debug, check the native bug button,
wait for MAIN_MENU, test background/resume and rotation handling, then copy/export
logs. Test a cold release launch too. Record any device-specific GL or surface
errors using Logcat.

## Scope limitation found during audit

The repository also has empty draw methods for gameplay and many secondary
screens. Their domain logic is retained. This startup repair does not implement
those missing renderers or claim that all gameplay visuals now work. Menu and
mode selection labels expose the existing hit areas; a full UI implementation
for the remaining screens is separate work. No advertisements were added.
