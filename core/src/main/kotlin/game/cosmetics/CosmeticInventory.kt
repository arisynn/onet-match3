package game.cosmetics

/**
 * Player inventory tracking owned cosmetics and currently equipped visual styles.
 */
class CosmeticInventory(
    initialUnlockedIds: Set<String> = CosmeticCatalog.DEFAULT_UNLOCKED_IDS,
    initialEquipped: Map<CosmeticType, String> = defaultEquippedMap()
) {
    companion object {
        fun defaultEquippedMap(): Map<CosmeticType, String> = mapOf(
            CosmeticType.TILE_SET to CosmeticCatalog.THEME_CANDY.id,
            CosmeticType.TILE_BODY to CosmeticCatalog.BODY_SQUARE_IVORY.id,
            CosmeticType.UI_PALETTE to CosmeticCatalog.PALETTE_IVORY.id
        )
    }

    private val unlocked = mutableSetOf<String>().apply {
        addAll(CosmeticCatalog.DEFAULT_UNLOCKED_IDS)
        addAll(initialUnlockedIds.filter { CosmeticCatalog.getItem(it) != null })
    }
    private val equipped = mutableMapOf<CosmeticType, String>().apply {
        putAll(defaultEquippedMap())
        initialEquipped.forEach { (type, id) ->
            if (id in unlocked && CosmeticCatalog.getItem(id)?.type == type) put(type, id)
        }
    }

    fun isOwned(id: String): Boolean = synchronized(unlocked) {
        unlocked.contains(id)
    }

    fun getUnlockedCount(): Int = synchronized(unlocked) {
        unlocked.size
    }

    fun getUnlockedIds(): Set<String> = synchronized(unlocked) {
        HashSet(unlocked)
    }

    fun unlock(id: String): Boolean = synchronized(unlocked) {
        if (CosmeticCatalog.getItem(id) == null) false else unlocked.add(id)
    }

    fun getEquipped(type: CosmeticType): String? = synchronized(equipped) {
        equipped[type]
    }

    fun equip(id: String, type: CosmeticType? = null): Boolean = synchronized(unlocked) {
        val targetType = type ?: CosmeticCatalog.getItem(id)?.type ?: return false
        if (!unlocked.contains(id) || CosmeticCatalog.getItem(id)?.type != targetType) return false
        synchronized(equipped) {
            equipped[targetType] = id
        }
        return true
    }

    fun getEquippedMap(): Map<CosmeticType, String> = synchronized(equipped) {
        HashMap(equipped)
    }
}
