# JL02 TV v2.7 — Direct Microsoft 365 Work Calendar

Replaces failed NAS calendar pairing with direct Microsoft device-code sign-in. Existing apps, favorites, news, weather, voice briefing retained from v2.6.

**Start with MICROSOFT-SETUP.md** for one-time Microsoft app registration and GitHub variable. The APK does not need a NAS, server, password, or OAuth client secret.

Install: Upload **all contents** of this ZIP at GitHub repo root (include hidden `.github/workflows/build-apk.yml`), commit, run **Build JL02 TV v2.7 APK**, download artifact.

Microsoft connection works only after you supply a valid registered public-client Application ID and your company authorizes the delegated calendar permission. Personal Google sign-in remains a separate future integration.
