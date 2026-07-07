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
    private val keyServerLevel = intPreferencesKey("server_level")
    private val keyCopilotLevel = intPreferencesKey("copilot_level")
    private val keyFrameworkLevel = intPreferencesKey("framework_level")

    // Nouveaux champs pour le niveau du joueur
    private val keyPlayerLevel = intPreferencesKey("player_level")
    private val keyPlayerXp = intPreferencesKey("player_xp")

    val gameStateFlow: Flow<SavedGameState> = context.dataStore.data.map { preferences ->
        SavedGameState(
            totalLinesOfCode = preferences[keyTotalLines] ?: 0.0,
            keyboardLevel = preferences[keyKeyboardLevel] ?: 0,
            juniorDevsCount = preferences[keyJuniorDevs] ?: 0,
            serverLevel = preferences[keyServerLevel] ?: 0,
            copilotLevel = preferences[keyCopilotLevel] ?: 0,
            frameworkLevel = preferences[keyFrameworkLevel] ?: 0,
            playerLevel = preferences[keyPlayerLevel] ?: 1, // Démarre au niveau 1
            playerXp = preferences[keyPlayerXp] ?: 0
        )
    }

    suspend fun saveTotalLines(lines: Double) {
        context.dataStore.edit { preferences: MutablePreferences -> preferences[keyTotalLines] = lines }
    }

    suspend fun saveKeyboardLevel(level: Int) {
        context.dataStore.edit { preferences: MutablePreferences -> preferences[keyKeyboardLevel] = level }
    }

    suspend fun saveJuniorDevsCount(count: Int) {
        context.dataStore.edit { preferences: MutablePreferences -> preferences[keyJuniorDevs] = count }
    }

    suspend fun saveServerLevel(level: Int) {
        context.dataStore.edit { preferences: MutablePreferences -> preferences[keyServerLevel] = level }
    }

    suspend fun saveCopilotLevel(level: Int) {
        context.dataStore.edit { preferences: MutablePreferences -> preferences[keyCopilotLevel] = level }
    }

    suspend fun saveFrameworkLevel(level: Int) {
        context.dataStore.edit { preferences: MutablePreferences -> preferences[keyFrameworkLevel] = level }
    }

    suspend fun savePlayerLevel(level: Int) {
        context.dataStore.edit { preferences: MutablePreferences -> preferences[keyPlayerLevel] = level }
    }

    suspend fun savePlayerXp(xp: Int) {
        context.dataStore.edit { preferences: MutablePreferences -> preferences[keyPlayerXp] = xp }
    }
}

data class SavedGameState(
    val totalLinesOfCode: Double,
    val keyboardLevel: Int,
    val juniorDevsCount: Int,
    val serverLevel: Int,
    val copilotLevel: Int,
    val frameworkLevel: Int,
    val playerLevel: Int,
    val playerXp: Int
)