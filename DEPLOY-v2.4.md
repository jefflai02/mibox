# JL02 TV v2.4 deployment

This version embeds Favorite Apps and All Apps shortcuts on the same page as the news, weather, schedule and spoken briefing. The yellow JL02 logo supplied by the user is included as Android app icon and dashboard artwork.

## GitHub
Upload the **contents** of this folder to the root of your existing `mibox` repository (do not nest in another folder), keeping `.github/workflows/build-apk.yml`. Commit, open Actions, run the Build JL02 TV workflow, and download its APK artifact.

## Important limits
- The project has **not been compiled or tested** in this environment.
- News comes from Google News RSS and tries `https` image URLs explicitly supplied by the news feed as media thumbnails; some feeds do not supply images, so cards may lack thumbnails. No fabricated cover images are shown. The original article opens on click. This is not a direct licensed publisher-photo integration.
- Weather comes from Open-Meteo, with a subtle animated weather symbol, not a live animated weather-provider asset.
- Calendar QR sign-in needs the independently deployed calendar backend and Google/Microsoft OAuth configuration. Installing APK alone will not enable it.
- If installing over an earlier debug APK signed with a different GitHub runner key fails, uninstall the old app first (favorites may be lost).
- Do not disable the original system launcher until JL02 TV is verified.
