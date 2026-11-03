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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VodScreen(onBack: () -> Unit, onSelectMovie: (VodItem) -> Unit) {
    val movies = listOf(
        VodItem("1", "Filme Exemplo 1", "", "", "8.5", "Ação", "", false, "Sinopse", "2024", "Ação"),
        VodItem("2", "Filme Exemplo 2", "", "", "7.8", "Drama", "", false, "Sinopse", "2023", "Drama")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "VOD - FILMES E SÉRIES", color = Color.White, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(movies) { movie ->
                    Card(
                        modifier = Modifier
                            .height(100.dp)
                            .clickable { onSelectMovie(movie) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13131A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = movie.title, tint = Color.Red, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = movie.title, color = Color.White, fontSize = 16.sp)
                                Text(text = movie.category, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
