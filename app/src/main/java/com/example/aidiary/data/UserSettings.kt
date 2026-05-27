package com.example.aidiary.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class UserSettings(
    val reminderTime: String = "21:30",
    val modelPath: String = "",
    val writingStyle: String = "标准",
)

class SettingsRepository(private val context: Context) {
    private val reminderTimeKey = stringPreferencesKey("reminder_time")
    private val modelPathKey = stringPreferencesKey("model_path")
    private val writingStyleKey = stringPreferencesKey("writing_style")

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            reminderTime = prefs[reminderTimeKey] ?: "21:30",
            modelPath = prefs[modelPathKey] ?: "",
            writingStyle = prefs[writingStyleKey] ?: "标准",
        )
    }

    suspend fun updateReminderTime(value: String) {
        context.settingsDataStore.edit { it[reminderTimeKey] = value }
    }

    suspend fun updateModelPath(value: String) {
        context.settingsDataStore.edit { it[modelPathKey] = value }
    }

    suspend fun updateWritingStyle(value: String) {
        context.settingsDataStore.edit { it[writingStyleKey] = value }
    }

    suspend fun settingsSnapshot(): UserSettings = settings.first()

    suspend fun clear() {
        context.settingsDataStore.edit { it.clear() }
    }
}
