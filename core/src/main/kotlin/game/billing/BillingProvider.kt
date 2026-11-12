package game.billing

/**
 * Record of a verified purchase transaction from Google Play Billing.
 */
data class PurchaseRecord(
    val skuId: String,
    val purchaseToken: String,
    val purchaseTimeMs: Long,
    val isAcknowledged: Boolean
)

/**
 * Result of a Google Play purchase flow.
 */
sealed class BillingResult {
    data class Success(val purchase: PurchaseRecord) : BillingResult()
    data object UserCancelled : BillingResult()
    data class ItemAlreadyOwned(val skuId: String) : BillingResult()
    data class Error(val code: Int, val message: String) : BillingResult()
    data class Pending(val skuId: String) : BillingResult()
}

/**
 * Abstraction over Google Play Billing Client.
 * Decouples core logic from Android platform dependencies.
 */
interface BillingProvider {
    fun isConnected(): Boolean
    fun getAvailableEntitlements(): List<Entitlement>
    fun queryPurchasedSkus(): Set<String>
    fun launchPurchaseFlow(skuId: String): BillingResult
    fun acknowledgePurchase(purchaseToken: String): Boolean
    /** Only settled purchases authenticated by the provider/backend may appear here. */
    fun queryPurchases(): List<PurchaseRecord> = emptyList()
    /** Implementations must fail closed when signature/server verification is unavailable. */
    fun verifyPurchase(purchase: PurchaseRecord): Boolean = false
}

/** Safe default for offline/dev builds: no product price or success is fabricated. */
class UnavailableBillingProvider : BillingProvider {
    override fun isConnected() = false
    override fun getAvailableEntitlements(): List<Entitlement> = emptyList()
    override fun queryPurchasedSkus(): Set<String> = emptySet()
    override fun launchPurchaseFlow(skuId: String): BillingResult =
        BillingResult.Error(-1, "Purchases are unavailable in this build")
    override fun acknowledgePurchase(purchaseToken: String) = false
}
