package game.progression

import java.util.UUID

/**
 * Player profile representing progression level, cumulative XP, and lifetime stats.
 */
data class PlayerProfile(
    val playerId: String = UUID.randomUUID().toString(),
    val level: Int = 1,
    val currentXp: Long = 0L,
    val stats: PlayerStats = PlayerStats()
)
