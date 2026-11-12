package game.cosmetics

import game.economy.EconomyService
import game.economy.TransactionResult

/**
 * Service managing cosmetic catalog browsing, purchases via Gems, and inventory equipping.
 * Strictly enforces Rule 10: Zero gameplay advantages attached to cosmetics.
 */
class CosmeticService(
    val inventory: CosmeticInventory = CosmeticInventory()
) {

    /**
     * Purchases an Earnable cosmetic item using earned Gems.
     * Enforces Rule 5 (sufficient balance) and Rule 6 (zero pay-to-win).
     */
    @Synchronized
    fun purchaseEarnable(
        itemId: String,
        economyService: EconomyService,
        timestampMs: Long = System.currentTimeMillis()
    ): PurchaseResult {
        val item = CosmeticCatalog.getItem(itemId)
            ?: throw IllegalArgumentException("Cosmetic '$itemId' does not exist in catalog.")

        require(item.acquisitionType == CosmeticAcquisitionType.EARNABLE) {
            "Item '${item.name}' cannot be purchased with gems; acquisition type is ${item.acquisitionType}."
        }

        if (inventory.isOwned(itemId)) {
            return PurchaseResult.AlreadyOwned(item)
        }

        val txResult = economyService.purchaseCosmetic(item.id, item.gemPrice, timestampMs)
        inventory.unlock(item.id)

        return PurchaseResult.Success(item, txResult)
    }

    /**
     * Unlocks a Premium cosmetic item granted via an authorized non-consumable IAP Entitlement.
     */
    fun unlockFromEntitlement(skuId: String): List<CosmeticItem> {
        val entitlement = game.billing.EntitlementCatalog.getEntitlement(skuId)
        val idsToUnlock: List<String> = entitlement?.unlockedCosmeticIds ?: emptyList()
        val matchingItems = CosmeticCatalog.ALL_ITEMS.filter { it.id in idsToUnlock || it.iapSku == skuId }
        for (item in matchingItems) {
            inventory.unlock(item.id)
        }

        return matchingItems
    }
}

sealed class PurchaseResult {
    data class Success(val item: CosmeticItem, val transactionResult: TransactionResult) : PurchaseResult()
    data class AlreadyOwned(val item: CosmeticItem) : PurchaseResult()
}
