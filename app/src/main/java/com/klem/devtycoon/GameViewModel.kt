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
    var progress: Float = 0f,
    val isCritical: Boolean = false
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
    var isCompleted: Boolean = false,
    val targetUpgradeKey: String = "" // Clé pour identifier l'upgrade ciblé si type == BUY_UPGRADE
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

    // Configuration des Catégories du Shop (Compteurs d'achats cumulés)
    var totalHardwareUpgrades by mutableStateOf(0)
        private set
    var totalSoftwareUpgrades by mutableStateOf(0)
        private set

    val clickParticles = mutableStateListOf<ClickParticle>()

    var activeEvent by mutableStateOf(ActiveEventType.NONE)
    var eventMultiplier by mutableStateOf(1.0)
        private set
    var eventMessage by mutableStateOf("")
        private set

    var selectedQuantity by mutableStateOf(PurchaseQuantity.X1)

    // --- FORMULES ÉVOLUTIVES ---
    val linesPerClick: Double get() = (1.0 + keyboardLevel) * (1.0 + (skillHtml * 0.10))
    val linesPerSecond: Double get() = ((juniorDevsCount * 2.0) + (serverLevel * 15.0) + (copilotLevel * 80.0) + (frameworkLevel * 500.0)) * eventMultiplier * (1.0 + (skillJs * 0.15))

    // FORMULE XP PROGRESSIVE ET NON-LINÉAIRE
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
                // Difficulté accrue : Score basé sur (Puissance de 1 clic * 50 clics) * 10
                val computedTarget = floor(linesPerClick * 50.0 * 10.0)
                // Évite un blocage si linesPerClick est à 0 par anomalie
                val target = if (computedTarget <= 0) 500.0 else computedTarget

                Quest(
                    title = "Refactoring de Production",
                    description = "Générez un volume critique de ${target.toInt()} LOC via des clics optimisés.",
                    type = chosenType,
                    targetGoal = target,
                    rewardLoc = floor((lvl * 150.0) * 2.0),
                    rewardTalentPoints = if (Random.nextDouble() > 0.75) 1 else 0
                )
            }
            QuestType.PRODUCE_LOC -> {
                // Quêtes de production de fond beaucoup plus ambitieuses (multiplié par 10 par rapport à l'ancienne version)
                val target = floor((lvl * 2000.0) * Random.nextDouble(1.5, 3.5))
                Quest(
                    title = "Architecture Cloud End-to-End",
                    description = "Déployez de nouvelles fonctionnalités pour atteindre +${target.toInt()} LOC globales.",
                    type = chosenType,
                    targetGoal = target,
                    rewardLoc = floor((lvl * 500.0) * 1.5),
                    rewardTalentPoints = if (Random.nextDouble() > 0.70) 1 else 0
                )
            }
            QuestType.BUY_UPGRADE -> {
                // Sélectionne une cible matérielle ou logicielle cohérente avec l'avancement
                val pool = mutableListOf<Pair<String, String>>()
                pool.add(Pair("KEYBOARD", "Clavier Mécanique"))
                if (playerLevel >= 2 && totalHardwareUpgrades >= 15) pool.add(Pair("JUNIOR", "Développeur Junior"))
                if (keyboardLevel >= 80) pool.add(Pair("SERVER", "Serveur Dédié"))
                if (juniorDevsCount >= 10 && totalHardwareUpgrades >= 15) pool.add(Pair("COPILOT", "IA Copilot"))
                if (serverLevel >= 5 && totalHardwareUpgrades >= 15) pool.add(Pair("FRAMEWORK", "Framework Custom"))

                val selectedUpgrade = pool[Random.nextInt(pool.size)]
                val currentLevelOfTarget = when (selectedUpgrade.first) {
                    "KEYBOARD" -> keyboardLevel
                    "JUNIOR" -> juniorDevsCount
                    "SERVER" -> serverLevel
                    "COPILOT" -> copilotLevel
                    "FRAMEWORK" -> frameworkLevel
                    else -> 0
                }

                // Objectif : Augmenter significativement le niveau actuel de cet élément spécifique
                val targetStep = Random.nextInt(2, 6) + (lvl / 2).coerceAtMost(10)
                val finalTargetGoal = (currentLevelOfTarget + targetStep).toDouble()

                Quest(
                    title = "Mise à Niveau : ${selectedUpgrade.second}",
                    description = "Atteignez le Niveau ${finalTargetGoal.toInt()} sur votre ${selectedUpgrade.second} pour restructurer la stack.",
                    type = chosenType,
                    targetGoal = finalTargetGoal,
                    currentProgress = currentLevelOfTarget.toDouble(),
                    rewardLoc = floor((lvl * 300.0) * 1.8),
                    rewardTalentPoints = if (Random.nextDouble() > 0.65) 1 else 0,
                    targetUpgradeKey = selectedUpgrade.first
                )
            }
        }
    }

    private fun updateQuestProgress(type: QuestType, amount: Double) {
        val quest = currentQuest ?: return
        if (quest.isCompleted || quest.type != type) return

        if (type == QuestType.BUY_UPGRADE) {
            // Pour les upgrades, la progression reflète le niveau actuel en temps réel
            val actualLevel = when (quest.targetUpgradeKey) {
                "KEYBOARD" -> keyboardLevel
                "JUNIOR" -> juniorDevsCount
                "SERVER" -> serverLevel
                "COPILOT" -> copilotLevel
                "FRAMEWORK" -> frameworkLevel
                else -> 0
            }
            quest.currentProgress = actualLevel.toDouble()
        } else {
            quest.currentProgress += amount
        }

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

    fun abandonAndSkipQuest() {
        // Optionnel : Je peut ajouter un coût en LOC si je veut éviter les abus,
        // mais pour l'instant je génère juste une nouvelle quyête proprement.
        generateNewQuest()
    }

    // --- INTERACTIONS CLICS ---
    fun codeClickedWithCoordinates(x: Float, y: Float) {
        val isCritical = Random.nextDouble() < 0.05
        val finalLinesGained = if (isCritical) linesPerClick * 3 else linesPerClick

        if (clickParticles.size < 12) {
            val textValue = if (isCritical) "CRITIQUE ! +${finalLinesGained.toInt()} LOC" else "+${finalLinesGained.toInt()} LOC"
            clickParticles.add(ClickParticle(x = x, y = y, text = textValue, isCritical = isCritical))
        }

        totalLinesOfCode += finalLinesGained

        // Progression des quêtes basées sur l'apport de code par clic
        updateQuestProgress(QuestType.CLICK_COUNT, finalLinesGained)
        updateQuestProgress(QuestType.PRODUCE_LOC, finalLinesGained)

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
                updateQuestProgress(QuestType.BUY_UPGRADE, 0.0)
            }
            2 -> {
                juniorDevsCount += 1
                totalSoftwareUpgrades += 1
                eventMessage = "[LEVEL_UP]: Niveau $newLvl ! +1 Point de Talent. BONUS : Dev Junior +1 !"
                viewModelScope.launch { repository.saveJuniorDevsCount(juniorDevsCount) }
                updateQuestProgress(QuestType.BUY_UPGRADE, 0.0)
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

    // --- ENREGISTREMENT ACHATS ET VÉRIFICATION DE LA QUÊTE CIBLE ---
    fun buyKeyboard() {
        val (cost, qty) = getUpgradeCostAndQuantity(15.0, 1.15, keyboardLevel)
        if (totalLinesOfCode >= cost && qty > 0) {
            totalLinesOfCode -= cost
            keyboardLevel += qty
            totalHardwareUpgrades += qty
            updateQuestProgress(QuestType.BUY_UPGRADE, 0.0)
            viewModelScope.launch { repository.saveTotalLines(totalLinesOfCode); repository.saveKeyboardLevel(keyboardLevel) }
        }
    }

    fun buyJuniorDev() {
        val (cost, qty) = getUpgradeCostAndQuantity(100.0, 1.25, juniorDevsCount)
        if (totalLinesOfCode >= cost && qty > 0 && playerLevel >= 2) {
            totalLinesOfCode -= cost
            juniorDevsCount += qty
            totalSoftwareUpgrades += qty
            updateQuestProgress(QuestType.BUY_UPGRADE, 0.0)
            viewModelScope.launch { repository.saveTotalLines(totalLinesOfCode); repository.saveJuniorDevsCount(juniorDevsCount) }
        }
    }

    fun buyServer() {
        val (cost, qty) = getUpgradeCostAndQuantity(800.0, 1.30, serverLevel)
        if (totalLinesOfCode >= cost && qty > 0 && keyboardLevel >= 80) {
            totalLinesOfCode -= cost
            serverLevel += qty
            totalHardwareUpgrades += qty
            updateQuestProgress(QuestType.BUY_UPGRADE, 0.0)
            viewModelScope.launch { repository.saveTotalLines(totalLinesOfCode); repository.saveServerLevel(serverLevel) }
        }
    }

    fun buyCopilot() {
        val (cost, qty) = getUpgradeCostAndQuantity(5000.0, 1.35, copilotLevel)
        if (totalLinesOfCode >= cost && qty > 0 && juniorDevsCount >= 10) {
            totalLinesOfCode -= cost
            copilotLevel += qty
            totalSoftwareUpgrades += qty
            updateQuestProgress(QuestType.BUY_UPGRADE, 0.0)
            viewModelScope.launch { repository.saveTotalLines(totalLinesOfCode); repository.saveCopilotLevel(copilotLevel) }
        }
    }

    fun buyFramework() {
        val (cost, qty) = getUpgradeCostAndQuantity(40000.0, 1.40, frameworkLevel)
        if (totalLinesOfCode >= cost && qty > 0 && serverLevel >= 5) {
            totalLinesOfCode -= cost
            frameworkLevel += qty
            totalSoftwareUpgrades += qty
            updateQuestProgress(QuestType.BUY_UPGRADE, 0.0)
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