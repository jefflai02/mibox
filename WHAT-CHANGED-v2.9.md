# JL02 TV v2.9 — focused corrections

- **Microsoft AADSTS50059:** supports company-specific Microsoft Entra tenant ID through GitHub Actions variable `JL02_MS_TENANT_ID`. This does not substitute for IT permission or an authorized app registration.
- **Favorite app artwork:** prefers the Android TV application's own landscape banner; enlarges official icon if no banner exists. Actual visual result depends on artwork packaged by each installed app.
- **News:** uses feed/image metadata and article preview; missing artwork stays neutral, not fabricated. Publishers may block downloads.
- **Speech:** OpenAI voice is **not included by default**. Existing `JL02_VOICE_PROXY_URL` works only with an independently deployed secure, authenticated OpenAI speech proxy. Never put OpenAI API keys in an APK or expose an unauthenticated paid endpoint. Android TTS remains fallback.

This is an **uncompiled source update**. Test APK build and TV layout before making it your default launcher.
