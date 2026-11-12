package game.billing

import game.cosmetics.CosmeticService
import game.economy.CurrencyRuleViolationException

/**
 * Service coordinating in-app billing entitlements with cosmetic unlocks.
 * Enforces Rule 3: Zero real-money currency purchases.
 */
class BillingService(
    private val provider: BillingProvider,
    private val cosmeticService: CosmeticService
) {

    private val ownedEntitlements = mutableSetOf<String>()

    init {
        restorePurchases()
    }

    /**
     * Checks if the player owns the Ad-Free entitlement.
     */
    fun isAdFree(): Boolean = synchronized(ownedEntitlements) {
        ownedEntitlements.any { sku ->
            EntitlementCatalog.getEntitlement(sku)?.removesAds == true
        }
    }

    /**
     * Queries and restores all previously purchased entitlements from Google Play.
     */
    fun restorePurchases(): Set<String> {
        val purchasedSkus = provider.queryPurchasedSkus()
        synchronized(ownedEntitlements) {
            ownedEntitlements.clear()
            for (sku in purchasedSkus) {
                ownedEntitlements.add(sku)
                cosmeticService.unlockFromEntitlement(sku)
            }
            return HashSet(ownedEntitlements)
        }
    }

    /**
     * Initiates purchase flow for a non-consumable entitlement.
     */
    fun purchaseEntitlement(skuId: String): BillingResult {
        // Enforce Rule 3: Reject any attempt to sell consumable currency
        if (skuId.contains("gem", ignoreCase = true) || skuId.contains("coin", ignoreCase = true)) {
            throw CurrencyRuleViolationException(
                ruleNumber = 3,
                message = "Prohibited real-money currency purchase attempt: SKU '$skuId'."
            )
        }

        val result = provider.launchPurchaseFlow(skuId)
        if (result is BillingResult.Success) {
            synchronized(ownedEntitlements) {
                ownedEntitlements.add(skuId)
            }
            cosmeticService.unlockFromEntitlement(skuId)
        }
        return result
    }

    fun isEntitlementOwned(skuId: String): Boolean = synchronized(ownedEntitlements) {
        ownedEntitlements.contains(skuId)
    }
}
