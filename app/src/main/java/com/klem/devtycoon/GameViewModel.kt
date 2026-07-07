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
enum class ActiveEventType { NONE, BUG, FREELANCE, LEVEL_UP_REWARD }
enum class QuestType { CLICK_COUNT, PRODUCE_LOC, BUY_UPGRADE }

// Structure d'animation graphique liée au Canvas natif
data class ClickParticle(
    val x: Float,
    val y: Float,
    val text: String,
    var progress: Float = 0f
)

// Structure pour le système de quêtes aléatoires
data class Quest(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val description: String,
    val type: QuestType,
    val targetGoal: Double,
    var currentProgress: Double = 0.0,
    val rewardLoc: Double,
    val rewardTalentPoints: Int,
    var isCompleted: Boolean = false
)

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    // --- ÉTATS DU JOUEUR ---
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
    var talentPoints by mutableStateOf(0)
        private set
    var skillHtml by mutableStateOf(0)
        private set
    var skillJs by mutableStateOf(0)
        private set
    var skillPhp by mutableStateOf(0)
        private set

    // --- SYSTÈME DE QUÊTES ---
    var currentQuest by mutableStateOf<Quest?>(null)
        private set
    private var totalClicksInCurrentQuest = 0.0
    private var locProducedInCurrentQuest = 0.0

    // Configuration des Catégories du Shop (Compteurs d'achats cumulés)
    var totalHardwareUpgrades by mutableStateOf(0)
        private set
    var totalSoftwareUpgrades by mutableStateOf(0)
        private set

    val clickParticles = mutableStateListOf<ClickParticle>()

    var activeEvent by mutableStateOf(ActiveEventType.NONE)
        private set
    var eventMultiplier by mutableStateOf(1.0)
        private set
    var eventMessage by mutableStateOf("")
        private set

    var selectedQuantity by mutableStateOf(PurchaseQuantity.X1)

    // --- FORMULES ÉVOLUTIVES ---
    val linesPerClick: Double get() = (1.0 + keyboardLevel) * (1.0 + (skillHtml * 0.10))
    val linesPerSecond: Double get() = ((juniorDevsCount * 2.0) + (serverLevel * 15.0) + (copilotLevel * 80.0) + (frameworkLevel * 500.0)) * eventMultiplier * (1.0 + (skillJs * 0.15))

    // FORMULE XP PROGRESSIVE ET NON-LINÉAIRE (Courbe de difficulté accrue)
    val xpNeededForNextLevel: Int get() = floor(100.0 * (playerLevel.toDouble().pow(1.5))).toInt()

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
                talentPoints = savedState.talentPoints
                skillHtml = savedState.skillHtml
                skillJs = savedState.skillJs
                skillPhp = savedState.skillPhp

                // Recalculer les totaux de catégories à partir des niveaux chargés
                totalHardwareUpgrades = keyboardLevel + serverLevel
                totalSoftwareUpgrades = juniorDevsCount + copilotLevel + frameworkLevel

                generateNewQuest()
            } catch (e: Exception) {
                generateNewQuest()
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

                    // Progression quête passive LOC
                    updateQuestProgress(QuestType.PRODUCE_LOC, linesPerSecond)
                }
            }
        }
    }

    // --- LOGIQUE DU SYSTÈME DE QUÊTES ---
    fun generateNewQuest() {
        val lvl = playerLevel
        val types = QuestType.values()
        val chosenType = types[Random.nextInt(types.size)]

        currentQuest = when (chosenType) {
            QuestType.CLICK_COUNT -> {
                val target = floor(15.0 + (lvl * 10.0) * Random.nextDouble(0.8, 1.3))
                Quest(
                    title = "Optimisation Compilateur",
                    description = "Effectuez ${target.toInt()} clics manuels pour stabiliser le build.",
                    type = chosenType,
                    targetGoal = target,
                    rewardLoc = floor((lvl * 50.0) * 1.5),
                    rewardTalentPoints = if (Random.nextDouble() > 0.85) 1 else 0
                )
            }
            QuestType.PRODUCE_LOC -> {
                val target = floor((lvl * 200.0) * Random.nextDouble(1.0, 2.5))
                Quest(
                    title = "Livraison Sprint",
                    description = "Générez ${target.toInt()} lignes de code au total.",
                    type = chosenType,
                    targetGoal = target,
                    rewardLoc = floor((lvl * 75.0) * 1.3),
                    rewardTalentPoints = if (Random.nextDouble() > 0.80) 1 else 0
                )
            }
            QuestType.BUY_UPGRADE -> {
                val target = floor(1.0 + (lvl / 3.0)).coerceAtMost(5.0)
                Quest(
                    title = "Mise à niveau Hardware/Software",
                    description = "Achetez ${target.toInt()} améliorations dans la boutique.",
                    type = chosenType,
                    targetGoal = target,
                    rewardLoc = floor((lvl * 60.0) * 1.4),
                    rewardTalentPoints = if (Random.nextDouble() > 0.90) 1 else 0
                )
            }
        }
        totalClicksInCurrentQuest = 0.0
        locProducedInCurrentQuest = 0.0
    }

    private fun updateQuestProgress(type: QuestType, amount: Double) {
        val quest = currentQuest ?: return
        if (quest.isCompleted || quest.type != type) return

        quest.currentProgress += amount
        if (quest.currentProgress >= quest.targetGoal) {
            quest.currentProgress = quest.targetGoal
            quest.isCompleted = true
        }
        currentQuest = quest.copy()
    }

    fun claimQuestRewards() {
        val quest = currentQuest ?: return
        if (!quest.isCompleted) return

        totalLinesOfCode += quest.rewardLoc
        talentPoints += quest.rewardTalentPoints

        viewModelScope.launch {
            repository.saveTotalLines(totalLinesOfCode)
            repository.saveTalentPoints(talentPoints)
        }
        generateNewQuest()
    }

    // --- INTERACTIONS CLICS ---
    fun codeClickedWithCoordinates(x: Float, y: Float) {
        if (clickParticles.size < 12) {
            clickParticles.add(ClickParticle(x = x, y = y, text = "+${linesPerClick.toInt()} LOC"))
        }

        totalLinesOfCode += linesPerClick

        // Progression quêtes
        updateQuestProgress(QuestType.CLICK_COUNT, 1.0)
        updateQuestProgress(QuestType.PRODUCE_LOC, linesPerClick)

        // Gestion XP avec formule progressive
        val xpGained = 1 + skillPhp
        playerXp += xpGained

        if (playerXp >= xpNeededForNextLevel) {
            playerXp -= xpNeededForNextLevel
            playerLevel += 1
            talentPoints += 1
            triggerLevelUpReward(playerLevel)
            viewModelScope.launch {
                repository.savePlayerLevel(playerLevel)
                repository.saveTalentPoints(talentPoints)
            }
        }

        viewModelScope.launch {
            repository.saveTotalLines(totalLinesOfCode)
            repository.savePlayerXp(playerXp)
        }
    }

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
            eventMultiplier = 0.5
            eventMessage = "[CRITICAL_ERR]: Bug de production ! Production passive ralentie de 50%."
        } else {
            activeEvent = ActiveEventType.FREELANCE
            eventMessage = "[OPPORTUNITY]: Contrat Freelance urgent disponible !"
        }
    }

    fun resolveBug() {
        activeEvent = ActiveEventType.NONE
        eventMultiplier = 1.0
        eventMessage = ""
    }

    fun acceptFreelance() {
        activeEvent = ActiveEventType.NONE
        val payout = (linesPerSecond * 60.0).coerceAtLeast(500.0)
        totalLinesOfCode += payout
        viewModelScope.launch { repository.saveTotalLines(totalLinesOfCode) }
    }

    fun dismissEvent() {
        activeEvent = ActiveEventType.NONE
        eventMessage = ""
    }

    private fun triggerLevelUpReward(newLvl: Int) {
        activeEvent = ActiveEventType.LEVEL_UP_REWARD
        when (Random.nextInt(3)) {
            0 -> {
                val bonusLoc = newLvl * 250.0
                totalLinesOfCode += bonusLoc
                eventMessage = "[LEVEL_UP]: Niveau $newLvl ! +1 Point de Talent. BONUS : +${bonusLoc.toInt()} LOC !"
            }
            1 -> {
                keyboardLevel += 1
                totalHardwareUpgrades += 1
                eventMessage = "[LEVEL_UP]: Niveau $newLvl ! +1 Point de Talent. BONUS : Clavier Mécanique +1 !"
                viewModelScope.launch { repository.saveKeyboardLevel(keyboardLevel) }
            }
            2 -> {
                juniorDevsCount += 1
                totalSoftwareUpgrades += 1
                eventMessage = "[LEVEL_UP]: Niveau $newLvl ! +1 Point de Talent. BONUS : Dev Junior +1 !"
                viewModelScope.launch { repository.saveJuniorDevsCount(juniorDevsCount) }
            }
        }
    }

    fun upgradeLanguage(lang: String) {
        if (talentPoints <= 0) return
        when (lang) {
            "HTML" -> {
                if (skillHtml < 5) {
                    skillHtml += 1
                    talentPoints -= 1
                    viewModelScope.launch { repository.saveSkillHtml(skillHtml); repository.saveTalentPoints(talentPoints) }
                }
            }
            "JS" -> {
                if (skillHtml >= 3 && skillJs < 5) {
                    skillJs += 1
                    talentPoints -= 1
                    viewModelScope.launch { repository.saveSkillJs(skillJs); repository.saveTalentPoints(talentPoints) }
                }
            }
            "PHP" -> {
                if (skillJs >= 4 && skillPhp < 5) {
                    skillPhp += 1
                    talentPoints -= 1
                    viewModelScope.launch { repository.saveSkillPhp(skillPhp); repository.saveTalentPoints(talentPoints) }
                }
            }
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
        for (i in 0 until qtyToBuy) { totalCost += baseCost * multiplier.pow(currentLevel + i) }
        return Pair(totalCost, qtyToBuy)
    }

    // --- MÀJ DES COMPTEURS DU SHOP PAR CATÉGORIES ---
    fun buyKeyboard() {
        val (cost, qty) = getUpgradeCostAndQuantity(15.0, 1.15, keyboardLevel)
        if (totalLinesOfCode >= cost && qty > 0) {
            totalLinesOfCode -= cost
            keyboardLevel += qty
            totalHardwareUpgrades += qty
            updateQuestProgress(QuestType.BUY_UPGRADE, qty.toDouble())
            viewModelScope.launch { repository.saveTotalLines(totalLinesOfCode); repository.saveKeyboardLevel(keyboardLevel) }
        }
    }

    fun buyJuniorDev() {
        val (cost, qty) = getUpgradeCostAndQuantity(100.0, 1.25, juniorDevsCount)
        if (totalLinesOfCode >= cost && qty > 0 && playerLevel >= 2) {
            totalLinesOfCode -= cost
            juniorDevsCount += qty
            totalSoftwareUpgrades += qty
            updateQuestProgress(QuestType.BUY_UPGRADE, qty.toDouble())
            viewModelScope.launch { repository.saveTotalLines(totalLinesOfCode); repository.saveJuniorDevsCount(juniorDevsCount) }
        }
    }

    fun buyServer() {
        val (cost, qty) = getUpgradeCostAndQuantity(800.0, 1.30, serverLevel)
        if (totalLinesOfCode >= cost && qty > 0 && keyboardLevel >= 80) {
            totalLinesOfCode -= cost
            serverLevel += qty
            totalHardwareUpgrades += qty
            updateQuestProgress(QuestType.BUY_UPGRADE, qty.toDouble())
            viewModelScope.launch { repository.saveTotalLines(totalLinesOfCode); repository.saveServerLevel(serverLevel) }
        }
    }

    fun buyCopilot() {
        val (cost, qty) = getUpgradeCostAndQuantity(5000.0, 1.35, copilotLevel)
        if (totalLinesOfCode >= cost && qty > 0 && juniorDevsCount >= 10) {
            totalLinesOfCode -= cost
            copilotLevel += qty
            totalSoftwareUpgrades += qty
            updateQuestProgress(QuestType.BUY_UPGRADE, qty.toDouble())
            viewModelScope.launch { repository.saveTotalLines(totalLinesOfCode); repository.saveCopilotLevel(copilotLevel) }
        }
    }

    fun buyFramework() {
        val (cost, qty) = getUpgradeCostAndQuantity(40000.0, 1.40, frameworkLevel)
        if (totalLinesOfCode >= cost && qty > 0 && serverLevel >= 5) {
            totalLinesOfCode -= cost
            frameworkLevel += qty
            totalSoftwareUpgrades += qty
            updateQuestProgress(QuestType.BUY_UPGRADE, qty.toDouble())
            viewModelScope.launch { repository.saveTotalLines(totalLinesOfCode); repository.saveFrameworkLevel(frameworkLevel) }
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