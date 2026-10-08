package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

private data class OnboardingPage(val eyebrow: String, val title: String, val description: String, val icon: ImageVector)

@Composable
fun CarVisionOnboardingScreen(onComplete: () -> Unit, onSignIn: () -> Unit) {
    val pages = remember {
        listOf(
            OnboardingPage("IDENTIFIER", "Reconnaissez une voiture en une photo", "Prenez une photo ou choisissez-en une dans votre galerie. CarVision retrouve le véhicule dans son catalogue.", Icons.Default.PhotoCamera),
            OnboardingPage("COLLECTIONNER", "Gardez les voitures qui comptent", "Ajoutez uniquement les découvertes que vous choisissez. Leur rareté et leurs XP viennent directement de la base CARVIS.", Icons.Default.CollectionsBookmark),
            OnboardingPage("COMPARER", "Comprenez les différences essentielles", "Comparez les véhicules identifiés sans écrans techniques inutiles. Les informations utiles restent au premier plan.", Icons.Default.CompareArrows)
        )
    }
    var page by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().background(SurfaceDark)) {
        Box(Modifier.fillMaxWidth().height(360.dp).background(Brush.verticalGradient(listOf(CyanDark.copy(alpha = .70f), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 34.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("CARVIS", color = CyberCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Passer", color = TextMuted, fontWeight = FontWeight.Medium, modifier = Modifier.clickable(onClick = onSignIn).padding(8.dp))
            }
            Spacer(Modifier.weight(.55f))
            AnimatedContent(
                targetState = page,
                transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()).togetherWith(slideOutHorizontally { -it / 3 } + fadeOut()) },
                label = "onboarding"
            ) { index ->
                val item = pages[index]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(172.dp).clip(RoundedCornerShape(48.dp)).background(SurfaceContainerLow), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(92.dp).clip(CircleShape).background(CyberCyan.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                            Icon(item.icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(44.dp))
                        }
                    }
                    Text(item.eyebrow, color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, modifier = Modifier.padding(top = 34.dp))
                    Text(item.title, color = TextWhite, fontSize = 31.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                    Text(item.description, color = TextMuted, fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
                }
            }
            Spacer(Modifier.weight(.45f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                pages.indices.forEach { index ->
                    Box(Modifier.padding(horizontal = 4.dp).size(if (page == index) 22.dp else 7.dp, 7.dp).clip(CircleShape).background(if (page == index) CyberCyan else TextMuted.copy(alpha = .35f)))
                }
            }
            Button(
                onClick = { if (page < pages.lastIndex) page++ else onComplete() },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 0.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyanDark)
            ) { Text(if (page == pages.lastIndex) "COMMENCER" else "CONTINUER", fontWeight = FontWeight.Bold) }
            AnimatedVisibility(page == 0) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center) {
                    Text("Déjà un compte ? ", color = TextMuted)
                    Text("Se connecter", color = CyberCyan, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onSignIn))
                }
            }
        }
    }
}
