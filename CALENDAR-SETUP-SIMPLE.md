# Calendar setup without technical links on the TV

## What works immediately
Open JL02 TV → Calendars → **Use calendars available on this TV**. Grant Android calendar access. Appointments only appear if that Android TV installation has a local/synced calendar provider. Many Mi Box S devices do not have one.

## What the future one-scan experience needs
A developer/managed service operator must supply a secure HTTPS endpoint and connect it to Google and Microsoft using hosted OAuth. Nylas is an integration platform option, not a fully preconfigured JL02 TV calendar app. A separate trusted, authenticated pairing relay must still be deployed. For privacy choose calendar-only permissions, suppress private titles on TV, keep server API keys and refresh tokens off the APK, allow account disconnection.

Once a real compliant relay is deployed, set the repository Actions variable `JL02_CALENDAR_URL` to its actual HTTPS origin and build the APK. Thereafter the TV shows **Connect by QR**, without asking users for URLs. Leave it unset until a working service is available; the app will clearly show Google/Outlook as not activated.

Never commit OAuth client secrets, refresh tokens, session tokens, or `.env` files. The `server/` folder is illustrative legacy backend code, not a pre-connected turnkey managed service.
