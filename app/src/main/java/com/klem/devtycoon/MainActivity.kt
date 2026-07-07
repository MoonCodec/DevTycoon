package com.klem.devtycoon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    label = { Text("CONSOLE", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    icon = { Text(">_", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    label = { Text("SHOP", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    icon = { Text("[$]", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> ClickerScreen(viewModel = viewModel)
                1 -> ShopScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ClickerScreen(viewModel: GameViewModel) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        viewModel.codeClickedWithCoordinates(offset.x, offset.y)
                    }
                )
            }
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "DEVELOPER LEVEL: ${viewModel.playerLevel}",
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "XP: ${viewModel.playerXp} / ${viewModel.xpNeededForNextLevel}",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "${viewModel.totalLinesOfCode.toInt()} LOC",
                fontFamily = FontFamily.Monospace,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (viewModel.activeEvent == ActiveEventType.BUG) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Text(
                text = "+${viewModel.linesPerClick.toInt()} loc/clic | +${viewModel.linesPerSecond.toInt()} loc/sec",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = "[ TAPPEZ N'IMPORTE OÙ POUR CODER ]\n\n<- Swipez pour la boutique ->",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        }

        // OPTIMISATION COMPLIANCE : Clé d'identifiant stable anti-lag pour le dessin
        viewModel.clickParticles.forEach { particle ->
            key(particle.id) {
                val xDp = with(density) { particle.x.toDp() }
                val yDp = with(density) { particle.y.toDp() }

                val elapsedTime = System.currentTimeMillis() - particle.creationTime
                val animationProgress = (elapsedTime / 600f).coerceIn(0f, 1f)

                val yOffset = - (animationProgress * 100f)
                val alpha = 1f - animationProgress

                Text(
                    text = particle.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .offset(x = xDp - 20.dp, y = yDp + yOffset.dp)
                        .graphicsLayer(alpha = alpha)
                )
            }
        }

        // SYSTÈME DE POPUP CENTRALISÉ (Gestion des alertes & CADEAUX)
        if (viewModel.activeEvent != ActiveEventType.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val isBug = viewModel.activeEvent == ActiveEventType.BUG
                val isReward = viewModel.activeEvent == ActiveEventType.LEVEL_UP_REWARD

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(
                        width = 2.dp,
                        color = when {
                            isBug -> MaterialTheme.colorScheme.error
                            isReward -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when {
                                isBug -> "!! KERNEL_PANIC !!"
                                isReward -> "== LEVEL_UP_REWARD =="
                                else -> "!! INCOMING_CONTRACT !!"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = when {
                                isBug -> MaterialTheme.colorScheme.error
                                isReward -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = viewModel.eventMessage,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        when (viewModel.activeEvent) {
                            ActiveEventType.BUG -> {
                                Button(
                                    onClick = { viewModel.resolveBug() },
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("[ DEBUGGER_LE_SYSTEME ]", fontFamily = FontFamily.Monospace, color = Color.White)
                                }
                            }
                            ActiveEventType.LEVEL_UP_REWARD -> {
                                Button(
                                    onClick = { viewModel.dismissEvent() },
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("[ RECLAIM_REWARD ]", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.background)
                                }
                            }
                            else -> {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Button(
                                        onClick = { viewModel.dismissEvent() },
                                        shape = RoundedCornerShape(2.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text("IGNORER", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = { viewModel.acceptFreelance() },
                                        shape = RoundedCornerShape(2.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("ACCEPTER", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.background)
                                    }
                                }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "=== QUANTITY_SELECTOR ===",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PurchaseQuantity.values().forEach { qty ->
                val isSelected = viewModel.selectedQuantity == qty
                Button(
                    onClick = { viewModel.selectedQuantity = qty },
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(text = qty.name, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "=== HARDWARE & STAFF ===",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        MarketItemCard(
            title = "Clavier Mécanique [Niv. ${viewModel.keyboardLevel}]",
            cost = viewModel.getCostForUI("KEYBOARD"),
            qtyToBuy = viewModel.getQtyForUI("KEYBOARD"),
            unlocked = true,
            requirementText = "",
            canAfford = viewModel.totalLinesOfCode >= viewModel.getCostForUI("KEYBOARD") && viewModel.getQtyForUI("KEYBOARD") > 0,
            onBuyClick = { viewModel.buyKeyboard() }
        )

        MarketItemCard(
            title = "Dev Junior [Qté: ${viewModel.juniorDevsCount}]",
            cost = viewModel.getCostForUI("JUNIOR"),
            qtyToBuy = viewModel.getQtyForUI("JUNIOR"),
            unlocked = viewModel.playerLevel >= 2,
            requirementText = "REQUIS: Développeur Niv. 2",
            canAfford = viewModel.totalLinesOfCode >= viewModel.getCostForUI("JUNIOR") && viewModel.getQtyForUI("JUNIOR") > 0,
            onBuyClick = { viewModel.buyJuniorDev() }
        )

        MarketItemCard(
            title = "Serveur Dédié [Niv. ${viewModel.serverLevel}]",
            cost = viewModel.getCostForUI("SERVER"),
            qtyToBuy = viewModel.getQtyForUI("SERVER"),
            unlocked = viewModel.keyboardLevel >= 80,
            requirementText = "REQUIS: Clavier Mécanique Niv. 80",
            canAfford = viewModel.totalLinesOfCode >= viewModel.getCostForUI("SERVER") && viewModel.getQtyForUI("SERVER") > 0,
            onBuyClick = { viewModel.buyServer() }
        )

        MarketItemCard(
            title = "IA Copilot [Niv. ${viewModel.copilotLevel}]",
            cost = viewModel.getCostForUI("COPILOT"),
            qtyToBuy = viewModel.getQtyForUI("COPILOT"),
            unlocked = viewModel.juniorDevsCount >= 10,
            requirementText = "REQUIS: Dev Junior Niv. 10",
            canAfford = viewModel.totalLinesOfCode >= viewModel.getCostForUI("COPILOT") && viewModel.getQtyForUI("COPILOT") > 0,
            onBuyClick = { viewModel.buyCopilot() }
        )

        MarketItemCard(
            title = "Framework Custom [Niv. ${viewModel.frameworkLevel}]",
            cost = viewModel.getCostForUI("FRAMEWORK"),
            qtyToBuy = viewModel.getQtyForUI("FRAMEWORK"),
            unlocked = viewModel.serverLevel >= 5,
            requirementText = "REQUIS: Serveur Dédié Niv. 5",
            canAfford = viewModel.totalLinesOfCode >= viewModel.getCostForUI("FRAMEWORK") && viewModel.getQtyForUI("FRAMEWORK") > 0,
            onBuyClick = { viewModel.buyFramework() }
        )
    }
}

@Composable
fun MarketItemCard(
    title: String,
    cost: Double,
    qtyToBuy: Int,
    unlocked: Boolean,
    requirementText: String,
    canAfford: Boolean,
    onBuyClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (unlocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (unlocked) {
                Text(text = title, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = "COÛT: ${cost.toInt()} LOC (x$qtyToBuy)", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onBuyClick,
                    enabled = canAfford,
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("EXECUTE_BUY", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.background)
                }
            } else {
                Text(text = "[ VERROUILLÉ ]", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text(text = requirementText, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
            }
        }
    }
}