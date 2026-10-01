package com.drdisagree.teledrive.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.drdisagree.teledrive.resources.Res
import com.drdisagree.teledrive.resources.common_cancel
import com.drdisagree.teledrive.resources.common_save
import com.drdisagree.teledrive.resources.lock_current_pin
import com.drdisagree.teledrive.resources.lock_new_pin
import com.drdisagree.teledrive.resources.lock_pins_do_not_match
import com.drdisagree.teledrive.resources.lock_pin_repeat
import com.drdisagree.teledrive.resources.lock_pin_too_short
import com.drdisagree.teledrive.resources.lock_set_pin
import com.drdisagree.teledrive.resources.lock_turn_off
import com.drdisagree.teledrive.resources.lock_turn_off
import com.drdisagree.teledrive.resources.lock_wrong_pin
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class AppLockMode { SET, CHANGE, TURN_OFF }

/**
 * The one place the app lock PIN can be changed. Asking for the current PIN
 * before anything else is what stops someone who found an unlocked phone from
 * taking the lock off again.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLockDialog(
    mode: AppLockMode,
    onSubmit: suspend (currentPin: String, newPin: String) -> Boolean,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var repeated by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val needsCurrent = mode != AppLockMode.SET
    val tooShort = new.isNotEmpty() && new.length < MIN_PIN_LENGTH
    val mismatch = repeated.isNotEmpty() && new != repeated
    val ready = !working && !tooShort && new == repeated &&
        new.length >= MIN_PIN_LENGTH && (!needsCurrent || current.isNotEmpty())

    fun submit() {
        if (!ready) return
        scope.launch {
            working = true
            wrong = !onSubmit(current, new)
            working = false
            if (!wrong) onDismiss() else current = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    when (mode) {
                        AppLockMode.SET -> Res.string.lock_set_pin
                        AppLockMode.CHANGE -> Res.string.lock_new_pin
                        AppLockMode.TURN_OFF -> Res.string.lock_turn_off
                    }
                )
            )
        },
        text = {
            Column {
                if (needsCurrent) {
                    PinField(current, { current = it; wrong = false }, Res.string.lock_current_pin)
                    Spacer(Modifier.height(12.dp))
                }
                if (mode != AppLockMode.TURN_OFF) {
                    PinField(new, { new = it; repeated = "" }, Res.string.lock_new_pin)
                    Spacer(Modifier.height(12.dp))
                    PinField(repeated, { repeated = it }, Res.string.lock_pin_repeat)
                }
                if (tooShort) {
                    Message(Res.string.lock_pin_too_short)
                }
                if (mismatch) {
                    Message(Res.string.lock_pins_do_not_match)
                }
                if (wrong) {
                    Message(Res.string.lock_wrong_pin)
                }
            }
        },
        confirmButton = {
            if (mode == AppLockMode.TURN_OFF) {
                TextButton(onClick = ::submit, enabled = ready) {
                    Text(stringResource(Res.string.lock_turn_off))
                }
            } else {
                TextButton(onClick = ::submit, enabled = ready) {
                    Text(stringResource(Res.string.common_save))
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        }
    )
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(MAX_PIN_LENGTH)) },
        label = { Text(stringResource(label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Message(label: StringResource) {
    Text(text = stringResource(label), color = MaterialTheme.colorScheme.error)
}

const val MIN_PIN_LENGTH = 4
private const val MAX_PIN_LENGTH = 6
