package game.achievements

/**
 * Standard tier rewards (Bronze, Silver, Gold, Platinum, Diamond).
 * Total gems per achievement: 25 + 50 + 75 + 100 + 150 = 400 Gems.
 * Enforces Rule 7: Deterministic and transparent rewards.
 */
object AchievementCatalog {

    private fun standardTiers(targets: List<Long>): List<AchievementTier> {
        require(targets.size == 5) { "Must provide exactly 5 tier targets" }
        val names = listOf("Bronze", "Silver", "Gold", "Platinum", "Diamond")
        val rewards = listOf(25, 50, 75, 100, 150)
        return targets.mapIndexed { index, target ->
            AchievementTier(
                tierIndex = index + 1,
                tierName = names[index],
                requiredValue = target,
                gemReward = rewards[index],
                isClaimed = false
            )
        }
    }

    val ALL_ACHIEVEMENTS: List<Achievement> = listOf(
        Achievement(
            id = "onet_master",
            title = "Onet Virtuoso",
            descriptionFormat = "Win %d Onet rounds",
            category = AchievementCategory.ONET_MASTERY,
            tiers = standardTiers(listOf(10L, 50L, 200L, 500L, 1000L))
        ),
        Achievement(
            id = "triple_master",
            title = "Stack Specialist",
            descriptionFormat = "Win %d Triple Match rounds",
            category = AchievementCategory.TRIPLE_MATCH_MASTERY,
            tiers = standardTiers(listOf(10L, 50L, 200L, 500L, 1000L))
        ),
        Achievement(
            id = "combo_king",
            title = "Cadence & Flow",
            descriptionFormat = "Achieve a combo streak of %d",
            category = AchievementCategory.COMBO_SKILL,
            tiers = standardTiers(listOf(5L, 10L, 15L, 20L, 25L))
        ),
        Achievement(
            id = "flawless_zen",
            title = "Untouchable",
            descriptionFormat = "Complete %d flawless rounds without hints, shuffles, or undos",
            category = AchievementCategory.FINESSE,
            tiers = standardTiers(listOf(5L, 25L, 100L, 250L, 500L))
        ),
        Achievement(
            id = "tile_cleanser",
            title = "Ceramic Sweeper",
            descriptionFormat = "Match %d tiles total across all games",
            category = AchievementCategory.LIFETIME,
            tiers = standardTiers(listOf(500L, 2500L, 10000L, 30000L, 100000L))
        ),
        Achievement(
            id = "cosmetic_curator",
            title = "Ceramic Collector",
            descriptionFormat = "Unlock %d cosmetics in the catalog",
            category = AchievementCategory.COLLECTION,
            tiers = standardTiers(listOf(3L, 8L, 15L, 25L, 40L))
        )
    )

    fun createInitialCatalog(): List<Achievement> = ALL_ACHIEVEMENTS.map { it.copy() }
}
