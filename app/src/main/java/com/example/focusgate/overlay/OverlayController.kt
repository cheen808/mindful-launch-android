package com.example.focusgate.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

object OverlayController {
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var windowManager: WindowManager? = null

    @Volatile
    private var overlayView: View? = null

    @Volatile
    private var isShowing = false

    @Volatile
    private var cancelCallback: (() -> Unit)? = null

    fun show(
        appContext: Context,
        onCancel: () -> Unit,
        content: @Composable () -> Unit
    ) {
        val canDraw = Settings.canDrawOverlays(appContext)
        Log.d(TAG, "show overlay canDraw=$canDraw")
        if (!canDraw) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW permission not granted")
            onCancel()
            return
        }

        handler.post {
            if (isShowing) {
                Log.w(TAG, "Overlay already showing, skipping new request")
                onCancel()
                return@post
            }

            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                dimAmount = 0.35f
            }

            val composeView = ComposeView(appContext).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                isFocusable = true
                isFocusableInTouchMode = true
                requestFocus()
                visibility = View.VISIBLE
                setContent {
                    content()
                }
            }

            try {
                wm.addView(composeView, params)
                windowManager = wm
                overlayView = composeView
                cancelCallback = onCancel
                isShowing = true
                Log.d(TAG, "overlay addView ok")
            } catch (t: Throwable) {
                Log.e(TAG, "overlay error", t)
                cancelCallback = null
                safeRemove(wm, composeView)
                onCancel()
            }
        }
    }

    fun dismiss(triggerCancel: Boolean = false) {
        handler.post {
            removeOverlayInternal(triggerCancel)
        }
    }

    private fun removeOverlayInternal(triggerCancel: Boolean) {
        if (!isShowing) return

        val wm = windowManager
        val view = overlayView

        try {
            if (view != null && wm != null) {
                wm.removeView(view)
                Log.d(TAG, "overlay remove ok")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to remove overlay", t)
        } finally {
            overlayView = null
            windowManager = null
            isShowing = false

            val cancel = cancelCallback
            cancelCallback = null
            if (triggerCancel && cancel != null) {
                cancel.invoke()
            }
        }
    }

    private fun safeRemove(wm: WindowManager, view: View) {
        try {
            wm.removeViewImmediate(view)
        } catch (t: Throwable) {
            Log.e(TAG, "safeRemove failed", t)
        }
    }

    fun showQ1Overlay(appContext: Context, onCancel: () -> Unit = {}) {
        show(
            appContext = appContext,
            onCancel = onCancel
        ) {
            Log.d(TAG, "compose start")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Q1 TEST",
                    color = Color.White,
                    fontSize = 32.sp
                )
            }
            Log.d(TAG, "compose end")
        }
    }

    private const val TAG = "FocusGate"
}
