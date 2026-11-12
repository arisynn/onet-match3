package game.missions

/**
 * Cadence of a mission.
 */
enum class MissionType {
    DAILY,
    WEEKLY
}

/**
 * Objective types supported by the mission system.
 */
enum class MissionObjective {
    MATCH_TILES,
    WIN_ONET_ROUNDS,
    WIN_TRIPLE_MATCH_ROUNDS,
    COMBO_STREAK,
    FAST_ROUNDS,
    FLAWLESS_ROUNDS,
    PLAY_THEME
}

/**
 * Current lifecycle status of a mission.
 */
enum class MissionStatus {
    IN_PROGRESS,
    COMPLETED,
    CLAIMED
}

/**
 * Immutable mission model.
 */
data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val type: MissionType,
    val objective: MissionObjective,
    val targetCount: Int,
    val currentCount: Int = 0,
    val gemReward: Int,
    val status: MissionStatus = MissionStatus.IN_PROGRESS,
    val createdAtMs: Long,
    val expiryTimeMs: Long,
    val targetTheme: String? = null
) {
    val isComplete: Boolean
        get() = currentCount >= targetCount

    val isClaimable: Boolean
        get() = isComplete && status == MissionStatus.COMPLETED

    val progressRatio: Float
        get() = if (targetCount > 0) (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0.0f, 1.0f) else 1.0f
}
