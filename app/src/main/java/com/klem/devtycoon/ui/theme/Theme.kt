package com.klem.devtycoon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette de couleurs "Cyber Terminal"
private val CyberDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00FF66),       // Vert Néon (Texte important, scores)
    secondary = Color(0xFF00E5FF),     // Cyan Électrique (Sous-titres, stats)
    background = Color(0xFF0A0F14),    // Noir/Bleu Nuit très sombre (Fond d'écran)
    surface = Color(0xFF101822),       // Gris/Bleu acier foncé (Fond des cartes)
    surfaceVariant = Color(0xFF16222F),// Version surélevée des cartes
    outline = Color(0xFF00FF66).copy(alpha = 0.5f) // Bordures cyber-lumineuses
)

@Composable
fun DevTycoonTheme(
    content: @Composable () -> Unit
) {
    // On force le thème sombre de manière permanente pour l'ambiance studio tech
    MaterialTheme(
        colorScheme = CyberDarkColorScheme,
        content = content
    )
}