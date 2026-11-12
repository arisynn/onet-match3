package game.billing

/**
 * In-memory test provider simulating Google Play Billing.
 */
class FakeBillingProvider(
    private val availableEntitlements: List<Entitlement> = EntitlementCatalog.ALL_ENTITLEMENTS
) : BillingProvider {

    private val ownedPurchases = mutableMapOf<String, PurchaseRecord>()
    private var connected: Boolean = true

    fun setConnected(status: Boolean) {
        connected = status
    }

    override fun isConnected(): Boolean = connected

    override fun getAvailableEntitlements(): List<Entitlement> = availableEntitlements

    override fun queryPurchasedSkus(): Set<String> = synchronized(ownedPurchases) {
        HashSet(ownedPurchases.keys)
    }

    override fun launchPurchaseFlow(skuId: String): BillingResult {
        if (!connected) {
            return BillingResult.Error(code = -1, message = "Billing client not connected")
        }

        // Rule 3 Validation: Explicit rejection of consumable currency purchases
        if (skuId.contains("gem", ignoreCase = true) || skuId.contains("coin", ignoreCase = true)) {
            return BillingResult.Error(
                code = 403,
                message = "Rule 3 Violation: In-app real money purchases for currency are prohibited."
            )
        }

        val entitlement = availableEntitlements.find { it.skuId == skuId }
            ?: return BillingResult.Error(code = 404, message = "SKU '$skuId' not found in entitlement catalog")

        synchronized(ownedPurchases) {
            if (ownedPurchases.containsKey(skuId)) {
                return BillingResult.ItemAlreadyOwned(skuId)
            }

            val record = PurchaseRecord(
                skuId = skuId,
                purchaseToken = "fake_token_${skuId}_${System.currentTimeMillis()}",
                purchaseTimeMs = System.currentTimeMillis(),
                isAcknowledged = true
            )
            ownedPurchases[skuId] = record
            return BillingResult.Success(record)
        }
    }

    override fun queryPurchases(): List<PurchaseRecord> = synchronized(ownedPurchases) { ownedPurchases.values.toList() }
    override fun verifyPurchase(purchase: PurchaseRecord): Boolean = synchronized(ownedPurchases) {
        ownedPurchases[purchase.skuId] == purchase
    }
    override fun acknowledgePurchase(purchaseToken: String): Boolean = synchronized(ownedPurchases) {
        ownedPurchases.values.any { it.purchaseToken == purchaseToken }
    }
}
