package com.nexus.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.delay

@Composable
fun HomeScreen() {
    val heroItems = listOf(
        "João e Maria: Caçadores de Bruxas",
        "Apocalypse New World",
        "The Birthday",
        "007 GoldenEye",
        "Channel Zero",
        "Inferno Sobre Rodas"
    )
    
    var currentHeroIndex by remember { mutableStateOf(0) }
    var isHeroFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isHeroFocused) {
        if (!isHeroFocused) {
            while (true) {
                delay(6000L)
                currentHeroIndex = (currentHeroIndex + 1) % heroItems.size
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1C1C1C))
                .onFocusChanged { isHeroFocused = it.isFocused }
                .focusable(),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "DESTAQUE: ${heroItems[currentHeroIndex]}", color = Color.White, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "4K UHD • Dolby Vision • Ação & Aventura", color = Color(0xFFA0A0A0), fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Trending Now", color = Color.White, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(heroItems) { title ->
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(90.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF252525))
                        .focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = title, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}
