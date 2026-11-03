@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package com.nexus.iptv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NX {
    val Black = Color(0xFF050507)
    val Surface = Color(0xFF101013)
    val Card = Color(0xFF17171C)
    val Crimson = Color(0xFFE50914)
    val Border = Color(0xFF2B2B33)
    val TextSec = Color(0xFFA3A3AD)
}

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = NX.Crimson, background = NX.Black, surface = NX.Surface,
            onSurface = Color.White, onBackground = Color.White),
        content = content
    )
}

@Composable
fun NexusBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.fillMaxSize().background(NX.Black).drawBehind {
            drawRect(Brush.radialGradient(listOf(NX.Crimson.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(size.width * 0.5f, -size.height * 0.1f), radius = size.width * 0.75f))
            drawRect(Brush.radialGradient(listOf(NX.Crimson.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(size.width, size.height), radius = size.width * 0.5f))
        },
        content = content
    )
}

// ================= SELETOR ÚNICO =================
// Borda carmesim com brilho + cantos arredondados + transição suave. Usado por TODOS os elementos focáveis.
fun Modifier.nexusStyle(level: Float, radius: Dp = 14.dp, container: Color = Color.Transparent, idleBorder: Color = NX.Border): Modifier =
    this
        .drawBehind {
            if (level > 0.01f) {
                for (i in 1..6) {
                    val g = (i * 2.5f).dp.toPx()
                    drawRoundRect(
                        color = NX.Crimson.copy(alpha = 0.22f * level * (1f - i / 7f)),
                        topLeft = Offset(-g, -g),
                        size = Size(size.width + g * 2, size.height + g * 2),
                        cornerRadius = CornerRadius(radius.toPx() + g),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }
        }
        .background(container, RoundedCornerShape(radius))
        .background(NX.Crimson.copy(alpha = 0.16f * level), RoundedCornerShape(radius))
        .border((1f + 1.5f * level).dp, lerp(idleBorder, NX.Crimson, level), RoundedCornerShape(radius))

fun Modifier.nexusFocus(
    radius: Dp = 14.dp,
    container: Color = Color.Transparent,
    selected: Boolean = false,
    idleBorder: Color = NX.Border,
    requester: FocusRequester? = null,
    onFocusChange: (Boolean) -> Unit = {},
    onMenu: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val level by animateFloatAsState(if (focused) 1f else if (selected) 0.55f else 0f, tween(160), label = "nx")
    var m: Modifier = this
    if (requester != null) m = m.focusRequester(requester)
    m.nexusStyle(level, radius, container, idleBorder)
        .onFocusChanged { focused = it.isFocused; onFocusChange(it.isFocused) }
        .onPreviewKeyEvent { ev ->
            if (onMenu != null && ev.type == KeyEventType.KeyUp && (ev.key == Key.Menu || ev.key == Key.ProgramYellow)) {
                onMenu(); true
            } else false
        }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

// Para campos de texto (o foco está num filho)
fun Modifier.nexusFocusGroup(radius: Dp = 12.dp): Modifier = composed {
    var f by remember { mutableStateOf(false) }
    val level by animateFloatAsState(if (f) 1f else 0f, tween(160), label = "nxg")
    nexusStyle(level, radius, Color(0x66000000)).onFocusChanged { f = it.hasFocus }
}

suspend fun FocusRequester.focusSoon(delayMs: Long = 150) { delay(delayMs); runCatching { requestFocus() } }

// ================= COMPONENTES =================
@Composable
fun NxButton(text: String, icon: ImageVector?, primary: Boolean, modifier: Modifier = Modifier,
             requester: FocusRequester? = null, onClick: () -> Unit) {
    Row(
        modifier.height(36.dp)
            .nexusFocus(9.dp, if (primary) NX.Crimson else Color(0x40FFFFFF), requester = requester, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun NxIconButton(icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(36.dp).nexusFocus(18.dp, Color(0x55000000), onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun Chip(text: String, accent: Boolean = false) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier.clip(shape).background(if (accent) Color(0x33E50914) else Color(0x40FFFFFF))
            .border(1.dp, if (accent) NX.Crimson else Color(0x55FFFFFF), shape).padding(horizontal = 6.dp, vertical = 2.dp)
    ) { Text(text, color = Color.White, fontSize = 10.sp) }
}

@Composable
fun PosterCard(
    item: VodItem, modifier: Modifier = Modifier, requester: FocusRequester? = null,
    onFocusChange: (Boolean) -> Unit = {}, onMenu: (() -> Unit)? = null, onClick: () -> Unit
) {
    Column(
        modifier.nexusFocus(10.dp, requester = requester, onFocusChange = onFocusChange, onMenu = onMenu, onClick = onClick).padding(5.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(7.dp)).background(NX.Card)) {
            AsyncImage(model = item.poster.ifEmpty { null }, contentDescription = item.name,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (item.rating > 0) {
                Row(
                    Modifier.align(Alignment.TopStart).padding(5.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xB3000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, null, tint = NX.Crimson, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("%.1f".format(item.rating), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(item.name, color = Color.White, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun CategoryRow(
    name: String, count: Int, icon: ImageVector?, selected: Boolean, modifier: Modifier = Modifier,
    requester: FocusRequester? = null, onFocusChange: (Boolean) -> Unit = {}, onClick: () -> Unit = {}
) {
    Row(
        modifier.fillMaxWidth()
            .nexusFocus(11.dp, selected = selected, requester = requester, onFocusChange = onFocusChange, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(icon, null, tint = if (selected) Color.White else NX.TextSec, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(9.dp)) }
        Text(name, Modifier.weight(1f), color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("($count)", color = NX.TextSec, fontSize = 11.sp)
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, requester: FocusRequester? = null) {
    val kb = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    Row(
        modifier.fillMaxWidth().nexusFocusGroup(22.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, null, tint = NX.TextSec, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = NX.TextSec, fontSize = 12.sp)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 12.sp), cursorBrush = SolidColor(NX.Crimson),
                modifier = Modifier.fillMaxWidth().let { if (requester != null) it.focusRequester(requester) else it }
                    .onPreviewKeyEvent { if (it.type == KeyEventType.KeyUp && (it.key == Key.DirectionCenter || it.key == Key.Enter)) { kb?.show(); true } else false }
            )
        }
    }
}

@Composable
fun ClockBar(vm: AppVm, modifier: Modifier = Modifier) {
    val now by produceState(Date()) { while (true) { value = Date(); delay(15000) } }
    val fmt = if (vm.prefs.use24h) "HH:mm" else "hh:mm a"
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(SimpleDateFormat(fmt, Locale.getDefault()).format(now), color = NX.TextSec, fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Outlined.Wifi, null, tint = NX.TextSec, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun Loading(error: String? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (error != null) Text(error, color = NX.Crimson, fontSize = 14.sp)
        else CircularProgressIndicator(color = NX.Crimson, modifier = Modifier.size(34.dp))
    }
}

// ================= DIÁLOGOS =================
@Composable
fun NxDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier.width(380.dp).clip(shape).background(Color(0xF2121216)).border(1.dp, NX.Crimson.copy(alpha = 0.5f), shape).padding(20.dp)
        ) {
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun ChoiceDialog(title: String, options: List<String>, selected: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val req = remember { FocusRequester() }
    NxDialog(title, onDismiss) {
        options.forEachIndexed { i, o ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    .nexusFocus(10.dp, selected = i == selected, requester = if (i == maxOf(selected, 0)) req else null, onClick = { onPick(i) })
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(o, Modifier.weight(1f), color = Color.White, fontSize = 13.sp)
                if (i == selected) Icon(Icons.Filled.Check, null, tint = NX.Crimson, modifier = Modifier.size(16.dp))
            }
        }
    }
    LaunchedEffect(Unit) { req.focusSoon(200) }
}

@Composable
fun InfoDialog(title: String, lines: List<String>, onDismiss: () -> Unit) {
    val req = remember { FocusRequester() }
    NxDialog(title, onDismiss) {
        lines.forEach { Text(it, color = NX.TextSec, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp)) }
        Spacer(Modifier.height(12.dp))
        NxButton("OK", null, true, requester = req, onClick = onDismiss)
    }
    LaunchedEffect(Unit) { req.focusSoon(200) }
}

@Composable
fun PinDialog(title: String, onPin: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    val req = remember { FocusRequester() }
    NxDialog(title, onDismiss) {
        Row(Modifier.fillMaxWidth().nexusFocusGroup(10.dp).padding(12.dp)) {
            BasicTextField(
                pin, { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it }, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp), cursorBrush = SolidColor(NX.Crimson),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().focusRequester(req)
            )
        }
        Spacer(Modifier.height(12.dp))
        NxButton("CONFIRMAR", null, true) { onPin(pin) }
    }
    LaunchedEffect(Unit) { req.focusSoon(250) }
}
