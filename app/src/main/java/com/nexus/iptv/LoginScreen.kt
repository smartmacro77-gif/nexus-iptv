package com.nexus.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Login SEM rolagem: o cartão ocupa 92% da altura visível e distribui os elementos com SpaceEvenly.
 * Tudo escala por `s` (0.5–1.0) conforme a altura real disponível (TV 16:9, celular em paisagem, teclado aberto).
 */
@Composable
fun LoginScreen(vm: AppVm, onExit: () -> Unit) {
    var host by remember { mutableStateOf(vm.prefs.host) }
    var user by remember { mutableStateOf(vm.prefs.user) }
    var pass by remember { mutableStateOf(vm.prefs.pass) }
    var save by remember { mutableStateOf(vm.prefs.rememberMe) }
    var showPass by remember { mutableStateOf(false) }
    val hostReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { hostReq.focusSoon(250) }
    val submit = { vm.login(host, user, pass, save) }

    NexusBackground {
        NxIconButton(Icons.AutoMirrored.Filled.ArrowBack, Modifier.align(Alignment.TopStart).padding(16.dp), onExit)

        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val s = (maxHeight.value / 540f).coerceIn(0.5f, 1f)
            val cardWidth = minOf(420.dp, maxWidth * 0.92f)
            val shape = RoundedCornerShape((20 * s).dp)

            Column(
                Modifier.width(cardWidth).fillMaxHeight(0.92f).heightIn(max = 520.dp)
                    .clip(shape).background(Color(0xB3141416)).border(1.dp, NX.Crimson.copy(alpha = 0.45f), shape)
                    .padding(horizontal = (22 * s).dp, vertical = (10 * s).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Entrar", color = Color.White, fontSize = (34 * s).sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Dns, null, tint = NX.TextSec, modifier = Modifier.size((16 * s).dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Xtream", color = NX.Crimson, fontSize = (14 * s).sp)
                        Text(" Codes API", color = NX.TextSec, fontSize = (14 * s).sp)
                    }
                }
                NxField("URL do Servidor (Host)", "http://exemplo.com:8080", host, { host = it }, Icons.Outlined.Language, KeyboardType.Uri, s, requester = hostReq)
                NxField("Nome de Usuário", "Usuário", user, { user = it }, Icons.Outlined.Person, KeyboardType.Text, s)
                NxField("Senha", "••••••••", pass, { pass = it }, Icons.Outlined.Lock, KeyboardType.Password, s,
                    password = !showPass, last = true, onDone = submit,
                    trailing = {
                        Box(Modifier.size((28 * s).dp).nexusFocus(14.dp, onClick = { showPass = !showPass }), contentAlignment = Alignment.Center) {
                            Icon(if (showPass) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = NX.TextSec, modifier = Modifier.size((16 * s).dp))
                        }
                    })
                Row(
                    Modifier.fillMaxWidth().nexusFocus(8.dp, onClick = { save = !save }).padding(horizontal = 8.dp, vertical = (5 * s).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(16.dp).border(1.dp, NX.TextSec, RoundedCornerShape(3.dp)).background(if (save) NX.Crimson else Color.Transparent, RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center) { if (save) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                    Spacer(Modifier.width(8.dp))
                    Text("Lembrar dados", color = Color.White, fontSize = (12 * s).sp)
                }
                // Espaço reservado para a mensagem de erro (o layout não "pula" quando ela aparece)
                Box(Modifier.fillMaxWidth().height((16 * s).dp), contentAlignment = Alignment.Center) {
                    vm.loginError?.let { Text(it, color = NX.Crimson, fontSize = (12 * s).sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Box(
                    Modifier.fillMaxWidth().height((44 * s).dp).nexusFocus(9.dp, NX.Crimson, onClick = { submit() }),
                    contentAlignment = Alignment.Center
                ) { Text(if (vm.loginBusy) "CONECTANDO..." else "CONECTAR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (15 * s).sp) }
            }
        }
    }
}

@Composable
private fun NxField(
    label: String, placeholder: String, value: String, onChange: (String) -> Unit, icon: ImageVector, type: KeyboardType, s: Float,
    requester: FocusRequester? = null, password: Boolean = false, last: Boolean = false, onDone: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null
) {
    val fm = LocalFocusManager.current
    val kb = LocalSoftwareKeyboardController.current
    Row(
        Modifier.fillMaxWidth().nexusFocusGroup(10.dp).padding(horizontal = 12.dp, vertical = (7 * s).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = NX.TextSec, modifier = Modifier.size((20 * s).dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = (12 * s).sp)
            Box {
                if (value.isEmpty()) Text(placeholder, color = NX.TextSec, fontSize = (13 * s).sp)
                BasicTextField(
                    value, onChange, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = (13 * s).sp), cursorBrush = SolidColor(NX.Crimson),
                    visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = type, imeAction = if (last) ImeAction.Done else ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { fm.moveFocus(FocusDirection.Down) }, onDone = { kb?.hide(); onDone() }),
                    modifier = Modifier.fillMaxWidth().let { if (requester != null) it.focusRequester(requester) else it }
                        .onPreviewKeyEvent { if (it.type == KeyEventType.KeyUp && (it.key == Key.DirectionCenter || it.key == Key.Enter)) { kb?.show(); true } else false }
                )
            }
        }
        trailing?.invoke()
    }
}
