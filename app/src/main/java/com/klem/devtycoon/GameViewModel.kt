package com.klem.devtycoon

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow

enum class PurchaseQuantity { X1, X10, X50, X100, MAX }

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    var totalLinesOfCode by mutableStateOf(0.0)
        private set
    var keyboardLevel by mutableStateOf(0)
        private set
    var juniorDevsCount by mutableStateOf(0)
        private set
    var serverLevel by mutableStateOf(0)
        private set
    var copilotLevel by mutableStateOf(0)
        private set
    var frameworkLevel by mutableStateOf(0)
        private set

    // Nouvelles variables d'expérience
    var playerLevel by mutableStateOf(1)
        private set
    var playerXp by mutableStateOf(0)
        private set

    var selectedQuantity by mutableStateOf(PurchaseQuantity.X1)

    val linesPerClick: Double get() = 1.0 + keyboardLevel
    val linesPerSecond: Double get() = (juniorDevsCount * 2.0) + (serverLevel * 15.0) + (copilotLevel * 80.0) + (frameworkLevel * 500.0)

    // Formule simple pour l'XP nécessaire au prochain niveau
    val xpNeededForNextLevel: Int get() = playerLevel * 50

    init {
        loadSavedState()
        startGameLoop()
    }

    private fun loadSavedState() {
        viewModelScope.launch {
            try {
                val savedState = repository.gameStateFlow.first()
                totalLinesOfCode = savedState.totalLinesOfCode
                keyboardLevel = savedState.keyboardLevel
                juniorDevsCount = savedState.juniorDevsCount
                serverLevel = savedState.serverLevel
                copilotLevel = savedState.copilotLevel
                frameworkLevel = savedState.frameworkLevel
                playerLevel = savedState.playerLevel
                playerXp = savedState.playerXp
            } catch (e: Exception) {}
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

    // Gestion du clic avec gain d'XP et Level Up
    fun codeClicked() {
        totalLinesOfCode += linesPerClick
        playerXp += 1

        if (playerXp >= xpNeededForNextLevel) {
            playerXp -= xpNeededForNextLevel
            playerLevel += 1
            viewModelScope.launch { repository.savePlayerLevel(playerLevel) }
        }

        viewModelScope.launch {
            repository.saveTotalLines(totalLinesOfCode)
            repository.savePlayerXp(playerXp)
        }
    }

    private fun getUpgradeCostAndQuantity(baseCost: Double, multiplier: Double, currentLevel: Int): Pair<Double, Int> {
        val qtyToBuy = when (selectedQuantity) {
            PurchaseQuantity.X1 -> 1
            PurchaseQuantity.X10 -> 10
            PurchaseQuantity.X50 -> 50
            PurchaseQuantity.X100 -> 100
            PurchaseQuantity.MAX -> {
                if (totalLinesOfCode < baseCost * multiplier.pow(currentLevel)) return Pair(0.0, 0)
                val maxQty = floor(log(((totalLinesOfCode * (multiplier - 1.0)) / (baseCost * multiplier.pow(currentLevel))) + 1.0, multiplier)).toInt()
                if (maxQty < 1) 1 else maxQty
            }
        }

        var totalCost = 0.0
        for (i in 0 until qtyToBuy) {
            totalCost += baseCost * multiplier.pow(currentLevel + i)
        }
        return Pair(totalCost, qtyToBuy)
    }

    fun buyKeyboard() {
        val (cost, qty) = getUpgradeCostAndQuantity(15.0, 1.15, keyboardLevel)
        if (totalLinesOfCode >= cost && qty > 0) {
            totalLinesOfCode -= cost
            keyboardLevel += qty
            viewModelScope.launch {
                repository.saveTotalLines(totalLinesOfCode)
                repository.saveKeyboardLevel(keyboardLevel)
            }
        }
    }

    fun buyJuniorDev() {
        val (cost, qty) = getUpgradeCostAndQuantity(100.0, 1.25, juniorDevsCount)
        if (totalLinesOfCode >= cost && qty > 0 && playerLevel >= 2) { // Condition de niveau de joueur ajoutée !
            totalLinesOfCode -= cost
            juniorDevsCount += qty
            viewModelScope.launch {
                repository.saveTotalLines(totalLinesOfCode)
                repository.saveJuniorDevsCount(juniorDevsCount)
            }
        }
    }

    fun buyServer() {
        val (cost, qty) = getUpgradeCostAndQuantity(800.0, 1.30, serverLevel)
        if (totalLinesOfCode >= cost && qty > 0 && keyboardLevel >= 80) {
            totalLinesOfCode -= cost
            serverLevel += qty
            viewModelScope.launch {
                repository.saveTotalLines(totalLinesOfCode)
                repository.saveServerLevel(serverLevel)
            }
        }
    }

    fun buyCopilot() {
        val (cost, qty) = getUpgradeCostAndQuantity(5000.0, 1.35, copilotLevel)
        if (totalLinesOfCode >= cost && qty > 0 && juniorDevsCount >= 10) {
            totalLinesOfCode -= cost
            copilotLevel += qty
            viewModelScope.launch {
                repository.saveTotalLines(totalLinesOfCode)
                repository.saveCopilotLevel(copilotLevel)
            }
        }
    }

    fun buyFramework() {
        val (cost, qty) = getUpgradeCostAndQuantity(40000.0, 1.40, frameworkLevel)
        if (totalLinesOfCode >= cost && qty > 0 && serverLevel >= 5) {
            totalLinesOfCode -= cost
            frameworkLevel += qty
            viewModelScope.launch {
                repository.saveTotalLines(totalLinesOfCode)
                repository.saveFrameworkLevel(frameworkLevel)
            }
        }
    }

    fun getCostForUI(type: String): Double = when(type) {
        "KEYBOARD" -> getUpgradeCostAndQuantity(15.0, 1.15, keyboardLevel).first
        "JUNIOR" -> getUpgradeCostAndQuantity(100.0, 1.25, juniorDevsCount).first
        "SERVER" -> getUpgradeCostAndQuantity(800.0, 1.30, serverLevel).first
        "COPILOT" -> getUpgradeCostAndQuantity(5000.0, 1.35, copilotLevel).first
        "FRAMEWORK" -> getUpgradeCostAndQuantity(40000.0, 1.40, frameworkLevel).first
        else -> 0.0
    }

    fun getQtyForUI(type: String): Int = when(type) {
        "KEYBOARD" -> getUpgradeCostAndQuantity(15.0, 1.15, keyboardLevel).second
        "JUNIOR" -> getUpgradeCostAndQuantity(100.0, 1.25, juniorDevsCount).second
        "SERVER" -> getUpgradeCostAndQuantity(800.0, 1.30, serverLevel).second
        "COPILOT" -> getUpgradeCostAndQuantity(5000.0, 1.35, copilotLevel).second
        "FRAMEWORK" -> getUpgradeCostAndQuantity(40000.0, 1.40, frameworkLevel).second
        else -> 0
    }
}