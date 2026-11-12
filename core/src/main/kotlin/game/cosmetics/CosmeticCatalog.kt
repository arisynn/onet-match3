package game.cosmetics

/**
 * Static catalog of all cosmetic items in Onet + Triple Match.
 * Built directly from repo assets in assets/tiles/, assets/surfaces/, assets/ui-kit/.
 */
object CosmeticCatalog {

    // --- FREE STARTER THEMES ---
    val THEME_CANDY = CosmeticItem(
        id = "theme_candy",
        name = "Sweet Candyland",
        description = "Charming confectionary tiles with vibrant sugary glazes.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.FREE,
        rarity = CosmeticRarity.COMMON,
        gemPrice = 0,
        assetPath = "assets/tiles/candy"
    )

    val THEME_OCEAN = CosmeticItem(
        id = "theme_ocean",
        name = "Deep Ocean",
        description = "Cool aquatic tiles reminiscent of sea glass and gentle tides.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.FREE,
        rarity = CosmeticRarity.COMMON,
        gemPrice = 0,
        assetPath = "assets/tiles/ocean"
    )

    val THEME_FOREST = CosmeticItem(
        id = "theme_forest",
        name = "Verdant Forest",
        description = "Lush botanical motifs inspired by mossy woodland paths.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.FREE,
        rarity = CosmeticRarity.COMMON,
        gemPrice = 0,
        assetPath = "assets/tiles/forest"
    )

    val THEME_SPACE = CosmeticItem(
        id = "theme_space",
        name = "Deep Cosmos",
        description = "Midnight celestial tiles celebrating distant galaxies and starlight.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.FREE,
        rarity = CosmeticRarity.COMMON,
        gemPrice = 0,
        assetPath = "assets/tiles/space"
    )

    val THEME_BEACH = CosmeticItem(
        id = "theme_beach",
        name = "Sunlit Beach",
        description = "Warm coastal memories with shells, sandals, and summer vibes.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.FREE,
        rarity = CosmeticRarity.COMMON,
        gemPrice = 0,
        assetPath = "assets/tiles/beach"
    )

    // --- EARNABLE REWARD THEMES (PURCHASED WITH GEMS) ---
    val THEME_REWARD_BAKERY = CosmeticItem(
        id = "theme_reward_bakery",
        name = "Patisserie Bliss",
        description = "Artisanal croissants, macarons, and warm oven delights.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.RARE,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.RARE),
        assetPath = "assets/tiles/reward_bakery"
    )

    val THEME_REWARD_FRUIT = CosmeticItem(
        id = "theme_reward_fruit",
        name = "Fresh Orchard",
        description = "Ripe berries, zesty citrus, and sun-ripened orchard fruits.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.RARE,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.RARE),
        assetPath = "assets/tiles/reward_fruit"
    )

    val THEME_REWARD_GALAXY = CosmeticItem(
        id = "theme_reward_galaxy",
        name = "Nebula Dreams",
        description = "Ethereal cosmic bodies floating across deep violet nebulae.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.EPIC,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.EPIC),
        assetPath = "assets/tiles/reward_galaxy"
    )

    val THEME_REWARD_MEADOW = CosmeticItem(
        id = "theme_reward_meadow",
        name = "Wildflower Meadow",
        description = "Delicate petals and peaceful springtime blossoms.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.RARE,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.RARE),
        assetPath = "assets/tiles/reward_meadow"
    )

    val THEME_REWARD_SAKURA = CosmeticItem(
        id = "theme_reward_sakura",
        name = "Cherry Blossom Shrine",
        description = "Serene pastel cherry petals dancing on a calm breeze.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.EPIC,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.EPIC),
        assetPath = "assets/tiles/reward_sakura"
    )

    // --- FREE STARTER UI PALETTES ---
    val PALETTE_IVORY = CosmeticItem(
        id = "palette_ivory",
        name = "Classic Ivory",
        description = "Warm unglazed porcelain tone providing natural contrast.",
        type = CosmeticType.UI_PALETTE,
        acquisitionType = CosmeticAcquisitionType.FREE,
        rarity = CosmeticRarity.COMMON,
        gemPrice = 0,
        assetPath = "assets/ui-kit/panels/ivory.webp"
    )

    val PALETTE_MINT = CosmeticItem(
        id = "palette_mint",
        name = "Sage Mint",
        description = "Refreshing soft green ceramic glaze.",
        type = CosmeticType.UI_PALETTE,
        acquisitionType = CosmeticAcquisitionType.FREE,
        rarity = CosmeticRarity.COMMON,
        gemPrice = 0,
        assetPath = "assets/ui-kit/panels/mint.webp"
    )

    val PALETTE_PEACH = CosmeticItem(
        id = "palette_peach",
        name = "Warm Peach",
        description = "Comforting clay warmth infused with soft coral hues.",
        type = CosmeticType.UI_PALETTE,
        acquisitionType = CosmeticAcquisitionType.FREE,
        rarity = CosmeticRarity.COMMON,
        gemPrice = 0,
        assetPath = "assets/ui-kit/panels/peach.webp"
    )

    // --- EARNABLE UI PALETTES (PURCHASED WITH GEMS) ---
    val PALETTE_LAVENDER = CosmeticItem(
        id = "palette_lavender",
        name = "Muted Lavender",
        description = "Delicate purple haze glaze creating a soothing ambiance.",
        type = CosmeticType.UI_PALETTE,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.RARE,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.RARE),
        assetPath = "assets/ui-kit/panels/lavender.webp"
    )

    val PALETTE_SKY = CosmeticItem(
        id = "palette_sky",
        name = "Cerulean Sky",
        description = "Clear morning azure ceramic finish.",
        type = CosmeticType.UI_PALETTE,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.RARE,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.RARE),
        assetPath = "assets/ui-kit/panels/sky.webp"
    )

    val PALETTE_ROSE = CosmeticItem(
        id = "palette_rose",
        name = "Dusk Rose",
        description = "Rich terracotta rose glaze with subtle mineral specks.",
        type = CosmeticType.UI_PALETTE,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.RARE,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.RARE),
        assetPath = "assets/ui-kit/panels/rose.webp"
    )

    val PALETTE_SAND = CosmeticItem(
        id = "palette_sand",
        name = "Desert Sand",
        description = "Earthy sandstone texture reminiscent of warm pottery kilns.",
        type = CosmeticType.UI_PALETTE,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.RARE,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.RARE),
        assetPath = "assets/ui-kit/panels/sand.webp"
    )

    val PALETTE_SLATE = CosmeticItem(
        id = "palette_slate",
        name = "Charcoal Slate",
        description = "Elegant dark ceramic finish with soft specular sheen.",
        type = CosmeticType.UI_PALETTE,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.EPIC,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.EPIC),
        assetPath = "assets/ui-kit/panels/slate.webp"
    )

    // --- TILE BODIES ---
    val BODY_SQUARE_IVORY = CosmeticItem(
        id = "body_square_ivory",
        name = "Square Ivory Tile",
        description = "Standard 1x1 tactile square ceramic body.",
        type = CosmeticType.TILE_BODY,
        acquisitionType = CosmeticAcquisitionType.FREE,
        rarity = CosmeticRarity.COMMON,
        gemPrice = 0,
        assetPath = "assets/ui-kit/tile-bodies/square-ivory.webp"
    )

    val BODY_HORIZONTAL_MINT = CosmeticItem(
        id = "body_horizontal_mint",
        name = "Mint Domino Horizontal",
        description = "Horizontal ceramic domino body in soft sage mint.",
        type = CosmeticType.TILE_BODY,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.COMMON,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.COMMON),
        assetPath = "assets/ui-kit/tile-bodies/horizontal-mint.webp"
    )

    val BODY_VERTICAL_PEACH = CosmeticItem(
        id = "body_vertical_peach",
        name = "Peach Domino Vertical",
        description = "Vertical ceramic domino body in comforting warm peach.",
        type = CosmeticType.TILE_BODY,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.COMMON,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.COMMON),
        assetPath = "assets/ui-kit/tile-bodies/vertical-peach.webp"
    )

    val BODY_LARGE_ROSE = CosmeticItem(
        id = "body_large_rose",
        name = "Large Rose Plaque",
        description = "Substantial 2x2 ceramic plaque with tactile beveled edges.",
        type = CosmeticType.TILE_BODY,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.EPIC,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.EPIC),
        assetPath = "assets/ui-kit/tile-bodies/large-rose.webp"
    )

    val BODY_LARGE_SLATE = CosmeticItem(
        id = "body_large_slate",
        name = "Large Slate Plaque",
        description = "Substantial 2x2 charcoal ceramic plaque.",
        type = CosmeticType.TILE_BODY,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.EPIC,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.EPIC),
        assetPath = "assets/ui-kit/tile-bodies/large-slate.webp"
    )

    // --- EFFECTS & ANIMATIONS ---
    val EFFECT_MATCH_BURST = CosmeticItem(
        id = "effect_match_burst",
        name = "Match Burst",
        description = "Joyous burst of colorful ceramic dust upon forming a match.",
        type = CosmeticType.EFFECT,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.RARE,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.RARE),
        assetPath = "assets/ui-kit/animations/match-burst.webp"
    )

    val EFFECT_TILE_POP_DUST = CosmeticItem(
        id = "effect_tile_pop_dust",
        name = "Tile Pop Dust",
        description = "Soft puff of clay powder as paired tiles dissolve.",
        type = CosmeticType.EFFECT,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.RARE,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.RARE),
        assetPath = "assets/ui-kit/animations/tile-pop-dust.webp"
    )

    val EFFECT_HINT_PULSE = CosmeticItem(
        id = "effect_hint_pulse",
        name = "Hint Pulse",
        description = "Radiant gentle ripple guiding your eyes to matching pairs.",
        type = CosmeticType.EFFECT,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.RARE,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.RARE),
        assetPath = "assets/ui-kit/animations/hint-pulse.webp"
    )

    val EFFECT_VICTORY_CONFETTI = CosmeticItem(
        id = "effect_victory_confetti",
        name = "Victory Confetti",
        description = "Celebratory cascade of pastel confetti upon clearing a stage.",
        type = CosmeticType.EFFECT,
        acquisitionType = CosmeticAcquisitionType.EARNABLE,
        rarity = CosmeticRarity.EPIC,
        gemPrice = game.economy.EconomyTuning.price(CosmeticRarity.EPIC),
        assetPath = "assets/ui-kit/animations/victory-confetti.webp"
    )

    // --- PREMIUM MASTERPIECE THEMES (DIRECT IN-APP ENTITLEMENTS) ---
    val THEME_PREMIUM_AUTUMN = CosmeticItem(
        id = "theme_autumn_harvest",
        name = "Autumn Harvest",
        description = "Golden maple leaves, warm pumpkins, and crisp fall charm.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.PREMIUM,
        rarity = CosmeticRarity.LEGENDARY,
        gemPrice = 0,
        iapSku = "entitlement_autumn_harvest",
        assetPath = "assets/tiles/autumn_harvest"
    )

    val THEME_PREMIUM_ATELIER = CosmeticItem(
        id = "theme_magic_atelier",
        name = "Magic Atelier",
        description = "Glowing arcane crystals, spellbooks, and starlight vials.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.PREMIUM,
        rarity = CosmeticRarity.LEGENDARY,
        gemPrice = 0,
        iapSku = "entitlement_magic_atelier",
        assetPath = "assets/tiles/magic_atelier"
    )

    val THEME_PREMIUM_ARCADE = CosmeticItem(
        id = "theme_retro_arcade",
        name = "Retro Arcade",
        description = "Nostalgic 8-bit neon sprites and retro arcade memories.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.PREMIUM,
        rarity = CosmeticRarity.LEGENDARY,
        gemPrice = 0,
        iapSku = "entitlement_retro_arcade",
        assetPath = "assets/tiles/retro_arcade"
    )

    val THEME_PREMIUM_ALPINE = CosmeticItem(
        id = "theme_alpine_winter",
        name = "Alpine Winter",
        description = "Crisp snowflakes, cozy mittens, and snow-dusted firs.",
        type = CosmeticType.TILE_SET,
        acquisitionType = CosmeticAcquisitionType.PREMIUM,
        rarity = CosmeticRarity.LEGENDARY,
        gemPrice = 0,
        iapSku = "entitlement_alpine_winter",
        assetPath = "assets/tiles/alpine_winter"
    )

    val ALL_ITEMS: List<CosmeticItem> = listOf(
        // Free Themes
        THEME_CANDY, THEME_OCEAN, THEME_FOREST, THEME_SPACE, THEME_BEACH,
        // Free Palettes & Bodies
        PALETTE_IVORY, PALETTE_MINT, PALETTE_PEACH, BODY_SQUARE_IVORY,
        // Earnable Themes
        THEME_REWARD_BAKERY, THEME_REWARD_FRUIT, THEME_REWARD_GALAXY, THEME_REWARD_MEADOW, THEME_REWARD_SAKURA,
        // Earnable Palettes
        PALETTE_LAVENDER, PALETTE_SKY, PALETTE_ROSE, PALETTE_SAND, PALETTE_SLATE,
        // Earnable Bodies
        BODY_HORIZONTAL_MINT, BODY_VERTICAL_PEACH, BODY_LARGE_ROSE, BODY_LARGE_SLATE,
        // Earnable Effects
        EFFECT_MATCH_BURST, EFFECT_TILE_POP_DUST, EFFECT_HINT_PULSE, EFFECT_VICTORY_CONFETTI,
        // Premium Themes
        THEME_PREMIUM_AUTUMN, THEME_PREMIUM_ATELIER, THEME_PREMIUM_ARCADE, THEME_PREMIUM_ALPINE
    )

    val DEFAULT_UNLOCKED_IDS: Set<String> = ALL_ITEMS
        .filter { it.acquisitionType == CosmeticAcquisitionType.FREE }
        .map { it.id }
        .toSet()

    fun getItem(id: String): CosmeticItem? = ALL_ITEMS.find { it.id == id }

    fun totalEarnableGemSink(): Int = ALL_ITEMS
        .filter { it.acquisitionType == CosmeticAcquisitionType.EARNABLE }
        .sumOf { it.gemPrice }
}
