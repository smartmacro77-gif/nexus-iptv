package com.nexus.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF161616).copy(alpha = 0.95f))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Entrar", color = Color.White, fontSize = 22.sp)
            Text(text = "Xtream Codes API", color = Color(0xFFA0A0A0), fontSize = 12.sp)

            // Campos de input (url, user, pass) com foco unificado vermelho
            listOf("Server URL", "Username", "Password").forEach { placeholder ->
                var isFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF222222))
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            color = if (isFocused) Color(0xFFE50914) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = "  $placeholder", color = Color(0xFF707070), fontSize = 14.sp)
                }
            }

            // Botão Conectar
            var btnFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE50914))
                    .border(
                        width = if (btnFocused) 2.dp else 0.dp,
                        color = if (btnFocused) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .onFocusChanged { btnFocused = it.isFocused }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "CONECTAR", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}
