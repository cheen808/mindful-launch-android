package com.example.focusgate.overlay

import android.content.Context
import android.content.Intent

object FlowResultBroadcaster {

    const val ACTION_FLOW_RESULT = "com.example.focusgate.action.QFLOW_RESULT"
    const val EXTRA_PACKAGE = "package"
    const val EXTRA_RESULT = "result"

    const val RESULT_COMPLETED = "completed"
    const val RESULT_CANCELED = "canceled"

    fun sendCompleted(context: Context, packageName: String) {
        send(context, packageName, RESULT_COMPLETED)
    }

    fun sendCanceled(context: Context, packageName: String) {
        send(context, packageName, RESULT_CANCELED)
    }

    private fun send(context: Context, packageName: String, result: String) {
        val intent = Intent(ACTION_FLOW_RESULT).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_PACKAGE, packageName)
            putExtra(EXTRA_RESULT, result)
        }
        context.sendBroadcast(intent)
    }
}
