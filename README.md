# BharatCall 🇮🇳

An open-source Android dialer starter focused on Indian users, local-first caller lookup, and a future opt-in online caller-ID service.

## Status

**Starter MVP — not production-ready.** The source code is committed, but compilation and physical-device SIM-call tests still need to be verified.

## Included
- Kotlin Android dial pad and default-dialer role request
- Android Telecom InCallService entry point and minimal in-call UI
- Permission-gated offline contact lookup
- Online caller-ID placeholder (disabled until a reviewed HTTPS API is configured)
- GitHub Actions workflow to build and upload a debug APK artifact
- MIT license

## Build
Requirements: Android Studio/Android SDK, JDK 17, Gradle 8.9.

```bash
gradle --no-daemon assembleDebug
```

APK output: app/build/outputs/apk/debug/app-debug.apk

Alternatively, open Actions → Android debug APK after pushing to GitHub. When a workflow run succeeds, download the BharatCall-debug-apk artifact from that run. An Actions artifact is not the same as a signed production release.

## Test on device
1. Install the debug APK on a test Android phone.
2. Open BharatCall and tap Set as default phone app.
3. Grant contacts permission only if you want local contact lookup.
4. Test ordinary calls with consent from the other participant. Never use emergency numbers for testing.
5. Validate incoming-call presentation, audio routing, dual-SIM selection, background behavior, and device/OEM compatibility.

## Privacy and security
- Contact lookup reads local contacts only after the user grants permission.
- Online lookup is disabled by default. Do not upload contact books or call logs without clear opt-in, a privacy notice, and a lawful basis.
- Never embed private API keys in an APK.
- Emergency calls are handled by the preloaded system dialer; this app does not replace emergency-service behavior.

## Roadmap
1. Get a successful CI build and resolve compilation issues.
2. Complete incoming/outgoing call lifecycle and UI requirements for Android's default dialer role.
3. Add device tests, call history, favorites, and dual-SIM validation.
4. Design a privacy-preserving caller-ID API, opt-in reports, moderation, rate limits, and deletion controls.
5. Add tests, security review, privacy policy, and secure production signing before any public release.

## License
MIT. See LICENSE.
