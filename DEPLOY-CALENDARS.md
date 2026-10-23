# JL02 TV v2.3 — Google + Outlook calendar pairing

## Status
This is **buildable prototype source, not a deployed service**. No Google/Microsoft OAuth credentials, HTTPS server, accounts, or live calendar data are included. APK still requires a GitHub Actions build and device testing. Test on a private staging server before placing sensitive work calendars into this system.

## Architecture
- TV shows an in-app QR code containing a randomly generated pairing URL (10-minute expiry).
- You scan with a trusted phone and choose Google or Microsoft OAuth. Authentication and MFA happen on the official provider page, never inside the TV app.
- The separate HTTPS service stores encrypted refresh tokens using Fernet, then queries Google Calendar API and Microsoft Graph. Android TV holds only a revocable JL02 device token.
- Combined schedule shows time + meeting title, with provider labels. Events marked private/confidential are replaced with "Private appointment". Voice reads times only by default; non-private titles are optional.
- Multiple accounts are supported: repeat pairing for every Google or Microsoft account. Use the Calendar menu to disconnect accounts or unlink TV.

## Hosting steps
1. Deploy `server/` on your own trusted HTTPS-hosted backend. Persistent, restricted storage is required for the SQLite database. For a personal pilot, a single instance with encrypted backups is sufficient; use managed Postgres and a production secret store for public/multi-user use.
2. Install requirements (`pip install -r server/requirements.txt`) or build the Dockerfile.
3. Generate a *stable* Fernet encryption key with `python -c 'from cryptography.fernet import Fernet;print(Fernet.generate_key().decode())'` and save it in your **hosting platform's secret manager**, not in a public GitHub repository or the APK. Losing the key makes existing tokens unreadable.
4. Register a Google Cloud OAuth **Web application** with an authorized redirect URI exactly `https://YOUR_DOMAIN/callback/google`. Configure the consent screen, permitted test users and the `https://www.googleapis.com/auth/calendar.events.readonly` scope. Google may require verification for broader/public usage.
5. Register a Microsoft Entra application with a **Web** redirect URI exactly `https://YOUR_DOMAIN/callback/microsoft`; allow suitable personal/work account types. Add delegated Microsoft Graph `Calendars.Read` permission and enable organizational admin consent when required. Configure application secret server-side only.
6. Set environment variables from `server/.env.example`: `PUBLIC_BASE_URL`, `FERNET_KEY`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `MS_CLIENT_ID`, `MS_CLIENT_SECRET`, `SQLITE_PATH`. Protect all secrets.
7. Serve backend via HTTPS with TLS, rate-limit `/v1/device`, `/v1/pair`, `/connect`, and `/auth` at the reverse proxy and monitor login abuse. Reject untrusted hosts, use outbound allowlisting where available, patch dependencies and restrict database access. Backup database securely. Consider cookie-based login and human confirmation code for wider deployment.
8. Start with `uvicorn app:app --host 127.0.0.1 --port 8080` behind your HTTPS reverse proxy.
9. Compile TV APK via existing `.github/workflows/build-apk.yml`; in JL02 TV open **Calendars → Set secure sync server URL** and enter `https://YOUR_DOMAIN`. Then choose **Connect Google / Outlook (QR)**. On completion choose **Refresh schedule**.

## Security boundaries and limitations
- Calendar secrets never go into Android APK, GitHub Actions workflow or repo. The calendar sync backend holds sensitive encrypted tokens: its security, backups and operational upkeep are your responsibility.
- The paired TV still displays meeting titles on a shared screen; choose an appropriate location. Private providers' entries are masked. You can turn spoken non-private meeting titles on/off. Event location and descriptions are not fetched.
- This sample uses `primary` Google Calendar and Microsoft's default calendar, not *every* sub-calendar. Connecting both accounts combines those primary/default schedules. Multi-calendar picker, event de-duplication, background push refresh and admin panel are future work.
- The app does not automatically retrieve schedules from ChatGPT-connected calendars. TV and backend are separately authorized.
- The server stores linked accounts persistently until disconnected. The TV token is stored in app-private preferences; secure your device, do not enable backups, and keep Android updated.
- No integrated end-to-end deployment, provider-consent verification, or on-device installation test has been performed here. Treat this as a pilot and use a restricted test account before production use.
- The connection link is a bearer-like secret. Don't share screenshots of the QR. It expires in 10 minutes and can only be used once for each authorization.
