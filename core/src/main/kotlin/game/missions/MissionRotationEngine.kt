package game.missions

import java.util.Random

/**
 * Procedural generation and rotation engine for Daily and Weekly missions.
 * Enforces Rule 1 (Gems can only be earned from claiming missions/achievements)
 * and Rule 7 (Deterministic and transparent rewards).
 */
class MissionRotationEngine(private val rng: Random = Random(42)) {

    companion object {
        const val DAILY_SLOT_COUNT = 3
        const val WEEKLY_SLOT_COUNT = 3
        const val MS_PER_DAY = 86_400_000L
        const val MS_PER_WEEK = 7 * MS_PER_DAY
    }

    private data class Template(
        val titleFormat: String,
        val descFormat: String,
        val objective: MissionObjective,
        val target: Int,
        val gemReward: Int,
        val targetTheme: String? = null
    )

    private val dailyTemplates = listOf(
        Template("Tile Novice", "Match %d tiles in any mode", MissionObjective.MATCH_TILES, 50, 15),
        Template("Tile Collector", "Match %d tiles in any mode", MissionObjective.MATCH_TILES, 100, 20),
        Template("Tile Master", "Match %d tiles in any mode", MissionObjective.MATCH_TILES, 200, 25),
        Template("Onet Linker", "Win %d Onet rounds", MissionObjective.WIN_ONET_ROUNDS, 2, 15),
        Template("Onet Maestro", "Win %d Onet rounds", MissionObjective.WIN_ONET_ROUNDS, 4, 20),
        Template("Triple Stacker", "Win %d Triple Match rounds", MissionObjective.WIN_TRIPLE_MATCH_ROUNDS, 2, 15),
        Template("Triple Virtuoso", "Win %d Triple Match rounds", MissionObjective.WIN_TRIPLE_MATCH_ROUNDS, 4, 20),
        Template("Smooth Flow", "Achieve a combo streak of %d", MissionObjective.COMBO_STREAK, 5, 20),
        Template("Fierce Rhythm", "Achieve a combo streak of %d", MissionObjective.COMBO_STREAK, 8, 25),
        Template("Swift Touch", "Win %d rounds with >= 50%% time remaining", MissionObjective.FAST_ROUNDS, 2, 20),
        Template("Zen Focus", "Win %d rounds with 0 hints, shuffles or undos", MissionObjective.FLAWLESS_ROUNDS, 1, 25)
    )

    private val weeklyTemplates = listOf(
        Template("Weekly Tile Marathon", "Match %d tiles total", MissionObjective.MATCH_TILES, 800, 80),
        Template("Grand Tile Harvest", "Match %d tiles total", MissionObjective.MATCH_TILES, 1500, 100),
        Template("Onet Champion", "Win %d Onet rounds this week", MissionObjective.WIN_ONET_ROUNDS, 15, 80),
        Template("Triple Match Legend", "Win %d Triple Match rounds this week", MissionObjective.WIN_TRIPLE_MATCH_ROUNDS, 15, 80),
        Template("Untouchable Flow", "Win %d flawless rounds without aids", MissionObjective.FLAWLESS_ROUNDS, 8, 100),
        Template("Master of Puzzles", "Win %d rounds across any mode", MissionObjective.WIN_ONET_ROUNDS, 25, 120),
        Template("Lightning Mind", "Win %d fast rounds with >= 50%% time remaining", MissionObjective.FAST_ROUNDS, 12, 120)
    )

    /**
     * Generates a batch of Daily Missions for the given day epoch.
     */
    fun generateDailyMissions(currentTimeMs: Long, dayEpoch: Long): List<Mission> {
        val seed = dayEpoch xor 0x5DEECE66DL
        val dailyRng = Random(seed)
        val shuffled = dailyTemplates.shuffled(dailyRng)
        val expiry = (dayEpoch + 1) * MS_PER_DAY

        return shuffled.take(DAILY_SLOT_COUNT).mapIndexed { index, template ->
            Mission(
                id = "daily_${dayEpoch}_$index",
                title = template.titleFormat.format(template.target),
                description = template.descFormat.format(template.target),
                type = MissionType.DAILY,
                objective = template.objective,
                targetCount = template.target,
                currentCount = 0,
                gemReward = template.gemReward,
                status = MissionStatus.IN_PROGRESS,
                createdAtMs = currentTimeMs,
                expiryTimeMs = expiry,
                targetTheme = template.targetTheme
            )
        }
    }

    /**
     * Generates a batch of Weekly Missions for the given week epoch.
     */
    fun generateWeeklyMissions(currentTimeMs: Long, weekEpoch: Long): List<Mission> {
        val seed = weekEpoch xor 0xB0BAFE77L
        val weeklyRng = Random(seed)
        val shuffled = weeklyTemplates.shuffled(weeklyRng)
        val expiry = (weekEpoch + 1) * MS_PER_WEEK

        return shuffled.take(WEEKLY_SLOT_COUNT).mapIndexed { index, template ->
            Mission(
                id = "weekly_${weekEpoch}_$index",
                title = template.titleFormat.format(template.target),
                description = template.descFormat.format(template.target),
                type = MissionType.WEEKLY,
                objective = template.objective,
                targetCount = template.target,
                currentCount = 0,
                gemReward = template.gemReward,
                status = MissionStatus.IN_PROGRESS,
                createdAtMs = currentTimeMs,
                expiryTimeMs = expiry,
                targetTheme = template.targetTheme
            )
        }
    }

    /**
     * Performs rotation and rollover:
     * - Completed but UNCLAIMED missions are preserved so players never forfeit rewards.
     * - Expired In-Progress or Claimed missions are replaced with fresh ones.
     */
    fun rolloverMissions(
        currentActive: List<Mission>,
        currentTimeMs: Long,
        type: MissionType
    ): List<Mission> {
        val epoch = if (type == MissionType.DAILY) currentTimeMs / MS_PER_DAY else currentTimeMs / MS_PER_WEEK
        val freshMissions = if (type == MissionType.DAILY) {
            generateDailyMissions(currentTimeMs, epoch)
        } else {
            generateWeeklyMissions(currentTimeMs, epoch)
        }

        val preservedUnclaimed = currentActive.filter { it.type == type && it.isClaimable }
        val nonExpired = currentActive.filter { it.type == type && it.expiryTimeMs > currentTimeMs && !it.isClaimable }

        val result = mutableListOf<Mission>()
        result.addAll(preservedUnclaimed) // Preserve unclaimed completed missions
        result.addAll(nonExpired)

        val targetCount = if (type == MissionType.DAILY) DAILY_SLOT_COUNT else WEEKLY_SLOT_COUNT
        for (fresh in freshMissions) {
            if (result.count { it.type == type } >= targetCount) break
            if (result.none { it.id == fresh.id }) {
                result.add(fresh)
            }
        }

        return result
    }
}
