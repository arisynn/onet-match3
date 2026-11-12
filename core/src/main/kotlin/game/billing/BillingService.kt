package game.billing

import game.cosmetics.CosmeticService
import game.economy.CurrencyRuleViolationException

/** Fail-closed cosmetic entitlements: pending, cancelled and unverified results never unlock. */
class BillingService(private val provider: BillingProvider, private val cosmeticService: CosmeticService) {
    private val ownedEntitlements = mutableSetOf<String>()
    init { restorePurchases() }

    fun isAvailable(): Boolean = provider.isConnected()
    fun availableEntitlements(): List<Entitlement> = if (isAvailable()) provider.getAvailableEntitlements() else emptyList()
    fun isAdFree(): Boolean = synchronized(ownedEntitlements) {
        ownedEntitlements.any { EntitlementCatalog.getEntitlement(it)?.removesAds == true }
    }

    fun restorePurchases(): Set<String> = synchronized(ownedEntitlements) {
        if (!provider.isConnected()) return@synchronized ownedEntitlements.toSet()
        val restored = provider.queryPurchases().filter(::verified).map { it.skuId }.toSet()
        ownedEntitlements.clear()
        ownedEntitlements.addAll(restored)
        restored.forEach(cosmeticService::unlockFromEntitlement)
        ownedEntitlements.toSet()
    }

    fun purchaseEntitlement(skuId: String): BillingResult {
        if (skuId.contains("gem", true) || skuId.contains("coin", true)) {
            throw CurrencyRuleViolationException(3, "Real-money currency purchases are prohibited")
        }
        if (EntitlementCatalog.getEntitlement(skuId) == null) return BillingResult.Error(404, "Unknown cosmetic product")
        if (!provider.isConnected()) return BillingResult.Error(-1, "Purchases are unavailable")
        return when (val result = provider.launchPurchaseFlow(skuId)) {
            is BillingResult.Success -> {
                if (result.purchase.skuId != skuId || !verified(result.purchase)) {
                    BillingResult.Error(403, "Purchase verification is unavailable or failed")
                } else {
                    synchronized(ownedEntitlements) { ownedEntitlements.add(skuId) }
                    cosmeticService.unlockFromEntitlement(skuId)
                    result
                }
            }
            // AlreadyOwned is a request to reconcile, never proof of ownership by itself.
            is BillingResult.ItemAlreadyOwned -> {
                restorePurchases()
                if (isEntitlementOwned(skuId)) result else BillingResult.Error(403, "Ownership could not be verified")
            }
            else -> result
        }
    }

    private fun verified(record: PurchaseRecord): Boolean =
        record.purchaseToken.isNotBlank() && EntitlementCatalog.getEntitlement(record.skuId) != null &&
            provider.verifyPurchase(record) && (record.isAcknowledged || provider.acknowledgePurchase(record.purchaseToken))

    fun isEntitlementOwned(skuId: String): Boolean = synchronized(ownedEntitlements) { skuId in ownedEntitlements }
}
