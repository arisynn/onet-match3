# Game Design Document (GDD): Onet + Triple Match

**Project Name:** Onet + Triple Match  
**Platform:** Android (Google Play) / Desktop (Headless & Testing)  
**Engine & Tech Stack:** Kotlin 2.0+, libGDX 1.13.1, Clean Architecture (Zero-Dependency Pure Kotlin Domain)  
**Visual Style:** Cozy Ceramic UI Kit (tactile ceramic tiles, soft pastel palettes, fluid animations)  
**Target Audience:** Casual & Mid-core puzzle enthusiasts seeking relaxing yet mentally engaging gameplay without predatory monetization.

---

## 1. Executive Summary & Vision

*Onet + Triple Match* is a premium-feel, dual-mode puzzle game combining two beloved mechanics into a unified, tactile experience:
1. **Onet (Link Matching / Mahjong Connect):** Pair matching on a grid where tiles must connect through an orthogonal path with no more than two 90-degree turns.
2. **Triple Match (Tray 3-Match):** Multi-layered spatial puzzle where tiles are picked from stacks into a 7-slot tray, matching trios to clear space before the tray overflows.

### Core Pillars
- **Zen & Tactile Satisfaction:** Beautiful Cozy Ceramic aesthetics, responsive animations, and crisp soundscapes produce a deeply calming flow state.
- **Ethical, Transparent Economy:** Strictly governed by **The 10 Non-Negotiable Currency Rules**. Zero pay-to-win, zero energy systems, zero gambling/loot boxes, and zero consumable currency purchases.
- **Durable Local-First Progression:** An immutable, idempotent transaction ledger and local event-sourcing guarantee tamper-resistant progression offline and online.

---

## 2. Core Game Modes & Mechanics

### 2.1 Onet Mode (Connect-2)
- **Board Grid:** 10 columns × 6 rows (60 tile cells total).
- **Tile Distribution:** 30 pairs (or multiples of 2), selected from active theme tile sets (16 distinct tiles per theme, or 10 for reward themes).
- **Matching Rules:**
  - Two matching tiles can be eliminated if connected by an orthogonal path containing **at most two 90-degree turns** (Path segments $\le 3$).
  - The path can traverse empty grid cells and travel through the border space outside the active grid.
- **Board Operations:**
  - **Pathfinding (`findPath`):** Deterministic BFS/Raycast checking 0-turn (straight line), 1-turn (L-shape), and 2-turn (Z/U-shape) connections.
  - **Hint (`hintFor`):** Identifies an available valid pair and highlights the connection path.
  - **Deadlock Detection & Shuffle (`needsShuffle`, `shuffleRound`):** When zero valid moves remain on the board, tiles are deterministically reshuffled to guarantee at least one valid match.
  - **Gravity / Edge Shifts:** Optional round variations with shifting tiles (falling down, sliding left/right, inward pull).

### 2.2 Triple Match Mode (Tray-Based 3-Match)
- **Board Structure:** 12 stacks arranged in a multi-layered configuration, each containing 3 tiles (36 tiles total, forming exactly 12 trios).
- **Tray Capacity:** Exactly 7 slots.
- **Matching Rules:**
  - The player taps an uncovered tile from the top of any stack; the tile transfers to the leftmost available slot in the tray.
  - Matching tiles in the tray cluster together.
  - When **3 identical tiles** occupy the tray, they immediately resolve into a trio match and are removed.
  - **Crucial Rule:** Trio resolution evaluates **before** checking tray overflow. If picking a tile creates a trio in the 7th slot, the match resolves successfully rather than triggering a game over.
  - **Overflow Condition:** If all 7 slots are occupied without forming a trio, the round ends in failure.
- **Undo Mechanic:** Single-use undo allows reverting the most recent tile pick back to its original stack.

---

## 3. The 10 Non-Negotiable Currency Rules

To protect player trust, prevent addictive dark patterns, and ensure regulatory compliance, the game enforces 10 strict economic invariants at the domain engine level:

1. **Gems Earned Exclusively Through Active Claims:**  
   Gems (the sole soft currency) can **ONLY** be credited via explicit player claims on completed Daily/Weekly Missions and Achievement Tiers.
2. **Zero Direct Gameplay Currency:**  
   Completing a level, clearing a round, or scoring combos yields **0 direct gems and 0 coins**. Gameplay rewards progression XP and mission objective progress only.
3. **Zero Real-Money Currency Sales:**  
   The in-game store **never** sells currency bundles (e.g., "500 Gems for $1.99" is strictly forbidden). Real money is strictly reserved for non-consumable cosmetic entitlements.
4. **Immutable Idempotent Transaction Ledger:**  
   Every balance change is recorded in an append-only transaction ledger with a unique UUID, timestamp, source audit trail, and cryptographic idempotency key. Duplicate transaction processing is rejected as a no-op.
5. **Strict Non-Negative Balance Invariant:**  
   Wallet balances can never drop below zero ($Balance \ge 0$). Debit operations atomically check balance sufficiency and abort if funds are inadequate.
6. **Zero Pay-to-Win Sinks or Boosters for Currency:**  
   Game boosters (hints, shuffles, undos) are core gameplay mechanics managed via cooldowns or round allocations. They cannot be purchased with gems or real money.
7. **Deterministic, Transparent Rewards:**  
   Reward amounts for every mission and achievement tier are statically declared and transparent in the UI. No random drops, gacha, or loot boxes exist in the game.
8. **Offline Integrity & Anti-Duplication:**  
   Mission progress and achievement state are persisted locally with SHA-256 integrity checks and monotonic counters to prevent clock manipulation and duplicate claim exploits.
9. **Non-Expiring Currency:**  
   Earned gems never expire, decay, or reset between seasons, updates, or calendar months.
10. **Strict Cosmetic Separation from Power:**  
    All items in the Cosmetic Catalog (tile sets, UI palettes, tile bodies, backgrounds, visual effects) are 100% visual and decorative. Zero gameplay bonuses, score multipliers, or timers are attached to cosmetics.

---

## 4. Player Progression & Experience (XP) Engine

### 4.1 XP Formula & Level Curves
Players accumulate Experience Points (XP) through gameplay engagement.
- **Onet Completion:** $50 \text{ XP} + (10 \times \text{RemainingTimePct}) + (5 \times \text{MaxCombo})$
- **Triple Match Completion:** $60 \text{ XP} + (15 \times \text{EmptyTrayBonus}) + (5 \times \text{TriosCleared})$
- **Level Requirement Curve:**
  $$\text{RequiredXP}(\text{level}) = \left\lfloor 100 \times \text{level}^{1.35} \right\rfloor$$

### 4.2 Level-Up Philosophy
Leveling up grants **prestige titles, cosmetic unlocks, and badge flair**, but **zero direct currency**. This maintains the integrity of Rule #1 and Rule #2.

---

## 5. Missions System & Rollover Dynamics

### 5.1 Mission Rotation & Cadence
- **Daily Missions (3 Active Slots):**
  - Rotates daily at 00:00 UTC.
  - Reset behavior: Completed but unclaimed missions remain claimable (safeguarding player effort). Incomplete missions roll over into new randomized daily templates.
  - Reward: 15–25 Gems per daily mission (average daily yield: 60 Gems).
- **Weekly Missions (3 Active Slots):**
  - Rotates weekly on Mondays at 00:00 UTC.
  - Longer-horizon challenges (e.g., "Clear 500 Onet pairs", "Finish 15 Triple Match rounds without undo").
  - Reward: 75–120 Gems per weekly mission (average weekly yield: 285 Gems).

### 5.2 Dynamic Mission Objective Categories
1. `MATCH_COUNT`: Total pairs or trios matched.
2. `ROUND_COMPLETIONS`: Clean round completions in Onet or Triple Match.
3. `COMBO_STREAK`: Achieving combos of $\ge 3$ or $\ge 5$.
4. `SPEED_RUN`: Finishing a round with $\ge 50\%$ time remaining.
5. `PERFECT_ROUND`: Finishing without using hints, shuffles, or undos.
6. `THEME_PLAY`: Playing with specific tile themes.

---

## 6. Achievement System: Multi-Tier Mastery

Achievements track lifetime milestones across 5 domains. Each achievement features 5 ascending tiers:
**Bronze (Tier 1) $\rightarrow$ Silver (Tier 2) $\rightarrow$ Gold (Tier 3) $\rightarrow$ Platinum (Tier 4) $\rightarrow$ Diamond (Tier 5)**.

### 6.1 Achievement Catalog Highlights
| ID | Achievement Title | Category | Tier 1 | Tier 2 | Tier 3 | Tier 4 | Tier 5 | Gem Total |
|---|---|---|---|---|---|---|---|---|
| `onet_master` | Onet Virtuoso | Onet | 10 rounds | 50 rounds | 200 rounds | 500 rounds | 1,000 rounds | 350 |
| `triple_master` | Stack Specialist | Triple Match | 10 rounds | 50 rounds | 200 rounds | 500 rounds | 1,000 rounds | 350 |
| `combo_king` | Cadence & Flow | Skill | 5 combo | 10 combo | 15 combo | 20 combo | 25 combo | 275 |
| `flawless_zen` | Untouchable | Finesse | 5 flawless | 25 flawless | 100 flawless | 250 flawless | 500 flawless | 400 |
| `tile_cleanser` | Ceramic Sweeper | Lifetime | 500 tiles | 2,500 tiles | 10,000 tiles | 30,000 tiles | 100,000 tiles | 500 |
| `cosmetic_curator` | Ceramic Collector | Collection | 3 items | 8 items | 15 items | 25 items | 40 items | 325 |

---

## 7. Cosmetic Catalog & Customization System

Customization is divided into 5 visual slots:
1. **Tile Set:** Face artwork rendered on tiles.
2. **Tile Body Shape & Texture:** 1x1 Square, Horizontal Domino, Vertical Domino, Large Tile.
3. **UI Palette:** Ceramic glaze themes (Ivory, Mint, Peach, Lavender, Sky, Rose, Sand, Slate).
4. **Background Artwork:** Calming thematic backdrops (Zen Garden, Ceramic Studio, Celestial Dream, etc.).
5. **Particle Burst Animation:** Visual feedback on matches (Match-Burst, Hint-Pulse, Tile-Pop-Dust, Victory-Confetti).

### 7.1 Tiered Acquisition Model
- **Free Tier (Default):**
  - Tile Sets: Candy, Ocean, Forest, Space, Beach.
  - Palettes: Ivory, Mint, Peach.
  - Tile Body: Square Ivory.
- **Earnable Tier (Purchased via Gems):**
  - Reward Themes: Reward Bakery (200 Gems), Reward Fruit (200 Gems), Reward Galaxy (250 Gems), Reward Meadow (200 Gems), Reward Sakura (250 Gems).
  - Palettes: Lavender (80 Gems), Sky (80 Gems), Rose (100 Gems), Sand (100 Gems), Slate (120 Gems).
  - Tile Bodies: Dominoes & Large formats across ceramic shades (60–120 Gems each).
  - Particle Animations: Match-Burst (150 Gems), Tile-Pop-Dust (150 Gems), Hint-Pulse (120 Gems), Victory-Confetti (200 Gems).
- **Premium Tier (Direct In-App Purchase Entitlements):**
  - Exclusive Masterpiece Themes: Autumn Harvest, Magic Atelier, Retro Arcade, Alpine Winter.
  - Non-consumable entitlements that also grant perpetual Ad-Free status.

---

## 8. Google Play Billing & Ethical Monetization

### 8.1 Non-Consumable Entitlements Only
The billing service accepts strictly non-consumable product definitions (`INAPP` products that do not consume):
- `entitlement_ad_free`: Permanent ad removal across all screens.
- `entitlement_autumn_harvest`: Autumn Harvest Theme Pack + Ad Free.
- `entitlement_magic_atelier`: Magic Atelier Theme Pack + Ad Free.
- `entitlement_retro_arcade`: Retro Arcade Theme Pack + Ad Free.
- `entitlement_alpine_winter`: Alpine Winter Theme Pack + Ad Free.
- `entitlement_collector_bundle`: All 4 Premium Themes + Deluxe Ceramic Frame + Permanent Ad Free.

### 8.2 Architectural Safeguards
- Any attempt to query or purchase consumable SKUs is rejected by `BillingService`.
- Re-querying Google Play Billing restores all owned entitlements upon fresh app install or device transfer.
