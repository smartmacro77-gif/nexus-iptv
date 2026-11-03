package com.nexus.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color

fun Modifier.tvFocusable(
    onFocusChanged: (Boolean) -> Unit = {}
): Modifier = this
    .focusable()
    .onFocusChanged { 
        onFocusChanged(it.isFocused)
    }

@Composable
fun NexusBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.background(Color(0xFF0B0B0F))
    ) {
        content()
    }
}
