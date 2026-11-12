package game.cosmetics

import game.economy.EconomyService
import game.economy.InsufficientFundsException
import game.economy.TransactionLedger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CosmeticCatalogTest {

    private lateinit var inventory: CosmeticInventory
    private lateinit var service: CosmeticService
    private lateinit var economyService: EconomyService

    @BeforeEach
    fun setUp() {
        inventory = CosmeticInventory()
        service = CosmeticService(inventory)
        economyService = EconomyService(TransactionLedger())
    }

    @Test
    fun testDefaultInventoryContainsFreeCosmetics() {
        // Free themes
        assertTrue(inventory.isOwned("theme_candy"))
        assertTrue(inventory.isOwned("theme_ocean"))
        assertTrue(inventory.isOwned("theme_forest"))
        assertTrue(inventory.isOwned("theme_space"))
        assertTrue(inventory.isOwned("theme_beach"))

        // Free palettes & body
        assertTrue(inventory.isOwned("palette_ivory"))
        assertTrue(inventory.isOwned("palette_mint"))
        assertTrue(inventory.isOwned("palette_peach"))
        assertTrue(inventory.isOwned("body_square_ivory"))

        // Earnable / Premium should not be owned by default
        assertFalse(inventory.isOwned("theme_reward_bakery"))
        assertFalse(inventory.isOwned("theme_autumn_harvest"))
    }

    @Test
    fun testPurchaseEarnableCosmetic() {
        // Credit sufficient gems
        economyService.claimMissionReward("m_seed", 300)
        assertEquals(300, economyService.wallet.gemBalance)

        val result = service.purchaseEarnable("theme_reward_bakery", economyService)
        assertTrue(result is PurchaseResult.Success)
        assertTrue(inventory.isOwned("theme_reward_bakery"))
        assertEquals(100, economyService.wallet.gemBalance) // 300 - 200 = 100

        // Repurchasing already owned returns AlreadyOwned without debiting
        val repeatResult = service.purchaseEarnable("theme_reward_bakery", economyService)
        assertTrue(repeatResult is PurchaseResult.AlreadyOwned)
        assertEquals(100, economyService.wallet.gemBalance)
    }

    @Test
    fun testPurchaseFailsWhenInsufficientGems() {
        economyService.claimMissionReward("m_seed", 50)
        assertEquals(50, economyService.wallet.gemBalance)

        // Cost is 200 gems
        assertThrows(InsufficientFundsException::class.java) {
            service.purchaseEarnable("theme_reward_bakery", economyService)
        }
        assertFalse(inventory.isOwned("theme_reward_bakery"))
        assertEquals(50, economyService.wallet.gemBalance)
    }

    @Test
    fun testEquipCosmetics() {
        assertEquals("theme_candy", inventory.getEquipped(CosmeticType.TILE_SET))

        // Cannot equip unowned item
        assertFalse(inventory.equip("theme_reward_bakery"))
        assertEquals("theme_candy", inventory.getEquipped(CosmeticType.TILE_SET))

        // Can equip owned free item
        assertTrue(inventory.equip("theme_ocean"))
        assertEquals("theme_ocean", inventory.getEquipped(CosmeticType.TILE_SET))
    }

    @Test
    fun testUnlockFromEntitlement() {
        assertFalse(inventory.isOwned("theme_autumn_harvest"))

        val unlocked = service.unlockFromEntitlement("entitlement_autumn_harvest")
        assertEquals(1, unlocked.size)
        assertEquals("theme_autumn_harvest", unlocked[0].id)
        assertTrue(inventory.isOwned("theme_autumn_harvest"))
    }
}
