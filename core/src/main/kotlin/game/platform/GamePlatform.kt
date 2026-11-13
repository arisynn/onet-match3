package game.platform

import game.billing.BillingProvider
import game.billing.UnavailableBillingProvider

data class SafeInsets(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0)

enum class HapticEffect { SELECT, MATCH, INVALID, CELEBRATE }

/** Device capabilities; no Android dependency enters gameplay or rendering. */
interface GamePlatform {
    val safeInsets: SafeInsets get() = SafeInsets()
    val billingProvider: BillingProvider get() = UnavailableBillingProvider()
    fun haptic(effect: HapticEffect) {}
    fun monotonicTimeMillis(): Long = System.nanoTime() / 1_000_000L
}

object NoopGamePlatform : GamePlatform
