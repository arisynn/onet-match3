package game.gameplay.onet

import kotlin.math.max
import kotlin.math.min

data class OnetDifficulty(
    val tier: Int,
    val label: String,
    val groups: Int,
    val time: Int,
    val separation: Int,
    val arrowPairs: Int
)

private val LABELS = listOf("", "Santai", "Fokus", "Menantang", "Ahli", "Master")

fun getTileVarietyCount(level: Int, maxAvailable: Int): Int = maxAvailable

fun getOnetDifficulty(input: Int = 1): OnetDifficulty {
    val level = max(1, input)
    val tier = when {
        level < 4 -> 1
        level < 8 -> 2
        level < 15 -> 3
        level < 25 -> 4
        else -> 5
    }
    val groups = when {
        level < 2 -> 0
        level < 6 -> 1
        else -> 2
    }
    val time = max(75L, 150L - (level.toLong() - 1L) * 2L).toInt()
    val separation = min(8, 2 + level / 3)
    val arrowPairs = if (level < 8) 0 else min(6, 1 + (level - 8) / 4)

    return OnetDifficulty(
        tier = tier,
        label = LABELS[tier],
        groups = groups,
        time = time,
        separation = separation,
        arrowPairs = arrowPairs
    )
}
