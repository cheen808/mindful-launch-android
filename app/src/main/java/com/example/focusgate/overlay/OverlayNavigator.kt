package com.example.focusgate.overlay

import android.content.Context
import android.content.Intent

object OverlayNavigator {

    const val EXTRA_TARGET_PACKAGE = "extra_target_package"

    fun showQFlow(context: Context, targetPackage: String) {
        val intent = Intent(context, OverlayDialogActivity::class.java)
            .putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        context.startActivity(intent)
    }
}
