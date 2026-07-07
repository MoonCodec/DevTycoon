package com.klem.devtycoon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klem.devtycoon.ui.theme.DevTycoonTheme

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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        viewModel = gameViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun GameScreen(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // En-tête Terminal
        Text(
            text = "> DEV_TYCOON.EXE",
            fontFamily = FontFamily.Monospace,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${viewModel.totalLinesOfCode.toInt()} LOC",
            fontFamily = FontFamily.Monospace,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "[SYS]: +${viewModel.linesPerClick.toInt()}/clic | +${viewModel.linesPerSecond.toInt()}/sec",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Zone Interactive Principale
        Button(
            onClick = { viewModel.codeClicked() },
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            Text("[ EXECUTE_COMPILE ]", fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // CONFIGURATION DU SÉLECTEUR QUANTITATIF
        Text(
            text = "=== QUANTITY_SELECTOR ===",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "=== CENTRAL_MARKET ===",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ITEM 1 : CLAVIER MÉCANIQUE
        MarketItemCard(
            title = "HARDWARE: Clavier Mécanique [Niv.${viewModel.keyboardLevel}]",
            cost = viewModel.getCostForUI("KEYBOARD"),
            qtyToBuy = viewModel.getQtyForUI("KEYBOARD"),
            unlocked = true,
            requirementText = "",
            canAfford = viewModel.totalLinesOfCode >= viewModel.getCostForUI("KEYBOARD") && viewModel.getQtyForUI("KEYBOARD") > 0,
            onBuyClick = { viewModel.buyKeyboard() }
        )

        // ITEM 2 : DÉVELOPPEUR JUNIOR
        MarketItemCard(
            title = "HUMAN: Dev Junior [Qté:${viewModel.juniorDevsCount}]",
            cost = viewModel.getCostForUI("JUNIOR"),
            qtyToBuy = viewModel.getQtyForUI("JUNIOR"),
            unlocked = true,
            requirementText = "",
            canAfford = viewModel.totalLinesOfCode >= viewModel.getCostForUI("JUNIOR") && viewModel.getQtyForUI("JUNIOR") > 0,
            onBuyClick = { viewModel.buyJuniorDev() }
        )

        // ITEM 3 : SERVEUR DÉDIÉ (Condition : Clavier x80)
        MarketItemCard(
            title = "INFRA: Serveur Dédié [Niv.${viewModel.serverLevel}]",
            cost = viewModel.getCostForUI("SERVER"),
            qtyToBuy = viewModel.getQtyForUI("SERVER"),
            unlocked = viewModel.keyboardLevel >= 80,
            requirementText = "REQUIS: Clavier Mecanique x80",
            canAfford = viewModel.totalLinesOfCode >= viewModel.getCostForUI("SERVER") && viewModel.getQtyForUI("SERVER") > 0,
            onBuyClick = { viewModel.buyServer() }
        )

        // ITEM 4 : IA COPILOT (Condition : Dev Junior x10)
        MarketItemCard(
            title = "SOFTWARE: IA Copilot [Niv.${viewModel.copilotLevel}]",
            cost = viewModel.getCostForUI("COPILOT"),
            qtyToBuy = viewModel.getQtyForUI("COPILOT"),
            unlocked = viewModel.juniorDevsCount >= 10,
            requirementText = "REQUIS: Dev Junior x10",
            canAfford = viewModel.totalLinesOfCode >= viewModel.getCostForUI("COPILOT") && viewModel.getQtyForUI("COPILOT") > 0,
            onBuyClick = { viewModel.buyCopilot() }
        )

        // ITEM 5 : FRAMEWORK CUSTOM (Condition : Serveur x5)
        MarketItemCard(
            title = "ARCH: Framework Custom [Niv.${viewModel.frameworkLevel}]",
            cost = viewModel.getCostForUI("FRAMEWORK"),
            qtyToBuy = viewModel.getQtyForUI("FRAMEWORK"),
            unlocked = viewModel.serverLevel >= 5,
            requirementText = "REQUIS: Serveur Dedie x5",
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
                Text(
                    text = "TOTAL_REQ: ${cost.toInt()} LOC (x$qtyToBuy)",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
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
                Text(text = requirementText, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
            }
        }
    }
}