package com.klem.devtycoon

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "devtycoon_save")

class GameRepository(private val context: Context) {

    private val keyTotalLines = doublePreferencesKey("total_lines")
    private val keyKeyboardLevel = intPreferencesKey("keyboard_level")
    private val keyJuniorDevs = intPreferencesKey("junior_devs_count")

    // Nouvelles clés pour l'extension du marché
    private val keyServerLevel = intPreferencesKey("server_level")
    private val keyCopilotLevel = intPreferencesKey("copilot_level")
    private val keyFrameworkLevel = intPreferencesKey("framework_level")

    val gameStateFlow: Flow<SavedGameState> = context.dataStore.data.map { preferences ->
        SavedGameState(
            totalLinesOfCode = preferences[keyTotalLines] ?: 0.0,
            keyboardLevel = preferences[keyKeyboardLevel] ?: 0,
            juniorDevsCount = preferences[keyJuniorDevs] ?: 0,
            serverLevel = preferences[keyServerLevel] ?: 0,
            copilotLevel = preferences[keyCopilotLevel] ?: 0,
            frameworkLevel = preferences[keyFrameworkLevel] ?: 0
        )
    }

    suspend fun saveTotalLines(lines: Double) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[keyTotalLines] = lines
        }
    }

    suspend fun saveKeyboardLevel(level: Int) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[keyKeyboardLevel] = level
        }
    }

    suspend fun saveJuniorDevsCount(count: Int) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[keyJuniorDevs] = count
        }
    }

    suspend fun saveServerLevel(level: Int) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[keyServerLevel] = level
        }
    }

    suspend fun saveCopilotLevel(level: Int) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[keyCopilotLevel] = level
        }
    }

    suspend fun saveFrameworkLevel(level: Int) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[keyFrameworkLevel] = level
        }
    }
}

data class SavedGameState(
    val totalLinesOfCode: Double,
    val keyboardLevel: Int,
    val juniorDevsCount: Int,
    val serverLevel: Int,
    val copilotLevel: Int,
    val frameworkLevel: Int
)