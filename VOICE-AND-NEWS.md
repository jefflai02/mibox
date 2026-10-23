# JL02 TV v2.8 — media improvements

## Natural briefing
The ChatGPT app voice is **not available to third-party APKs**. The optional OpenAI `gpt-4o-mini-tts` API uses natural voices, but is separate from ChatGPT Voice and requires its own billed API project. Never put an OpenAI API key in the APK, GitHub Actions variables, or client-side source. The app can POST {"text":"..."} to a privately operated HTTPS endpoint configured with repository variable `JL02_VOICE_PROXY_URL`. The endpoint must return MP3 bytes. **Do not set this variable until an authenticated, rate-limited proxy is deployed**; the APK falls back to local Android TTS instead. The client currently does not support identifying an individual user to the proxy, so protect endpoint access at your network layer before enabling it. Do not publish an unauthenticated URL with paid API access.

## News images
RSS now checks BBC Technology, Business, World and Asia feeds, prioritizing their media:thumbnail tags and falling back to article OpenGraph previews. Some publishers block image fetches, and images will stay blank if no legitimate HTTPS image is supplied. The app does not fabricate news images. News may be unavailable when these sources are blocked.

## Favorite apps
Use icons provided by Android PackageManager, larger for pinned apps, with a tiny star. Android TV apps sometimes package plain adaptive icons instead of landscape banners; this uses installed app icons, not artificial brand recreations. Launchpad favourites persist.

## Build
Upload the contents of this project to repository root and use Actions > Build JL02 TV v2.8 APK > Run workflow. v2.8 is source-only and has not been device-tested.
