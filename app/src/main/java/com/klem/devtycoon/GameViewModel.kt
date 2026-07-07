package com.klem.devtycoon

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.pow

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    var totalLinesOfCode by mutableStateOf(0.0)
        private set

    var keyboardLevel by mutableStateOf(0)
        private set

    var juniorDevsCount by mutableStateOf(0)
        private set

    val linesPerClick: Double
        get() = 1.0 + keyboardLevel

    val linesPerSecond: Double
        get() = juniorDevsCount * 2.0

    val keyboardUpgradeCost: Double
        get() = 15.0 * 1.15.pow(keyboardLevel)

    val juniorDevCost: Double
        get() = 100.0 * 1.25.pow(juniorDevsCount)

    init {
        loadSavedState()
        startGameLoop()
    }

    // FIX: On récupère le snapshot de la sauvegarde UNE SEULE FOIS au démarrage
    private fun loadSavedState() {
        viewModelScope.launch {
            try {
                val savedState = repository.gameStateFlow.first()
                totalLinesOfCode = savedState.totalLinesOfCode
                keyboardLevel = savedState.keyboardLevel
                juniorDevsCount = savedState.juniorDevsCount
            } catch (e: Exception) {
                // En cas d'erreur de lecture, on conserve les valeurs par défaut (0)
            }
        }
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (linesPerSecond > 0) {
                    totalLinesOfCode += linesPerSecond
                    repository.saveTotalLines(totalLinesOfCode)
                }
            }
        }
    }

    fun codeClicked() {
        totalLinesOfCode += linesPerClick
        viewModelScope.launch {
            repository.saveTotalLines(totalLinesOfCode)
        }
    }

    fun buyKeyboardUpgrade() {
        val cost = keyboardUpgradeCost
        if (totalLinesOfCode >= cost) {
            totalLinesOfCode -= cost
            keyboardLevel++
            viewModelScope.launch {
                repository.saveTotalLines(totalLinesOfCode)
                repository.saveKeyboardLevel(keyboardLevel)
            }
        }
    }

    fun hireJuniorDev() {
        val cost = juniorDevCost
        if (totalLinesOfCode >= cost) {
            totalLinesOfCode -= cost
            juniorDevsCount++
            viewModelScope.launch {
                repository.saveTotalLines(totalLinesOfCode)
                repository.saveJuniorDevsCount(juniorDevsCount)
            }
        }
    }
}