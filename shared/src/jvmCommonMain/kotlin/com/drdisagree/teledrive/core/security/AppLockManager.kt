package com.drdisagree.teledrive.core.security

import com.drdisagree.teledrive.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AppLockManager(
    private val settingsRepository: SettingsRepository
) {

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _lockedOut = MutableStateFlow(false)
    val lockedOut: StateFlow<Boolean> = _lockedOut.asStateFlow()

    private var backgroundedAt: Long? = null
    private var initialized = false
    private var throttleUntil: Long = 0

    suspend fun onAppStarted() {
        val prefs = settingsRepository.preferences.first()
        if (!prefs.appLockEnabled) {
            _locked.value = false
            return
        }
        if (!initialized) {
            initialized = true
            _locked.value = true
            return
        }
        val elapsedMinutes = backgroundedAt?.let {
            (System.nanoTime() / 1_000_000 - it) / 60_000
        }
        if (elapsedMinutes != null && elapsedMinutes >= prefs.autoLockTimeoutMinutes) {
            _locked.value = true
        }
    }

    fun onAppStopped() {
        backgroundedAt = System.nanoTime() / 1_000_000
    }

    fun lockNow() {
        _locked.value = true
    }

    suspend fun hasPin(): Boolean = settingsRepository.preferences.first().appLockPin.isNotEmpty()

    suspend fun setPin(pin: String) {
        settingsRepository.update {
            it.copy(appLockPin = PinCredential.create(pin), appLockEnabled = true)
        }
        _locked.value = false
    }

    suspend fun clearPin() {
        settingsRepository.update { it.copy(appLockPin = "", appLockEnabled = false) }
        resetThrottle()
        _locked.value = false
    }

    suspend fun unlockWith(pin: String): Boolean {
        if (_lockedOut.value || System.nanoTime() / 1_000_000 < throttleUntil) return false
        val stored = settingsRepository.preferences.first().appLockPin
        if (stored.isEmpty()) {
            _locked.value = false
            return true
        }
        val correct = withContext(Dispatchers.Default) { PinCredential.verify(pin, stored) }
        if (correct) {
            _locked.value = false
            backgroundedAt = null
            resetThrottle()
        } else {
            registerFailure()
        }
        return correct
    }

    private fun registerFailure() {
        _failedAttempts.value += 1
        val attempts = _failedAttempts.value
        if (attempts >= LOCKOUT_THRESHOLD) {
            _lockedOut.value = true
            throttleUntil = System.nanoTime() / 1_000_000 + LOCKOUT_MS
        } else {
            throttleUntil = System.nanoTime() / 1_000_000 + attempts * STEP_MS
        }
    }

    /** Called when the app comes back to the foreground, so waiting works out. */
    fun refreshThrottle() {
        if (System.nanoTime() / 1_000_000 >= throttleUntil) {
            _lockedOut.value = false
            _failedAttempts.value = 0
        }
    }

    private fun resetThrottle() {
        _failedAttempts.value = 0
        _lockedOut.value = false
        throttleUntil = 0
    }

    private companion object {
        const val STEP_MS = 1_500L
        const val LOCKOUT_THRESHOLD = 5
        const val LOCKOUT_MS = 60_000L
    }
}
