# JL02 TV v2.3 — Voice Dashboard + Google and Outlook pairing

See **DEPLOY-CALENDARS.md** for required HTTPS backend and provider OAuth setup. No real calendar account is connected by default.

# JL02 TV 2.2 — Personal Briefing Dashboard

For Xiaomi Mi Box S MDZ-22-AB (Android TV). Replaces first screen with welcome dashboard and keeps original app grid in **Apps**.

## Included

- **Voice briefing:** Remote-selectable **Listen to briefing / Stop briefing** button reads live weather, the next three days, available calendar events, up to five recent headlines, and important notes aloud using Android Text-to-Speech. Playback is on demand, and automatically stops when exiting the app. If TTS is missing, install/enable a compatible speech engine on the TV. Voice language is English; no microphone permission, recording or cloud speech API required.

- Welcome, local clock, large remote-focus buttons.
- **Last 24h headlines:** five Google News RSS searches (AI/technology, semiconductors, Malaysia business, global markets, trade). Only items with parsed publication time within 24 hours are displayed. Select a headline to open its link in an installed browser; older stories and failed feeds are not fabricated.
- **Weather:** Open-Meteo online forecast, using fixed Kuala Lumpur coordinates (3.1390, 101.6869), 24 hourly readings for today in a horizontally scrollable strip, and three following days with rain probabilities.
- **Schedule:** Reads local Android Calendar instances for today *with user permission*. Android TV devices may have no calendar provider or no synced calendars; this is stated in the interface. Google Calendar in ChatGPT is **not** automatically accessible to the APK.
- **Things to note:** dynamically compiled from actual weather, schedule, and news data; never invented appointments.
- Existing app launcher (previous version's favorites, apps & settings) via **Apps**.

## Build and install

Extract files at the GitHub repository root; overwrite the existing files including `.github/workflows/build-apk.yml`. Push/commit to `main` then open **Actions → Build JL02 TV APK**, open successful run **Summary → Artifacts**, download `JL02-TV-MDZ-22-AB-APK` and extract `app-debug.apk`. Install with TV file manager or `adb install -r app-debug.apk` (same package ID; debug signing key might differ across GitHub builds, in which case uninstall old test app first, losing its saved favorites).

## Requirements / caveats

Requires Wi-Fi or network access; public APIs may be unavailable in some networks. Calendar permission is optional. Calendar provider must exist and have synchronized events for schedule to show. News links may require an installed Android TV browser. The dashboard does not generate AI summaries or access personal cloud accounts. It shows its compiled day notes from available data. Kotlin, Google services, and third-party advertising SDKs are not included. It is a source package, not a pre-built APK, and hasn't been verified on the physical box.

## Customization

Edit the `topics` array in MainActivity.java for subjects. Edit Open-Meteo `latitude` and `longitude` for another city. Change welcome text in `build()`.

**Upgrade compatibility:** Android application ID remains `com.minimaltv.launcher` intentionally so JL02 TV replaces the previous launcher rather than appearing as a separate app. New debug builds may be signed with a different key.
