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
        val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val KEY_APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val KEY_AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val KEY_LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
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

    val appLockPin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LOCK_PIN] ?: ""
    }

    val autoLockMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_LOCK_MINUTES] ?: 5
    }

    val lastBackupTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_BACKUP_TIME] ?: 0L
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
        context.dataStore.edit { prefs -> prefs[KEY_APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setAppLockPin(pin: String) {
        context.dataStore.edit { prefs -> prefs[KEY_APP_LOCK_PIN] = pin }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_AUTO_LOCK_MINUTES] = minutes }
    }

    suspend fun updateLastBackupTime(time: Long) {
        context.dataStore.edit { prefs -> prefs[KEY_LAST_BACKUP_TIME] = time }
    }
}
