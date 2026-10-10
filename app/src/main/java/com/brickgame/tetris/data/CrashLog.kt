package com.brickgame.tetris.data

import android.content.Context
import android.os.Build
import com.brickgame.tetris.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * If the app crashes, the reason is written to a small file on the phone (nothing is sent
 * anywhere). The next launch shows it once, so the player can share it with us.
 */
object CrashLog {
    private const val FILE = "last_crash.txt"
    @Volatile private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val file = File(context.applicationContext.filesDir, FILE)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
                file.writeText(buildString {
                    appendLine("Brickwell ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                    appendLine("${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    appendLine("Thread: ${thread.name}")
                    appendLine()
                    append(trace.take(12_000))
                })
            } catch (_: Throwable) { }
            previous?.uncaughtException(thread, error)
        }
    }

    /** The report from the last crash, removed once read (so it shows only once). */
    fun takeLast(context: Context): String? {
        val file = File(context.filesDir, FILE)
        if (!file.exists()) return null
        return try { file.readText().also { file.delete() } } catch (_: Exception) { null }
    }
}
