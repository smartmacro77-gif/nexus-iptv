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

data class VodItem(val title: String, val category: String)

@Composable
fun VodScreen(onBack: () -> Unit, onSelectMovie: (VodItem) -> Unit) {
    var focusedIndex by remember { mutableStateOf(0) }

    val movies = listOf(
        VodItem("Filme Exemplo 1", "Ação"),
        VodItem("Filme Exemplo 2", "Drama"),
        VodItem("Filme Exemplo 3", "Ficção"),
        VodItem("Filme Exemplo 4", "Comédia"),
        VodItem("Filme Exemplo 5", "Terror"),
        VodItem("Filme Exemplo 6", "Aventura")
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
                    text = "VOD - FILMES E SÉRIES",
                    color = Color.White,
                    fontSize = 22.sp,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Grid de VOD (2 colunas principais focadas em TV)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(movies) { movie ->
                    var isFocused by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .height(120.dp)
                            .tvFocusable { isFocused = it }
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) Color(0xFFE50914) else Color(0xFF1F1F2E),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectMovie(movie) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13131A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = movie.title,
                                tint = if (isFocused) Color(0xFFE50914) else Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = movie.title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = movie.category,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
