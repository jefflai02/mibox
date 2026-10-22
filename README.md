# Minimal TV — Xiaomi / Android TV launcher

A small, ad-free, Java-native launcher. No network access, advertisements, analytics, recommendations, account sign-in, Compose or third-party dependencies.

## Features
- D-pad navigation and high-contrast remote focus states
- Live date and 24-hour clock
- Installed TV apps (plus compatible phone-style apps) and app launch
- Up to six favorite tiles; long-press OK to toggle favorite (MENU/INFO key also works)
- Wi-Fi and system settings shortcuts
- Persistent favorite list; compact all-apps panel
- Android TV launcher and HOME intents, API 23+

## One-click cloud build
See `BUILD-APK-EASY.md` for GitHub Actions APK build instructions.

## Build (Android Studio)
1. Install a current Android Studio version with Android SDK 35 and JDK 17.
2. Open this folder as a project, allow Gradle to sync (Android Gradle Plugin 8.6.1).
3. Build > Build APK(s). APK output: `app/build/outputs/apk/debug/app-debug.apk`.
4. Transfer to your TV using `adb install -r app-debug.apk` or install using a file manager.
5. Open **Minimal TV** in the TV's apps list. Confirm remote focus, app launch, and Settings work before attempting to make it the default HOME app.

## HOME button / replacing the stock launcher
Android TV and Google TV firmware vary. The HOME intent in the manifest makes this app *eligible* as a Home app, but firmware may ignore a different HOME preference. Never disable stock launcher until you have confirmed this launcher works and you have a recovery path. If available, pick Minimal TV in system's Home app/default apps menu. Some devices require a model-specific launcher manager or ADB procedure. DO NOT blindly disable Xiaomi/Google system packages.

## Limitations
- This is a **source project**, not a prebuilt or signed APK. Android SDK and Gradle build tooling are not present in the authoring environment.
- Not benchmarked on a physical Xiaomi device; performance gains are expected mainly from the simpler launcher UI, not faster video playback or system-level changes.
- Android package visibility differs by firmware; apps with neither TV nor standard launcher activities may not appear.
- Some applications block third-party launching, and manufacturer remote shortcuts may continue to open stock apps.
