package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val themeMode: StateFlow<String> = repository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "system"
    )

    val fontSize: StateFlow<String> = repository.fontSize.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "medium"
    )

    val appLockEnabled: StateFlow<Boolean> = repository.appLockEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val appLockPin: StateFlow<String> = repository.appLockPin.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val autoLockMinutes: StateFlow<Int> = repository.autoLockMinutes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 5
    )

    val lastBackupTime: StateFlow<Long> = repository.lastBackupTime.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setFontSize(size: String) {
        viewModelScope.launch {
            repository.setFontSize(size)
        }
    }

    fun setAppLock(enabled: Boolean, pin: String = "") {
        viewModelScope.launch {
            repository.setAppLockEnabled(enabled)
            if (pin.isNotBlank()) {
                repository.setAppLockPin(pin)
            }
        }
    }

    fun setAutoLockMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setAutoLockMinutes(minutes)
        }
    }
}
