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

    val repository = SettingsRepository(application)

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

    val lockType: StateFlow<String> = repository.lockType.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "none"
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

    val securityQuestion: StateFlow<String> = repository.securityQuestion.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "What is your favorite book?"
    )

    val securityAnswer: StateFlow<String> = repository.securityAnswer.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val protectedNotesPassword: StateFlow<String> = repository.protectedNotesPassword.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "1234"
    )

    val userName: StateFlow<String> = repository.userName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "My Notes User"
    )

    val userEmail: StateFlow<String> = repository.userEmail.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val userAvatar: StateFlow<String> = repository.userAvatar.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val lastBackupTime: StateFlow<Long> = repository.lastBackupTime.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val driveFolderName: StateFlow<String> = repository.driveFolderName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "My Drive / MyNotes_Backups /"
    )

    val lastRotatingSlot: StateFlow<Int> = repository.lastRotatingSlot.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val schemaVersion: StateFlow<Int> = repository.schemaVersion.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 2
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

    fun setAppLock(enabled: Boolean, pin: String = "", lockType: String = if (enabled) "pin" else "none") {
        viewModelScope.launch {
            repository.setAppLockEnabled(enabled)
            repository.setLockType(lockType)
            if (pin.isNotBlank()) {
                repository.setAppLockPin(pin)
            }
        }
    }

    fun setLockType(type: String) {
        viewModelScope.launch {
            repository.setLockType(type)
        }
    }

    fun setAutoLockMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setAutoLockMinutes(minutes)
        }
    }

    fun setSecurityQuestionAndAnswer(question: String, answer: String) {
        viewModelScope.launch {
            repository.setSecurityQuestionAndAnswer(question, answer)
        }
    }

    fun setProtectedNotesPassword(password: String) {
        viewModelScope.launch {
            repository.setProtectedNotesPassword(password)
        }
    }

    fun setUserProfile(name: String, email: String, avatar: String) {
        viewModelScope.launch {
            repository.setUserProfile(name, email, avatar)
        }
    }

    fun setDriveFolderName(name: String) {
        viewModelScope.launch {
            repository.setDriveFolderName(name)
        }
    }

    fun setLastRotatingSlot(slot: Int) {
        viewModelScope.launch {
            repository.setLastRotatingSlot(slot)
        }
    }
}
