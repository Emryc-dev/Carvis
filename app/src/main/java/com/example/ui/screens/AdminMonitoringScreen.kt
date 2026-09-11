package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CarVisionTopBar
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun AdminMonitoringScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = { CarVisionTopBar(title = "Administration", showBackButton = true, onBackClick = onBackClick) },
        containerColor = SurfaceDark
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Administration", color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "Aucune donnée d’administration n’est disponible. Le backend ne fournit pas encore d’endpoint sécurisé pour cet écran.",
                color = TextMuted,
                fontSize = 16.sp,
                lineHeight = 23.sp
            )
            OutlinedButton(onClick = onBackClick) { Text("Retour") }
        }
    }
}

