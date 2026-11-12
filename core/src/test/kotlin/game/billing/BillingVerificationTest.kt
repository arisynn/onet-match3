package game.billing

import game.cosmetics.CosmeticInventory
import game.cosmetics.CosmeticService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BillingVerificationTest {
    private val sku = "entitlement_autumn_harvest"
    private class UntrustedProvider(var result: BillingResult) : BillingProvider {
        override fun isConnected() = true
        override fun getAvailableEntitlements() = EntitlementCatalog.ALL_ENTITLEMENTS
        override fun queryPurchasedSkus() = setOf("entitlement_autumn_harvest")
        override fun launchPurchaseFlow(skuId: String) = result
        override fun acknowledgePurchase(purchaseToken: String) = true
        // No authenticated queryPurchases/verifyPurchase implementation: fail closed.
    }

    @Test fun `unavailable default never simulates a successful purchase`() {
        val inventory = CosmeticInventory()
        val service = BillingService(UnavailableBillingProvider(), CosmeticService(inventory))
        assertFalse(service.isAvailable())
        assertTrue(service.availableEntitlements().isEmpty())
        assertTrue(service.purchaseEntitlement(sku) is BillingResult.Error)
        assertFalse(inventory.isOwned("theme_autumn_harvest"))
    }

    @Test fun `pending cancelled and forged success do not unlock cosmetics`() {
        val forged = PurchaseRecord(sku, "unverified-token", 1L, true)
        val provider = UntrustedProvider(BillingResult.Pending(sku))
        val inventory = CosmeticInventory()
        val service = BillingService(provider, CosmeticService(inventory))
        for (result in listOf(BillingResult.Pending(sku), BillingResult.UserCancelled, BillingResult.Success(forged), BillingResult.ItemAlreadyOwned(sku))) {
            provider.result = result
            service.purchaseEntitlement(sku)
            assertFalse(service.isEntitlementOwned(sku))
            assertFalse(inventory.isOwned("theme_autumn_harvest"))
        }
        assertTrue(service.restorePurchases().isEmpty(), "SKU strings are not verified purchase evidence")
    }
}
