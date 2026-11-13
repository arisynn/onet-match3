package com.onetmatch3.game

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import game.platform.GamePlatform
import game.platform.HapticEffect

/** Uses the declared VIBRATE permission; gracefully handles devices without a vibrator. */
class AndroidGamePlatform(context: Context) : GamePlatform {
    @Volatile override var safeInsets = game.platform.SafeInsets()
    private val vibrator = context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    override fun monotonicTimeMillis(): Long = SystemClock.elapsedRealtime()
    override fun haptic(effect: HapticEffect) {
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        val duration = when (effect) {
            HapticEffect.SELECT -> 10L
            HapticEffect.MATCH -> 18L
            HapticEffect.INVALID -> 12L
            HapticEffect.CELEBRATE -> 32L
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                device.vibrate(VibrationEffect.createOneShot(duration, 65))
            } else {
                @Suppress("DEPRECATION")
                device.vibrate(duration)
            }
        } catch (_: SecurityException) { /* Device policy can disable haptics. */ }
    }
}
