package game.missions

import game.economy.EconomyService
import game.economy.TransactionResult

/**
 * Service managing active missions, progress evaluation, and claiming.
 */
class MissionService(
    private val rotationEngine: MissionRotationEngine = MissionRotationEngine(),
    initialMissions: List<Mission> = emptyList()
) {
    private val activeMissions = mutableListOf<Mission>()

    init {
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
            val updatedDailies = rotationEngine.rolloverMissions(activeMissions, currentTimeMs, MissionType.DAILY)
            val updatedWeeklies = rotationEngine.rolloverMissions(activeMissions, currentTimeMs, MissionType.WEEKLY)

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
        synchronized(activeMissions) {
            for (i in activeMissions.indices) {
                val mission = activeMissions[i]
                if (mission.status == MissionStatus.IN_PROGRESS && mission.objective == objective) {
                    if (mission.targetTheme == null || mission.targetTheme == theme) {
                        val newCount = mission.currentCount + amount
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
