package com.onetmatch3.game
import android.app.Activity
import android.view.ViewGroup
class DebugConsole(activity: Activity, root: ViewGroup) {
    fun log(message: String) {}
    fun failure(error: Throwable) { throw error }
    fun close() {}
}
