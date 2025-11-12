package com.example.focusgate.permission

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object PermissionChecker {

    fun isOverlayPermissionGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isAccessibilityServiceEnabled(
        context: Context,
        serviceClass: Class<*>
    ): Boolean {
        val expectedComponent = ComponentName(context, serviceClass).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val isEnabled = enabledServices
            .split(':')
            .any { it.equals(expectedComponent, ignoreCase = true) }
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        ) == 1
        return accessibilityEnabled && isEnabled
    }
}
