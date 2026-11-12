# Onet + Triple Match: Exhaustive Asset Inventory

**Inventory Version:** 1.0.0  
**Audit Date:** 2026-11-12  
**Auditor:** Agent 01 — Repository Audit & Asset Specialist (`@auditor`)  
**Target Repository:** `/root/.gemini/antigravity/scratch/onet-match3`  

---

## 1. Global Asset Overview & Summary Metrics

The repository contains **584** files under `assets/`, **6** visual contact sheet previews under `docs/previews/`, and associated metadata catalogs.

### High-Level Asset Breakdown

| Asset Category | File Count | Primary Dimensions | Format(s) | Color Mode | Total Size (KB) | Alpha Transparency |
|---|---|---|---|---|---|---|
| **Base Game Tiles** | 64 | 313×313 – 314×314 | PNG | RGBA | 3,949.8 | Full Alpha |
| **Premium Game Tiles** | 144 | 256×256 | WebP | RGBA | 1,440.9 | Full Alpha |
| **Reward Game Tiles** | 50 | 512×512 | WebP | RGBA | 6,583.2 | Full Alpha ($\ge 40$px margin) |
| **Beach / Vanilla Tiles** | 15 | 256×256 | PNG | Mode P | 291.3 | Palette Index Alpha |
| **Master Atlases** | 9 | 1254×1254, 1983×793 | PNG | RGBA | 10,086.5 | Full Alpha |
| **Tile Surfaces / Backplates** | 13 | 256×256 | WebP | RGBA | 95.0 | Full Alpha |
| **Ambient / Match Effects** | 7 | 256×256 | WebP | RGBA | 51.5 | Full Alpha |
| **Tools & Game Boosters** | 4 | 256×256 | WebP | RGBA | 38.8 | Full Alpha |
| **Cozy Ceramic UI Kit (Ready)** | 122 | Various (256px – 1536px) | WebP, PNG | RGBA / RGB | 17,215.7 | Alpha (except 8 bgs) |
| **Cozy Ceramic UI Kit (Strips)** | 4 | 2048×256 | PNG | RGBA | 1,913.1 | Full Alpha |
| **Cozy Ceramic UI Kit (Sources)**| 18 | 1024px – 1774px | PNG | RGBA / RGB | 31,986.7 | Master Source Raws |
| **UI Kit Manifest** | 1 | JSON | JSON | UTF-8 | 107.1 | N/A |
| **Originals Archive (Public Art)**| 118 | 220×220, 512×512 | WebP | RGB / RGBA | 2,051.8 | Source WebPs |
| **Originals Archive (Source Sheets)**| 15 | 1024×1024 | JPG | RGB | 8,781.5 | Generative Raw Sheets |
| **Documentation Previews** | 6 | High-Res Previews | JPG | RGB | 1,322.7 | Contact Sheets |
| **TOTALS** | **590** | — | — | — | **~85,915.6 KB (~83.9 MB)** | — |

---

## 2. Tiles Inventory (273 Active Tile IDs)

The game logic supports 19 themes totaling 273 unique tile IDs (`src/shared/tiles.js`).

### 2.1 Base Themes (4 Themes × 16 Tiles = 64 Tiles)
Extracted from upstream `public/art/` and processed into transparent RGBA PNGs using 4×4 atlas slicing (`assets/atlases/*-transparent.png`).

| Theme Key | Tile ID Pattern | Count | Dimensions | Format | Mode | Size Range (KB) | Disk Path |
|---|---|---|---|---|---|---|---|
| `candy` | `candy_0` .. `candy_15` | 16 | 314×314 (313–314) | PNG | RGBA | 45.4 – 69.8 | `assets/tiles/candy/{0..15}.png` |
| `forest` | `forest_0` .. `forest_15` | 16 | 314×314 (313–314) | PNG | RGBA | 52.8 – 76.5 | `assets/tiles/forest/{0..15}.png` |
| `ocean` | `ocean_0` .. `ocean_15` | 16 | 314×314 (313–314) | PNG | RGBA | 50.1 – 81.3 | `assets/tiles/ocean/{0..15}.png` |
| `space` | `space_0` .. `space_15` | 16 | 314×314 (313–314) | PNG | RGBA | 46.4 – 92.0 | `assets/tiles/space/{0..15}.png` |

#### Base Tile Subject Matter
- **Candy:** Gumdrops, peppermints, wrapped candies, jellybeans, lollipops, candy canes, chocolates, marshmallows, bonbons.
- **Forest:** Acorns, pinecones, oak leaves, mushrooms, toadstools, berries, twigs, owls, foxes, hedgehogs, leaves.
- **Ocean:** Starfish, seahorses, clams, jellyfish, conches, pearls, corals, tropical shells, octopuses, sea turtles.
- **Space:** Planets, rings, rocket ships, asteroids, alien saucers, comets, satellites, nebulae, astronauts, stars.

---

### 2.2 Premium Themes (9 Themes × 16 Tiles = 144 Tiles)
AI-generated via Gemini 3.1 Flash Image Preview (Emergent Agent platform), delivered as clean transparent WebP icons.

| Theme Key | Tile ID Pattern | Count | Dimensions | Format | Mode | Size Range (KB) | Disk Path |
|---|---|---|---|---|---|---|---|
| `alpine_winter` | `alpine_winter_0` .. `15` | 16 | 256×256 | WebP | RGBA | 7.9 – 14.8 | `assets/tiles/alpine_winter/{0..15}.webp` |
| `autumn_harvest`| `autumn_harvest_0` .. `15` | 16 | 256×256 | WebP | RGBA | 6.4 – 11.2 | `assets/tiles/autumn_harvest/{0..15}.webp` |
| `bakery` | `bakery_0` .. `bakery_15` | 16 | 256×256 | WebP | RGBA | 6.8 – 14.9 | `assets/tiles/bakery/{0..15}.webp` |
| `fruit` | `fruit_0` .. `fruit_15` | 16 | 256×256 | WebP | RGBA | 7.3 – 13.9 | `assets/tiles/fruit/{0..15}.webp` |
| `galaxy` | `galaxy_0` .. `galaxy_15` | 16 | 256×256 | WebP | RGBA | 6.5 – 12.8 | `assets/tiles/galaxy/{0..15}.webp` |
| `magic_atelier` | `magic_atelier_0` .. `15` | 16 | 256×256 | WebP | RGBA | 7.2 – 14.2 | `assets/tiles/magic_atelier/{0..15}.webp` |
| `meadow` | `meadow_0` .. `meadow_15` | 16 | 256×256 | WebP | RGBA | 7.6 – 15.6 | `assets/tiles/meadow/{0..15}.webp` |
| `retro_arcade` | `retro_arcade_0` .. `15` | 16 | 256×256 | WebP | RGBA | 5.2 – 10.4 | `assets/tiles/retro_arcade/{0..15}.webp` |
| `sakura` | `sakura_0` .. `sakura_15` | 16 | 256×256 | WebP | RGBA | 6.8 – 16.6 | `assets/tiles/sakura/{0..15}.webp` |

#### Premium Tile Subject Matter
- **Alpine Winter:** Mittens, woolen beanies, snow sleds, ice skates, thermos, hot cocoa mugs, pine sprigs, snow goggles.
- **Autumn Harvest:** Pumpkins, squash, golden corn, harvest baskets, red apples, maple leaves, acorns, scarecrow hats.
- **Bakery:** Croissants, baguettes, pretzel twists, cupcakes, strawberry tarts, cinnamon rolls, flour sacks, rolling pins.
- **Fruit:** Apples, bananas, oranges, watermelons, grapes, cherries, lemons, pineapples, kiwis, strawberries.
- **Galaxy:** Cosmic vortex, spiral galaxy, ringed planet, pulsar, solar flare, deep space telescope, cosmic crystals.
- **Magic Atelier:** Spellbooks, crystal balls, glowing vials, magic wands, cauldron, tarot cards, feathers, witch hats.
- **Meadow:** Daisies, honey bees, ladybugs, butterflies, clovers, bluebells, dandelions, caterpillars, snails.
- **Retro Arcade:** Joysticks, arcade cabinets, game cartridges, pixel ghosts, gamepads, CRT monitors, high score trophies.
- **Sakura:** Cherry blossoms, paper lanterns, wagasa umbrellas, folding fans, tea sets, koi fish, torii gates, origami cranes.

---

### 2.3 Reward Themes (5 Themes × 10 Tiles = 50 Tiles)
Regenerated in commit `bd4e536` to eliminate baked-in checkerboards and uneven crops found in upstream `connectify-new`. All tiles are normalized to centered 512×512 canvases with $\ge 40$px uniform edge margins.

| Theme Key | Tile ID Pattern | Count | Dimensions | Format | Mode | Size Range (KB) | Disk Path |
|---|---|---|---|---|---|---|---|
| `reward_bakery` | `reward_bakery_0` .. `9` | 10 | 512×512 | WebP | RGBA | 66.8 – 203.4 | `assets/tiles/reward_bakery/{0..9}.webp` |
| `reward_fruit` | `reward_fruit_0` .. `9` | 10 | 512×512 | WebP | RGBA | 78.4 – 156.2 | `assets/tiles/reward_fruit/{0..9}.webp` |
| `reward_galaxy` | `reward_galaxy_0` .. `9` | 10 | 512×512 | WebP | RGBA | 99.4 – 247.8 | `assets/tiles/reward_galaxy/{0..9}.webp` |
| `reward_meadow` | `reward_meadow_0` .. `9` | 10 | 512×512 | WebP | RGBA | 24.3 – 201.2 | `assets/tiles/reward_meadow/{0..9}.webp` |
| `reward_sakura` | `reward_sakura_0` .. `9` | 10 | 512×512 | WebP | RGBA | 20.0 – 75.1 | `assets/tiles/reward_sakura/{0..9}.webp` |

---

### 2.4 Beach / Vanilla Theme (15 Tiles)
Legacy default theme extracted from upstream `src/assets/vanilla/tiles/*.png`. Mapped as the fallback for `'vanilla'` and `'sweets'`.

| Tile ID | File Name | Dimensions | Format | Mode | Size (KB) | Subject Description |
|---|---|---|---|---|---|---|
| `beach_ball` | `beach_ball.png` | 256×256 | PNG | Mode P | 17.5 | Striped inflatable beach ball |
| `beach_hat` | `beach_hat.png` | 256×256 | PNG | Mode P | 19.1 | Straw sun hat with blue ribbon |
| `camera` | `camera.png` | 256×256 | PNG | Mode P | 16.9 | Vintage tourist camera |
| `coconut` | `coconut.png` | 256×256 | PNG | Mode P | 20.1 | Coconut with drinking straw and flower |
| `crab` | `crab.png` | 256×256 | PNG | Mode P | 22.4 | Friendly red beach crab |
| `ice_cream` | `ice_cream.png` | 256×256 | PNG | Mode P | 17.6 | Triple-scoop ice cream cone |
| `sandal` | `sandal.png` | 256×256 | PNG | Mode P | 19.8 | Summer flip-flop / sandal |
| `seagull` | `seagull.png` | 256×256 | PNG | Mode P | 20.8 | Coastal seagull bird |
| `seashell` | `seashell.png` | 256×256 | PNG | Mode P | 18.5 | Spiral nautical seashell |
| `summer_shirt` | `summer_shirt.png` | 256×256 | PNG | Mode P | 21.0 | Hawaiian floral summer shirt |
| `summer_shorts`| `summer_shorts.png`| 256×256 | PNG | Mode P | 22.4 | Board shorts / swimwear |
| `sunglasses` | `sunglasses.png` | 256×256 | PNG | Mode P | 18.1 | UV protection sunglasses |
| `swim_ring` | `swim_ring.png` | 256×256 | PNG | Mode P | 21.8 | Red-and-white life buoy / float ring |
| `tropical_fish` | `tropical_fish.png` | 256×256 | PNG | Mode P | 18.2 | Yellow and blue reef fish |
| `tropical_juice`| `tropical_juice.png`| 256×256 | PNG | Mode P | 17.1 | Fruity cocktail glass with umbrella |

---

## 3. Master Atlases (`assets/atlases/`, 9 Files)

Composite sheets used to extract base and reward tiles. Ideal for GPU texture atlasing or sprite batching.

| File Name | Grid | Dimensions | Format | Mode | Size (KB) | Content Description |
|---|---|---|---|---|---|---|
| `candy-transparent.png` | 4×4 | 1254×1254 | PNG | RGBA | 934.0 | 16 Candy tiles with transparent alpha background |
| `forest-transparent.png` | 4×4 | 1254×1254 | PNG | RGBA | 1,071.9 | 16 Forest tiles with transparent alpha background |
| `ocean-transparent.png` | 4×4 | 1254×1254 | PNG | RGBA | 1,069.1 | 16 Ocean tiles with transparent alpha background |
| `space-transparent.png` | 4×4 | 1254×1254 | PNG | RGBA | 1,094.1 | 16 Space tiles with transparent alpha background |
| `reward-bakery-transparent.png` | 5×2 | 1983×793 | PNG | RGBA | 1,249.7 | 10 Regenerated Reward Bakery icons |
| `reward-fruit-transparent.png` | 5×2 | 1983×793 | PNG | RGBA | 992.3 | 10 Regenerated Reward Fruit icons |
| `reward-galaxy-transparent.png` | 5×2 | 1983×793 | PNG | RGBA | 1,429.0 | 10 Regenerated Reward Galaxy icons |
| `reward-meadow-transparent.png` | 5×2 | 1983×793 | PNG | RGBA | 1,144.4 | 10 Regenerated Reward Meadow icons |
| `reward-sakura-transparent.png` | 5×2 | 1983×793 | PNG | RGBA | 1,102.0 | 10 Regenerated Reward Sakura icons |

---

## 4. Tile Surfaces, Effects, and Tools

### 4.1 Tile Surfaces (`assets/surfaces/`, 13 Files)
Textured ceramic/clay backplates designed to sit behind tile icons or represent empty/blocked grid cells.

| File Name | Dimensions | Format | Mode | Size (KB) | Visual Description |
|---|---|---|---|---|---|
| `amber.webp` | 256×256 | WebP | RGBA | 5.3 | Translucent warm honey/amber resin surface |
| `candy.webp` | 256×256 | WebP | RGBA | 8.8 | Soft pastel pink sugar glaze surface |
| `cloud.webp` | 256×256 | WebP | RGBA | 9.1 | Fluffy light cumulus white/sky blue surface |
| `cream.webp` | 256×256 | WebP | RGBA | 7.8 | Matte off-white ivory ceramic surface |
| `enchanted.webp`| 256×256 | WebP | RGBA | 8.1 | Mystical violet glow surface with sparkles |
| `frost.webp` | 256×256 | WebP | RGBA | 5.0 | Crystalline chilled pale ice surface |
| `jelly.webp` | 256×256 | WebP | RGBA | 6.4 | Vibrant translucent gelatinous ruby surface |
| `mint.webp` | 256×256 | WebP | RGBA | 7.6 | Calming pale spearmint ceramic surface |
| `night.webp` | 256×256 | WebP | RGBA | 6.6 | Deep indigo star-dusted midnight surface |
| `opal.webp` | 256×256 | WebP | RGBA | 5.8 | Iridescent pearl-toned shimmering surface |
| `pixel.webp` | 256×256 | WebP | RGBA | 6.0 | Retro low-fi 8-bit grid pattern surface |
| `sunset.webp` | 256×256 | WebP | RGBA | 7.9 | Warm orange/coral gradient dusk surface |
| `wood.webp` | 256×256 | WebP | RGBA | 10.6 | Polished natural timber grain surface |

### 4.2 Effects (`assets/effects/`, 7 Files)
Static visual particle and ambient effect textures.

| File Name | Dimensions | Format | Mode | Size (KB) | Effect Description |
|---|---|---|---|---|---|
| `autumn.webp` | 256×256 | WebP | RGBA | 7.3 | Swirling golden autumn leaves |
| `blossom.webp` | 256×256 | WebP | RGBA | 6.4 | Floating sakura blossom petals |
| `confetti.webp`| 256×256 | WebP | RGBA | 9.7 | Multicolored victory celebration ribbons |
| `magic.webp` | 256×256 | WebP | RGBA | 7.2 | Arcane purple magic sparkles & stars |
| `pixel.webp` | 256×256 | WebP | RGBA | 4.4 | Chiptune 8-bit sparkle particle burst |
| `snow.webp` | 256×256 | WebP | RGBA | 10.8 | Delicate crystalline falling snowflakes |
| `stars.webp` | 256×256 | WebP | RGBA | 5.6 | Shimmering golden four-point stars |

### 4.3 Tools & Boosters (`assets/tools/`, 4 Files)

| File Name | Dimensions | Format | Mode | Size (KB) | Purpose & Usage |
|---|---|---|---|---|---|
| `hint.webp` | 256×256 | WebP | RGBA | 9.2 | Lightbulb booster icon (reveals legal Onet match) |
| `shuffle.webp` | 256×256 | WebP | RGBA | 9.4 | Circular arrows booster icon (reshuffles board) |
| `onet.webp` | 256×256 | WebP | RGBA | 10.6 | Mode selector icon for Onet Connect |
| `triple.webp` | 256×256 | WebP | RGBA | 9.6 | Mode selector icon for Triple Match |

---

## 5. Cozy Ceramic UI Kit (`assets/ui-kit/`, 145 Files)

A design-system-driven visual kit featuring 8 calm matte ceramic palettes:
`ivory`, `mint`, `peach`, `lavender`, `sky`, `rose`, `sand`, and `slate`.

### 5.1 Ready-to-Use UI Assets (122 Files)

#### A. Tile Bodies (32 Files)
4 aspect ratios across all 8 ceramic palettes:
- `square-*` (1×1 tile body): 512×512, RGBA WebP (~104 KB each)
- `large-*` (2×2 tile body): 768×768, RGBA WebP (~216 KB each)
- `horizontal-*` (2×1 domino tile body): 768×384, RGBA WebP (~166 KB each)
- `vertical-*` (1×2 domino tile body): 384×768, RGBA WebP (~113 KB each)

#### B. Trays & Slots (16 Files)
- **Trays (8 Files):** Empty ceramic tray container bases without embedded slots (`1024x320`, RGBA WebP, ~135 KB each). Available in all 8 palettes.
- **Slots (8 Files):** Beveled recessed ceramic slot cavities (`384x384`, RGBA WebP, ~73 KB each). Layered on top of trays according to game mode capacity (7 for Triple Match).

#### C. Panels (8 Files)
Blank ceramic modal dialogue boxes and scorecard frames (`768x512`, RGBA WebP, ~165 KB each).

#### D. Results & Modals (6 Files)
Illustrated outcome ceramic headers (`768x768`, RGBA WebP):
- `new-level.webp` (272 KB): New stage unlock ribbon & ceramic star
- `pause.webp` (191 KB): Calming zen pause tea/stones illustration
- `reward.webp` (310 KB): Ceramic gift chest bursting with sparkle
- `time-up.webp` (299 KB): Sand timer / hourglass warning
- `tray-full.webp` (314 KB): Overflow warning sign
- `victory.webp` (308 KB): Ceramic laurel wreath & golden trophy

#### E. Backgrounds (8 Files, Opaque RGB)
- **Portrait (4 Files, 1024×1536):** `cozy-room-portrait.webp`, `zen-garden-portrait.webp`, `spa-portrait.webp`, `sakura-portrait.webp` (~150–198 KB each).
- **Landscape (4 Files, 1536×1024):** `clouds-landscape.webp`, `lakeside-landscape.webp`, `night-landscape.webp`, `sunset-landscape.webp` (~101–199 KB each).

#### F. Decorative Ornaments (16 Files, 512×512 RGBA)
Corner and edge decorations: `autumn-edge`, `bamboo-corner`, `candle-towels`, `cloud-wisps`, `eucalyptus-edge`, `fern-stones`, `frosted-pine`, `gold-ribbon`, `lantern-branch`, `lavender-corner`, `linen-corner`, `lotus-edge`, `sakura-branch`, `sand-ripples`, `succulent-cluster`, `zen-stones` (~71–183 KB each).

#### G. Animations & Effects
- **Animated WebP (4 Files, 256×256):** `hint-pulse.webp` (looping, 398 KB), `match-burst.webp` (244 KB), `tile-pop-dust.webp` (340 KB), `victory-confetti.webp` (381 KB). All 8 frames, 80ms/frame, 640ms total duration.
- **Individual PNG Frames (32 Files, 256×256 RGBA):** 8 numbered frames (`00.png` .. `07.png`) for each of the 4 effects.
- **Sprite Strips (4 Files, 2048×256 RGBA):** Horizontal 8-frame composite strips for engines without animated WebP decoders (`hint-pulse-strip.png`, `match-burst-strip.png`, `tile-pop-dust-strip.png`, `victory-confetti-strip.png`).

### 5.2 UI Kit Sources & Atlases (18 Files)
- `assets/ui-kit/source-atlases/`: 10 master PNG sheets (`decorations.png`, `effects-sequence.png`, `panels.png`, `results.png`, `slots.png`, `tiles-horizontal.png`, `tiles-large.png`, `tiles-square.png`, `tiles-vertical.png`, `trays.png`).
- `assets/ui-kit/source-backgrounds/`: 8 uncompressed master PNG backgrounds (`bg-*.png`).

---

## 6. Archival Originals (`assets/originals/`, 133 Files)

Archived upstream source files retained for cryptographic provenance and rollback verification:
1. `assets/originals/public/art/` (118 Files):
   - 4 base theme directories (`candy`, `forest`, `ocean`, `space`): 16 original 220×220 WebPs + 1 master `sheet.webp` per theme = 68 files.
   - 5 legacy reward theme directories (`bakery`, `fruit`, `galaxy`, `meadow`, `sakura`): 10 original 512×512 WebPs containing baked checkerboard artifacts = 50 files.
2. `assets/originals/scripts/art-originals/` (15 Files):
   - Uncut 1024×1024 RGB JPG sheets generated during the original Emergent Agent Gemini API jobs.

---

## 7. Quality & Integrity Audit Sign-Off

- **Cryptographic Audit:** 100% of files match their recorded SHA-256 digests in `provenance.json`, `transparency.json`, and `manifest.json`.
- **Dimensions & Visual Quality:** No corrupted headers, zero truncated bitstreams, valid alpha channels across all sprites.
- **Engine Porting Readiness:** Tiles are uniformly dimensioned (Base: ~314px, Premium: 256px, Reward: 512px) and centered, making them immediately compatible with libGDX `TextureRegion` slicing and packing.

*Inventory verified by AGENT 01 (`@auditor`)*
