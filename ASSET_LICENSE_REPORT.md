# Commercial asset rights: evidence review

Review date: 2026-11-13, integrated asset tree `1093892`. This supersedes the prior report's **CONDITIONALLY CLEARED**, blanket AI public-domain, and “zero infringement” assertions. Those conclusions were not established by repository evidence.

## Findings

| Asset class | Evidence available | Commercial distribution status |
| --- | --- | --- |
| Existing tiles, original sheets, surfaces, tools, effects | Source paths, generation metadata, hashes, extraction history | **Unconfirmed**: provenance is not a license grant or account/terms attestation |
| Cozy Ceramic artwork | Generation/source/crop manifests | **Unconfirmed** for the same reason |
| `ui-native` PNG derivatives | Lossless conversion and source hashes in manifest/script | Inherit the source artwork's unresolved rights; conversion grants no new permission |
| Nunito and DM Serif Display fonts | Bundled SIL OFL 1.1 license files and matching file hashes; source Google Fonts repository | Confirmed under OFL conditions, including bundled license notices |
| Newly authored procedural audio | 13 generated PCM files, reproducible original synthesis script, per-file hashes and explicit CC0 dedication in `assets/audio/LICENSE.txt` | Evidence recorded as CC0-1.0 for these exact audio hashes; does not clear unrelated art |
| Upstream code | Repository lineage; README expressly adds no new license; no root LICENSE | No new open-source/commercial license is inferred here |

Same-account repositories, AI-generation prompts and “generic art” do not alone prove the necessary usage rights. No legal judgment on copyrightability or infringement is made. The owner must provide applicable permissions/ownership evidence and provider terms where relevant; reviewers must record them against the exact file hashes.

## Enforcement

`docs/commercial-asset-rights.json` records every currently bundled `assets/` file, hash, reviewed status and evidence. `scripts/check-commercial-rights.mjs` rejects:

- Unknown, omitted or newly added files.
- Changed assets after a rights review.
- Absent, changed or out-of-project license evidence.
- Assets whose resolved path escapes the bundled directory.

Public packaging must depend on this check. It has no force/skip approval flag. Until approved runtime art can form a complete package, the public release build fails rather than silently bundling disputed artwork or producing a broken game. This keeps unresolved assets out of public APK/AAB outputs while retaining every original in source control. Internal debug validation remains available and must not be published as a workaround.

To resolve a file, record `status: confirmed`, `commercialUseAllowed: true`, its current SHA-256, license, source, local evidence path and evidence SHA-256 after actual review. Do not mass-approve files based on this report alone.

## Current inventory result

The integrated scan checked 824 bundled files: 19 have recorded matching evidence (four font/license files and fifteen original-audio/provenance files); 805 remain unconfirmed. The public gate correctly exits 1. No public artifact is cleared. All 194 native tile transcodes inherit the source art's unresolved status. All 219 native UI/tile transcodes were independently checked for source/output hashes and exact decoded RGBA identity; encoding compatibility is not permission.
