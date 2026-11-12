package game.simulation

/**
 * Executable runner that evaluates the 7, 30, 90, 180, and 365-day Monte Carlo simulations
 * across Casual, Core, and Hardcore archetypes and formats the result.
 */
object SimulationRunner {

    val HORIZONS = listOf(7, 30, 90, 180, 365)
    val ARCHETYPES = listOf(
        PlayerArchetype.CASUAL,
        PlayerArchetype.CORE,
        PlayerArchetype.HARDCORE
    )

    fun runAll(iterationsPerCohort: Int = 100): List<CohortSummary> {
        val simulator = MonteCarloSimulator(baseSeed = 20261112L)
        val summaries = mutableListOf<CohortSummary>()

        for (days in HORIZONS) {
            for (archetype in ARCHETYPES) {
                val summary = simulator.simulateCohort(archetype, days, iterationsPerCohort)
                summaries.add(summary)
            }
        }
        return summaries
    }

    fun formatMarkdownTable(summaries: List<CohortSummary>): String {
        val sb = StringBuilder()
        sb.append("| Horizon | Archetype | Mean Gems | StdDev | P10 | P50 | P90 | Mean Items Unlocked | Satiation % |\n")
        sb.append("|---|---|---|---|---|---|---|---|---|\n")

        for (s in summaries) {
            sb.append("| **${s.days} Days** | ${s.archetype.name} | ")
            sb.append("${s.meanGemsEarned.toInt()} | ")
            sb.append("${s.stdDevGemsEarned.toInt()} | ")
            sb.append("${s.p10Gems} | ")
            sb.append("${s.p50Gems} | ")
            sb.append("${s.p90Gems} | ")
            sb.append("%.1f | ".format(s.meanItemsUnlocked))
            sb.append("%.1f%% |\n".format(s.meanSatiationPercent))
        }
        return sb.toString()
    }
}
