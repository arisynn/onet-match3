package game.simulation

import game.achievements.AchievementCatalog
import game.achievements.AchievementService
import game.cosmetics.CosmeticAcquisitionType
import game.cosmetics.CosmeticCatalog
import game.cosmetics.CosmeticInventory
import game.cosmetics.CosmeticService
import game.economy.EconomyService
import game.economy.TransactionLedger
import game.missions.MissionObjective
import game.missions.MissionRotationEngine
import game.missions.MissionService
import game.missions.MissionStatus
import game.missions.MissionType
import game.progression.PlayerStats
import java.util.Random
import kotlin.math.sqrt

/**
 * Monte Carlo simulation engine validating economy sustainability and progression pacing.
 */
class MonteCarloSimulator(private val baseSeed: Long = 1337L) {

    /**
     * Runs a single player trajectory for the specified number of days.
     */
    fun simulateTrajectory(
        archetype: PlayerArchetype,
        days: Int,
        rng: Random
    ): TrajectoryMetrics {
        require(days > 0)
        val ledger = TransactionLedger()
        val economyService = EconomyService(ledger)
        val missionRotationEngine = MissionRotationEngine(Random(rng.nextLong()))
        val missionService = MissionService(missionRotationEngine)
        val achievementService = AchievementService(AchievementCatalog.createInitialCatalog())
        val cosmeticInventory = CosmeticInventory()
        val cosmeticService = CosmeticService(cosmeticInventory)

        var stats = PlayerStats()
        var dayFirstUnlock: Int? = null
        var dayCatalogExhausted: Int? = null

        val earnableItems = CosmeticCatalog.ALL_ITEMS
            .filter { it.acquisitionType == CosmeticAcquisitionType.EARNABLE }
            .sortedBy { it.gemPrice }
        val totalEarnableCount = earnableItems.size
        val totalEarnableCost = earnableItems.sumOf { it.gemPrice }

        var cumulativeGemsEarned = 0
        var cumulativeGemsSpent = 0

        for (day in 1..days) {
            val currentTimeMs = day * MissionRotationEngine.MS_PER_DAY

            // 1. Mission Rotation & Rollover
            missionService.refreshMissions(currentTimeMs)

            // Engagement is probabilistic; objective progress only comes from
            // simulated gameplay events, never an invented "mission completed" roll.
            val engagedToday = rng.nextDouble() < archetype.dailyMissionCompletionRate
            for (round in 0 until (if (engagedToday) archetype.roundsPerDay else 0)) {
                val isOnet = rng.nextBoolean()
                val tilesInRound = (archetype.averageTilesPerRound + rng.nextInt(15) - 7).coerceAtLeast(0)
                val combo = (archetype.averageCombo + rng.nextInt(5) - 2).coerceAtLeast(0)
                val won = rng.nextDouble() < (0.65 + archetype.weeklyMissionCompletionRate * 0.25)
                val isFlawless = won && rng.nextDouble() < archetype.flawlessRate
                val remainingRatio = if (won) rng.nextFloat() * 0.75f else 0f
                val theme = if (isOnet) "candy" else "ocean"
                if (isOnet) {
                    stats = stats.recordOnetRound(won, tilesInRound, combo,
                        if (isFlawless) 0 else 1, 0, remainingRatio, theme)
                } else {
                    stats = stats.recordTripleMatchRound(won, tilesInRound / 3,
                        if (isFlawless) 0 else 1, remainingRatio, theme)
                }
                missionService.recordProgress(MissionObjective.MATCH_TILES, tilesInRound)
                missionService.recordProgress(MissionObjective.COMBO_STREAK, combo)
                missionService.recordProgress(MissionObjective.PLAY_THEME, theme = theme)
                if (won) {
                    missionService.recordProgress(if (isOnet) MissionObjective.WIN_ONET_ROUNDS else MissionObjective.WIN_TRIPLE_MATCH_ROUNDS)
                    if (isFlawless) missionService.recordProgress(MissionObjective.FLAWLESS_ROUNDS)
                    if (remainingRatio >= 0.5f) missionService.recordProgress(MissionObjective.FAST_ROUNDS)
                }
            }

            // 4. Claim Completed Missions (Rule 1)
            for (mission in missionService.getActiveMissions()) {
                if (mission.status == MissionStatus.COMPLETED) {
                    val (_, tx) = missionService.claimMission(mission.id, economyService, currentTimeMs)
                    cumulativeGemsEarned += mission.gemReward
                }
            }

            // 5. Synchronize and Claim Achievement Tiers (Rule 1)
            val unlockedCosmetics = cosmeticInventory.getUnlockedCount()
            achievementService.syncStats(stats, unlockedCosmetics)

            for (ach in achievementService.getAchievements()) {
                for (tier in ach.tiers) {
                    if (ach.isTierClaimable(tier.tierIndex)) {
                        val (_, tx) = achievementService.claimTier(ach.id, tier.tierIndex, economyService, currentTimeMs)
                        cumulativeGemsEarned += tier.gemReward
                    }
                }
            }

            // 6. Spend Currency on Unowned Earnable Cosmetics (Rule 5 & 6)
            for (item in earnableItems) {
                if (!cosmeticInventory.isOwned(item.id) && economyService.wallet.hasSufficientGems(item.gemPrice)) {
                    val result = cosmeticService.purchaseEarnable(item.id, economyService, currentTimeMs)
                    cumulativeGemsSpent += item.gemPrice

                    if (dayFirstUnlock == null) {
                        dayFirstUnlock = day
                    }

                    val currentlyOwnedEarnable = earnableItems.count { cosmeticInventory.isOwned(it.id) }
                    if (currentlyOwnedEarnable >= totalEarnableCount && dayCatalogExhausted == null) {
                        dayCatalogExhausted = day
                    }
                }
            }
        }

        val finalBalance = economyService.wallet.gemBalance
        val earnableUnlockedCount = earnableItems.count { cosmeticInventory.isOwned(it.id) }
        val satiationRatio = (cumulativeGemsSpent.toDouble() / totalEarnableCost.toDouble()).coerceAtMost(1.0)

        return TrajectoryMetrics(
            days = days,
            cumulativeGemsEarned = cumulativeGemsEarned,
            cumulativeGemsSpent = cumulativeGemsSpent,
            finalBalance = finalBalance,
            earnableCosmeticsPurchased = earnableUnlockedCount,
            catalogSatiationRatio = satiationRatio,
            dayFirstCosmeticUnlocked = dayFirstUnlock,
            dayCatalogExhausted = dayCatalogExhausted
        )
    }

    /**
     * Runs a Monte Carlo simulation over an ensemble of N trajectories.
     */
    fun simulateCohort(
        archetype: PlayerArchetype,
        days: Int,
        iterations: Int = 100
    ): CohortSummary {
        require(days > 0 && iterations > 0)
        val runs = mutableListOf<TrajectoryMetrics>()
        for (i in 0 until iterations) {
            val trajectoryRng = Random(baseSeed + (i * 7919L) + (days * 31L))
            runs.add(simulateTrajectory(archetype, days, trajectoryRng))
        }

        val gemsEarnedList = runs.map { it.cumulativeGemsEarned }.sorted()
        val meanGems = gemsEarnedList.average()
        val variance = gemsEarnedList.map { (it - meanGems) * (it - meanGems) }.average()
        val stdDev = sqrt(variance)

        val p10Index = (iterations * 0.10).toInt().coerceIn(0, iterations - 1)
        val p50Index = (iterations * 0.50).toInt().coerceIn(0, iterations - 1)
        val p90Index = (iterations * 0.90).toInt().coerceIn(0, iterations - 1)

        val meanUnlocked = runs.map { it.earnableCosmeticsPurchased }.average()
        val meanSatiation = runs.map { it.catalogSatiationRatio * 100.0 }.average()

        val firstUnlockDays = runs.mapNotNull { it.dayFirstCosmeticUnlocked }.sorted()
        val medianFirstUnlock = if (firstUnlockDays.isNotEmpty()) {
            firstUnlockDays[firstUnlockDays.size / 2]
        } else null

        return CohortSummary(
            archetype = archetype,
            days = days,
            iterations = iterations,
            meanGemsEarned = meanGems,
            stdDevGemsEarned = stdDev,
            p10Gems = gemsEarnedList[p10Index],
            p50Gems = gemsEarnedList[p50Index],
            p90Gems = gemsEarnedList[p90Index],
            meanItemsUnlocked = meanUnlocked,
            meanSatiationPercent = meanSatiation,
            medianFirstUnlockDay = medianFirstUnlock,
            meanGemsSpent = runs.map { it.cumulativeGemsSpent }.average(),
            meanUnspentBalance = runs.map { it.finalBalance }.average(),
            meanCatalogExhaustionDay = runs.mapNotNull { it.dayCatalogExhausted }.takeIf { it.isNotEmpty() }?.average()
        )
    }
}
