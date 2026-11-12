package game.billing

/**
 * Non-consumable in-app purchase entitlement.
 * Rule 3: Real-money purchases are restricted exclusively to permanent non-consumable entitlements.
 * Zero currency packs are ever sold.
 */
data class Entitlement(
    val skuId: String,
    val name: String,
    val description: String,
    val unlockedCosmeticIds: List<String>,
    val removesAds: Boolean = true,
    val priceDisplay: String = ""
)

/**
 * Standard non-consumable entitlement catalog.
 */
object EntitlementCatalog {
    val ENTITLEMENT_AD_FREE = Entitlement(
        skuId = "entitlement_ad_free",
        name = "Zen Serenity (Ad Free)",
        description = "Permanently removes all interstitial and banner advertisements.",
        unlockedCosmeticIds = emptyList(),
        removesAds = true,
        priceDisplay = ""
    )

    val ENTITLEMENT_AUTUMN_HARVEST = Entitlement(
        skuId = "entitlement_autumn_harvest",
        name = "Autumn Harvest Pack",
        description = "Unlocks the complete Autumn Harvest theme plus permanent Ad-Free play.",
        unlockedCosmeticIds = listOf("theme_autumn_harvest"),
        removesAds = true,
        priceDisplay = ""
    )

    val ENTITLEMENT_MAGIC_ATELIER = Entitlement(
        skuId = "entitlement_magic_atelier",
        name = "Magic Atelier Pack",
        description = "Unlocks the complete Magic Atelier theme plus permanent Ad-Free play.",
        unlockedCosmeticIds = listOf("theme_magic_atelier"),
        removesAds = true,
        priceDisplay = ""
    )

    val ENTITLEMENT_RETRO_ARCADE = Entitlement(
        skuId = "entitlement_retro_arcade",
        name = "Retro Arcade Pack",
        description = "Unlocks the complete Retro Arcade theme plus permanent Ad-Free play.",
        unlockedCosmeticIds = listOf("theme_retro_arcade"),
        removesAds = true,
        priceDisplay = ""
    )

    val ENTITLEMENT_ALPINE_WINTER = Entitlement(
        skuId = "entitlement_alpine_winter",
        name = "Alpine Winter Pack",
        description = "Unlocks the complete Alpine Winter theme plus permanent Ad-Free play.",
        unlockedCosmeticIds = listOf("theme_alpine_winter"),
        removesAds = true,
        priceDisplay = ""
    )

    val ENTITLEMENT_COLLECTOR_BUNDLE = Entitlement(
        skuId = "entitlement_collector_bundle",
        name = "Grand Ceramic Collector's Bundle",
        description = "Unlocks all 4 Premium Themes, exclusive collector's badge, and permanent Ad-Free play.",
        unlockedCosmeticIds = listOf(
            "theme_autumn_harvest",
            "theme_magic_atelier",
            "theme_retro_arcade",
            "theme_alpine_winter"
        ),
        removesAds = true,
        priceDisplay = ""
    )

    val ALL_ENTITLEMENTS = listOf(
        ENTITLEMENT_AUTUMN_HARVEST,
        ENTITLEMENT_MAGIC_ATELIER,
        ENTITLEMENT_RETRO_ARCADE,
        ENTITLEMENT_ALPINE_WINTER,
        ENTITLEMENT_COLLECTOR_BUNDLE
    )

    fun getEntitlement(skuId: String): Entitlement? = ALL_ENTITLEMENTS.find { it.skuId == skuId }
}
