# CyberOps Lock — Android project

An ultra-dark, neon-green cyber-ops themed Android app and live wallpaper based on the supplied `build.yml` concept.

## Features
- Animated cyber HUD with radar rings, scan beam, grid, and eye/triangle emblem.
- Dedicated simulated face-scan panel. The animation is visual only and does not capture or recognize a face.
- Real AndroidX `BiometricPrompt` verification for enrolled **strong biometrics**. Android chooses the supported modality (for example, face or fingerprint); the app cannot force every phone to use face recognition.
- If strong biometrics are unavailable, the app tries the system device-credential confirmation flow when the device has a PIN, pattern, or password configured. If neither is available or verification fails, the UI reports failure rather than granting access.
- Demo code `2468` is explicitly only an in-app demo and never unlocks the phone or verifies the device credential.
- Quick actions, live wallpaper setup, and lock-screen notification controls.
- The hacking/network logs are a visual simulation only; the app does not hack targets or exfiltrate data.

## Build the APK
1. Extract this ZIP.
2. Open the `CyberOpsLock` folder in Android Studio, or upload the **contents** of that folder to the root of a GitHub repository.
3. In Android Studio, allow Gradle sync and select **Build → Build APK(s)**. Or push to GitHub and run the included **Build CyberOps APK** Actions workflow.
4. The debug APK output is `app/build/outputs/apk/debug/app-debug.apk`.

The build workflow is configured, but this package has not been compiled or device-tested in the current environment because an Android SDK/Gradle installation is not available here.

## Android security limitations
An ordinary app cannot replace or bypass Android's real system lock screen, read its PIN/password, or guarantee touch events on a wallpaper while the system lock screen is visible. The app's own access panel is a separate app UI; Android's system prompt is the authority for real authentication.
