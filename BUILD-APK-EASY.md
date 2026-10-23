# Build JL02 TV v2.7 on GitHub

1. Create or open your GitHub `mibox` repository. Upload the contents of this ZIP **at the repository root** including the hidden `.github` directory.
2. Follow `MICROSOFT-SETUP.md` to register a Microsoft Entra public-client app and set GitHub Actions variable `JL02_MS_CLIENT_ID`. This is a public application identifier, not a password or client secret.
3. Commit to the default branch. Open **Actions → Build JL02 TV v2.7 APK → Run workflow**.
4. If successful, select **Summary → Artifacts**, download `JL02-TV-v2.7-MDZ-22-AB-APK` and extract `app-debug.apk`.
5. Install on Mi Box S. Go to Calendars → Connect Microsoft 365 on my phone.

If no app ID is configured, the sign-in UI will explain that setup is required instead of issuing an invalid code. Company policies may require IT consent.

This is an uncompiled project; verify on your device before replacing the system launcher.
