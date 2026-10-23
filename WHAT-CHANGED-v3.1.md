# JL02 TV v3.1 — Changes

- Favorite apps now use full-tile imagery where installed app banners exist; otherwise app icons scale up. No stars or instruction text on favorites.
- Weather and schedule are above Latest News, with 425dp height. News follows, and All Apps remains at the bottom.
- GitHub Actions now use Node 24-ready checkout@v6, setup-java@v5, setup-gradle@v6, upload-artifact@v6.
- Google Calendar → "Send Google iCal link from phone (QR)" creates a short-lived local Wi-Fi page. Scan QR, paste secret iCal URL on phone, then explicitly approve on TV.

## Privacy warning
The phone transfer uses local HTTP, **not encrypted TLS**. Only use on a private trusted home Wi-Fi network, never public or guest Wi-Fi. A nearby network observer could capture the secret iCal URL. The session expires after 3 minutes and uses a random token; it does not make HTTP encrypted. For sensitive calendars, prefer managed OAuth sign-in.

## Natural offline voice
JL02 still uses the Android TTS engine when no configured OpenAI speech service exists. You can try the free offline neural engine HayaiTTS or Marmalade TTS as your system engine, but Mi Box S compatibility/performance is unverified; voice packs require space and CPU. In TV Settings choose the newly installed Text-to-speech engine if supported.

## Build
Upload contents to repo root, including hidden .github, commit, then Actions > Build JL02 TV v3.1 APK > Run workflow. Requires GitHub runners with Node24-compatible actions.
