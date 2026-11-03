package com.nexus.iptv

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class SItem(val id: String, val label: String, val icon: ImageVector)

private val ITEMS = listOf(
    SItem("general", "Configurações Gerais", Icons.Outlined.Settings), SItem("epg", "EPG", Icons.Outlined.CalendarMonth),
    SItem("stream", "Formato de Fluxo", Icons.Outlined.Shuffle), SItem("time", "Formato da hora", Icons.Outlined.Schedule),
    SItem("auto", "Automação", Icons.Outlined.SettingsSuggest), SItem("player", "Seleção do Mediaplayer", Icons.Outlined.SmartDisplay),
    SItem("playercfg", "Configurações do Mediaplayer", Icons.Outlined.DisplaySettings), SItem("parental", "Controle dos pais", Icons.Outlined.Security),
    SItem("external", "Mediaplayer Externos", Icons.Outlined.OpenInNew), SItem("multi", "MULTI-SCREEN", Icons.Outlined.Devices),
    SItem("speed", "Teste de velocidade", Icons.Outlined.Speed), SItem("vpn", "VPN", Icons.Outlined.VpnKey),
    SItem("switch", "Switch Device Mode", Icons.Outlined.PhoneAndroid), SItem("account", "Informações da Conta", Icons.Outlined.Badge)
)

@Composable
fun SettingsScreen(vm: AppVm) {
    val ctx = LocalContext.current
    val p = vm.prefs
    var dialog by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) } // força recomposição após mudar prefs
    val firstReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstReq.focusSoon(300) }
    val now by produceState(Date()) { while (true) { value = Date(); delay(15000) } }
    val close: () -> Unit = { dialog = null; tick++ }

    NexusBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NxIconButton(Icons.AutoMirrored.Filled.ArrowBack) { vm.back() }
                Spacer(Modifier.width(12.dp))
                Text("NEXUS", color = NX.Crimson, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(" IPTV", color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.width(18.dp))
                Text("CONFIGURAÇÕES", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(SimpleDateFormat("MMMM dd, yyyy", Locale("pt", "BR")).format(now), color = NX.TextSec, fontSize = 11.sp)
                    Text(SimpleDateFormat(if (p.use24h) "HH:mm" else "hh:mm a", Locale.getDefault()).format(now), color = Color.White, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.weight(1f)) {
                LazyVerticalGrid(GridCells.Fixed(5), contentPadding = PaddingValues(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ITEMS) { it ->
                        Column(
                            Modifier.fillMaxWidth().height(86.dp)
                                .nexusFocus(12.dp, NX.Card, requester = if (it.id == "general") firstReq else null) {
                                    if (it.id == "vpn") {
                                        runCatching { ctx.startActivity(Intent(Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                    } else {
                                        dialog = it.id
                                    }
                                }.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                        ) {
                            Icon(it.icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(it.label, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 13.sp, maxLines = 2)
                        }
                    }
                }
                Row(
                    Modifier.align(Alignment.BottomEnd).nexusFocus(10.dp, onClick = {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/5511977888609")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SUPORTE", color = NX.Crimson, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("5511977888609", color = Color.White, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.Chat, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    key(tick) {
        when (dialog) {
            "general" -> ChoiceDialog("Configurações Gerais", listOf("Atualizar listas (limpar cache)", "Limpar favoritos e Minha Lista", "Nexus IPTV v1.0.0"), -1,
                { i -> if (i == 0) vm.refreshAll(); if (i == 1) vm.clearFavorites(); close() }, close)
            "epg" -> ChoiceDialog("EPG (guia de programação)", listOf("Ativado", "Desativado"), if (p.showEpg) 0 else 1, { p.showEpg = it == 0; close() }, close)
            "stream" -> ChoiceDialog("Formato de Fluxo", listOf("HLS (.m3u8)", "MPEG-TS (.ts)"), if (p.streamFormat == "m3u8") 0 else 1,
                { p.streamFormat = if (it == 0) "m3u8" else "ts"; close() }, close)
            "time" -> ChoiceDialog("Formato da hora", listOf("24 horas", "12 horas (AM/PM)"), if (p.use24h) 0 else 1, { p.use24h = it == 0; close() }, close)
            "auto" -> ChoiceDialog("Login automático ao abrir", listOf("Ativado", "Desativado"), if (p.autoLogin) 0 else 1, { p.autoLogin = it == 0; close() }, close)
            "player" -> ChoiceDialog("Seleção do Mediaplayer", listOf("Media3 ExoPlayer (interno)", "Player externo"), if (p.externalPlayer) 1 else 0,
                { p.externalPlayer = it == 1; close() }, close)
            "playercfg" -> ChoiceDialog("Buffer do Mediaplayer", listOf("Baixo (zapping rápido)", "Normal", "Alto (conexão instável)"), p.bufferMode, { p.bufferMode = it; close() }, close)
            "external" -> ChoiceDialog("Mediaplayer Externo", listOf("Perguntar sempre", "VLC", "MX Player"),
                when (p.externalPkg) { "org.videolan.vlc" -> 1; "com.mxtech.videoplayer.ad" -> 2; else -> 0 },
                { p.externalPkg = listOf("", "org.videolan.vlc", "com.mxtech.videoplayer.ad")[it]; close() }, close)
            "parental" ->
                if (p.parentalPin.isEmpty()) PinDialog("Defina um PIN de 4 dígitos para bloquear conteúdo adulto", { pin ->
                    if (pin.length == 4) { p.parentalPin = pin; vm.refreshAll() }; close()
                }, close)
                else PinDialog("Digite o PIN para desativar o bloqueio", { pin ->
                    if (pin == p.parentalPin) { p.parentalPin = ""; vm.refreshAll() }; close()
                }, close)
            "multi" -> InfoDialog("MULTI-SCREEN", listOf("Recurso ainda não implementado nesta versão."), close)
            "speed" -> SpeedDialog(close)
            "switch" -> ChoiceDialog("Switch Device Mode", listOf("Trocar de conta / sair"), -1, { vm.logout(); close() }, close)
            "account" -> {
                val a = vm.accountInfo
                InfoDialog("Informações da Conta", listOf(
                    "Servidor: ${p.host.ifEmpty { "-" }}", "Usuário: ${p.user.ifEmpty { "-" }}", "Status: ${a?.status ?: "-"}",
                    "Expira em: ${a?.expDate?.takeIf { it > 0 }?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it * 1000)) } ?: "Ilimitado"}",
                    "Conexões: ${a?.activeConn ?: "-"} / ${a?.maxConn ?: "-"}", "Teste: ${if (a?.trial == true) "Sim" else "Não"}"
                ), close)
            }
        }
    }
}

@Composable
private fun SpeedDialog(onDismiss: () -> Unit) {
    var result by remember { mutableStateOf("Testando…") }
    LaunchedEffect(Unit) {
        result = withContext(Dispatchers.IO) {
            runCatching {
                val t0 = System.nanoTime()
                val c = URL("https://speed.cloudflare.com/__down?bytes=15000000").openConnection() as HttpURLConnection
                var total = 0L
                c.inputStream.use { s -> val buf = ByteArray(65536); while (true) { val n = s.read(buf); if (n < 0) break; total += n } }
                "%.1f Mbps".format(total * 8 / 1e6 / ((System.nanoTime() - t0) / 1e9))
            }.getOrElse { "Falha no teste (sem internet?)" }
        }
    }
    InfoDialog("Teste de velocidade", listOf(result), onDismiss)
}
