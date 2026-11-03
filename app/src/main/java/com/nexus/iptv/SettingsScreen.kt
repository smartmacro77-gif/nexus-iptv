package com.nexus.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SettingItem(val title: String, val icon: ImageVector)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var focusedIndex by remember { mutableStateOf(0) }

    val settings = listOf(
        SettingItem("Configurações Gerais", Icons.Default.Settings),
        SettingItem("EPG", Icons.Default.DateRange),
        SettingItem("Formato de Fluxo", Icons.Default.SwapHoriz),
        SettingItem("Formato da hora", Icons.Default.Schedule),
        SettingItem("Automação", Icons.Default.Autorenew),
        SettingItem("Seleção do Mediaplayer", Icons.Default.PlayArrow),
        SettingItem("Configurações do Mediaplayer", Icons.Default.Tune),
        SettingItem("Controle dos pais", Icons.Default.Lock),
        SettingItem("Mediaplayer Externos", Icons.Default.OpenInNew),
        SettingItem("MULTI-SCREEN", Icons.Default.GridView),
        SettingItem("Teste de velocidade", Icons.Default.Speed),
        SettingItem("VPN", Icons.Default.NetworkCheck),
        SettingItem("Switch Device Mode", Icons.Default.Devices),
        SettingItem("Informações da Conta", Icons.Default.Person)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "CONFIGURAÇÕES",
                    color = Color.White,
                    fontSize = 22.sp,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.weight(1.dp))
                Text(
                    text = "novembro 03, 2026   08:27 AM",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Grid de Configurações
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(settings) { item ->
                    var isFocused by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .height(100.dp)
                            .tvFocusable { isFocused = it }
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) Color(0xFFE50914) else Color(0xFF1F1F2E),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13131A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isFocused) Color(0xFFE50914) else Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Rodapé Suporte
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUPORTE: 5511977888609",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
