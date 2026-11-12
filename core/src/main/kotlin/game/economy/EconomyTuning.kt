package game.economy

import game.cosmetics.CosmeticRarity

/** One source of tuning defaults; never grants currency by itself. */
object EconomyTuning {
    val cosmeticPrices = mapOf(
        CosmeticRarity.COMMON to 400,
        CosmeticRarity.RARE to 1200,
        CosmeticRarity.EPIC to 3000,
        CosmeticRarity.LEGENDARY to 6000
    )
    val achievementRewards = listOf(50, 100, 200, 500, 1000)
    fun price(rarity: CosmeticRarity): Int = cosmeticPrices.getValue(rarity)
}
