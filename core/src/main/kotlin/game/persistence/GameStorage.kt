package game.persistence

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import game.OnetMatch3Game
import game.cosmetics.CosmeticType
import game.economy.*
import game.progression.PlayerProfile
import game.progression.PlayerStats

/**
 * Handles cross-platform persistent storage using libGDX Preferences (SharedPreferences on Android).
 * Persists player profile, lifetime stats, cryptographically-chained transaction ledger,
 * unlocked cosmetics, and user audio settings.
 */
class GameStorage(
    private val prefsName: String = "onet_match3_prefs",
    private val customPrefs: Preferences? = null
) {

    private fun getPreferences(): Preferences? {
        if (customPrefs != null) return customPrefs
        return try {
            Gdx.app?.getPreferences(prefsName)
        } catch (_: Exception) {
            null
        }
    }

    fun save(game: OnetMatch3Game) {
        val prefs = getPreferences() ?: return
        try {
            // 1. Player Profile & Stats
            val profile = game.playerProfile
            prefs.putString("player_id", profile.playerId)
            prefs.putInteger("level", profile.level)
            prefs.putLong("current_xp", profile.currentXp)

            val stats = profile.stats
            prefs.putInteger("stat_onet_played", stats.totalOnetRoundsPlayed)
            prefs.putInteger("stat_onet_won", stats.totalOnetRoundsWon)
            prefs.putInteger("stat_triple_played", stats.totalTripleMatchRoundsPlayed)
            prefs.putInteger("stat_triple_won", stats.totalTripleMatchRoundsWon)
            prefs.putLong("stat_tiles_matched", stats.totalTilesMatched)
            prefs.putInteger("stat_highest_combo", stats.highestComboStreak)
            prefs.putInteger("stat_flawless", stats.flawlessRoundsCount)
            prefs.putInteger("stat_fast", stats.fastRoundsCount)
            prefs.putInteger("stat_shuffles", stats.shufflesUsed)
            prefs.putInteger("stat_hints", stats.hintsUsed)
            prefs.putInteger("stat_undos", stats.undosUsed)

            val themePlaysStr = stats.themePlays.entries.joinToString(";") { "${it.key}:${it.value}" }
            prefs.putString("stat_theme_plays", themePlaysStr)

            // 2. Cosmetics & Selected Theme
            prefs.putString("selected_theme", game.selectedTheme)
            val unlockedStr = game.cosmeticService.inventory.getUnlockedIds().joinToString(",")
            prefs.putString("cosmetics_unlocked", unlockedStr)

            val equippedStr = game.cosmeticService.inventory.getEquippedMap()
                .entries.joinToString(";") { "${it.key.name}=${it.value}" }
            prefs.putString("cosmetics_equipped", equippedStr)

            // 3. Transactions Ledger
            val txList = game.economyService.ledger.getTransactions()
            val txLines = txList.joinToString("\n") { tx ->
                "${tx.id}|${tx.timestampMs}|${tx.type.name}|${tx.currency.name}|${tx.amount}|${tx.source.name}|${tx.sourceId}|${tx.idempotencyKey}|${tx.previousHash}|${tx.hash}"
            }
            prefs.putString("ledger_transactions", txLines)

            // 4. Audio settings
            prefs.putBoolean("audio_sfx_muted", game.soundService.isSfxMuted)
            prefs.putBoolean("audio_music_muted", game.soundService.isMusicMuted)
            prefs.putFloat("audio_sfx_volume", game.soundService.sfxVolume)
            prefs.putFloat("audio_music_volume", game.soundService.musicVolume)

            // 5. Selected Level
            prefs.putInteger("selected_level", game.selectedLevel)

            prefs.flush()
        } catch (e: Exception) {
            Gdx.app?.error("GameStorage", "Failed to save game state", e)
        }
    }

    fun load(game: OnetMatch3Game) {
        val prefs = getPreferences() ?: return
        try {
            if (!prefs.contains("player_id")) return

            // 1. Audio Settings
            if (prefs.contains("audio_sfx_muted")) {
                game.soundService.isSfxMuted = prefs.getBoolean("audio_sfx_muted", false)
            }
            if (prefs.contains("audio_music_muted")) {
                game.soundService.isMusicMuted = prefs.getBoolean("audio_music_muted", false)
            }
            if (prefs.contains("audio_sfx_volume")) {
                game.soundService.sfxVolume = prefs.getFloat("audio_sfx_volume", 1.0f)
            }
            if (prefs.contains("audio_music_volume")) {
                game.soundService.musicVolume = prefs.getFloat("audio_music_volume", 0.8f)
            }

            // 2. Selected Level & Theme
            if (prefs.contains("selected_level")) {
                game.selectedLevel = prefs.getInteger("selected_level", 1)
            }
            if (prefs.contains("selected_theme")) {
                game.selectedTheme = prefs.getString("selected_theme", "candy")
            }

            // 3. Cosmetics Inventory
            if (prefs.contains("cosmetics_unlocked")) {
                val unlockedStr = prefs.getString("cosmetics_unlocked", "")
                unlockedStr.split(",").filter { it.isNotBlank() }.forEach { id ->
                    game.cosmeticService.inventory.unlock(id)
                }
            }

            if (prefs.contains("cosmetics_equipped")) {
                val equippedStr = prefs.getString("cosmetics_equipped", "")
                equippedStr.split(";").filter { it.isNotBlank() }.forEach { entry ->
                    val parts = entry.split("=")
                    if (parts.size == 2) {
                        try {
                            val type = CosmeticType.valueOf(parts[0])
                            game.cosmeticService.inventory.equip(parts[1], type)
                        } catch (_: Exception) {}
                    }
                }
            }

            // 4. Transaction Ledger
            if (prefs.contains("ledger_transactions")) {
                val txLines = prefs.getString("ledger_transactions", "")
                if (txLines.isNotBlank()) {
                    val restoredTransactions = txLines.split("\n").mapNotNull { line ->
                        val parts = line.split("|")
                        if (parts.size >= 10) {
                            try {
                                Transaction(
                                    id = parts[0],
                                    timestampMs = parts[1].toLong(),
                                    type = TransactionType.valueOf(parts[2]),
                                    currency = Currency.valueOf(parts[3]),
                                    amount = parts[4].toInt(),
                                    source = TransactionSource.valueOf(parts[5]),
                                    sourceId = parts[6],
                                    idempotencyKey = parts[7],
                                    previousHash = parts[8],
                                    hash = parts[9]
                                )
                            } catch (_: Exception) { null }
                        } else null
                    }

                    if (restoredTransactions.isNotEmpty()) {
                        val restoredLedger = TransactionLedger(restoredTransactions)
                        if (restoredLedger.verifyIntegrity()) {
                            game.economyService.replaceLedger(restoredLedger)
                        }
                    }
                }
            }

            // 5. Player Stats & Profile
            val themePlaysStr = prefs.getString("stat_theme_plays", "")
            val themePlays = if (themePlaysStr.isNotBlank()) {
                themePlaysStr.split(";").mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        val count = parts[1].toIntOrNull() ?: 0
                        parts[0] to count
                    } else null
                }.toMap()
            } else emptyMap()

            val stats = PlayerStats(
                totalOnetRoundsPlayed = prefs.getInteger("stat_onet_played", 0),
                totalOnetRoundsWon = prefs.getInteger("stat_onet_won", 0),
                totalTripleMatchRoundsPlayed = prefs.getInteger("stat_triple_played", 0),
                totalTripleMatchRoundsWon = prefs.getInteger("stat_triple_won", 0),
                totalTilesMatched = prefs.getLong("stat_tiles_matched", 0L),
                highestComboStreak = prefs.getInteger("stat_highest_combo", 0),
                flawlessRoundsCount = prefs.getInteger("stat_flawless", 0),
                fastRoundsCount = prefs.getInteger("stat_fast", 0),
                shufflesUsed = prefs.getInteger("stat_shuffles", 0),
                hintsUsed = prefs.getInteger("stat_hints", 0),
                undosUsed = prefs.getInteger("stat_undos", 0),
                themePlays = themePlays
            )

            game.playerProfile = PlayerProfile(
                playerId = prefs.getString("player_id", game.playerProfile.playerId),
                level = prefs.getInteger("level", 1),
                currentXp = prefs.getLong("current_xp", 0L),
                stats = stats
            )

            game.achievementService.syncStats(stats, game.cosmeticService.inventory.getUnlockedCount())
        } catch (e: Exception) {
            Gdx.app?.error("GameStorage", "Failed to load game state", e)
        }
    }
}
