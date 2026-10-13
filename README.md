# BharatCall 🇮🇳

An open-source Android dialer starter focused on Indian users, local-first caller lookup, and a future opt-in online caller-ID service.

## Status

**Starter MVP — not production-ready.** CI verifies a debug build on the current AI telephony branch, but physical-device SIM-call tests, a signed production release, and live provider integration are not complete. A successful backend health check does not mean that AI can answer or speak on a real cellular call.

## Included

- Kotlin Android dial pad and default-dialer role request
- Android Telecom InCallService entry point and minimal in-call UI
- Permission-gated offline contact lookup
- Online caller-ID placeholder (disabled until a reviewed HTTPS API is configured)
- GitHub Actions workflow to test the Node mock API, build the Android debug APK, and upload an artifact
- HTTPS backend URL setting and health diagnostics (backend mode, request ID, and declared capabilities)
- Provider-neutral AI telephony interface and mock-only Node API scaffold
- MIT license

## AI telephony status

The API scaffold **does not** place carrier/SIP calls, connect call audio, invoke Gemini for live conversation, or transfer a real call. It intentionally reports telephony capabilities as unavailable. Its shared-token protected routes are for controlled testing only; do not embed that token in a public APK. Android session creation is not enabled until per-user authentication and session authorization are implemented.

See:
- [Render phone-only guide (Bengali)](backend/RENDER_PHONE_GUIDE_BN.md)
- [Backend deployment guide](backend/DEPLOYMENT.md)
- [AI telephony API documentation](backend/README.md)
- [Production readiness checklist](docs/AI_TELEPHONY_PRODUCTION_CHECKLIST.md)

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
5. In Settings → AI telephony backend URL, save the deployed HTTPS base URL; use Test telephony backend to view diagnostics. This checks the backend only, not live calling.
6. Validate incoming-call presentation, audio routing, dual-SIM selection, background behavior, and device/OEM compatibility.

## Privacy and security

- Contact lookup reads local contacts only after the user grants permission.
- Online lookup is disabled by default. Do not upload contact books or call logs without clear opt-in, a privacy notice, and a lawful basis.
- Never embed private API keys or the shared backend token in an APK.
- Emergency calls are handled by the preloaded system dialer; this app does not replace emergency-service behavior.
- Real AI telephony needs a provider/media gateway and user consent; public telephone calls may incur charges. No paid service is enabled by this repository configuration.

## Roadmap

1. Complete per-user authentication, token expiry/revocation, and session-level authorization.
2. Connect Android to protected session APIs only after the authentication security tests pass.
3. Select a SIP/PSTN provider and implement verified webhooks plus a genuine bidirectional audio gateway (requires owner approval of any costs).
4. Complete device tests, call history, favorites, dual-SIM validation, privacy review, and security review.
5. Configure production signing and release only after CI and authorized end-to-end tests pass.

## License

MIT. See LICENSE.
