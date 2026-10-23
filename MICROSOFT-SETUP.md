# JL02 TV v2.7 — Microsoft 365 sign-in (no NAS)

The app uses the Microsoft device-code flow. **One administrator/developer action is required before it can log in:** register a Microsoft Entra public client and supply the resulting **Application (client) ID** at GitHub build time. No client secret is used. Your company may deny user consent and require IT approval.

1. Visit https://entra.microsoft.com/ with an account permitted to register applications. Go to **Identity → Applications → App registrations → New registration**. If registrations are disabled, ask company IT to register/approve a calendar-only app.
2. Name it `JL02 TV`. Choose **Accounts in any organizational directory** if your IT policy allows, or **Single tenant** if it is only for your company. For a single-tenant company app, use the specific Directory (tenant) ID, not just the generic organizations authority.
3. Under **Authentication → Advanced settings**, set **Allow public client flows** to **Yes** (required for device-code flow). Do not create a client secret for TV.
4. Under **API permissions**, add Microsoft Graph **Delegated** `Calendars.ReadBasic`. The app also requests `offline_access` to preserve sign-in. Your company may require approval/consent.
5. Copy the **Application (client) ID** (not a secret). Open your GitHub `mibox` repository → **Settings → Secrets and variables → Actions → Variables → New repository variable**. Name `JL02_MS_CLIENT_ID`, paste the ID, save.
6. Commit these files to your repository, run GitHub Actions **Build JL02 TV v2.7 APK**, then install its generated `app-debug.apk`.
7. On TV choose **Calendars → Connect Microsoft 365 on my phone**, open the displayed Microsoft page on your phone, enter the device code and authorize.

**Security:** access uses Microsoft's official authentication site, HTTPS Graph API, Android Keystore AES-GCM for the refresh token, and delegated read-only `Calendars.ReadBasic`. Private events are not shown; the app currently displays non-private work meeting titles. Avoid using this on a publicly shared TV if work meetings are confidential. **Disconnect** removes the locally cached refresh token, but organization-level app consent can be revoked in Microsoft account/company admin settings.

**Limitations:** Google Calendar integration is not included in this APK. The new build has not been compiled or security-tested on a physical Mi Box. Some Microsoft 365 tenants block device-code flow entirely; your IT can confirm.

## Fix for AADSTS50059
1. Ask the person who registered the JL02 TV Microsoft Entra application for its **Directory (tenant) ID**, a GUID. It is not a password.
2. On GitHub go to **Settings → Secrets and variables → Actions → Variables**. Add `JL02_MS_TENANT_ID` with that Directory (tenant) ID.
3. Ensure `JL02_MS_CLIENT_ID` belongs to an application in that same tenant and **Allow public client flows = Yes**. Rebuild the APK.
4. Your company's policy may block device code or require admin consent. Do not bypass these policies.
