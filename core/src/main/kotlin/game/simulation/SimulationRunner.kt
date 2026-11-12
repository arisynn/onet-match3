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
        PlayerArchetype.HARDCORE,
        PlayerArchetype.NEW_PLAYER,
        PlayerArchetype.LONG_TERM
    )

    @JvmStatic fun main(args: Array<String>) { println(formatMarkdownTable(runAll(args.firstOrNull()?.toInt() ?: 10))) }

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
        sb.append("| Horizon | Archetype | Mean Gems | StdDev | P10 | P50 | P90 | Mean Items Unlocked | Satiation % | Mean Spent | Mean Balance | First Unlock (median day) | Catalog Exhaustion (mean day, if any) |\n")
        sb.append("|---|---|---|---|---|---|---|---|---|---|---|---|---|\n")

        for (s in summaries) {
            sb.append("| **${s.days} Days** | ${s.archetype.name} | ")
            sb.append("${s.meanGemsEarned.toInt()} | ")
            sb.append("${s.stdDevGemsEarned.toInt()} | ")
            sb.append("${s.p10Gems} | ")
            sb.append("${s.p50Gems} | ")
            sb.append("${s.p90Gems} | ")
            sb.append("%.1f | ".format(s.meanItemsUnlocked))
            sb.append("%.1f%% | ".format(s.meanSatiationPercent))
            sb.append("${s.meanGemsSpent.toInt()} | ${s.meanUnspentBalance.toInt()} | ")
            sb.append("${s.medianFirstUnlockDay ?: "not reached"} | ${s.meanCatalogExhaustionDay ?: "not reached"} |\n")
        }
        return sb.toString()
    }
}
