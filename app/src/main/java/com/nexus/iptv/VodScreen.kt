package com.nexus.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VodScreen(vm: AppVm, isSeries: Boolean = false) {
    val title = if (isSeries) "Séries" else "Filmes"
    val categories = listOf("Todos", "Pedidos", "Lançamentos", "Cinema", "Dramas Curtos", "4K", "Ação", "Doramas")
    var selectedCategory by remember { mutableStateOf("Todos") }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = selectedCategory == cat

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF8B0000).copy(alpha = 0.4f) else Color(0xFF1A1A1A))
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            color = if (isFocused) Color(0xFFE50914) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .onKeyEvent { event ->
                            if (event.key == Key.DirectionCenter || event.key == Key.Enter) {
                                selectedCategory = cat
                                true
                            } else {
                                false
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "  $cat",
                        color = if (isFocused || isSelected) Color.White else Color(0xFFA0A0A0),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text(
                text = "$title > $selectedCategory",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(12) { index ->
                    var cardFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF222222))
                            .border(
                                width = if (cardFocused) 2.dp else 0.dp,
                                color = if (cardFocused) Color(0xFFE50914) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .onFocusChanged { cardFocused = it.isFocused }
                            .focusable()
                    )
                }
            }
        }
    }
}
