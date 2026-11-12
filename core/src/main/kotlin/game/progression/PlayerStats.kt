package game.progression

/**
 * Immutable gameplay statistics tracked across player lifetime.
 * Feeds directly into Mission and Achievement evaluation engines.
 */
data class PlayerStats(
    val totalOnetRoundsPlayed: Int = 0,
    val totalOnetRoundsWon: Int = 0,
    val totalTripleMatchRoundsPlayed: Int = 0,
    val totalTripleMatchRoundsWon: Int = 0,
    val totalTilesMatched: Long = 0L,
    val highestComboStreak: Int = 0,
    val flawlessRoundsCount: Int = 0, // rounds won with 0 hints, 0 shuffles, 0 undos
    val fastRoundsCount: Int = 0,     // rounds won with >= 50% timer remaining
    val shufflesUsed: Int = 0,
    val hintsUsed: Int = 0,
    val undosUsed: Int = 0,
    val themePlays: Map<String, Int> = emptyMap()
) {
    val totalRoundsWon: Int
        get() = totalOnetRoundsWon + totalTripleMatchRoundsWon

    val totalRoundsPlayed: Int
        get() = totalOnetRoundsPlayed + totalTripleMatchRoundsPlayed

    fun recordOnetRound(
        won: Boolean,
        tilesMatched: Int,
        maxCombo: Int,
        hintsUsedInRound: Int,
        shufflesUsedInRound: Int,
        remainingTimeRatio: Float,
        theme: String
    ): PlayerStats {
        val isFlawless = won && hintsUsedInRound == 0 && shufflesUsedInRound == 0
        val isFast = won && remainingTimeRatio >= 0.50f
        val newThemePlays = themePlays.toMutableMap()
        newThemePlays[theme] = (newThemePlays[theme] ?: 0) + 1

        return copy(
            totalOnetRoundsPlayed = totalOnetRoundsPlayed + 1,
            totalOnetRoundsWon = if (won) totalOnetRoundsWon + 1 else totalOnetRoundsWon,
            totalTilesMatched = totalTilesMatched + tilesMatched,
            highestComboStreak = maxOf(highestComboStreak, maxCombo),
            flawlessRoundsCount = if (isFlawless) flawlessRoundsCount + 1 else flawlessRoundsCount,
            fastRoundsCount = if (isFast) fastRoundsCount + 1 else fastRoundsCount,
            hintsUsed = hintsUsed + hintsUsedInRound,
            shufflesUsed = shufflesUsed + shufflesUsedInRound,
            themePlays = newThemePlays
        )
    }

    fun recordTripleMatchRound(
        won: Boolean,
        triosMatched: Int,
        undosUsedInRound: Int,
        remainingTimeRatio: Float,
        theme: String
    ): PlayerStats {
        val isFlawless = won && undosUsedInRound == 0
        val isFast = won && remainingTimeRatio >= 0.50f
        val newThemePlays = themePlays.toMutableMap()
        newThemePlays[theme] = (newThemePlays[theme] ?: 0) + 1

        return copy(
            totalTripleMatchRoundsPlayed = totalTripleMatchRoundsPlayed + 1,
            totalTripleMatchRoundsWon = if (won) totalTripleMatchRoundsWon + 1 else totalTripleMatchRoundsWon,
            totalTilesMatched = totalTilesMatched + (triosMatched * 3),
            flawlessRoundsCount = if (isFlawless) flawlessRoundsCount + 1 else flawlessRoundsCount,
            fastRoundsCount = if (isFast) fastRoundsCount + 1 else fastRoundsCount,
            undosUsed = undosUsed + undosUsedInRound,
            themePlays = newThemePlays
        )
    }
}
