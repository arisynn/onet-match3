package game.achievements

/**
 * Category of an achievement.
 */
enum class AchievementCategory {
    ONET_MASTERY,
    TRIPLE_MATCH_MASTERY,
    COMBO_SKILL,
    FINESSE,
    LIFETIME,
    COLLECTION
}

/**
 * An individual tier within a multi-tiered achievement.
 */
data class AchievementTier(
    val tierIndex: Int, // 1 to 5
    val tierName: String, // Bronze, Silver, Gold, Platinum, Diamond
    val requiredValue: Long,
    val gemReward: Int,
    val isClaimed: Boolean = false
)

/**
 * Multi-tiered achievement definition and state.
 */
data class Achievement(
    val id: String,
    val title: String,
    val descriptionFormat: String,
    val category: AchievementCategory,
    val tiers: List<AchievementTier>,
    val currentValue: Long = 0L
) {
    val currentTierIndex: Int
        get() {
            var highestCompleted = 0
            for (tier in tiers) {
                if (currentValue >= tier.requiredValue) {
                    highestCompleted = tier.tierIndex
                }
            }
            return highestCompleted
        }

    fun isTierClaimable(tierIndex: Int): Boolean {
        val tier = tiers.find { it.tierIndex == tierIndex } ?: return false
        return currentValue >= tier.requiredValue && !tier.isClaimed
    }

    fun hasAnyClaimableTier(): Boolean {
        return tiers.any { currentValue >= it.requiredValue && !it.isClaimed }
    }
}
