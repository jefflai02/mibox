# JL02 TV v2.5 — Unified TV Dashboard

A television launcher for Xiaomi Mi Box S MDZ-22-AB. Layout: top welcome/actions, large favorites, 4-card live news row, paired weather and calendar row, compact bottom All Apps strip.

## Build APK
Upload **contents** of this ZIP directly to the root of GitHub repo (including hidden `.github/workflows/build-apk.yml`), commit, then GitHub > Actions > Build JL02 TV v2.5 APK > Run workflow. From the successful workflow run select Summary > Artifacts > JL02-TV-v2.5-MDZ-22-AB-APK. Extract `app-debug.apk`.

This is source code, not a prebuilt APK. It has not been compiled or device-tested in this environment.

## Calendars
Important: a QR code requires a deployed HTTPS calendar backend and Google/Microsoft OAuth registrations. Without this server it is not possible to display a functional QR sign-in. The APK opens an explanatory settings prompt instead of generating a fake pairing QR. See `DEPLOY-CALENDARS.md` for backend setup. Do not commit real .env secrets or put them in the APK.

## Content
News via Google News RSS; publisher images appear only where feed returns a valid image URL. Weather via Open-Meteo. Both require a network connection. Calendar entries only come from connected server or Android device calendars. Mockup is illustrative and not a promise of identical third-party thumbnail availability.

## Installation
Install debug APK on TV. Verify remote focus, network and Home key. Do not disable original launcher until tested. Previous versions use application ID `com.minimaltv.launcher`; build keeps the same ID for compatibility but a debug APK can only update one signed with the same signing key. If signing differs, you may need to uninstall the previous debug build (back up local favorite preferences first).
