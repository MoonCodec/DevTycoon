package com.klem.devtycoon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.klem.devtycoon.ui.theme.DevTycoonTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = GameRepository(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return GameViewModel(repository) as T
            }
        })[GameViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DevTycoonTheme {
                MainNavigationStructure(viewModel = gameViewModel)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainNavigationStructure(viewModel: GameViewModel) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    label = { Text("CONSOLE", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    icon = { Text(">_", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    label = { Text("SHOP", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    icon = { Text("[$]", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                    label = { Text("TALENTS", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    icon = { Text("{*}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                )
            }
        }
    ) { innerPadding ->
        HorizontalPager(state = pagerState, modifier = Modifier.padding(innerPadding)) { page ->
            when (page) {
                0 -> ClickerScreen(viewModel = viewModel)
                1 -> ShopScreen(viewModel = viewModel)
                2 -> TechTreeScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ClickerScreen(viewModel: GameViewModel) {
    val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()
    // Couleur dorée exclusive pour l'affichage visuel des coups critiques
    val criticalColorArgb = android.graphics.Color.parseColor("#FFD700")
    val nextFrameSignal = remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { frameTime ->
                nextFrameSignal.value = frameTime
                if (viewModel.clickParticles.isNotEmpty()) {
                    val iterator: MutableIterator<ClickParticle> = viewModel.clickParticles.iterator()
                    while (iterator.hasNext()) {
                        val particle: ClickParticle = iterator.next()
                        particle.progress += 0.04f
                        if (particle.progress >= 1f) {
                            iterator.remove()
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset -> viewModel.codeClickedWithCoordinates(offset.x, offset.y) })
            }
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Section Supérieure (Stats)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                Text(text = "DEVELOPER LEVEL: ${viewModel.playerLevel}", fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Text(text = "XP: ${viewModel.playerXp} / ${viewModel.xpNeededForNextLevel}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
            }

            // Section Centrale (LOC)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${viewModel.totalLinesOfCode.toInt()} LOC", fontFamily = FontFamily.Monospace, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = if (viewModel.activeEvent == ActiveEventType.BUG) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text(text = "+${viewModel.linesPerClick.toInt()} loc/clic | +${viewModel.linesPerSecond.toInt()} loc/sec", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            }

            // CORRECTION DE LA BOÎTE DE QUÊTE (Taille adaptative sans superposition)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 16.dp)
            ) {
                viewModel.currentQuest?.let { quest ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(), // Gère fluidement l'expansion de la boîte
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, if (quest.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = ">_ QUÊTE: ${quest.title}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                                )
                                Text(
                                    text = "[${quest.currentProgress.toInt()}/${quest.targetGoal.toInt()}]",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = quest.description,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            if (quest.isCompleted) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { viewModel.claimQuestRewards() },
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(text = "RÉCUPÉRER: +${quest.rewardLoc.toInt()} LOC ${if(quest.rewardTalentPoints > 0) " +1 PT" else ""}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.background)
                                }
                            }
                        }
                    }
                }
            }
        }

        val textPaint = remember {
            android.graphics.Paint().apply {
                textSize = 44f
                typeface = android.graphics.Typeface.MONOSPACE
                isAntiAlias = true
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val _signal = nextFrameSignal.value
            viewModel.clickParticles.forEach { particle: ClickParticle ->
                val alpha = ((1f - particle.progress) * 255).toInt().coerceIn(0, 255)
                textPaint.alpha = alpha
                // Assigne dynamiquement la couleur selon si le clic est critique ou non
                textPaint.color = if (particle.isCritical) criticalColorArgb else primaryColorArgb

                val yOffset = particle.progress * 180f
                drawContext.canvas.nativeCanvas.drawText(particle.text, particle.x - 50f, particle.y - yOffset, textPaint)
            }
        }

        // Fenêtre Pop-up d'Événements
        if (viewModel.activeEvent != ActiveEventType.NONE) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).pointerInput(Unit) { detectTapGestures { } }.padding(16.dp), contentAlignment = Alignment.Center) {
                val isBug = viewModel.activeEvent == ActiveEventType.BUG
                val isReward = viewModel.activeEvent == ActiveEventType.LEVEL_UP_REWARD
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(0.dp), border = BorderStroke(width = 2.dp, color = if (isBug) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isBug) "!! KERNEL_PANIC !!" else if (isReward) "== LEVEL_UP_REWARD ==" else "!! INCOMING_CONTRACT !!", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (isBug) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = viewModel.eventMessage, fontFamily = FontFamily.Monospace, fontSize = 13.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(24.dp))
                        if (isBug) {
                            Button(onClick = { viewModel.resolveBug() }, shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("[ DEBUGGER_LE_SYSTEME ]", fontFamily = FontFamily.Monospace, color = Color.White) }
                        } else if (isReward) {
                            Button(onClick = { viewModel.dismissEvent() }, shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("[ RECLAIM_REWARD ]", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.background) }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Button(onClick = { viewModel.dismissEvent() }, shape = RoundedCornerShape(2.dp), modifier = Modifier.weight(1f)) { Text("IGNORER", fontFamily = FontFamily.Monospace) }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(onClick = { viewModel.acceptFreelance() }, shape = RoundedCornerShape(2.dp), modifier = Modifier.weight(1f)) { Text("ACCEPTER", fontFamily = FontFamily.Monospace) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopScreen(viewModel: GameViewModel) {
    val scrollState = rememberScrollState()
    val selectedCategory = remember { mutableStateOf("HARDWARE") }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp).verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "=== QUANTITY_SELECTOR ===", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PurchaseQuantity.values().forEach { qty ->
                val isSelected = viewModel.selectedQuantity == qty
                Button(onClick = { viewModel.selectedQuantity = qty }, modifier = Modifier.weight(1f).padding(horizontal = 2.dp), shape = RoundedCornerShape(2.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text(text = qty.name, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { selectedCategory.value = "HARDWARE" },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedCategory.value == "HARDWARE") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("MATÉRIEL (${viewModel.totalHardwareUpgrades})", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
            Button(
                onClick = { selectedCategory.value = "SOFTWARE" },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedCategory.value == "SOFTWARE") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("LOGICIEL (${viewModel.totalSoftwareUpgrades})", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedCategory.value == "HARDWARE") {
            Text(text = "--- COMPOSANTS MATÉRIELS ---", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))
            MarketItemCard("Clavier Mécanique [Niv. ${viewModel.keyboardLevel}]", viewModel.getCostForUI("KEYBOARD"), viewModel.getQtyForUI("KEYBOARD"), true, "", viewModel.totalLinesOfCode >= viewModel.getCostForUI("KEYBOARD"), { viewModel.buyKeyboard() })
            MarketItemCard("Serveur Dédié [Niv. ${viewModel.serverLevel}]", viewModel.getCostForUI("SERVER"), viewModel.getQtyForUI("SERVER"), viewModel.keyboardLevel >= 80, "REQUIS: Clavier Mécanique Niv. 80", viewModel.totalLinesOfCode >= viewModel.getCostForUI("SERVER"), { viewModel.buyServer() })
        } else {
            val isSoftwareUnlocked = viewModel.totalHardwareUpgrades >= 15

            if (isSoftwareUnlocked) {
                Text(text = "--- COMPOSANTS LOGICIELS ---", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))
                MarketItemCard("Dev Junior [Qté: ${viewModel.juniorDevsCount}]", viewModel.getCostForUI("JUNIOR"), viewModel.getQtyForUI("JUNIOR"), viewModel.playerLevel >= 2, "REQUIS: Développeur Niv. 2", viewModel.totalLinesOfCode >= viewModel.getCostForUI("JUNIOR"), { viewModel.buyJuniorDev() })
                MarketItemCard("IA Copilot [Niv. ${viewModel.copilotLevel}]", viewModel.getCostForUI("COPILOT"), viewModel.getQtyForUI("COPILOT"), viewModel.juniorDevsCount >= 10, "REQUIS: Dev Junior Niv. 10", viewModel.totalLinesOfCode >= viewModel.getCostForUI("COPILOT"), { viewModel.buyCopilot() })
                MarketItemCard("Framework Custom [Niv. ${viewModel.frameworkLevel}]", viewModel.getCostForUI("FRAMEWORK"), viewModel.getQtyForUI("FRAMEWORK"), viewModel.serverLevel >= 5, "REQUIS: Serveur Dédié Niv. 5", viewModel.totalLinesOfCode >= viewModel.getCostForUI("FRAMEWORK"), { viewModel.buyFramework() })
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "[ CATÉGORIE VERROUILLÉE ]\n\nAchetez encore ${15 - viewModel.totalHardwareUpgrades} composants dans la section MATÉRIEL pour débloquer l'ingénierie Logicielle.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun TechTreeScreen(viewModel: GameViewModel) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp).verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "=== TECH_SKILL_TREE ===", fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = "Points de Talent disponibles : ${viewModel.talentPoints}", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(24.dp))
        SkillTreeNodeCard("HTML / CSS [Niv. ${viewModel.skillHtml}/5]", "Bases du Web. Multiplie la force de vos clics manuels (+10% par niveau).", true, "Disponible immédiatement", viewModel.talentPoints > 0 && viewModel.skillHtml < 5, { viewModel.upgradeLanguage("HTML") })
        Text(text = "│\n▼", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
        val jsUnlocked = viewModel.skillHtml >= 3
        SkillTreeNodeCard("JavaScript [Niv. ${viewModel.skillJs}/5]", "Ajoute du dynamisme. Augmente la production de tout votre matériel passif (+15% par niveau).", jsUnlocked, "BLOQUÉ : Requiert HTML / CSS Niv. 3", viewModel.talentPoints > 0 && jsUnlocked && viewModel.skillJs < 5, { viewModel.upgradeLanguage("JS") })
        Text(text = "│\n▼", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
        val phpUnlocked = viewModel.skillJs >= 4
        SkillTreeNodeCard("PHP Server [Niv. ${viewModel.skillPhp}/5]", "Moteur Backend. Augmente l'XP reçue à chaque clic manuel (+1 XP par niveau).", phpUnlocked, "BLOQUÉ : Requiert JavaScript Niv. 4", viewModel.talentPoints > 0 && phpUnlocked && viewModel.skillPhp < 5, { viewModel.upgradeLanguage("PHP") })
    }
}

@Composable
fun SkillTreeNodeCard(name: String, description: String, unlocked: Boolean, reqText: String, canAfford: Boolean, onUpgrade: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = name, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (unlocked) MaterialTheme.colorScheme.primary else Color.Gray)
            Text(text = description, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(vertical = 4.dp))
            if (!unlocked) {
                Text(text = reqText, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            } else {
                Button(onClick = onUpgrade, enabled = canAfford, shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("INVESTIR 1 PT", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.background) }
            }
        }
    }
}

@Composable
fun MarketItemCard(title: String, cost: Double, qtyToBuy: Int, unlocked: Boolean, requirementText: String, canAfford: Boolean, onBuyClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, if (unlocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (unlocked) {
                Text(text = title, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = "COÛT: ${cost.toInt()} LOC (x$qtyToBuy)", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onBuyClick, enabled = canAfford, shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("EXECUTE_BUY", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.background) }
            } else {
                Text(text = "[ VERROUILLÉ ]", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text(text = requirementText, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
            }
        }
    }
}