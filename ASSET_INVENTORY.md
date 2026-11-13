# Asset inventory — audited baseline

Snapshot: integrated checkout at `1093892` plus the lead’s six additional pending UI transcodes, reviewed 2026-11-13. Counts are from the actual integrated `assets/` tree, including native tile conversions and original procedural audio. Original art is preserved.

| Directory | File count | Use / compatibility |
| --- | ---: | --- |
| `assets/tiles` | 273 | Tile IDs across 19 themes; base/beach PNG, most other themes WebP |
| `assets/originals` | 133 | Source/older variants retained; not needed for normal rendering but currently bundled by Android source set |
| `assets/ui-kit` | 145 | 122 ready assets plus source/strip/manifest material; includes animated WebP and PNG effect frames |
| `assets/ui-native` | 26 | 25 lossless static PNG transcodes plus source/hash/bounds manifest |
| `assets/tiles-native` | 195 | 194 lossless static PNG transcodes plus source/hash manifest |
| `assets/audio` | 15 | 13 original PCM WAVs, provenance manifest and CC0 dedication |
| `assets/atlases` | 9 | Master transparent art sheets, not libGDX `.atlas` metadata |
| `assets/surfaces` | 13 | WebP tile surfaces |
| `assets/effects` | 7 | WebP effects |
| `assets/tools` | 4 | WebP tool/mode images |
| `assets/fonts` | 4 | Two TTF files plus two OFL license notices |
| **Total** | **824** | **110,446,245 bytes** before APK compression |

All 802 image files (830 frames) were decoded, including every frame of animated files, using Pillow during this audit: zero decode errors. Format totals: 361 PNG, 426 WebP, 15 JPG, 13 WAV, 2 TTF, 3 TXT, 4 JSON. Zero duplicate SHA-256 groups were found; visually similar images or format derivatives are not byte duplicates.

The 19 theme counts and exact category/format counters are recorded in `docs/asset-inventory-runtime.json`. Existing provenance checks separately verified 316 recorded originals, 114 derivatives, 9 atlases, 273 tile IDs, 122 ready UI assets and 4 animations; these overlapping manifest counts are not additional distinct files.

## Integration decisions

- PNG/JPEG are directly supported by libGDX's usual Pixmap loader. Arbitrary WebP/animated WebP loading must not be assumed; use supported conversion/decoding and verify actual runtime paths.
- Native internal paths start at the contents of `assets/`, not `assets/assets/`. JavaScript's repository-relative paths remain a separate reference API.
- The audit independently compared all 219 native UI/tile transcodes to their source decoded RGBA pixels and verified both hashes; all 13 original PCM files match manifest hashes and WAV headers. The public rights gate dynamically discovers future assets and rejects omitted/unreviewed files.
- Originals, source sheets and unused art should be excluded from a future public runtime package without deleting them from the repository. Packaging optimization must not bypass commercial-use review.
- No decoder failures or byte duplicates means neither legal clearance nor visual QA. Commercial status is separately recorded in `ASSET_LICENSE_REPORT.md` and the rights manifest.
