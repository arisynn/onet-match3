# Release readiness checklist

A checklist item is complete only with an executed result and artifact/commit evidence. Current specialist work is not a store-ready claim.

## Internal integration/debug

- [x] Actual specialist delegation and isolated branches with shared contracts.
- [x] Original assets preserved; baseline asset integrity and decoder checks passed.
- [x] Commercial rights manifest and negative gate tests prepared by audit workstream.
- [x] Lead integrated gameplay, economy, platform and first session commits; exact SHAs recorded in AGENT_HANDOFFS. UI and session QA follow-up remain pending.
- [ ] Full Kotlin/JS and durable-save/reward/inventory regressions pass on integrated tree.
- [ ] Real Android screenshots/input/lifecycle pass at normal and tall portrait sizes; user-facing ceramic quality reviewed.
- [ ] Active round resumes after process termination without duplicate XP/reward claims.
- [ ] Audio, haptic, music/SFX mute and accessibility options behave on supported Android devices.
- [ ] Debug APK actually generated and package/native libraries/signature verified.

## Public APK/AAB release — blocked until all required evidence exists

- [ ] Rights manifest approves every packaged asset with matching SHA-256 and license evidence; gate passes. Unknown assets are excluded, not copied into a debug public release workaround.
- [ ] Upstream code ownership/license for commercial distribution resolved by owner; no license invented by tooling.
- [ ] Production provider never simulates successful purchases; no fake tokens or hardcoded entitlements.
- [ ] Real billing products, signing/package identity, pending/cancel/restore and verified entitlement/acknowledgment flow configured and tested if purchases are offered.
- [ ] Version codes, min/target SDK, R8, all supported native ABIs, page alignment and artifact signatures verified.
- [ ] Release APK/AAB physically exists; record path, SHA-256, certificate digest and exact source commit.
- [ ] Physical ARM/API coverage, offline operation, cutouts/densities, memory/frame pacing and interruption stress reviewed.
- [ ] Save schema migration/recovery and relevant privacy/backup policy documented.
- [ ] No secrets, signing keys, tokens or payment credentials in commit/artifacts.
- [ ] Public distribution is authorized and all blocking issues resolved.

Google Play publication additionally requires explicit user authorization. An artifact successfully uploaded to GitHub Actions or a signed APK is not legal clearance or Play submission readiness.
