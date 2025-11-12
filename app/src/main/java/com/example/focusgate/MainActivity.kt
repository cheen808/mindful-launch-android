package com.example.focusgate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.focusgate.accessibility.AppLaunchWatcherService
import com.example.focusgate.permission.PermissionChecker
import com.example.focusgate.ui.theme.FocusGateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusGateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    PermissionScreen(
                        modifier = Modifier
                            .padding(inner)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var overlayGranted by rememberSaveable {
        mutableStateOf(PermissionChecker.isOverlayPermissionGranted(context))
    }
    var accessibilityGranted by rememberSaveable {
        mutableStateOf(
            PermissionChecker.isAccessibilityServiceEnabled(
                context,
                AppLaunchWatcherService::class.java
            )
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = PermissionChecker.isOverlayPermissionGranted(context)
                accessibilityGranted = PermissionChecker.isAccessibilityServiceEnabled(
                    context,
                    AppLaunchWatcherService::class.java
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "権限チェック",
            style = MaterialTheme.typography.headlineSmall
        )
        PermissionCard(
            title = "Accessibility Service",
            description = "アプリ起動を検知し、Qフローを表示するために必要です。",
            granted = accessibilityGranted,
            actionLabel = "設定を開く"
        ) {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        PermissionCard(
            title = "オーバーレイ表示",
            description = "Q1/Q2/Q3 ポップアップを他アプリ上に表示するために必要です。",
            granted = overlayGranted,
            actionLabel = "権限を設定"
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        Text(
            text = "両方の権限が ON の状態で対象アプリを開くと、Qフローが安定して表示されます。",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (granted) "ステータス: ON" else "ステータス: OFF",
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionCardPreview() {
    FocusGateTheme {
        PermissionCard(
            title = "Accessibility Service",
            description = "アプリ起動を検知してフローを表示します。",
            granted = false,
            actionLabel = "設定を開く",
            onAction = {}
        )
    }
}
