package com.example.focusgate.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppLaunchWatcherService : AccessibilityService() {

    @Volatile
    private var allowPackages: Set<String> = emptySet()

    private val ignorePackages: Set<String> by lazy {
        setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.yourcompany.focusgate",
            applicationContext.packageName
        )
    }

    @Volatile
    private var lastLoggedPackage: String? = null

    @Volatile
    private var lastEventTimestamp: Long = 0L

    private val debounceMillis = 300L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        try {
            if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

            val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return

            if (allowPackages.isNotEmpty() && packageName !in allowPackages) return
            if (packageName in ignorePackages) return

            val now = SystemClock.elapsedRealtime()
            val lastPackage = lastLoggedPackage
            if (packageName == lastPackage) return
            if (now - lastEventTimestamp < debounceMillis) return

            lastLoggedPackage = packageName
            lastEventTimestamp = now

            Log.d("FocusGate", "foreground=$packageName")
        } catch (t: Throwable) {
            // ここでは例外を再送出せず、安全に握り潰す
        }
    }

    override fun onInterrupt() {
        // 監視のみなので特別な処理は不要
    }

    fun updateAllowList(packages: Set<String>) {
        allowPackages = packages
    }
}
