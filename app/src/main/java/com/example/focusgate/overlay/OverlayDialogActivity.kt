package com.example.focusgate.overlay

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.focusgate.overlay.FlowResultBroadcaster
import com.example.focusgate.overlay.OverlayNavigator.EXTRA_TARGET_PACKAGE

class OverlayDialogActivity : ComponentActivity() {

    private var targetPackage: String? = null
    private var resultDispatched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "overlay start")
        val pkg = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        if (pkg.isNullOrBlank()) {
            finish()
            return
        }
        targetPackage = pkg
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            OverlayDialogContent(
                onCancel = {
                    FlowResultBroadcaster.sendCanceled(this, pkg)
                    resultDispatched = true
                    finish()
                },
                onComplete = {
                    FlowResultBroadcaster.sendCompleted(this, pkg)
                    resultDispatched = true
                    finish()
                }
            )
        }
        overridePendingTransition(0, 0)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
        Log.d(TAG, "overlay finish")
        OverlayState.release()
    }

    override fun onDestroy() {
        if (!resultDispatched) {
            targetPackage?.let {
                FlowResultBroadcaster.sendCanceled(this, it)
            }
        }
        super.onDestroy()
        OverlayState.release()
    }

    companion object {
        private const val TAG = "FocusGate"
    }
}

@Composable
private fun OverlayDialogContent(
    onCancel: () -> Unit,
    onComplete: () -> Unit
) {
    Log.d("FocusGate", "compose start")
    BackHandler(onBack = onCancel)

    var stepName by rememberSaveable { mutableStateOf(OverlayStep.Q1.name) }
    val step = remember(stepName) { OverlayStep.valueOf(stepName) }
    var minutesText by rememberSaveable { mutableStateOf("") }
    var purposeText by rememberSaveable { mutableStateOf("") }

    val minutes = minutesText.toIntOrNull()
    val isMinutesValid = minutes != null && minutes in 1..MAX_MINUTES
    val purpose = purposeText.trim()
    val isPurposeValid = purpose.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    OverlayStep.Q1 -> {
                        Text(
                            text = "Q1: 何分使いますか？",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = minutesText,
                            onValueChange = { input ->
                                minutesText = input.filter { it.isDigit() }.take(4)
                            },
                            label = { Text("分数 (1〜$MAX_MINUTES)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            )
                        )
                        Text(
                            text = if (isMinutesValid) "OKを押してください" else "1〜$MAX_MINUTES の整数を入力",
                            color = if (isMinutesValid) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Button(
                            onClick = { stepName = OverlayStep.Q2.name },
                            enabled = isMinutesValid,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("OK")
                        }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onCancel
                        ) {
                            Text("キャンセル")
                        }
                    }

                    OverlayStep.Q2 -> {
                        Text(
                            text = "Q2: 何のために使いますか？",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = purposeText,
                            onValueChange = { purposeText = it.take(MAX_PURPOSE) },
                            label = { Text("目的 (1〜${MAX_PURPOSE}文字)") },
                            singleLine = false,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            )
                        )
                        Text(
                            text = "${purposeText.length}/$MAX_PURPOSE 文字",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { stepName = OverlayStep.Q3.name },
                            enabled = isPurposeValid,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("OK")
                        }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onCancel
                        ) {
                            Text("キャンセル")
                        }
                    }

                    OverlayStep.Q3 -> {
                        Text(
                            text = "Q3: 確認",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val minutesValue = minutes ?: 0
                        Text(
                            text = "${minutesValue}分間、${purpose}のためにアプリを使用します。終了後は制限がかかります。同意しますか？",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("はい")
                        }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onCancel
                        ) {
                            Text("いいえ")
                        }
                    }
                }
            }
        }
    }
}

private enum class OverlayStep {
    Q1, Q2, Q3
}

private const val MAX_MINUTES = 1440
private const val MAX_PURPOSE = 200
