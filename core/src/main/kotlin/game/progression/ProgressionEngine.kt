package game.progression

import kotlin.math.floor
import kotlin.math.pow

/**
 * Progression result produced after a round or XP grant.
 * Strictly guarantees Rule 2: directCurrencyReward = 0.
 */
data class ProgressionUpdate(
    val previousLevel: Int,
    val newLevel: Int,
    val xpEarned: Long,
    val currentXp: Long,
    val xpForNextLevel: Long,
    val directCurrencyReward: Int = 0, // Invariant: Always 0 (Rule 2)
    val unlockedTitles: List<String> = emptyList()
) {
    val didLevelUp: Boolean
        get() = newLevel > previousLevel
}

/**
 * Domain engine managing XP calculation, level progression, and title milestones.
 */
class ProgressionEngine {

    /**
     * Required cumulative XP to reach a target level.
     * Curve: floor(100 * level^1.35)
     */
    fun xpRequiredForLevel(level: Int): Long {
        require(level >= 1) { "Level must be >= 1" }
        if (level == 1) return 0L
        return floor(100.0 * (level - 1).toDouble().pow(1.35)).toLong()
    }

    /**
     * Determines player level given total cumulative XP.
     */
    fun calculateLevelForXp(totalXp: Long): Int {
        require(totalXp >= 0) { "XP cannot be negative" }
        var low = 1
        var high = Int.MAX_VALUE
        while (low < high) {
            val middle = low + ((high.toLong() - low + 1) / 2).toInt()
            if (xpRequiredForLevel(middle) <= totalXp) low = middle else high = middle - 1
        }
        return low
    }

    /**
     * Awards XP for an Onet round outcome.
     * Note: Pure XP award. 0 Gems granted.
     */
    fun calculateOnetXp(
        won: Boolean,
        tilesMatched: Int,
        maxCombo: Int,
        remainingTimeRatio: Float
    ): Long {
        if (!won) {
            return (tilesMatched * 2L)
        }
        val baseWin = 50L
        val comboBonus = (maxCombo * 5L)
        val speedBonus = (remainingTimeRatio * 20.0f).toLong()
        return baseWin + comboBonus + speedBonus + (tilesMatched * 2L)
    }

    /**
     * Awards XP for a Triple Match round outcome.
     * Note: Pure XP award. 0 Gems granted.
     */
    fun calculateTripleMatchXp(
        won: Boolean,
        triosMatched: Int,
        emptyTraySlotsRemaining: Int
    ): Long {
        if (!won) {
            return (triosMatched * 5L)
        }
        val baseWin = 60L
        val trayBonus = (emptyTraySlotsRemaining * 10L)
        return baseWin + trayBonus + (triosMatched * 5L)
    }

    /**
     * Applies earned XP to a profile and computes level transition.
     */
    fun applyXp(profile: PlayerProfile, xpEarned: Long): Pair<PlayerProfile, ProgressionUpdate> {
        require(xpEarned >= 0) { "Earned XP must be >= 0" }
        val newTotalXp = Math.addExact(profile.currentXp, xpEarned)
        val newLevel = calculateLevelForXp(newTotalXp)
        val xpForNext = xpRequiredForLevel(newLevel + 1)

        val unlockedTitles = mutableListOf<String>()
        if (newLevel > profile.level) {
            for (lvl in listOf(5, 10, 25, 50, 100).filter { it > profile.level && it <= newLevel }) {
                getTitleForLevel(lvl)?.let { unlockedTitles.add(it) }
            }
        }

        val updatedProfile = profile.copy(
            level = newLevel,
            currentXp = newTotalXp
        )

        val update = ProgressionUpdate(
            previousLevel = profile.level,
            newLevel = newLevel,
            xpEarned = xpEarned,
            currentXp = newTotalXp,
            xpForNextLevel = xpForNext,
            directCurrencyReward = 0, // Strict Rule 2 enforcement
            unlockedTitles = unlockedTitles
        )

        return Pair(updatedProfile, update)
    }

    private fun getTitleForLevel(level: Int): String? = when (level) {
        5 -> "Ceramic Apprentice"
        10 -> "Glaze Artisan"
        25 -> "Master of Connections"
        50 -> "Zen Grandmaster"
        100 -> "Legendary Kilnmaster"
        else -> null
    }
}
