# Native game behavior and presentation contract

Two native offline-first modes share an EXP/profile layer and cosmetic collection. No energy system limits play. Cosmetic ownership never changes gameplay difficulty, score, timers, undo, hint availability or XP.

## Onet Connect

Match identical tile groups with a valid route of at most two turns. Retain the reference engine's supported outside-board paths, directional/split rules and boundary behavior. Provide selected/invalid feedback, hints, shuffle, deadlock handling, timer, combo/score, win/loss, restart, pause/resume and deterministic test generation. Initial boards must have a legal solving strategy; animations must not determine validity.

## Triple Match

Accessible stack tops enter a seven-slot tray. Three matching IDs clear automatically. A trio formed by the seventh pick clears before overflow loss is evaluated. Preserve original one-use undo semantics and immutable round transitions. Level-aware generation must supply actual solvability evidence, vary depth/variety/time gradually and introduce tutorial complexity before hard levels. Swap/cascade behavior is excluded.

## Shared progression

Track mode-specific unlocked levels, replay, best scores/stars, tutorial completion and shared EXP. Persist active rounds and settings; returning after pause or interruption must not duplicate completion rewards. The session layer owns record/XP updates and UI reads them. Currency rewards remain claim-only, never victory/level-up rewards.

## Ceramic design direction

The user selected cream, mint and rounded tactile tiles. Use actual Cozy Ceramic source art, crisp font rendering, restrained motion, readable timer/score and clear touch targets. Scale layout and hit geometry together across portrait aspect ratios; respect display cutouts/safe areas. The old functional renderer is not the quality bar.

All 17 requested screens must have actual rendering and usable controls; aliases are not additional screens. Required game feel includes press/selection, valid/invalid match feedback, Onet paths, tray movement/trio removal, combo, reward/achievement/XP/level-up feedback and reduced-motion/effects options. UI specialist handoff must distinguish completed effects from remaining ones.

60 FPS is a performance target, not an established measurement. Emulator screenshots cannot establish physical low-end Android frame pacing or thermal/memory behavior. Such coverage is a release checklist item.
