package com.example.focusgate.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppLaunchWatcherService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        try {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
                Log.d("FocusGate", "foreground=$packageName")
            }
        } catch (t: Throwable) {
            // ここでは例外を再送出せず、安全に握り潰す
        }
    }

    override fun onInterrupt() {
        // 監視のみなので特別な処理は不要
    }
}
