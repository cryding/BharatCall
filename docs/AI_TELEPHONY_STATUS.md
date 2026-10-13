# AI telephony implementation status

- [x] Add provider-neutral Android bridge contract.
- [x] Validate HTTPS backend URL, agent ID and caller disclosure configuration.
- [x] Document security, consent, hand-off and emergency-call requirements.
- [ ] Implement authenticated backend API.
- [ ] Select and configure a SIP/PSTN provider.
- [ ] Connect provider media to a real-time AI voice model.
- [ ] Add UI actions for AI Answer and AI Call, gated on backend readiness.
- [ ] Add automated tests and verify CI.
- [ ] Complete consented real-device incoming/outgoing test calls.

No live telephony provider or paid service has been enabled. Do not describe AI
Answer or AI Call as working until the unchecked integration and call-test
items are complete.
