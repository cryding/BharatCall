# BharatCall: remaining production work and safe implementation boundary

## Current verified implementation

- Android Settings can save an HTTPS backend URL locally and call the public `/health` endpoint.
- The diagnostics dialog reports backend mode, request ID, and declared capabilities when the server returns them.
- The Node API is a mock-only scaffold with request IDs, failed-auth rate limiting, session expiry, disclosure validation, and explicit false capability flags.
- Automated GitHub Actions runs have passed for the backend safeguards and documentation commits. The latest Android diagnostics change still needs its own CI result.

## What is not implemented

- No actual SIM/PSTN/SIP call can be placed by the mock backend.
- No call audio is streamed from Android to the server or from the server back into a cellular call.
- No live Gemini voice conversation, carrier webhook, provider signature verification, human transfer, or production user identity is connected.
- The Render URL is only a deployment address; a successful `/health` response is not proof of live telephony.

## Required order before Android may use protected session APIs

1. **Identity:** implement per-user sign-in and server-side identity records. Do not use a single global API token as the identity of every app installation.
2. **Short-lived access tokens:** issue and validate expiring access tokens, with rotation/revocation. Store mobile tokens using Android Keystore-backed encryption and never ship provider secrets in the APK.
3. **Authorization and isolation:** associate each session with the authenticated user; reject cross-user reads, handoffs, and termination attempts.
4. **Abuse prevention:** add persistent rate limits, request-size limits, audit events without secrets or call audio, retention policy, and operational alerting.
5. **Provider selection:** choose a SIP/PSTN provider with verified account ownership, caller-ID rules, inbound/outbound support, and transparent pricing. Do not activate billing until explicitly approved.
6. **Media gateway:** implement real bidirectional provider media streaming into an approved real-time speech/AI service. Test interruptions, DTMF, silence, network failures, and latency.
7. **Disclosure and consent:** disclose AI use at the start of each call, obtain any required consent, provide a human fallback, and apply regional calling/recording rules.
8. **Staging end-to-end tests:** use a controlled, authorized test number. Verify provider webhook signatures, call routing, media in both directions, failure paths, and user-specific authorization.
9. **Production release:** deploy only after CI, security review, privacy review, and a signed Android release build have passed.

## Why no public mock-session button is enabled in Android yet

The protected mock endpoints currently use a shared bearer token. A public APK must not contain that shared server secret, and putting it in a public app would allow anyone to create and access mock sessions under the same credential. Android-to-session integration is intentionally blocked until per-user authentication is implemented. The current URL test is safe because `/health` is public and read-only.

## ₹0 policy

Continue with free-tier health checks, local mock tests, API contracts, and CI. Do not upgrade Render, add a paid service, buy credits, or enable provider billing without the owner's explicit approval. Real public telephone calls may incur provider and carrier costs, so a fully live AI phone agent cannot honestly be promised as a zero-cost feature until a suitable no-cost provider path is verified.

## Next actionable milestone

Implement per-user authentication with server-side persistence, then add authenticated Android session creation/status/end. Only after that milestone passes security tests should a real SIP/PSTN provider be integrated.