package com.nexus.iptv

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.tvFocusable(
    onFocusChanged: (Boolean) -> Unit = {}
): Modifier = this
    .focusable()
    .onFocusChanged { 
        onFocusChanged(it.isFocused)
    }
