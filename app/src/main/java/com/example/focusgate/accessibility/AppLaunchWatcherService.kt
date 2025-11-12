package com.example.focusgate.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.example.focusgate.overlay.FlowResultBroadcaster
import com.example.focusgate.overlay.OverlayNavigator
import com.example.focusgate.overlay.OverlayState
import java.util.concurrent.CopyOnWriteArraySet

class AppLaunchWatcherService : AccessibilityService() {

    @Volatile
    private var allowPackages: Set<String> = emptySet()

    private val ignorePackages: Set<String> by lazy {
        setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.googlequicksearchbox",
            "com.yourcompany.focusgate",
            applicationContext.packageName
        )
    }

    @Volatile
    private var lastLoggedPackage: String? = null

    @Volatile
    private var lastEventTimestamp: Long = 0L

    @Volatile
    private var handledPackage: String? = null

    private val handledPackages: MutableMap<String, Long> = mutableMapOf()
    private val handledCooldownMillis = 30_000L
    private val cancelCooldownMillis = 3_000L

    private val packageDebounceMillis = 2000L
    private val imeRefreshIntervalMillis = 60_000L
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val imePackages: MutableSet<String> = CopyOnWriteArraySet()
    private val imeFallbackPackages = setOf(
        "com.google.android.inputmethod.latin",
            "com.android.inputmethod.latin",
            "com.baidu.input",
            "com.samsung.android.honeyboard",
            "com.touchtype.swiftkey",
            "com.apple.inputmethod",
            "com.adamrocker.android.input.simeji"
    )
    private val inputMethodChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshImePackages()
        }
    }
    private val packageChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshImePackages()
        }
    }
    private val flowResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pkg = intent?.getStringExtra(FlowResultBroadcaster.EXTRA_PACKAGE) ?: return
            val result = intent.getStringExtra(FlowResultBroadcaster.EXTRA_RESULT) ?: return
            val now = SystemClock.elapsedRealtime()
            when (result) {
                FlowResultBroadcaster.RESULT_COMPLETED -> {
                    handledPackages[pkg] = now + handledCooldownMillis
                    Log.d(TAG, "flow_complete:$pkg")
                }
                FlowResultBroadcaster.RESULT_CANCELED -> {
                    handledPackages[pkg] = now + cancelCooldownMillis
                    Log.d(TAG, "flow_canceled:$pkg")
                }
            }
            if (handledPackage == pkg) {
                handledPackage = null
            }
            OverlayState.release()
        }
    }

    @Volatile
    private var lastImeRefreshAt: Long = 0L

    override fun onCreate() {
        super.onCreate()
        registerReceiver(
            inputMethodChangedReceiver,
            IntentFilter(Intent.ACTION_INPUT_METHOD_CHANGED)
        )
        refreshImePackages()
        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_DATA_CLEARED)
            addDataScheme("package")
        }
        registerReceiver(packageChangedReceiver, packageFilter)
        registerReceiver(flowResultReceiver, IntentFilter(FlowResultBroadcaster.ACTION_FLOW_RESULT))
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(inputMethodChangedReceiver)
        } catch (_: IllegalArgumentException) {
        }
        try {
            unregisterReceiver(packageChangedReceiver)
        } catch (_: IllegalArgumentException) {
        }
        try {
            unregisterReceiver(flowResultReceiver)
        } catch (_: IllegalArgumentException) {
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val now = SystemClock.elapsedRealtime()

        if (event == null) {
            logDrop("event=null")
            return
        }

        val eventTypeName = eventTypeToName(event.eventType)
        val packageNameRaw = event.packageName?.toString()
        val className = event.className?.toString()
        val delta = now - lastEventTimestamp

        Log.d(
            TAG,
            "event ts=$now type=$eventTypeName pkg=$packageNameRaw class=$className overlay=${OverlayState.isShowing()} lastPkg=$lastLoggedPackage delta=${delta}ms"
        )

        try {
            if (now - lastImeRefreshAt > imeRefreshIntervalMillis) {
                refreshImePackages()
            }

            pruneHandledPackages(now)

            if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                logDrop("event_type:$eventTypeName")
                return
            }

            val packageName = packageNameRaw?.takeIf { it.isNotBlank() }
            if (packageName == null) {
                logDrop("pkg=null")
                return
            }

            if (OverlayState.isShowing()) {
                logDrop("overlay_active:$packageName")
                return
            }

            if (packageName == handledPackage) {
                logDrop("handled_active:$packageName")
                return
            }

            if (packageName in imePackages) {
                logDrop("ime_pkg:$packageName")
                return
            }

            if (className?.contains("InputMethod") == true) {
                logDrop("ime_class:$className")
                return
            }

            if (isOverlayActivity(className)) {
                logDrop("overlay_class:$className")
                return
            }

            val handledUntil = handledPackages[packageName]
            if (handledUntil != null && now < handledUntil) {
                logDrop("handled_pkg:${handledUntil - now}ms:$packageName")
                return
            }

            if (allowPackages.isNotEmpty() && packageName !in allowPackages) {
                logDrop("not_allowed:$packageName")
                return
            }

            if (packageName in ignorePackages) {
                logDrop("ignore_pkg:$packageName")
                return
            }

            val lastPackage = lastLoggedPackage
            if (packageName == lastPackage && delta < packageDebounceMillis) {
                logDrop("pkg_debounce:${delta}ms:$packageName")
                return
            }

            if (!OverlayState.tryAcquire()) {
                logDrop("overlay_lock:$packageName")
                return
            }

            handledPackage = packageName
            lastLoggedPackage = packageName
            lastEventTimestamp = now

            mainHandler.post {
                try {
                    Log.d(TAG, "start:$packageName")
                    OverlayNavigator.showQFlow(applicationContext, packageName)
                } catch (t: Throwable) {
                    Log.e(TAG, "qflow error", t)
                    handledPackage = null
                    OverlayState.release()
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "event error", t)
            handledPackage = null
            OverlayState.release()
        }
    }

    private fun isOverlayActivity(className: String?): Boolean {
        return className?.contains("OverlayDialogActivity") == true
    }

    private fun refreshImePackages() {
        try {
            val packages = mutableSetOf<String>()
            packages.addAll(imeFallbackPackages)

            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.enabledInputMethodList?.forEach {
                packages.add(it.packageName)
            }

            val defaultIme = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
            defaultIme?.substringBefore('/')?.takeIf { it.isNotBlank() }?.let { packages.add(it) }

            val pm = packageManager
            val queryIntent = Intent("android.view.InputMethod")
            pm.queryIntentServices(queryIntent, 0)?.forEach { info ->
                info.serviceInfo?.packageName?.let { packages.add(it) }
            }

            imePackages.clear()
            imePackages.addAll(packages)
            lastImeRefreshAt = SystemClock.elapsedRealtime()
            Log.d(TAG, "IME packages: ${imePackages.joinToString()}")
        } catch (t: Throwable) {
            Log.e(TAG, "ime-detect error", t)
        }
    }

    override fun onInterrupt() {
        // 監視のみなので特別な処理は不要
    }

    fun updateAllowList(packages: Set<String>) {
        allowPackages = packages
    }

    private fun pruneHandledPackages(now: Long) {
        val iterator = handledPackages.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now >= entry.value) {
                iterator.remove()
            }
        }
    }

    private fun logDrop(reason: String) {
        Log.d(TAG, "drop:$reason")
    }

    private fun eventTypeToName(type: Int): String = when (type) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
        else -> "OTHER($type)"
    }

    companion object {
        private const val TAG = "FocusGate"
    }
}
