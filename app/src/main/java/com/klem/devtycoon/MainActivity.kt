package com.klem.devtycoon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klem.devtycoon.ui.theme.DevTycoonTheme

class MainActivity : ComponentActivity() {

    // Déclaration de l'initialisation du ViewModel via une Factory personnalisée pour passer le Repository
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "DevTycoon Studio",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${viewModel.totalLinesOfCode.toInt()} lignes",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "+${viewModel.linesPerClick.toInt()} / clic  |  +${viewModel.linesPerSecond.toInt()} / sec",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.codeClicked() },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            Text(text = "ÉCRIRE DU CODE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Boutique de Recrutement & Matériel",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start),
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Clavier Mécanique (Niveau ${viewModel.keyboardLevel})", fontWeight = FontWeight.Bold)
                Text(text = "Coût : ${viewModel.keyboardUpgradeCost.toInt()} lignes", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.buyKeyboardUpgrade() },
                    enabled = viewModel.totalLinesOfCode >= viewModel.keyboardUpgradeCost,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Améliorer (+1 / clic)")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Développeur Junior (Possédés : ${viewModel.juniorDevsCount})", fontWeight = FontWeight.Bold)
                Text(text = "Coût : ${viewModel.juniorDevCost.toInt()} lignes", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.hireJuniorDev() },
                    enabled = viewModel.totalLinesOfCode >= viewModel.juniorDevCost,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Recruter (+2 / sec)")
                }
            }
        }
    }
}