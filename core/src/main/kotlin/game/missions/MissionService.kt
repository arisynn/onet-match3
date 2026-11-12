package game.missions

import game.economy.EconomyService
import game.economy.TransactionResult

/**
 * Service managing active missions, progress evaluation, and claiming.
 */
class MissionService(
    private val rotationEngine: MissionRotationEngine = MissionRotationEngine(),
    initialMissions: List<Mission> = emptyList(),
    initialLastRefreshMs: Long = 0L
) {
    var lastRefreshTimeMs: Long = initialLastRefreshMs
        private set

    private val activeMissions = mutableListOf<Mission>()

    init {
        require(initialLastRefreshMs >= 0)
        require(initialMissions.map { it.id }.distinct().size == initialMissions.size) { "Duplicate mission IDs" }
        initialMissions.forEach {
            require(it.id.isNotBlank() && it.targetCount > 0 && it.gemReward > 0 && it.currentCount >= 0)
            require(it.createdAtMs >= 0 && it.expiryTimeMs > it.createdAtMs)
            require((it.status == MissionStatus.IN_PROGRESS) == !it.isComplete) { "Invalid restored mission status" }
            val generated = Regex("(daily|weekly)_([0-9]+)_([0-2])").matchEntire(it.id)
            if (generated != null) {
                val epoch = generated.groupValues[2].toLong()
                val duration = if (it.type == MissionType.DAILY) MissionRotationEngine.MS_PER_DAY else MissionRotationEngine.MS_PER_WEEK
                require(epoch <= Long.MAX_VALUE / duration - 1)
                val catalog = if (it.type == MissionType.DAILY) rotationEngine.generateDailyMissions(epoch * duration, epoch)
                    else rotationEngine.generateWeeklyMissions(epoch * duration, epoch)
                val expected = catalog.firstOrNull { candidate -> candidate.id == it.id }
                require(expected != null && it.gemReward == expected.gemReward && it.targetCount == expected.targetCount &&
                    it.objective == expected.objective && it.targetTheme == expected.targetTheme && it.expiryTimeMs == expected.expiryTimeMs) {
                    "Restored mission does not match deterministic definition"
                }
            }
        }
        activeMissions.addAll(initialMissions)
    }

    fun getActiveMissions(): List<Mission> = synchronized(activeMissions) {
        ArrayList(activeMissions)
    }

    /**
     * Initializes or rolls over missions based on current wall clock timestamp.
     */
    fun refreshMissions(currentTimeMs: Long) {
        synchronized(activeMissions) {
            require(currentTimeMs >= 0)
            val effectiveTime = maxOf(currentTimeMs, lastRefreshTimeMs,
                activeMissions.maxOfOrNull { it.createdAtMs } ?: 0L)
            lastRefreshTimeMs = effectiveTime
            val updatedDailies = rotationEngine.rolloverMissions(activeMissions, effectiveTime, MissionType.DAILY)
            val updatedWeeklies = rotationEngine.rolloverMissions(activeMissions, effectiveTime, MissionType.WEEKLY)

            activeMissions.clear()
            activeMissions.addAll(updatedDailies)
            activeMissions.addAll(updatedWeeklies)
        }
    }

    /**
     * Evaluates gameplay event against active missions and increments matching objectives.
     */
    fun recordProgress(
        objective: MissionObjective,
        amount: Int = 1,
        theme: String? = null
    ) {
        require(amount >= 0) { "Mission progress cannot be negative" }
        synchronized(activeMissions) {
            for (i in activeMissions.indices) {
                val mission = activeMissions[i]
                if (mission.status == MissionStatus.IN_PROGRESS && mission.objective == objective) {
                    if (mission.targetTheme == null || mission.targetTheme == theme) {
                        val newCount = if (objective == MissionObjective.COMBO_STREAK) maxOf(mission.currentCount, amount).coerceAtMost(mission.targetCount)
                        else (mission.currentCount.toLong() + amount).coerceAtMost(mission.targetCount.toLong()).toInt()
                        val newStatus = if (newCount >= mission.targetCount) MissionStatus.COMPLETED else MissionStatus.IN_PROGRESS
                        activeMissions[i] = mission.copy(
                            currentCount = newCount,
                            status = newStatus
                        )
                    }
                }
            }
        }
    }

    /**
     * Claims a completed mission, crediting gems through EconomyService with idempotency.
     * Rule 1 enforcement: Player must actively claim completed mission.
     */
    fun claimMission(
        missionId: String,
        economyService: EconomyService,
        timestampMs: Long = System.currentTimeMillis()
    ): Pair<Mission, TransactionResult> {
        synchronized(activeMissions) {
            val index = activeMissions.indexOfFirst { it.id == missionId }
            require(index >= 0) { "Mission with id '$missionId' not found." }

            val mission = activeMissions[index]
            if (mission.status == MissionStatus.CLAIMED) {
                check(economyService.ledger.findByIdempotencyKey("CLAIM_MISSION_${mission.id}") != null) {
                    "Claimed mission is missing its ledger transaction"
                }
                // Return idempotent result
                val txResult = economyService.claimMissionReward(mission.id, mission.gemReward, timestampMs)
                return Pair(mission, txResult)
            }

            check(mission.status == MissionStatus.COMPLETED) {
                "Cannot claim mission '$missionId' with status: ${mission.status} (progress: ${mission.currentCount}/${mission.targetCount})"
            }

            val txResult = economyService.claimMissionReward(mission.id, mission.gemReward, timestampMs)
            val updatedMission = mission.copy(status = MissionStatus.CLAIMED)
            activeMissions[index] = updatedMission
            return Pair(updatedMission, txResult)
        }
    }
}
