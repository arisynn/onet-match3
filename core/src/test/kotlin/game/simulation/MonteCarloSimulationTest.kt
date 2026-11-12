package game.simulation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Random

class MonteCarloSimulationTest {

    @Test
    @DisplayName("Monte Carlo: Verify 7, 30, 90, 180, 365-day simulations across all player archetypes")
    fun testMultiHorizonSimulation() {
        val simulator = MonteCarloSimulator(baseSeed = 42L)

        for (days in listOf(7, 30, 90, 180, 365)) {
            for (archetype in listOf(PlayerArchetype.CASUAL, PlayerArchetype.CORE, PlayerArchetype.HARDCORE)) {
                val trajectory = simulator.simulateTrajectory(archetype, days, Random(42L + days))

                // Invariant: Non-negative ending balance
                assertTrue(trajectory.finalBalance >= 0, "Final balance must be non-negative")

                // Invariant: Cumulative gems earned must equal spent + balance
                assertEquals(
                    trajectory.cumulativeGemsEarned,
                    trajectory.cumulativeGemsSpent + trajectory.finalBalance,
                    "Earned gems must equal spent + final balance"
                )

                // Invariant: Satiation ratio between 0.0 and 1.0
                assertTrue(trajectory.catalogSatiationRatio in 0.0..1.0)
            }
        }
    }

    @Test
    @DisplayName("Monte Carlo: Statistical Cohort Simulation & Markdown Report Output")
    fun testStatisticalCohortRun() {
        val summaries = SimulationRunner.runAll(iterationsPerCohort = 5)
        assertEquals(25, summaries.size) // 5 horizons x 3 archetypes

        val reportTable = SimulationRunner.formatMarkdownTable(summaries)
        assertNotNull(reportTable)
        assertTrue(reportTable.contains("Casual (Zen Explorer)"))
        assertTrue(reportTable.contains("Core (Daily Puzzler)"))
        assertTrue(reportTable.contains("Hardcore (Grandmaster)"))

        // Print report table for test log verification
        println("\n=== MONTE CARLO SIMULATION RESULTS (7/30/90/180/365 DAYS) ===")
        println(reportTable)
    }
}
