@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.nexus.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun catIcon(c: Category): ImageVector {
    val n = c.name.lowercase()
    return when {
        c.id == "fav" -> Icons.Outlined.Star
        c.id == "all" -> Icons.Outlined.GridView
        "esporte" in n || "sport" in n || "futebol" in n -> Icons.Outlined.SportsSoccer
        "filme" in n || "movie" in n || "cine" in n -> Icons.Outlined.Movie
        "infantil" in n || "kids" in n || "desenho" in n -> Icons.Outlined.ChildCare
        else -> Icons.Outlined.Tv
    }
}

fun fmtClock(sec: Long, h24: Boolean): String =
    SimpleDateFormat(if (h24) "HH:mm" else "hh:mm a", Locale.getDefault()).format(Date(sec * 1000))

@Composable
fun LiveScreen(vm: AppVm) {
    LaunchedEffect(Unit) { vm.ensureLive() }
    val ctx = LocalContext.current
    val cats = remember(vm.liveCats, vm.channels, vm.favLive.size) {
        listOf(Category("fav", "Favoritos", vm.favLive.size), Category("all", "Todos os canais", vm.channels.size)) + vm.liveCats
    }
    val catIdx = vm.liveCatIdx.coerceIn(0, cats.size - 1)
    val cat = cats[catIdx]
    val list = remember(cat.id, vm.channels, vm.favLive.size) {
        when (cat.id) {
            "fav" -> vm.channels.filter { it.id in vm.favLive }
            "all" -> vm.channels
            else -> vm.channels.filter { it.catId == cat.id }
        }
    }
    val selReq = remember { FocusRequester() }
    val restoreReq = remember { FocusRequester() }
    val restoreId = remember { vm.liveFocusId }
    var restored by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { if (restoreId == null) selReq.focusSoon(250) }
    LaunchedEffect(list.size) {
        if (restoreId != null && !restored && list.isNotEmpty()) {
            restored = true
            val idx = list.indexOfFirst { it.id == restoreId }
            if (idx >= 0) { listState.scrollToItem(idx); restoreReq.focusSoon(200) } else selReq.focusSoon(100)
        }
    }

    // Player de prévia
    val player = remember { buildPlayer(ctx, vm.prefs.bufferMode) }
    DisposableEffect(Unit) { onDispose { player.release() } }
    val previewCh = vm.livePreviewId?.let { id -> vm.channels.firstOrNull { it.id == id } }
    LaunchedEffect(previewCh?.id) {
        previewCh?.let { player.setMediaItem(MediaItem.fromUri(vm.liveUrl(it))); player.prepare(); player.playWhenReady = true }
    }
    val focusedCh = vm.liveFocusId?.let { id -> vm.channels.firstOrNull { it.id == id } } ?: previewCh

    NexusBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("NEXUS", color = NX.Crimson, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(" IPTV", color = Color.White, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                ClockBar(vm)
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Coluna 1: categorias
                Panel(Modifier.width(190.dp)) {
                    LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        itemsIndexed(cats) { i, c ->
                            CategoryRow(c.name, c.count, catIcon(c), selected = i == catIdx,
                                requester = if (i == catIdx) selReq else null,
                                onFocusChange = { if (it) vm.liveCatIdx = i })
                        }
                    }
                }
                // Coluna 2: canais
                Panel(Modifier.weight(1f)) {
                    Column {
                        Text("${cat.name.uppercase()} (${list.size})", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 4.dp))
                        if (list.isEmpty()) Loading(if (vm.liveLoading) null else vm.loadError ?: "Nenhum canal")
                        else LazyColumn(state = listState, contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            itemsIndexed(list) { i, ch ->
                                LaunchedEffect(ch.id) { if (vm.prefs.showEpg) vm.epgFor(ch.id) }
                                ChannelRow(
                                    ch, vm.epg[ch.id], vm.prefs.showEpg, ch.id in vm.favLive, ch.id == vm.livePreviewId,
                                    Modifier.focusProperties { left = selReq },
                                    if (ch.id == restoreId) restoreReq else null,
                                    onFocus = { vm.liveFocusId = ch.id },
                                    onMenu = { vm.toggleFavLive(ch.id) },
                                    onClick = { if (vm.livePreviewId == ch.id) vm.playLive(list, i) else vm.livePreviewId = ch.id }
                                )
                            }
                        }
                    }
                }
                // Coluna 3: prévia + informações
                Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Panel(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                        AndroidView(
                            factory = { c -> PlayerView(c).apply { useController = false; this.player = player; isFocusable = false } },
                            modifier = Modifier.fillMaxSize()
                        )
                        if (previewCh != null) Box(Modifier.padding(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xAA000000)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("● AO VIVO", color = NX.Crimson, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(if (previewCh != null && previewCh.id == focusedCh?.id) "OK = tela cheia" else "OK = pré-visualizar",
                            color = Color.White, fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xAA000000)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Panel(Modifier.fillMaxWidth().weight(1f)) { InfoPanel(vm, focusedCh) }
                }
            }
            Text("Amarelo / Menu: Favoritar   •   OK: Pré-visualizar / Tela cheia   •   Voltar: Início",
                color = NX.TextSec, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
        }
    }
}

@Composable
private fun Panel(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    val s = RoundedCornerShape(14.dp)
    Box(modifier.clip(s).background(Color(0xE6101013)).border(1.dp, NX.Border, s), content = content)
}

@Composable
private fun ChannelRow(
    ch: Channel, epg: List<EpgEntry>?, showEpg: Boolean, fav: Boolean, playing: Boolean, modifier: Modifier,
    requester: FocusRequester?, onFocus: () -> Unit, onMenu: () -> Unit, onClick: () -> Unit
) {
    val now = System.currentTimeMillis() / 1000
    val cur = epg?.firstOrNull { now in it.start until it.end } ?: epg?.firstOrNull()
    Row(
        modifier.fillMaxWidth()
            .nexusFocus(10.dp, selected = playing, requester = requester, onFocusChange = { if (it) onFocus() }, onMenu = onMenu, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(ch.num.toString(), color = NX.TextSec, fontSize = 11.sp, modifier = Modifier.width(30.dp))
        AsyncImage(ch.icon.ifEmpty { null }, null, Modifier.size(30.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(ch.name, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (showEpg && cur != null) {
                Text(cur.title, color = NX.TextSec, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val frac = ((now - cur.start).toFloat() / (cur.end - cur.start).coerceAtLeast(1)).coerceIn(0f, 1f)
                Box(Modifier.padding(top = 3.dp).fillMaxWidth().height(2.dp).background(Color(0x33FFFFFF))) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(NX.Crimson))
                }
            }
        }
        if (fav) Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
        if (playing) Text("  AO VIVO", color = NX.Crimson, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoPanel(vm: AppVm, ch: Channel?) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        if (ch == null) { Text("Selecione um canal", color = NX.TextSec, fontSize = 12.sp); return }
        val epg = vm.epg[ch.id].orEmpty()
        val now = System.currentTimeMillis() / 1000
        val cur = epg.firstOrNull { now in it.start until it.end } ?: epg.firstOrNull()
        val next = epg.filter { it.start >= (cur?.end ?: Long.MAX_VALUE) }.take(2)
        val h24 = vm.prefs.use24h
        Text("${ch.num}  ${ch.name}", color = NX.TextSec, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(cur?.title?.uppercase() ?: "SEM INFORMAÇÕES DE EPG", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (cur != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Text(fmtClock(cur.start, h24), color = NX.TextSec, fontSize = 10.sp)
                val frac = ((now - cur.start).toFloat() / (cur.end - cur.start).coerceAtLeast(1)).coerceIn(0f, 1f)
                Box(Modifier.weight(1f).padding(horizontal = 6.dp).height(3.dp).background(Color(0x33FFFFFF))) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(NX.Crimson))
                }
                Text(fmtClock(cur.end, h24), color = NX.TextSec, fontSize = 10.sp)
            }
            Text(cur.desc, color = Color(0xFFD0D0D6), fontSize = 11.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
        if (next.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("A SEGUIR", color = NX.TextSec, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            next.forEach { Text("${fmtClock(it.start, h24)} - ${it.title}", color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}
