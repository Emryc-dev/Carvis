package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TitaniumBorder
import com.example.ui.viewmodel.AuthUiState

@Composable
fun AuthScreen(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String) -> Unit,
    onBack: () -> Unit,
    onModeChange: () -> Unit
) {
    var signUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = SurfaceContainer,
        unfocusedContainerColor = SurfaceContainer,
        focusedBorderColor = CyberCyan,
        unfocusedBorderColor = Color.Transparent,
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        focusedLabelColor = CyberCyan,
        unfocusedLabelColor = TextMuted
    )

    fun submit() {
        validation = when {
            signUp && name.trim().length < 2 -> "Saisissez votre nom."
            !email.contains("@") -> "Saisissez une adresse e-mail valide."
            password.length < 6 -> "Le mot de passe doit contenir au moins 6 caractères."
            else -> null
        }
        if (validation == null) {
            focus.clearFocus()
            if (signUp) onSignUp(email.trim(), password, name.trim()) else onSignIn(email.trim(), password)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SurfaceDark).imePadding()) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 16.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = TextWhite)
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))
            Box(
                modifier = Modifier.size(76.dp).background(CyberCyan, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsCar, "CarVision", tint = CyanDark, modifier = Modifier.size(38.dp))
            }

            Spacer(Modifier.height(38.dp))
            Text(
                if (signUp) "Créer un compte" else "Se connecter",
                color = TextWhite,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (signUp) "Enregistrez vos analyses et vos véhicules." else "Retrouvez vos analyses et votre garage.",
                color = TextMuted,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
            )

            if (signUp) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; validation = null },
                    label = { Text("Nom") },
                    singleLine = true,
                    enabled = !state.isLoading,
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim(); validation = null },
                label = { Text("Adresse e-mail") },
                singleLine = true,
                enabled = !state.isLoading,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; validation = null },
                label = { Text("Mot de passe") },
                singleLine = true,
                enabled = !state.isLoading,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                "Masquer le mot de passe"
                            } else {
                                "Afficher le mot de passe"
                            },
                            tint = TextMuted
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth()
            )

            (validation ?: state.error)?.let {
                Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            }
            state.notice?.let {
                Text(it, color = CyberCyan, fontSize = 14.sp, lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            }

            Button(
                onClick = { submit() },
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyanDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 4.dp)
            ) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = CyanDark, strokeWidth = 2.dp)
                else Text(if (signUp) "Créer mon compte" else "Continuer", fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = TitaniumBorder)
                Text("ou", color = TextMuted)
                HorizontalDivider(modifier = Modifier.weight(1f), color = TitaniumBorder)
            }

            SocialButton(R.drawable.ic_google, "Continuer avec Google")
            Spacer(Modifier.height(12.dp))
            SocialButton(R.drawable.ic_apple, "Continuer avec Apple")

            TextButton(
                onClick = {
                    signUp = !signUp
                    validation = null
                    onModeChange()
                },
                modifier = Modifier.padding(top = 22.dp)
            ) {
                Text(
                    if (signUp) "Déjà un compte ? Se connecter" else "Pas encore de compte ? S’inscrire",
                    color = CyberCyan,
                    fontWeight = FontWeight.Medium
                )
            }
            Text("Google et Apple seront disponibles après configuration OAuth.", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SocialButton(iconRes: Int, label: String) {
    OutlinedButton(
        onClick = {},
        enabled = false,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(54.dp),
        colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = TextMuted)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.size(14.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}





