# JL02 TV v3.0 changes

- Favorite cards: removed instructional caption and yellow stars, enlarged Android-provided art and use landscape TV banners when installed apps include them.
- Weather: enlarged panel, current-hour-first timeline, three larger forecast cards.
- News: requests full-resolution publisher images instead of fixed 2x downsampling; still depends on publisher source quality and availability.
- Google Calendar: Calendar menu → Add Google Calendar iCal link (read-only). Uses a personal Google Calendar secret iCal URL; Google may not offer this for every account, and updates may be delayed. Treat this URL as a sensitive credential. It is stored in app-private preferences on the TV and is **not encrypted** in this version; use only if you accept that device-access risk. Removing it in Calendar menu clears it. The app cannot sign into Google with OAuth without additional integration.
- Microsoft 365 sign-in retained. When Microsoft 365 is connected, it takes priority over iCal in today's schedule: both are not merged in this version.
- OpenAI natural voice still requires the separately configured HTTPS voice proxy. This package does not embed an API key or activate ChatGPT's in-app voice.

The visual result depends on Android TV app assets and publisher thumbnail resolution. Not hardware-tested.
