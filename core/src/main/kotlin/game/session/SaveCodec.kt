package game.session

import game.progression.*
import game.economy.*
import game.missions.*
import game.achievements.*
import game.cosmetics.*
import game.gameplay.onet.*
import game.gameplay.triplematch.*
import game.assets.TileAssetResolver
import kotlinx.serialization.json.*
import java.lang.reflect.Modifier

/** Explicit decoder avoids unsafe allocation/reflection when restoring immutable models. */
object SaveCodec {
    fun encode(state: SaveState): String = value(state).toString()
    fun decode(text: String): SaveState = readSaveState(Json.parseToJsonElement(text).jsonObject)
    private fun value(input: Any?): JsonElement = when (input) {
        null -> JsonNull
        is String -> JsonPrimitive(input)
        is Number -> JsonPrimitive(input)
        is Boolean -> JsonPrimitive(input)
        is Enum<*> -> JsonPrimitive(input.name)
        is Map<*, *> -> JsonObject(input.entries.associate { (k, v) -> k.toString() to value(v) })
        is Iterable<*> -> JsonArray(input.map { value(it) })
        else -> JsonObject(input.javaClass.declaredFields.filter {
            !Modifier.isStatic(it.modifiers) && !it.isSynthetic
        }.associate { field -> field.isAccessible = true; field.name to value(field.get(input)) })
    }

    fun validate(s: SaveState) {
        require(s.schemaVersion == 1) { "Unsupported save version" }
        require(s.profile.currentXp in 0..100_000_000_000L && s.profile.level >= 1)
        require(s.unlockedOnet in 1..100000 && s.unlockedTriple in 1..100000 && s.roundLevel in 1..100000)
        require(s.records.size <= 200000 && s.transactions.size <= 50000)
        require(s.transactions.map { it.idempotencyKey }.toSet().size == s.transactions.size)
        val ledger = TransactionLedger(s.transactions)
        require(ledger.verifyIntegrity() && ledger.getBalance() >= 0) { "Invalid transaction history" }
        require(s.records.values.all { it.bestScore >= 0 && it.bestStars in 0..3 && it.wins >= 0 })
        require(s.unlocked.all { CosmeticCatalog.getItem(it) != null })
        require(s.equipped.all { (type, id) -> id in s.unlocked && CosmeticCatalog.getItem(id)?.type == type })
        require(s.missionTimeHighWater >= 0)
        require(s.roundMode in setOf("ONET_GAME", "TRIPLE_MATCH_GAME"))
        require(s.onet == null || s.triple == null)
        require(s.roundHints >= 0 && s.roundShuffles >= 0)
        require(s.settings.sfxVolume.isFinite() && s.settings.sfxVolume in 0f..1f)
        require(s.settings.musicVolume.isFinite() && s.settings.musicVolume in 0f..1f)
        s.onet?.let { round ->
            require(round.board.size == 12 && round.board.all { it.size == 8 })
            require(round.phase in setOf("playing", "won", "lost") && round.time in 0..round.duration && round.duration > 0)
            require(round.board.flatten().filterNotNull().all { TileAssetResolver.tileAssetFor(it.id) != null })
            round.board.forEachIndexed { r, row -> row.forEachIndexed { c, tile ->
                if (tile?.isSlave == true) require(tile.masterR in 1..10 && tile.masterC in 1..6 && round.board[tile.masterR][tile.masterC]?.isSlave == false)
                if (r == 0 || r == 11 || c == 0 || c == 7) require(tile == null)
            } }
        }
        s.triple?.let { round ->
            require(round.stacks.size == 12 && round.tray.size <= 7 && round.cleared >= 0)
            require(round.time in 0..round.duration && round.duration > 0 && round.phase in setOf("ready", "playing", "won", "lost"))
            require((round.stacks.flatten() + round.tray).all { TileAssetResolver.tileAssetFor(it) != null })
        }
    }

    private fun readSaveState(o: JsonObject) = SaveState(
        schemaVersion = o["schemaVersion"]!!.jsonPrimitive.int,
        profile = readPlayerProfile(o["profile"]!!.jsonObject),
        transactions = o["transactions"]!!.jsonArray.map { item -> readTransaction(item!!.jsonObject) },
        missions = o["missions"]!!.jsonArray.map { item -> readMission(item!!.jsonObject) },
        achievements = o["achievements"]!!.jsonArray.map { item -> readAchievement(item!!.jsonObject) },
        unlocked = o["unlocked"]!!.jsonArray.map { item -> item!!.jsonPrimitive.content }.toSet(),
        equipped = o["equipped"]!!.jsonObject.map { (key, value) -> enumValueOf<CosmeticType>(key) to value!!.jsonPrimitive.content }.toMap(),
        records = o["records"]!!.jsonObject.map { (key, value) -> key to readLevelRecord(value!!.jsonObject) }.toMap(),
        unlockedOnet = o["unlockedOnet"]!!.jsonPrimitive.int,
        unlockedTriple = o["unlockedTriple"]!!.jsonPrimitive.int,
        settings = readGameSettings(o["settings"]!!.jsonObject),
        tutorialModes = o["tutorialModes"]!!.jsonArray.map { item -> item!!.jsonPrimitive.content }.toSet(),
        roundId = o["roundId"]!!.jsonPrimitive.content,
        roundMode = o["roundMode"]!!.jsonPrimitive.content,
        roundLevel = o["roundLevel"]!!.jsonPrimitive.int,
        roundTheme = o["roundTheme"]!!.jsonPrimitive.content,
        onet = o["onet"]?.takeUnless { it is JsonNull }?.let { readOnetRoundState(it!!.jsonObject) },
        triple = o["triple"]?.takeUnless { it is JsonNull }?.let { readTripleRoundState(it!!.jsonObject) },
        roundHints = o["roundHints"]!!.jsonPrimitive.int,
        roundShuffles = o["roundShuffles"]!!.jsonPrimitive.int,
        completedRoundIds = o["completedRoundIds"]!!.jsonArray.map { item -> item!!.jsonPrimitive.content }.toSet(),
        missionTimeHighWater = o["missionTimeHighWater"]!!.jsonPrimitive.long,
    )

    private fun readLevelRecord(o: JsonObject) = LevelRecord(
        bestScore = o["bestScore"]!!.jsonPrimitive.int,
        bestStars = o["bestStars"]!!.jsonPrimitive.int,
        wins = o["wins"]!!.jsonPrimitive.int,
    )

    private fun readGameSettings(o: JsonObject) = GameSettings(
        musicEnabled = o["musicEnabled"]!!.jsonPrimitive.boolean,
        soundEnabled = o["soundEnabled"]!!.jsonPrimitive.boolean,
        hapticsEnabled = o["hapticsEnabled"]!!.jsonPrimitive.boolean,
        reducedMotion = o["reducedMotion"]!!.jsonPrimitive.boolean,
        reducedEffects = o["reducedEffects"]!!.jsonPrimitive.boolean,
        sfxVolume = o["sfxVolume"]!!.jsonPrimitive.float,
        musicVolume = o["musicVolume"]!!.jsonPrimitive.float,
    )

    private fun readPlayerProfile(o: JsonObject) = PlayerProfile(
        playerId = o["playerId"]!!.jsonPrimitive.content,
        level = o["level"]!!.jsonPrimitive.int,
        currentXp = o["currentXp"]!!.jsonPrimitive.long,
        stats = readPlayerStats(o["stats"]!!.jsonObject),
    )

    private fun readPlayerStats(o: JsonObject) = PlayerStats(
        totalOnetRoundsPlayed = o["totalOnetRoundsPlayed"]!!.jsonPrimitive.int,
        totalOnetRoundsWon = o["totalOnetRoundsWon"]!!.jsonPrimitive.int,
        totalTripleMatchRoundsPlayed = o["totalTripleMatchRoundsPlayed"]!!.jsonPrimitive.int,
        totalTripleMatchRoundsWon = o["totalTripleMatchRoundsWon"]!!.jsonPrimitive.int,
        totalTilesMatched = o["totalTilesMatched"]!!.jsonPrimitive.long,
        highestComboStreak = o["highestComboStreak"]!!.jsonPrimitive.int,
        flawlessRoundsCount = o["flawlessRoundsCount"]!!.jsonPrimitive.int,
        fastRoundsCount = o["fastRoundsCount"]!!.jsonPrimitive.int,
        shufflesUsed = o["shufflesUsed"]!!.jsonPrimitive.int,
        hintsUsed = o["hintsUsed"]!!.jsonPrimitive.int,
        undosUsed = o["undosUsed"]!!.jsonPrimitive.int,
        themePlays = o["themePlays"]!!.jsonObject.map { (key, value) -> key to value!!.jsonPrimitive.int }.toMap(),
    )

    private fun readTransaction(o: JsonObject) = Transaction(
        id = o["id"]!!.jsonPrimitive.content,
        timestampMs = o["timestampMs"]!!.jsonPrimitive.long,
        type = enumValueOf<TransactionType>(o["type"]!!.jsonPrimitive.content),
        currency = enumValueOf<Currency>(o["currency"]!!.jsonPrimitive.content),
        amount = o["amount"]!!.jsonPrimitive.int,
        source = enumValueOf<TransactionSource>(o["source"]!!.jsonPrimitive.content),
        sourceId = o["sourceId"]!!.jsonPrimitive.content,
        idempotencyKey = o["idempotencyKey"]!!.jsonPrimitive.content,
        previousHash = o["previousHash"]!!.jsonPrimitive.content,
        hash = o["hash"]!!.jsonPrimitive.content,
    )

    private fun readMission(o: JsonObject) = Mission(
        id = o["id"]!!.jsonPrimitive.content,
        title = o["title"]!!.jsonPrimitive.content,
        description = o["description"]!!.jsonPrimitive.content,
        type = enumValueOf<MissionType>(o["type"]!!.jsonPrimitive.content),
        objective = enumValueOf<MissionObjective>(o["objective"]!!.jsonPrimitive.content),
        targetCount = o["targetCount"]!!.jsonPrimitive.int,
        currentCount = o["currentCount"]!!.jsonPrimitive.int,
        gemReward = o["gemReward"]!!.jsonPrimitive.int,
        status = enumValueOf<MissionStatus>(o["status"]!!.jsonPrimitive.content),
        createdAtMs = o["createdAtMs"]!!.jsonPrimitive.long,
        expiryTimeMs = o["expiryTimeMs"]!!.jsonPrimitive.long,
        targetTheme = o["targetTheme"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.content },
    )

    private fun readAchievement(o: JsonObject) = Achievement(
        id = o["id"]!!.jsonPrimitive.content,
        title = o["title"]!!.jsonPrimitive.content,
        descriptionFormat = o["descriptionFormat"]!!.jsonPrimitive.content,
        category = enumValueOf<AchievementCategory>(o["category"]!!.jsonPrimitive.content),
        tiers = o["tiers"]!!.jsonArray.map { item -> readAchievementTier(item!!.jsonObject) },
        currentValue = o["currentValue"]!!.jsonPrimitive.long,
    )

    private fun readAchievementTier(o: JsonObject) = AchievementTier(
        tierIndex = o["tierIndex"]!!.jsonPrimitive.int,
        tierName = o["tierName"]!!.jsonPrimitive.content,
        requiredValue = o["requiredValue"]!!.jsonPrimitive.long,
        gemReward = o["gemReward"]!!.jsonPrimitive.int,
        isClaimed = o["isClaimed"]!!.jsonPrimitive.boolean,
    )

    private fun readOnetRoundState(o: JsonObject) = OnetRoundState(
        theme = o["theme"]!!.jsonPrimitive.content,
        level = o["level"]!!.jsonPrimitive.int,
        board = o["board"]!!.jsonArray.map { item -> item!!.jsonArray.map { item -> item?.takeUnless { it is JsonNull }?.let { readTile(it!!.jsonObject) } } },
        phase = o["phase"]!!.jsonPrimitive.content,
        paused = o["paused"]!!.jsonPrimitive.boolean,
        selected = o["selected"]?.takeUnless { it is JsonNull }?.let { readPoint(it!!.jsonObject) },
        pending = o["pending"]?.takeUnless { it is JsonNull }?.let { readPendingMatch(it!!.jsonObject) },
        score = o["score"]!!.jsonPrimitive.int,
        combo = o["combo"]!!.jsonPrimitive.int,
        highestCombo = o["highestCombo"]!!.jsonPrimitive.int,
        lastMatchAt = o["lastMatchAt"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.long },
        matches = o["matches"]!!.jsonPrimitive.int,
        mistakes = o["mistakes"]!!.jsonPrimitive.int,
        splits = o["splits"]!!.jsonPrimitive.int,
        time = o["time"]!!.jsonPrimitive.int,
        duration = o["duration"]!!.jsonPrimitive.int,
        event = o["event"]?.takeUnless { it is JsonNull }?.let { readOnetEvent(it!!.jsonObject) },
    )

    private fun readTile(o: JsonObject) = Tile(
        id = o["id"]!!.jsonPrimitive.content,
        themeKey = o["themeKey"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.content },
        groupRows = o["groupRows"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.int },
        groupCols = o["groupCols"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.int },
        isSplit = o["isSplit"]!!.jsonPrimitive.boolean,
        splitOrientation = o["splitOrientation"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.content },
        openSide = o["openSide"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.content },
        splitDepth = o["splitDepth"]!!.jsonPrimitive.int,
        splitAxis = o["splitAxis"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.content },
        justSplit = o["justSplit"]!!.jsonPrimitive.boolean,
        splitAnimKey = o["splitAnimKey"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.content },
        splitFromX = o["splitFromX"]!!.jsonPrimitive.int,
        splitFromY = o["splitFromY"]!!.jsonPrimitive.int,
        isSlave = o["isSlave"]!!.jsonPrimitive.boolean,
        masterR = o["masterR"]!!.jsonPrimitive.int,
        masterC = o["masterC"]!!.jsonPrimitive.int,
        direction = o["direction"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.content },
        broken = o["broken"]!!.jsonPrimitive.boolean,
        shape = o["shape"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.content },
    )

    private fun readPoint(o: JsonObject) = Point(
        r = o["r"]!!.jsonPrimitive.int,
        c = o["c"]!!.jsonPrimitive.int,
    )

    private fun readPendingMatch(o: JsonObject) = PendingMatch(
        a = readPoint(o["a"]!!.jsonObject),
        b = readPoint(o["b"]!!.jsonObject),
        action = o["action"]!!.jsonPrimitive.content,
        path = o["path"]!!.jsonArray.map { item -> readPoint(item!!.jsonObject) },
        points = o["points"]!!.jsonPrimitive.int,
    )

    private fun readOnetEvent(o: JsonObject) = OnetEvent(
        type = o["type"]!!.jsonPrimitive.content,
        tiles = o["tiles"]?.takeUnless { it is JsonNull }?.let { it!!.jsonArray.map { item -> readPoint(item!!.jsonObject) } },
        a = o["a"]?.takeUnless { it is JsonNull }?.let { readPoint(it!!.jsonObject) },
        b = o["b"]?.takeUnless { it is JsonNull }?.let { readPoint(it!!.jsonObject) },
        action = o["action"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.content },
        path = o["path"]?.takeUnless { it is JsonNull }?.let { it!!.jsonArray.map { item -> readPoint(item!!.jsonObject) } },
        points = o["points"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.int },
        duration = o["duration"]?.takeUnless { it is JsonNull }?.let { it!!.jsonPrimitive.long },
    )

    private fun readTripleRoundState(o: JsonObject) = TripleRoundState(
        stacks = o["stacks"]!!.jsonArray.map { item -> item!!.jsonArray.map { item -> item!!.jsonPrimitive.content } },
        tray = o["tray"]!!.jsonArray.map { item -> item!!.jsonPrimitive.content },
        cleared = o["cleared"]!!.jsonPrimitive.int,
        phase = o["phase"]!!.jsonPrimitive.content,
        paused = o["paused"]!!.jsonPrimitive.boolean,
        time = o["time"]!!.jsonPrimitive.int,
        duration = o["duration"]!!.jsonPrimitive.int,
        undo = o["undo"]?.takeUnless { it is JsonNull }?.let { readUndoState(it!!.jsonObject) },
        undoUsed = o["undoUsed"]!!.jsonPrimitive.boolean,
    )

    private fun readUndoState(o: JsonObject) = UndoState(
        stacks = o["stacks"]!!.jsonArray.map { item -> item!!.jsonArray.map { item -> item!!.jsonPrimitive.content } },
        tray = o["tray"]!!.jsonArray.map { item -> item!!.jsonPrimitive.content },
        cleared = o["cleared"]!!.jsonPrimitive.int,
    )
}
