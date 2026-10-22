# Build the APK — Xiaomi Mi Box S MDZ-22-AB

This project contains a **GitHub Actions** build workflow that generates an installable, debug-signed APK without installing Android Studio.

1. Log in to GitHub on a desktop browser and create a new **private** repository.
2. Upload the **contents** of the `XiaomiMinimalTV` folder to the repository root, **including** the hidden `.github` folder. The upload page supports drag-and-drop; alternatively use GitHub Desktop or git to include hidden files.
3. Go to **Actions > Build Minimal TV APK > Run workflow** (the workflow may also start automatically on push to main/master).
4. Once the green build succeeds, open the workflow run and download the artifact named **MinimalTV-MDZ-22-AB-APK**.
5. Unzip the artifact to get `app-debug.apk`. It is an APK, not a ZIP.
6. Copy the APK to the Xiaomi Mi Box S (MDZ-22-AB), then install with a TV file manager with unknown-app installation permission; or use ADB: `adb install -r app-debug.apk`.
7. Open Minimal TV and test your remote and applications **before** making it the default launcher.

**Cautions:** The APK is debug-signed for personal testing, not a production release. This environment has not run the build or tested the APK on your hardware. The Home button may remain tied to Xiaomi's system launcher depending on your firmware. Do **not** disable the stock launcher before a reliable fallback is in place.

The workflow requires GitHub-hosted runners and network access in GitHub Actions. For local builds, open the project in Android Studio with JDK 17 and Android SDK 35, then Build > Build APK(s).
