# Economy invariants and validation contract

One virtual currency: Gems. Currency is earned **only** by completing and explicitly claiming a valid, unique mission or achievement reward. Gameplay, score, EXP, first wins, level-ups, login, ads, referrals, purchases and paid cosmetics grant zero Gems. Real money never buys currency or gameplay advantages; no substitute premium currency is introduced.

## Initial configurable tuning

Daily missions: 20/30/50 Gems (100 standard daily total). Weekly standard mission budget: approximately 200 Gems, giving approximately 900/week at full standard completion before achievements. Common cosmetics 400–600, rare 1,200–1,800, epic 3,000–4,500, legendary 6,000–9,000. Achievement bands follow the supplied master prompt. These are tuning inputs, not a measured result or an irrevocable price schedule.

## Transaction requirements

- Each ledger record includes unique ID, source type/ID, amount, timestamp, status and relevant save version; restoration validates integrity and economic invariants.
- Amounts and arithmetic are checked. Debits cannot create negative balances; credits accept only mission/achievement claim sources.
- Duplicate claim IDs are idempotent, including after process termination and restoration.
- Mission completion/claim status, ledger delta and acquired inventory must persist as a recoverable unit. A synchronized in-memory method alone is not durable atomicity.
- Catalog lookup decides eligibility and price; caller-supplied item IDs/prices cannot create arbitrary debit/unlock behavior.
- Device-clock rollback cannot freely reset missions. Expiry does not erase earned currency.

## Progression and exploitation

EXP and independent mode unlocks reward play and improvement, not spending. First clear and exceptional play may earn capped XP bonuses; repeated easy-level replay should receive bounded/tapered XP rather than an uncapped farming path. Round-completion IDs prevent rewarding an interrupted outcome twice.

Offline saves and hash chains do not defeat a modified APK, edited device data, restore of an old entire save or all device-clock manipulation. No anti-cheat guarantee is made. Android backup restrictions reduce one rollback path but are not server authority.

## Simulator evidence

The economy specialist owns an actual deterministic simulator for casual/regular/active and new/long-term behavior over 7/30/90/180/365 days. Required metrics: earned/spent/unspent Gems, completion rates, rarity affordability, catalog exhaustion and achievement saturation. Simulation assumptions, seed, caps and output must accompany results. The simulator code is integrated in `2f785d3`, but no new-run numerical results are claimed until the lead actually executes it. Synthetic schedules are design exploration, not observed player behavior.
