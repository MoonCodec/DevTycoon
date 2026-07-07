package com.klem.devtycoon

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow
import kotlin.random.Random

enum class PurchaseQuantity { X1, X10, X50, X100, MAX }
enum class ActiveEventType { NONE, BUG, FREELANCE }

data class ClickParticle(
    val id: UUID = UUID.randomUUID(),
    val x: Float,
    val y: Float,
    val text: String,
    val creationTime: Long = System.currentTimeMillis()
)

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    // États persistés
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
    var playerLevel by mutableStateOf(1)
        private set
    var playerXp by mutableStateOf(0)
        private set

    // États UI Volatils (Non persistés)
    val clickParticles = mutableStateListOf<ClickParticle>()
    var activeEvent by mutableStateOf(ActiveEventType.NONE)
        private set
    var eventMultiplier by mutableStateOf(1.0)
        private set
    var eventMessage by mutableStateOf("")
        private set

    var selectedQuantity by mutableStateOf(PurchaseQuantity.X1)

    // Getters de production avec altération par événements
    val linesPerClick: Double get() = 1.0 + keyboardLevel
    val linesPerSecond: Double get() = ((juniorDevsCount * 2.0) + (serverLevel * 15.0) + (copilotLevel * 80.0) + (frameworkLevel * 500.0)) * eventMultiplier
    val xpNeededForNextLevel: Int get() = playerLevel * 50

    init {
        loadSavedState()
        startGameLoop()
        startEventOrchestrator()
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
                // Nettoyage automatique des particules vieilles de plus de 800ms
                val now = System.currentTimeMillis()
                clickParticles.removeAll { now - it.creationTime > 800 }
            }
        }
    }

    // Boucle asynchrone de génération d'événements aléatoires (toutes les 45 à 90 secondes)
    private fun startEventOrchestrator() {
        viewModelScope.launch {
            while (true) {
                delay(Random.nextLong(45000L, 90000L))
                if (activeEvent == ActiveEventType.NONE) {
                    triggerRandomEvent()
                }
            }
        }
    }

    private fun triggerRandomEvent() {
        val isBug = Random.nextBoolean()
        if (isBug) {
            activeEvent = ActiveEventType.BUG
            eventMultiplier = 0.5 // Perte de 50% de la production passive
            eventMessage = "[CRITICAL_ERR]: Bug de production ! Production passive ralentie de 50%."
        } else {
            activeEvent = ActiveEventType.FREELANCE
            eventMessage = "[OPPORTUNITY]: Contrat Freelance urgent disponible !"
        }
    }

    // Actions utilisateur sur les alertes
    fun resolveBug() {
        activeEvent = ActiveEventType.NONE
        eventMultiplier = 1.0
        eventMessage = ""
    }

    fun acceptFreelance() {
        activeEvent = ActiveEventType.NONE
        val payout = (linesPerSecond * 60.0).coerceAtLeast(500.0) // Gagne l'équivalent de 1min de prod
        totalLinesOfCode += payout
        viewModelScope.launch { repository.saveTotalLines(totalLinesOfCode) }
    }

    fun dismissFreelance() {
        activeEvent = ActiveEventType.NONE
        eventMessage = ""
    }

    // Enregistrement d'un clic avec transmission des coordonnées géométriques de l'écran
    fun codeClickedWithCoordinates(x: Float, y: Float) {
        codeClicked()
        val text = "+${linesPerClick.toInt()} LOC"
        clickParticles.add(ClickParticle(x = x, y = y, text = text))
    }

    private fun codeClicked() {
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
        if (totalLinesOfCode >= cost && qty > 0 && playerLevel >= 2) {
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