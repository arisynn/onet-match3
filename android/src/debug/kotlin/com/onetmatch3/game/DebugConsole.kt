package com.onetmatch3.game

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
import java.util.ArrayDeque

/** Native UI, independent of GL. Cannot recover native crashes, process death or ANRs. */
class DebugConsole(private val activity: Activity, root: ViewGroup) {
    private val lines = ArrayDeque<String>()
    private val handler = Handler(Looper.getMainLooper())
    private val text = TextView(activity).apply { textSize = 11f; setTextColor(Color.WHITE); setTextIsSelectable(true) }
    private val panel = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL; setBackgroundColor(0xF0222222.toInt()); visibility = View.GONE
        setPadding(12, 12, 12, 12)
    }
    private val crashFile = File(activity.filesDir, "last-debug-crash.txt")
    private val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val crashHandler = Thread.UncaughtExceptionHandler { thread, error ->
        runCatching { crashFile.writeText(report() + "\n" + error.stackTraceToString()) }
        previousHandler?.uncaughtException(thread, error) ?: android.os.Process.killProcess(android.os.Process.myPid())
    }
    @Volatile private var rendered = false
    private val refresh = object : Runnable {
        override fun run() {
            if (panel.visibility == View.VISIBLE) renderLogs()
            handler.postDelayed(this, 1000)
        }
    }
    init {
        val overlay = FrameLayout(activity)
        root.addView(overlay, ViewGroup.LayoutParams(-1, -1))
        val buttons = LinearLayout(activity)
        fun button(label: String, action: () -> Unit) {
            buttons.addView(Button(activity).apply { this.text = label; textSize = 10f; setOnClickListener { action() } }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        button("Copy") { (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Startup diagnostics", report())) }
        button("Export") { share() }
        button("Clear") { synchronized(lines) { lines.clear() }; crashFile.delete(); renderLogs() }
        button("Close") { panel.visibility = View.GONE }
        panel.addView(buttons)
        panel.addView(ScrollView(activity).apply { addView(text) }, LinearLayout.LayoutParams(-1, 0, 1f))
        overlay.addView(panel, FrameLayout.LayoutParams(-1, (activity.resources.displayMetrics.heightPixels * .7).toInt(), Gravity.BOTTOM))
        overlay.addView(Button(activity).apply {
            this.text = "🐞"; contentDescription = "Debug console"
            setOnClickListener { panel.visibility = if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE; renderLogs() }
        }, FrameLayout.LayoutParams(64.dp(), 56.dp(), Gravity.TOP or Gravity.END).apply { topMargin = 32.dp() })
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        log("Application ${activity.packageName} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) debug=${BuildConfig.DEBUG}")
        log("Device ${Build.MANUFACTURER} ${Build.MODEL}; Android ${Build.VERSION.RELEASE} API ${Build.VERSION.SDK_INT}; ABI ${Build.SUPPORTED_ABIS.joinToString()}")
        if (crashFile.exists()) {
            log("ERROR previous crash:\n" + crashFile.readText().takeLast(65536))
            panel.visibility = View.VISIBLE
        }
        handler.post(refresh)
        handler.postDelayed({
            if (!rendered) {
                log("WARN no render heartbeat after 15 seconds. Inspect Logcat for GL initialization failure or an ANR.")
                panel.visibility = View.VISIBLE
                renderLogs()
            }
        }, 15000)
    }
    private fun Int.dp() = (this * activity.resources.displayMetrics.density).toInt()
    fun log(message: String) {
        if (message.startsWith("Render:")) rendered = true
        android.util.Log.println(if (message.contains("ERROR")) android.util.Log.ERROR else if (message.contains("WARN")) android.util.Log.WARN else android.util.Log.INFO, "OnetDiagnostics", message)
        synchronized(lines) {
            lines.addLast("${System.currentTimeMillis()} ${message.take(16000)}")
            while (lines.size > 250 || lines.sumOf { it.length } > 200000) lines.removeFirst()
        }
    }
    private fun report(): String = synchronized(lines) { lines.joinToString("\n") } +
        "\nJVM used=${Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()} max=${Runtime.getRuntime().maxMemory()} nativeHeap=${Debug.getNativeHeapAllocatedSize()}"
    private fun renderLogs() {
        val value = report()
        val styled = SpannableString(value)
        var start = 0
        value.lines().forEach { line ->
            val color = if (line.contains("ERROR") || line.contains("Exception")) Color.RED else if (line.contains("WARN")) Color.YELLOW else Color.WHITE
            styled.setSpan(ForegroundColorSpan(color), start, start + line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            start += line.length + 1
        }
        text.text = styled
    }
    private fun share() {
        try {
            val dir = File(activity.cacheDir, "diagnostics").apply { mkdirs() }
            val file = File(dir, "onet-diagnostics.txt").apply { writeText(report()) }
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.diagnostics", file)
            activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Export diagnostics"))
        } catch (error: Exception) { log("ERROR export: ${error.stackTraceToString()}") }
    }
    fun failure(error: Throwable) {
        log("ERROR startup: ${error.stackTraceToString()}")
        crashFile.writeText(report() + "\n" + error.stackTraceToString())
        handler.post { panel.visibility = View.VISIBLE; renderLogs() }
        // Caller must rethrow: recording diagnostics must not suppress a fatal exception.
    }
    fun close() {
        handler.removeCallbacksAndMessages(null)
        if (Thread.getDefaultUncaughtExceptionHandler() === crashHandler) Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    }
}
