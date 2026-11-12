package game.achievements

import game.economy.EconomyService
import game.economy.TransactionResult
import game.progression.PlayerStats

/**
 * Service managing achievement progression tracking and tier reward claims.
 */
class AchievementService(
    initialAchievements: List<Achievement> = AchievementCatalog.createInitialCatalog()
) {
    private val achievements = mutableMapOf<String, Achievement>()

    init {
        require(initialAchievements.map { it.id }.distinct().size == initialAchievements.size)
        for (ach in initialAchievements) {
            val canonical = AchievementCatalog.createInitialCatalog().firstOrNull { it.id == ach.id }
                ?: throw IllegalArgumentException("Unknown restored achievement: ${ach.id}")
            require(ach.currentValue >= 0 && ach.tiers.size == canonical.tiers.size)
            require(ach.tiers.zip(canonical.tiers).all { (a, b) ->
                a.tierIndex == b.tierIndex && a.requiredValue == b.requiredValue && a.gemReward == b.gemReward &&
                    (!a.isClaimed || ach.currentValue >= a.requiredValue)
            }) { "Invalid restored achievement tiers" }
            achievements[ach.id] = ach
        }
    }

    fun getAchievements(): List<Achievement> = synchronized(achievements) {
        achievements.values.toList()
    }

    fun getAchievement(id: String): Achievement? = synchronized(achievements) {
        achievements[id]
    }

    /**
     * Updates achievement progress values from player stats.
     */
    fun syncStats(stats: PlayerStats, unlockedCosmeticsCount: Int = 0) {
        synchronized(achievements) {
            updateValue("onet_master", stats.totalOnetRoundsWon.toLong())
            updateValue("triple_master", stats.totalTripleMatchRoundsWon.toLong())
            updateValue("combo_king", stats.highestComboStreak.toLong())
            updateValue("flawless_zen", stats.flawlessRoundsCount.toLong())
            updateValue("tile_cleanser", stats.totalTilesMatched)
            updateValue("cosmetic_curator", unlockedCosmeticsCount.toLong())
        }
    }

    private fun updateValue(id: String, newValue: Long) {
        val existing = achievements[id] ?: return
        achievements[id] = existing.copy(currentValue = maxOf(existing.currentValue, newValue))
    }

    /**
     * Claims a specific tier of an achievement.
     * Enforces Rule 1: Gems are earned from claiming completed achievements.
     * Enforces Rule 4: Idempotent ledger transaction prevents double-claiming.
     */
    fun claimTier(
        achievementId: String,
        tierIndex: Int,
        economyService: EconomyService,
        timestampMs: Long = System.currentTimeMillis()
    ): Pair<Achievement, TransactionResult> {
        synchronized(achievements) {
            val achievement = achievements[achievementId]
                ?: throw IllegalArgumentException("Achievement '$achievementId' not found")

            val tier = achievement.tiers.find { it.tierIndex == tierIndex }
                ?: throw IllegalArgumentException("Tier $tierIndex not found in achievement '$achievementId'")

            if (tier.isClaimed) {
                check(economyService.ledger.findByIdempotencyKey("CLAIM_ACH_${achievementId}_TIER_${tierIndex}") != null) {
                    "Claimed achievement is missing its ledger transaction"
                }
                // Return idempotent result
                val txResult = economyService.claimAchievementReward(achievementId, tierIndex, tier.gemReward, timestampMs)
                return Pair(achievement, txResult)
            }

            check(achievement.currentValue >= tier.requiredValue) {
                "Cannot claim tier $tierIndex: required ${tier.requiredValue}, but current progress is ${achievement.currentValue}"
            }

            val txResult = economyService.claimAchievementReward(achievementId, tierIndex, tier.gemReward, timestampMs)

            val updatedTiers = achievement.tiers.map {
                if (it.tierIndex == tierIndex) it.copy(isClaimed = true) else it
            }
            val updatedAchievement = achievement.copy(tiers = updatedTiers)
            achievements[achievementId] = updatedAchievement

            return Pair(updatedAchievement, txResult)
        }
    }
}
