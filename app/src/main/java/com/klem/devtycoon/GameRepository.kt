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
    private val keyPlayerLevel = intPreferencesKey("player_level")
    private val keyPlayerXp = intPreferencesKey("player_xp")

    // NOUVEAU : Clés pour l'arbre de compétences
    private val keyTalentPoints = intPreferencesKey("talent_points")
    private val keySkillHtml = intPreferencesKey("skill_html")
    private val keySkillJs = intPreferencesKey("skill_js")
    private val keySkillPhp = intPreferencesKey("skill_php")

    val gameStateFlow: Flow<SavedGameState> = context.dataStore.data.map { preferences ->
        SavedGameState(
            totalLinesOfCode = preferences[keyTotalLines] ?: 0.0,
            keyboardLevel = preferences[keyKeyboardLevel] ?: 0,
            juniorDevsCount = preferences[keyJuniorDevs] ?: 0,
            serverLevel = preferences[keyServerLevel] ?: 0,
            copilotLevel = preferences[keyCopilotLevel] ?: 0,
            frameworkLevel = preferences[keyFrameworkLevel] ?: 0,
            playerLevel = preferences[keyPlayerLevel] ?: 1,
            playerXp = preferences[keyPlayerXp] ?: 0,
            // Initialisation des compétences
            talentPoints = preferences[keyTalentPoints] ?: 0,
            skillHtml = preferences[keySkillHtml] ?: 0,
            skillJs = preferences[keySkillJs] ?: 0,
            skillPhp = preferences[keySkillPhp] ?: 0
        )
    }

    suspend fun saveTotalLines(lines: Double) = context.dataStore.edit { it[keyTotalLines] = lines }
    suspend fun saveKeyboardLevel(level: Int) = context.dataStore.edit { it[keyKeyboardLevel] = level }
    suspend fun saveJuniorDevsCount(count: Int) = context.dataStore.edit { it[keyJuniorDevs] = count }
    suspend fun saveServerLevel(level: Int) = context.dataStore.edit { it[keyServerLevel] = level }
    suspend fun saveCopilotLevel(level: Int) = context.dataStore.edit { it[keyCopilotLevel] = level }
    suspend fun saveFrameworkLevel(level: Int) = context.dataStore.edit { it[keyFrameworkLevel] = level }
    suspend fun savePlayerLevel(level: Int) = context.dataStore.edit { it[keyPlayerLevel] = level }
    suspend fun savePlayerXp(xp: Int) = context.dataStore.edit { it[keyPlayerXp] = xp }

    // Setters compétences
    suspend fun saveTalentPoints(pts: Int) = context.dataStore.edit { it[keyTalentPoints] = pts }
    suspend fun saveSkillHtml(lvl: Int) = context.dataStore.edit { it[keySkillHtml] = lvl }
    suspend fun saveSkillJs(lvl: Int) = context.dataStore.edit { it[keySkillJs] = lvl }
    suspend fun saveSkillPhp(lvl: Int) = context.dataStore.edit { it[keySkillPhp] = lvl }
}

data class SavedGameState(
    val totalLinesOfCode: Double,
    val keyboardLevel: Int,
    val juniorDevsCount: Int,
    val serverLevel: Int,
    val copilotLevel: Int,
    val frameworkLevel: Int,
    val playerLevel: Int,
    val playerXp: Int,
    val talentPoints: Int,
    val skillHtml: Int,
    val skillJs: Int,
    val skillPhp: Int
)