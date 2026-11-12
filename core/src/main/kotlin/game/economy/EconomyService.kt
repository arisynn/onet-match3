package game.economy

/**
 * High-level domain service orchestrating player economy operations.
 * Strictly enforces all 10 Non-Negotiable Currency Rules.
 */
class EconomyService(
    val ledger: TransactionLedger = TransactionLedger(),
    val wallet: Wallet = Wallet(ledger)
) {

    @get:JvmName("gemBalanceValue")
    val gemBalance: Int
        get() = wallet.gemBalance

    fun getGemBalance(): Int = wallet.gemBalance

    /**
     * Rule 1: Claim mission reward.
     * Credits Gems from a completed mission.
     */
    fun claimMissionReward(
        missionId: String,
        amount: Int,
        timestampMs: Long = System.currentTimeMillis()
    ): TransactionResult {
        require(amount > 0) { "Mission reward amount must be positive: $amount" }
        val idempotencyKey = "CLAIM_MISSION_${missionId}"
        return ledger.record(
            type = TransactionType.CREDIT,
            currency = Currency.GEM,
            amount = amount,
            source = TransactionSource.MISSION_CLAIM,
            sourceId = missionId,
            idempotencyKey = idempotencyKey,
            timestampMs = timestampMs
        )
    }

    /**
     * Rule 1: Claim achievement tier reward.
     * Credits Gems from a completed achievement tier.
     */
    fun claimAchievementReward(
        achievementId: String,
        tierIndex: Int,
        amount: Int,
        timestampMs: Long = System.currentTimeMillis()
    ): TransactionResult {
        require(amount > 0) { "Achievement reward amount must be positive: $amount" }
        val idempotencyKey = "CLAIM_ACH_${achievementId}_TIER_${tierIndex}"
        return ledger.record(
            type = TransactionType.CREDIT,
            currency = Currency.GEM,
            amount = amount,
            source = TransactionSource.ACHIEVEMENT_CLAIM,
            sourceId = "${achievementId}_tier_$tierIndex",
            idempotencyKey = idempotencyKey,
            timestampMs = timestampMs
        )
    }

    /**
     * Rule 5 & 6: Purchase cosmetic from catalog.
     * Debits Gems for a visual cosmetic item.
     */
    fun purchaseCosmetic(
        cosmeticId: String,
        gemCost: Int,
        timestampMs: Long = System.currentTimeMillis()
    ): TransactionResult {
        require(gemCost > 0) { "Cosmetic gem cost must be positive: $gemCost" }
        val idempotencyKey = "PURCHASE_COSMETIC_${cosmeticId}"
        return ledger.record(
            type = TransactionType.DEBIT,
            currency = Currency.GEM,
            amount = gemCost,
            source = TransactionSource.COSMETIC_PURCHASE,
            sourceId = cosmeticId,
            idempotencyKey = idempotencyKey,
            timestampMs = timestampMs
        )
    }

    // --- Explicit Invariant Verification Methods (For Security Audit & Tests) ---

    /**
     * Rejection of Rule 2: Attempting to credit currency from direct gameplay.
     */
    fun rejectDirectGameplayReward(roundId: String, score: Int) {
        ledger.record(
            type = TransactionType.CREDIT,
            currency = Currency.GEM,
            amount = score,
            source = TransactionSource.FORBIDDEN_DIRECT_GAMEPLAY,
            sourceId = roundId,
            idempotencyKey = "GAMEPLAY_$roundId"
        )
    }

    /**
     * Rejection of Rule 2: Attempting to credit currency from a level-up.
     */
    fun rejectLevelUpReward(newLevel: Int, amount: Int) {
        ledger.record(
            type = TransactionType.CREDIT,
            currency = Currency.GEM,
            amount = amount,
            source = TransactionSource.FORBIDDEN_LEVEL_UP,
            sourceId = "level_$newLevel",
            idempotencyKey = "LEVEL_UP_$newLevel"
        )
    }

    /**
     * Rejection of Rule 3: Attempting to purchase currency with real money.
     */
    fun rejectRealMoneyCurrencyPurchase(sku: String, amount: Int) {
        ledger.record(
            type = TransactionType.CREDIT,
            currency = Currency.GEM,
            amount = amount,
            source = TransactionSource.FORBIDDEN_REAL_MONEY_IAP,
            sourceId = sku,
            idempotencyKey = "IAP_$sku"
        )
    }

    /**
     * Rejection of Rule 6: Attempting to buy a pay-to-win booster with currency.
     */
    fun rejectPayToWinBoosterPurchase(boosterType: String, cost: Int) {
        ledger.record(
            type = TransactionType.DEBIT,
            currency = Currency.GEM,
            amount = cost,
            source = TransactionSource.FORBIDDEN_PAY_TO_WIN_BOOSTER,
            sourceId = boosterType,
            idempotencyKey = "BOOSTER_$boosterType"
        )
    }
}
