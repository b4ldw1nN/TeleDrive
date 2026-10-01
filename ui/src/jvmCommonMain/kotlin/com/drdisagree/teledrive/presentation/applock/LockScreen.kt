package com.drdisagree.teledrive.presentation.applock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drdisagree.teledrive.resources.Res
import com.drdisagree.teledrive.resources.lock_backspace
import com.drdisagree.teledrive.resources.lock_keypad_clear
import com.drdisagree.teledrive.resources.lock_screen_title
import com.drdisagree.teledrive.resources.lock_too_many_attempts
import com.drdisagree.teledrive.resources.lock_unlock
import com.drdisagree.teledrive.resources.lock_wrong_pin
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LockScreen(
    lockedOut: StateFlow<Boolean>,
    failedAttempts: StateFlow<Int>,
    onSubmit: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var seenAttempts by remember { mutableIntStateOf(0) }
    val lockedOutNow by lockedOut.collectAsStateWithLifecycle()
    val attempts by failedAttempts.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { onRefresh() }

    val wrong = attempts > seenAttempts
    LaunchedEffect(attempts) { seenAttempts = attempts }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(Res.string.lock_screen_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "•".repeat(pin.length.coerceAtLeast(MIN_DOTS)),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = when {
                    lockedOutNow -> stringResource(Res.string.lock_too_many_attempts)
                    wrong -> stringResource(Res.string.lock_wrong_pin)
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (pin.isNotEmpty()) onSubmit(pin).also { pin = "" }
                },
                enabled = pin.isNotEmpty() && !lockedOutNow,
                shapes = androidx.compose.material3.ButtonDefaults.shapes()
            ) {
                Text(stringResource(Res.string.lock_unlock))
            }
            Spacer(Modifier.height(16.dp))
            PinKeypad(
                enabled = !lockedOutNow,
                onDigit = { digit -> pin = (pin + digit).take(MAX_PIN) },
                onBackspace = { pin = pin.dropLast(1) },
                onClear = { pin = "" }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PinKeypad(
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        listOf("123", "456", "789").forEach { row -> DigitRow(row, enabled, onDigit) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClear, enabled = enabled) {
                Text(stringResource(Res.string.lock_keypad_clear))
            }
            DigitButton('0', enabled, onDigit)
            IconButton(onClick = onBackspace, enabled = enabled) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = stringResource(Res.string.lock_backspace)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DigitRow(digits: String, enabled: Boolean, onDigit: (Char) -> Unit) {
    Row {
        digits.forEach { digit -> DigitButton(digit, enabled, onDigit) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DigitButton(digit: Char, enabled: Boolean, onDigit: (Char) -> Unit) {
    TextButton(
        onClick = { onDigit(digit) },
        enabled = enabled,
        modifier = Modifier.size(72.dp)
    ) {
        Text(digit.toString(), style = MaterialTheme.typography.headlineSmall)
    }
}

private const val MAX_PIN = 6
private const val MIN_DOTS = 4
