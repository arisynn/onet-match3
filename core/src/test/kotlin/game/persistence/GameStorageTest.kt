package game.persistence

import com.badlogic.gdx.Preferences
import game.OnetMatch3Game
import game.economy.Currency
import game.economy.TransactionSource
import game.economy.TransactionType
import game.progression.PlayerProfile
import game.progression.PlayerStats
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameStorageTest {

    private class InMemoryPreferences : Preferences {
        private val data = mutableMapOf<String, Any>()

        override fun putBoolean(key: String, `val`: Boolean): Preferences = apply { data[key] = `val` }
        override fun putInteger(key: String, `val`: Int): Preferences = apply { data[key] = `val` }
        override fun putLong(key: String, `val`: Long): Preferences = apply { data[key] = `val` }
        override fun putFloat(key: String, `val`: Float): Preferences = apply { data[key] = `val` }
        override fun putString(key: String, `val`: String): Preferences = apply { data[key] = `val` }
        override fun put(`vals`: Map<String, *>?): Preferences = apply {
            `vals`?.forEach { (k, v) -> if (v != null) data[k] = v }
        }

        override fun getBoolean(key: String): Boolean = data[key] as? Boolean ?: false
        override fun getInteger(key: String): Int = data[key] as? Int ?: 0
        override fun getLong(key: String): Long = data[key] as? Long ?: 0L
        override fun getFloat(key: String): Float = data[key] as? Float ?: 0f
        override fun getString(key: String): String = data[key] as? String ?: ""

        override fun getBoolean(key: String, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun getInteger(key: String, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getString(key: String, defValue: String): String = data[key] as? String ?: defValue

        override fun get(): Map<String, *> = HashMap(data)
        override fun contains(key: String): Boolean = data.containsKey(key)
        override fun clear() { data.clear() }
        override fun remove(key: String) { data.remove(key) }
        override fun flush() {}
    }

    @Test
    fun `save and load restores player profile, lifetime stats, and cryptographic ledger`() {
        val prefs = InMemoryPreferences()
        val storage = GameStorage(customPrefs = prefs)

        // 1. Setup initial game state
        val gameOriginal = OnetMatch3Game()
        gameOriginal.playerProfile = PlayerProfile(
            playerId = "test_player_42",
            level = 5,
            currentXp = 2300L,
            stats = PlayerStats(
                totalOnetRoundsPlayed = 12,
                totalOnetRoundsWon = 10,
                totalTripleMatchRoundsPlayed = 8,
                totalTripleMatchRoundsWon = 7,
                totalTilesMatched = 350L,
                highestComboStreak = 6,
                flawlessRoundsCount = 4,
                fastRoundsCount = 3,
                shufflesUsed = 1,
                hintsUsed = 2,
                undosUsed = 0,
                themePlays = mapOf("candy" to 10, "ocean" to 10)
            )
        )
        gameOriginal.selectedLevel = 7
        gameOriginal.selectedTheme = "ocean"

        // Credit some gems to ledger
        gameOriginal.economyService.claimMissionReward("daily_win_onet", 15, 1000L)
        gameOriginal.economyService.claimAchievementReward("onet_master", 0, 50, 2000L)
        assertEquals(65, gameOriginal.economyService.gemBalance)

        // Unlock a theme
        gameOriginal.cosmeticService.inventory.unlock("theme_reward_bakery")
        assertTrue(gameOriginal.cosmeticService.inventory.isOwned("theme_reward_bakery"))

        // Save
        storage.save(gameOriginal)

        // 2. Create a clean fresh game instance and restore
        val gameRestored = OnetMatch3Game()
        assertEquals(0, gameRestored.economyService.gemBalance)
        assertEquals(1, gameRestored.playerProfile.level)
        assertFalse(gameRestored.cosmeticService.inventory.isOwned("theme_reward_bakery"))

        // Load
        storage.load(gameRestored)

        // 3. Verify exact restoration
        assertEquals("test_player_42", gameRestored.playerProfile.playerId)
        assertEquals(5, gameRestored.playerProfile.level)
        assertEquals(2300L, gameRestored.playerProfile.currentXp)
        assertEquals(12, gameRestored.playerProfile.stats.totalOnetRoundsPlayed)
        assertEquals(10, gameRestored.playerProfile.stats.totalOnetRoundsWon)
        assertEquals(350L, gameRestored.playerProfile.stats.totalTilesMatched)
        assertEquals(6, gameRestored.playerProfile.stats.highestComboStreak)
        assertEquals(10, gameRestored.playerProfile.stats.themePlays["candy"])

        assertEquals(7, gameRestored.selectedLevel)
        assertEquals("ocean", gameRestored.selectedTheme)

        assertEquals(65, gameRestored.economyService.gemBalance)
        assertEquals(2, gameRestored.economyService.ledger.getTransactionCount())
        assertTrue(gameRestored.economyService.ledger.verifyIntegrity(), "Restored ledger must have unbroken SHA-256 chain")

        assertTrue(gameRestored.cosmeticService.inventory.isOwned("theme_reward_bakery"))
    }
}
