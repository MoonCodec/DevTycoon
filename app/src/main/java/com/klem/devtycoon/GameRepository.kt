package com.klem.devtycoon

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension pour instancier un singleton unique de DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "devtycoon_save")

class GameRepository(private val context: Context) {

    // Clés typées pour identifier les données dans le stockage numérique
    private val keyTotalLines = doublePreferencesKey("total_lines")
    private val keyKeyboardLevel = intPreferencesKey("keyboard_level")
    private val keyJuniorDevs = intPreferencesKey("junior_devs_count")

    // Flux de données exposant l'état sauvegardé (avec valeurs de secours en cas de première installation ou mise à jour)
    val gameStateFlow: Flow<SavedGameState> = context.dataStore.data.map { preferences ->
        SavedGameState(
            totalLinesOfCode = preferences[keyTotalLines] ?: 0.0,
            keyboardLevel = preferences[keyKeyboardLevel] ?: 0,
            juniorDevsCount = preferences[keyJuniorDevs] ?: 0
        )
    }

    // Fonctions d'écriture asynchrones
    suspend fun saveTotalLines(lines: Double) {
        context.dataStore.edit { preferences ->
            preferences[keyTotalLines] = lines
        }
    }

    suspend fun saveKeyboardLevel(level: Int) {
        context.dataStore.edit { preferences ->
            preferences[keyKeyboardLevel] = level
        }
    }

    suspend fun saveJuniorDevsCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[keyJuniorDevs] = count
        }
    }
}

// Structure de données de transport pour l'état du jeu
data class SavedGameState(
    val totalLinesOfCode: Double,
    val keyboardLevel: Int,
    val juniorDevsCount: Int
)