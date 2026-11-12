package game.billing

import game.cosmetics.CosmeticInventory
import game.cosmetics.CosmeticService
import game.economy.CurrencyRuleViolationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BillingServiceTest {

    private lateinit var fakeProvider: FakeBillingProvider
    private lateinit var cosmeticInventory: CosmeticInventory
    private lateinit var cosmeticService: CosmeticService
    private lateinit var billingService: BillingService

    @BeforeEach
    fun setUp() {
        fakeProvider = FakeBillingProvider()
        cosmeticInventory = CosmeticInventory()
        cosmeticService = CosmeticService(cosmeticInventory)
        billingService = BillingService(fakeProvider, cosmeticService)
    }

    @Test
    fun testPurchaseEntitlementAndAdFreeUnlock() {
        assertFalse(billingService.isAdFree())
        assertFalse(cosmeticInventory.isOwned("theme_autumn_harvest"))

        val result = billingService.purchaseEntitlement("entitlement_autumn_harvest")
        assertTrue(result is BillingResult.Success)
        assertTrue(billingService.isEntitlementOwned("entitlement_autumn_harvest"))
        assertTrue(billingService.isAdFree()) // All entitlements include ad removal
        assertTrue(cosmeticInventory.isOwned("theme_autumn_harvest"))
    }

    @Test
    fun testCollectorBundleUnlocksAllThemes() {
        val result = billingService.purchaseEntitlement("entitlement_collector_bundle")
        assertTrue(result is BillingResult.Success)
        assertTrue(cosmeticInventory.isOwned("theme_autumn_harvest"))
        assertTrue(cosmeticInventory.isOwned("theme_magic_atelier"))
        assertTrue(cosmeticInventory.isOwned("theme_retro_arcade"))
        assertTrue(cosmeticInventory.isOwned("theme_alpine_winter"))
    }

    @Test
    fun testRule3_RejectionOfConsumableCurrency() {
        assertThrows(CurrencyRuleViolationException::class.java) {
            billingService.purchaseEntitlement("gem_pack_100")
        }
        assertThrows(CurrencyRuleViolationException::class.java) {
            billingService.purchaseEntitlement("coin_bundle_large")
        }
    }

    @Test
    fun testRestorePurchases() {
        fakeProvider.launchPurchaseFlow("entitlement_magic_atelier")

        // New service instance simulates reinstall / restore on new device
        val freshCosmetics = CosmeticInventory()
        val freshCosmeticService = CosmeticService(freshCosmetics)
        val freshBilling = BillingService(fakeProvider, freshCosmeticService)

        val restored = freshBilling.restorePurchases()
        assertTrue(restored.contains("entitlement_magic_atelier"))
        assertTrue(freshCosmetics.isOwned("theme_magic_atelier"))
    }
}
