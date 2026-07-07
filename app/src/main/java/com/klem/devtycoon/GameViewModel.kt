package com.klem.devtycoon

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

class GameViewModel: ViewModel() {

    // États du jeu (Source de vérité)
    var totalLinesOfCode by mutableStateOf(0.0)
        private set

    var keyboardLevel by mutableStateOf(0)
        private set

    var juniorDevsCount by mutableStateOf(0)
        private set

    // Formules de calcul (Getters dynamiques)
    val linesPerClick: Double
        get() = 1.0 + keyboardLevel

    val linesPerSecond: Double
        get() = juniorDevsCount * 2.0 // Chaque dev Junior produit 2 lignes/secondes

    val keyboardUpgradeCost: Double
        get() = 15.0 * 1.15.pow(keyboardLevel)

    val juniorDevCost: Double
        get() = 100.0 * 1.25.pow(juniorDevsCount)

    // Initialisation du moteur de jeu passif
    init {
        startGameLoop()
    }

    private fun startGameLoop() {
        // Lance un thread ultra-léger (Coroutine) lié à la durée de vie de l'application
        viewModelScope.launch {
            while (true) {
                delay(1000L) // Attend exactement 1 seconde
                totalLinesOfCode += linesPerSecond
            }
        }
    }

    // Actions de l'utilisateur
    fun codeClicked() {
        totalLinesOfCode += linesPerClick
    }

    fun buyKeyboardUpgrade() {
        val cost = keyboardUpgradeCost
        if (totalLinesOfCode >= cost) {
            totalLinesOfCode -= cost
            keyboardLevel++
        }
    }

    fun hireJuniorDev() {
        val cost = juniorDevCost
        if (totalLinesOfCode >= cost) {
            totalLinesOfCode -= cost
            juniorDevsCount++
        }
    }
}