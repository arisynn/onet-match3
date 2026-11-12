package game.platform

import game.billing.BillingProvider
import game.billing.UnavailableBillingProvider

enum class HapticEffect { SELECT, MATCH, INVALID, CELEBRATE }

/** Device capabilities; no Android dependency enters gameplay or rendering. */
interface GamePlatform {
    val billingProvider: BillingProvider get() = UnavailableBillingProvider()
    fun haptic(effect: HapticEffect) {}
    fun monotonicTimeMillis(): Long = System.nanoTime() / 1_000_000L
}

object NoopGamePlatform : GamePlatform
