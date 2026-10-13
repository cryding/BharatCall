# BharatCall AI Telephony Bridge — implementation plan

## Current state

This change adds a provider-neutral Kotlin contract and input validation only.
It does not connect Android SIM-call audio to an AI model and does not create a
PSTN/SIP call. No telephony provider has been configured and no paid service
has been enabled.

## Correct call architecture

1. Android asks the backend whether an AI telephony route is ready.
2. The backend authenticates the user and validates the requested destination.
3. For incoming AI calls, a telephony provider routes a DID/PSTN call to a
   provider webhook/SIP endpoint. The backend creates an AI agent session.
4. For outgoing AI calls, the backend asks the provider to originate the call
   from a provisioned/verified caller ID. Do not assume the handset SIM number
   can be presented as caller ID.
5. Provider media (or SIP/RTP media gateway) connects to a real-time AI voice
   session. The backend owns audio transport, interruptions, timeouts,
   disclosure and human hand-off.
6. The app displays provider-confirmed session state. ACTIVE is allowed only
   after the backend confirms call connection and media connection.
7. On errors, show a clear failure and leave normal SIM calling available.
   Never report AI answering when no bridge exists.

## Security requirements

- Keep telephony credentials and model API keys on the backend, not the APK.
- Require HTTPS, authenticated requests, rate limits, development destination
  allow-lists and server-side request validation.
- Disclose that the caller is speaking to an AI before collecting information.
- Provide human transfer and an immediate hang-up path.
- Avoid recording by default. If recording/transcription is enabled, obtain
  appropriate consent and document retention/deletion.
- Never route emergency calls to an AI agent. Keep emergency calls on the
  native/system dialer.
- Do not enable paid billing or incur provider charges without explicit approval.

## Free-first development gates

### Gate 1 — local code
- Add config validation and provider-neutral bridge contract.
- Add unit tests for URL validation, missing agent ID and disclosure text.
- Keep provider credentials out of source control.

### Gate 2 — backend sandbox
- Select a provider with an explicitly available sandbox.
- Implement authenticated readiness/session endpoints and provider webhooks.
- Test session lifecycle with mock media; no public phone calls.

### Gate 3 — real PSTN/SIP
- Verify provider account, DID/caller ID, supported regions and current charges.
- Connect provider media to the selected real-time voice model.
- Perform consented inbound/outbound test calls and verify two-way audio,
  DTMF if required, interruptions, hand-off and failure fallback.

## Why Android alone is insufficient

InCallService manages Android Telecom call UI/state; it does not grant a regular
third-party app unrestricted access to cellular uplink/downlink audio. A SIP/PSTN
media path must be provided by a telephony provider or privileged carrier/OEM
integration. Speakerphone plus microphone capture is not a reliable substitute.
