package game.simulation

/**
 * Stochastic player archetype parameters for Monte Carlo economic simulation.
 */
data class PlayerArchetype(
    val name: String,
    val roundsPerDay: Int,
    val dailyMissionCompletionRate: Double,
    val weeklyMissionCompletionRate: Double,
    val averageTilesPerRound: Int = 45,
    val averageCombo: Int = 5,
    val flawlessRate: Double = 0.30
) {
    companion object {
        val CASUAL = PlayerArchetype(
            name = "Casual (Zen Explorer)",
            roundsPerDay = 3,
            dailyMissionCompletionRate = 0.50,
            weeklyMissionCompletionRate = 0.35,
            averageTilesPerRound = 40,
            averageCombo = 4,
            flawlessRate = 0.20
        )

        val CORE = PlayerArchetype(
            name = "Core (Daily Puzzler)",
            roundsPerDay = 12,
            dailyMissionCompletionRate = 0.85,
            weeklyMissionCompletionRate = 0.80,
            averageTilesPerRound = 50,
            averageCombo = 7,
            flawlessRate = 0.35
        )

        val HARDCORE = PlayerArchetype(
            name = "Hardcore (Grandmaster)",
            roundsPerDay = 35,
            dailyMissionCompletionRate = 0.98,
            weeklyMissionCompletionRate = 0.98,
            averageTilesPerRound = 55,
            averageCombo = 12,
            flawlessRate = 0.50
        )
    }
}

/**
 * Result metrics from a single simulated player trajectory.
 */
data class TrajectoryMetrics(
    val days: Int,
    val cumulativeGemsEarned: Int,
    val cumulativeGemsSpent: Int,
    val finalBalance: Int,
    val earnableCosmeticsPurchased: Int,
    val catalogSatiationRatio: Double,
    val dayFirstCosmeticUnlocked: Int?,
    val dayCatalogExhausted: Int?
)

/**
 * Statistical summary across a simulated cohort of players.
 */
data class CohortSummary(
    val archetype: PlayerArchetype,
    val days: Int,
    val iterations: Int,
    val meanGemsEarned: Double,
    val stdDevGemsEarned: Double,
    val p10Gems: Int,
    val p50Gems: Int,
    val p90Gems: Int,
    val meanItemsUnlocked: Double,
    val meanSatiationPercent: Double,
    val medianFirstUnlockDay: Int?
)
