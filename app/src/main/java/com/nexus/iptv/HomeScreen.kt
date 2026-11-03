package com.nexus.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onNavigateLive: () -> Unit,
    onNavigateVod: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "NEXUS IPTV",
                color = Color.White,
                fontSize = 32.sp,
                style = MaterialTheme.typography.headlineLarge
            )
            
            Spacer(modifier = Modifier.height(40.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Button(
                    onClick = onNavigateLive,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(180.dp, 80.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LiveTv, contentDescription = "Canais", tint = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Canais Ao Vivo", color = Color.White, fontSize = 14.sp)
                    }
                }

                Button(
                    onClick = onNavigateVod,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1F2E)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(180.dp, 80.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Movie, contentDescription = "Filmes", tint = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Filmes & Séries", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
