package game.session

import game.OnetMatch3Game
import game.screen.ScreenType
import game.gameplay.onet.*
import game.gameplay.shared.SeededRandom
import game.gameplay.triplematch.*
import game.assets.TileAssetResolver
import game.economy.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SessionPersistenceTest {
    @TempDir lateinit var directory: File

    @Test fun `full model round trips including pending animation and undo`() {
        var onet = createOnetRound("candy", 1, SeededRandom("save"))
        val hint = hintFor(onet)!!
        onet = selectTile(selectTile(onet, hint.p1.r, hint.p1.c, 1), hint.p2.r, hint.p2.c, 2)
        val save = SaveState(onet = onet, roundId = "pending", roundHints = 3)
        val store = AtomicSaveStore(directory)
        store.save(save)
        assertEquals(save, store.load())
        var triple = startTripleRound(createTripleRound(TileAssetResolver.tileIdsFor("candy"), SeededRandom("triple")))
        triple = pickTile(triple, 0)
        val next = SaveState(triple = triple, roundMode = "TRIPLE_MATCH_GAME")
        store.save(next)
        assertEquals(next, store.load())
        assertEquals(undoPick(triple), undoPick(store.load()!!.triple!!))
    }

    @Test fun `corrupt primary recovers backup and incomplete temp cannot replace a commit`() {
        val store = AtomicSaveStore(directory)
        val first = SaveState(records = mapOf("ONET_GAME:1" to LevelRecord(100, 3, 1)))
        store.save(first)
        store.save(first.copy(unlockedOnet = 2))
        File(directory, "player-v1.pending").writeText("interrupted")
        File(directory, "player-v1.json").writeText("corrupt")
        assertEquals(first, store.load())
        assertTrue(store.recoveredFromBackup)
        store.save(first.copy(unlockedOnet = 3))
        assertEquals(3, store.load()!!.unlockedOnet)
    }

    @Test fun `round settlement is durable once and never grants gems`() {
        val game = OnetMatch3Game(store = AtomicSaveStore(directory))
        game.startNewRound(ScreenType.ONET_GAME, 1)
        val won = createOnetRound("candy", 1, SeededRandom("won")).copy(phase = "won", score = 900)
        game.completeOnet(won)
        val xp = game.playerProfile.currentXp
        game.completeOnet(won)
        assertEquals(xp, game.playerProfile.currentXp)
        assertEquals(0, game.economyService.gemBalance)
        val restored = AtomicSaveStore(directory).load()!!
        assertEquals(2, restored.unlockedOnet)
        assertEquals(1, restored.unlockedTriple)
        assertEquals(xp, restored.profile.currentXp)
        assertEquals(1, restored.records["ONET_GAME:1"]!!.wins)
        game.startNewRound(ScreenType.ONET_GAME, 1)
        game.completeOnet(won)
        assertTrue(game.lastRoundXp < xp)
        assertThrows(IllegalArgumentException::class.java) { game.startNewRound(ScreenType.TRIPLE_MATCH_GAME, 2) }
    }

    @Test fun `failed storage does not leave in-memory round rewards committed`() {
        val badDirectory = File(directory, "not-directory").apply { writeText("file") }
        val game = OnetMatch3Game(store = AtomicSaveStore(badDirectory))
        val state = createOnetRound("candy", 1, SeededRandom("rollback")).copy(phase = "won")
        assertThrows(Exception::class.java) { game.completeOnet(state) }
        assertEquals(0, game.playerProfile.currentXp)
        assertEquals(0, game.playerProfile.stats.totalOnetRoundsWon)
    }

    @Test fun `forged ledger and invalid equipped IDs cannot load`() {
        val tx = Transaction(type = TransactionType.CREDIT, amount = 20, source = TransactionSource.MISSION_CLAIM,
            sourceId = "mission", idempotencyKey = "claim")
        assertThrows(IllegalArgumentException::class.java) { SaveCodec.validate(SaveState(transactions = listOf(tx.copy(hash = "bad")))) }
        assertThrows(IllegalArgumentException::class.java) { SaveCodec.validate(SaveState(unlocked = setOf("unknown"))) }
    }
    private fun writeUncheckedPrimary(state: SaveState) {
        val payload = SaveCodec.encode(state)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray()).joinToString("") { "%02x".format(it) }
        val envelope = kotlinx.serialization.json.buildJsonObject {
            put("payload", kotlinx.serialization.json.JsonPrimitive(payload))
            put("sha256", kotlinx.serialization.json.JsonPrimitive(digest))
        }
        File(directory, "player-v1.json").writeText(envelope.toString())
    }

    @Test fun `checksum valid semantic corruption recovers the valid backup before restore`() {
        val store = AtomicSaveStore(directory)
        val first = SaveState(unlockedOnet = 2)
        store.save(first)
        store.save(first.copy(unlockedOnet = 3))
        val invalid = game.achievements.AchievementCatalog.createInitialCatalog().toMutableList()
        invalid[0] = invalid[0].copy(id = "missing-achievement")
        writeUncheckedPrimary(first.copy(achievements = invalid))
        assertEquals(first, store.load())
        assertTrue(store.recoveredFromBackup)
        val mission = game.missions.MissionRotationEngine().generateDailyMissions(0, 0).first()
        writeUncheckedPrimary(first.copy(missions = listOf(mission.copy(targetCount = -1))))
        assertEquals(first, store.load())
        assertTrue(store.recoveredFromBackup)
    }

    @Test fun `invalid pending selections and undo footprints are rejected`() {
        val onet = createOnetRound("candy", 1, SeededRandom("pending-invalid"))
        assertThrows(IllegalArgumentException::class.java) {
            SaveCodec.validate(SaveState(onet = onet.copy(selected = Point(11, 7))))
        }
        val hint = hintFor(onet)!!
        val pending = selectTile(selectTile(onet, hint.p1.r, hint.p1.c, 1), hint.p2.r, hint.p2.c, 2)
        assertThrows(IllegalArgumentException::class.java) {
            SaveCodec.validate(SaveState(onet = pending.copy(pending = pending.pending!!.copy(points = Int.MAX_VALUE))))
        }
        val triple = pickTile(startTripleRound(createTripleRound(TileAssetResolver.tileIdsFor("candy"), SeededRandom("bad-undo"))), 0)
        assertThrows(IllegalArgumentException::class.java) {
            SaveCodec.validate(SaveState(roundMode = "TRIPLE_MATCH_GAME", triple = triple.copy(undo = triple.undo!!.copy(stacks = emptyList()))))
        }
        // Single-use undo retains its snapshot even after the pick is restored.
        SaveCodec.validate(SaveState(roundMode = "TRIPLE_MATCH_GAME", triple = undoPick(triple)))
    }

    @Test fun `mission claim disk failure rolls back reward and permits exactly one retry`() {
        val store = AtomicSaveStore(directory)
        val instance = OnetMatch3Game(store = store)
        val generated = game.missions.MissionRotationEngine().generateDailyMissions(System.currentTimeMillis(),
            System.currentTimeMillis() / game.missions.MissionRotationEngine.MS_PER_DAY).first()
        val completed = generated.copy(currentCount = generated.targetCount, status = game.missions.MissionStatus.COMPLETED)
        instance.missionService = game.missions.MissionService(initialMissions = listOf(completed))
        instance.saveNow()
        val committed = store.load()
        val obstruction = File(directory, "player-v1.pending").apply { mkdir() }
        assertThrows(Exception::class.java) { instance.claimMission(completed.id) }
        assertEquals(0, instance.economyService.gemBalance)
        assertEquals(0, instance.economyService.ledger.getTransactionCount())
        assertEquals(game.missions.MissionStatus.COMPLETED,
            instance.missionService.getActiveMissions().first { it.id == completed.id }.status)
        assertEquals(committed, store.load())
        assertTrue(obstruction.delete())
        instance.claimMission(completed.id)
        instance.claimMission(completed.id)
        assertEquals(completed.gemReward, instance.economyService.gemBalance)
        assertEquals(1, instance.economyService.ledger.getTransactionCount())
        assertEquals(1, store.load()!!.transactions.size)
    }

    @Test fun `late gameplay capture cannot resurrect an already settled round`() {
        val instance = OnetMatch3Game(store = AtomicSaveStore(directory))
        instance.startNewRound(ScreenType.ONET_GAME, 1)
        val playing = createOnetRound("candy", 1, SeededRandom("late-callback"))
        instance.captureOnet(playing)
        instance.completeOnet(playing.copy(phase = "won", score = 100))
        val xp = instance.playerProfile.currentXp
        instance.captureOnet(playing)
        assertNull(instance.resumedOnet())
        instance.completeOnet(playing.copy(phase = "won", score = 100))
        assertEquals(xp, instance.playerProfile.currentXp)
        assertNull(AtomicSaveStore(directory).load()!!.onet)
    }

}
