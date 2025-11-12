package com.example.focusgate.overlay

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

object QFlow {

    fun start(
        context: Context,
        onDone: (minutes: Int, purpose: String) -> Unit,
        onCancel: () -> Unit
    ) {
        OverlayController.show(
            appContext = context,
            onCancel = {
                Log.d("FocusGate", "QFlow canceled")
                onCancel()
            }
        ) {
            QFlowDialog(
                onComplete = { minutes, purpose ->
                    OverlayController.dismiss()
                    onDone(minutes, purpose)
                },
                onCancel = {
                    OverlayController.dismiss(triggerCancel = true)
                }
            )
        }
    }
}

private enum class QStep {
    Q1, Q2, Q3
}

@Composable
private fun QFlowDialog(
    onComplete: (Int, String) -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableStateOf(QStep.Q1) }
    var minutesText by remember { mutableStateOf("") }
    var purposeText by remember { mutableStateOf("") }

    val minutes = minutesText.toIntOrNull()
    val isMinutesValid = minutes != null && minutes in 1..MAX_MINUTES

    val purpose = purposeText.trim()
    val isPurposeValid = purpose.isNotEmpty() && purpose.length <= MAX_PURPOSE

    BackHandler(onBack = onCancel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    QStep.Q1 -> {
                        Text(
                            text = "Q1: 何分間利用しますか？",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = minutesText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(4)
                                minutesText = filtered
                            },
                            label = { Text("利用時間 (分)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (isMinutesValid) {
                                        step = QStep.Q2
                                    }
                                }
                            )
                        )
                        Text(
                            text = "1〜$MAX_MINUTES 分の整数で入力してください。",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isMinutesValid) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        ActionButtons(
                            positiveLabel = "OK",
                            onPositive = { step = QStep.Q2 },
                            positiveEnabled = isMinutesValid,
                            onNegative = onCancel
                        )
                    }

                    QStep.Q2 -> {
                        Text(
                            text = "Q2: 何のために使用しますか？",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = purposeText,
                            onValueChange = { input ->
                                purposeText = input.take(MAX_PURPOSE)
                            },
                            label = { Text("目的 (1〜$MAX_PURPOSE 文字)") },
                            singleLine = false,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (isPurposeValid) {
                                        step = QStep.Q3
                                    }
                                }
                            )
                        )
                        Text(
                            text = "${purpose.length}/$MAX_PURPOSE 文字",
                            style = MaterialTheme.typography.bodySmall
                        )
                        ActionButtons(
                            positiveLabel = "OK",
                            onPositive = { step = QStep.Q3 },
                            positiveEnabled = isPurposeValid,
                            onNegative = onCancel
                        )
                    }

                    QStep.Q3 -> {
                        val minutesValue = minutes ?: 0
                        Text(
                            text = "Q3: ここで確認します",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${minutesValue}分、${purpose}のために開きます。終了後はクールダウン30分です。よろしいですか？",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ActionButtons(
                            positiveLabel = "はい",
                            onPositive = {
                                if (minutes != null && isPurposeValid) {
                                    onComplete(minutes, purpose)
                                } else {
                                    onCancel()
                                }
                            },
                            positiveEnabled = true,
                            negativeLabel = "いいえ",
                            onNegative = onCancel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    positiveLabel: String,
    onPositive: () -> Unit,
    positiveEnabled: Boolean,
    onNegative: () -> Unit,
    negativeLabel: String = "キャンセル"
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onPositive,
            enabled = positiveEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(positiveLabel)
        }
        TextButton(
            onClick = onNegative,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(negativeLabel)
        }
    }
}

private const val MAX_MINUTES = 1440
private const val MAX_PURPOSE = 200
