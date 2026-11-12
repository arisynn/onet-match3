package game.cosmetics

/**
 * Visual customization slot categories.
 */
enum class CosmeticType {
    TILE_SET,
    TILE_BODY,
    UI_PALETTE,
    BACKGROUND,
    EFFECT
}

/**
 * Acquisition model for cosmetic items.
 */
enum class CosmeticAcquisitionType {
    FREE,       // Unlocked by default
    EARNABLE,   // Purchased exclusively with earned Gems
    PREMIUM     // Unlocked via direct non-consumable Google Play IAP
}

/**
 * Cosmetic visual rarity tiers.
 */
enum class CosmeticRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}

/**
 * Immutable cosmetic item definition.
 * Strictly enforces Rule 10: Zero gameplay stats, timers, or score bonuses.
 */
data class CosmeticItem(
    val id: String,
    val name: String,
    val description: String,
    val type: CosmeticType,
    val acquisitionType: CosmeticAcquisitionType,
    val rarity: CosmeticRarity,
    val gemPrice: Int = 0,
    val iapSku: String? = null,
    val assetPath: String,
    val previewAssetPath: String? = null
) {
    init {
        when (acquisitionType) {
            CosmeticAcquisitionType.FREE -> {
                require(gemPrice == 0) { "Free cosmetic must have gemPrice = 0" }
                require(iapSku == null) { "Free cosmetic cannot have an IAP SKU" }
            }
            CosmeticAcquisitionType.EARNABLE -> {
                require(gemPrice > 0) { "Earnable cosmetic must have gemPrice > 0, was: $gemPrice" }
                require(iapSku == null) { "Earnable cosmetic cannot have an IAP SKU" }
            }
            CosmeticAcquisitionType.PREMIUM -> {
                require(gemPrice == 0) { "Premium cosmetic cannot be purchased with gems" }
                require(!iapSku.isNullOrBlank()) { "Premium cosmetic must have an IAP SKU" }
            }
        }
    }
}
