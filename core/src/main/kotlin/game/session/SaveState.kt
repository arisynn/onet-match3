package game.session

import game.progression.PlayerProfile
import game.economy.Transaction
import game.missions.Mission
import game.achievements.Achievement
import game.cosmetics.CosmeticInventory
import game.cosmetics.CosmeticType
import game.cosmetics.CosmeticCatalog
import game.gameplay.onet.OnetRoundState
import game.gameplay.triplematch.TripleRoundState

/** A single recoverable transaction includes every reward-affecting domain. */
data class SaveState(
    val schemaVersion: Int = 1,
    val profile: PlayerProfile = PlayerProfile(),
    val transactions: List<Transaction> = emptyList(),
    val missions: List<Mission> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val unlocked: Set<String> = CosmeticCatalog.DEFAULT_UNLOCKED_IDS,
    val equipped: Map<CosmeticType, String> = CosmeticInventory.defaultEquippedMap(),
    val records: Map<String, LevelRecord> = emptyMap(),
    val unlockedOnet: Int = 1,
    val unlockedTriple: Int = 1,
    val settings: GameSettings = GameSettings(),
    val tutorialModes: Set<String> = emptySet(),
    val roundId: String = "",
    val roundMode: String = "ONET_GAME",
    val roundLevel: Int = 1,
    val roundTheme: String = "candy",
    val onet: OnetRoundState? = null,
    val triple: TripleRoundState? = null,
    val roundHints: Int = 0,
    val roundShuffles: Int = 0,
    val completedRoundIds: Set<String> = emptySet(),
    val missionTimeHighWater: Long = 0L
)

data class LevelRecord(val bestScore: Int = 0, val bestStars: Int = 0, val wins: Int = 0)
data class GameSettings(
    var musicEnabled: Boolean = true,
    var soundEnabled: Boolean = true,
    var hapticsEnabled: Boolean = true,
    var reducedMotion: Boolean = false,
    var reducedEffects: Boolean = false,
    var sfxVolume: Float = 1f,
    var musicVolume: Float = 0.6f
)

data class RewardTuning(
    val easyXp: Long = 30,
    val normalXp: Long = 50,
    val hardXp: Long = 80,
    val firstWinBonus: Long = 20,
    val improvedRecordBonus: Long = 10,
    val maximumPerformanceBonus: Long = 10,
    val replayPercent: Int = 20
) {
    fun award(level: Int, firstWin: Boolean, improved: Boolean, stars: Int, priorWins: Int): Long {
        val base = when { level <= 3 -> easyXp; level <= 9 -> normalXp; else -> hardXp }
        val replayBase = if (priorWins == 0) base else (base * replayPercent / 100).coerceAtLeast(1)
        return replayBase + (if (firstWin) firstWinBonus else 0) +
            (if (improved && !firstWin) improvedRecordBonus else 0) +
            (if (priorWins == 0 && stars == 3) maximumPerformanceBonus else 0)
    }
}
