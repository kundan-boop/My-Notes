package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_VIEW_MODE = stringPreferencesKey("view_mode") // "grid" or "list"
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
        val KEY_FONT_SIZE = stringPreferencesKey("font_size") // "small", "medium", "large"
        
        // App Lock & Biometrics
        val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val KEY_LOCK_TYPE = stringPreferencesKey("lock_type") // "none", "pin", "biometric"
        val KEY_APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val KEY_AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val KEY_SECURITY_QUESTION = stringPreferencesKey("security_question")
        val KEY_SECURITY_ANSWER = stringPreferencesKey("security_answer")
        val KEY_PROTECTED_NOTES_PASSWORD = stringPreferencesKey("protected_notes_password")

        // User Profile
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_AVATAR = stringPreferencesKey("user_avatar")

        // Rotating Google Drive / Cloud Backup
        val KEY_LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        val KEY_DRIVE_FOLDER_NAME = stringPreferencesKey("drive_folder_name")
        val KEY_DRIVE_FOLDER_URI = stringPreferencesKey("drive_folder_uri")
        val KEY_AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val KEY_LAST_ROTATING_SLOT = intPreferencesKey("last_rotating_slot")
        val KEY_SCHEMA_VERSION = intPreferencesKey("schema_version")
    }

    val viewMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_VIEW_MODE] ?: "grid"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "system"
    }

    val fontSize: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_FONT_SIZE] ?: "medium"
    }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LOCK_ENABLED] ?: false
    }

    val lockType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOCK_TYPE] ?: if (prefs[KEY_APP_LOCK_ENABLED] == true) "pin" else "none"
    }

    val appLockPin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LOCK_PIN] ?: ""
    }

    val autoLockMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_LOCK_MINUTES] ?: 5
    }

    val securityQuestion: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SECURITY_QUESTION] ?: "What is your favorite book?"
    }

    val securityAnswer: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SECURITY_ANSWER] ?: ""
    }

    val protectedNotesPassword: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PROTECTED_NOTES_PASSWORD] ?: (prefs[KEY_APP_LOCK_PIN] ?: "1234")
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: "My Notes User"
    }

    val userEmail: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_EMAIL] ?: ""
    }

    val userAvatar: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_AVATAR] ?: ""
    }

    val lastBackupTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_BACKUP_TIME] ?: 0L
    }

    val driveFolderName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DRIVE_FOLDER_NAME] ?: "My Drive / MyNotes_Backups /"
    }

    val driveFolderUri: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DRIVE_FOLDER_URI] ?: ""
    }

    val autoBackupEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_BACKUP_ENABLED] ?: true
    }

    val lastRotatingSlot: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_ROTATING_SLOT] ?: 0
    }

    val schemaVersion: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SCHEMA_VERSION] ?: 2
    }

    suspend fun setViewMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[KEY_VIEW_MODE] = mode }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode }
    }

    suspend fun setFontSize(size: String) {
        context.dataStore.edit { prefs -> prefs[KEY_FONT_SIZE] = size }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
            if (!enabled) prefs[KEY_LOCK_TYPE] = "none"
            else if (prefs[KEY_LOCK_TYPE] == null || prefs[KEY_LOCK_TYPE] == "none") {
                prefs[KEY_LOCK_TYPE] = "pin"
            }
        }
    }

    suspend fun setLockType(type: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOCK_TYPE] = type
            prefs[KEY_APP_LOCK_ENABLED] = (type != "none")
        }
    }

    suspend fun setAppLockPin(pin: String) {
        context.dataStore.edit { prefs -> prefs[KEY_APP_LOCK_PIN] = pin }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_AUTO_LOCK_MINUTES] = minutes }
    }

    suspend fun setSecurityQuestionAndAnswer(question: String, answer: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SECURITY_QUESTION] = question
            prefs[KEY_SECURITY_ANSWER] = answer
        }
    }

    suspend fun setProtectedNotesPassword(password: String) {
        context.dataStore.edit { prefs -> prefs[KEY_PROTECTED_NOTES_PASSWORD] = password }
    }

    suspend fun setUserProfile(name: String, email: String, avatar: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_USER_AVATAR] = avatar
        }
    }

    suspend fun updateLastBackupTime(time: Long) {
        context.dataStore.edit { prefs -> prefs[KEY_LAST_BACKUP_TIME] = time }
    }

    suspend fun setDriveFolderName(name: String) {
        context.dataStore.edit { prefs -> prefs[KEY_DRIVE_FOLDER_NAME] = name }
    }

    suspend fun setDriveFolderUri(uriString: String) {
        context.dataStore.edit { prefs -> prefs[KEY_DRIVE_FOLDER_URI] = uriString }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setLastRotatingSlot(slot: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_LAST_ROTATING_SLOT] = slot }
    }
}
