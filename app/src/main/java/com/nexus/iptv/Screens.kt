package com.nexus.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchScreen(vm: AppVm) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)),
        contentAlignment = Alignment.Center
    ) {
        Text("Buscar", color = Color.White, fontSize = 20.sp)
    }
}

@Composable
fun MyListScreen(vm: AppVm) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)),
        contentAlignment = Alignment.Center
    ) {
        Text("Minha Lista", color = Color.White, fontSize = 20.sp)
    }
}

@Composable
fun DetailScreen(vm: AppVm, item: VodItem) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)),
        contentAlignment = Alignment.Center
    ) {
        Text("Detalhes: ${item.name}", color = Color.White, fontSize = 20.sp)
    }
}
