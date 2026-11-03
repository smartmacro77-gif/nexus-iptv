@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.nexus.iptv

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

fun buildPlayer(ctx: Context, bufferMode: Int): ExoPlayer {
    val ms = when (bufferMode) { 0 -> 15_000; 2 -> 60_000; else -> 30_000 }
    val load = DefaultLoadControl.Builder().setBufferDurationsMs(ms / 2, ms, 1500, 3000).build()
    val http = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android 11; TV) NexusIPTV")
        .setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(15000).setReadTimeoutMs(15000)
    return ExoPlayer.Builder(ctx).setLoadControl(load)
        .setMediaSourceFactory(DefaultMediaSourceFactory(ctx).setDataSourceFactory(http)).build()
}

private fun fmt(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60) else "%02d:%02d".format(s / 60, s % 60)
}

@Composable
fun PlayerScreen(vm: AppVm) {
    val np = vm.nowPlaying
    if (np == null) { LaunchedEffect(Unit) { vm.back() }; return }
    val ctx = LocalContext.current
    val player = remember { buildPlayer(ctx, vm.prefs.bufferMode) }
    var error by remember { mutableStateOf<String?>(null) }
    var buffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var overlayUntil by remember { mutableLongStateOf(0L) }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val focus = remember { FocusRequester() }
    fun show() { overlayUntil = System.currentTimeMillis() + 4000 }

    DisposableEffect(Unit) {
        val l = object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) { error = "Falha na reprodução (${e.errorCodeName})" }
            override fun onPlaybackStateChanged(state: Int) { buffering = state == Player.STATE_BUFFERING; if (state == Player.STATE_READY) error = null }
            override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
        }
        player.addListener(l)
        onDispose {
            val cur = vm.nowPlaying
            if (cur != null && !cur.live && cur.resumeKey != null) {
                val d = player.duration
                vm.prefs.setPos(cur.resumeKey, if (d > 0 && player.currentPosition > d - 30_000) 0L else player.currentPosition)
            }
            player.removeListener(l); player.release()
        }
    }

    LaunchedEffect(np.url) {
        error = null
        player.setMediaItem(MediaItem.fromUri(np.url))
        val resume = np.resumeKey?.let { vm.prefs.getPos(it) } ?: 0L
        if (resume > 0) player.seekTo(resume)
        player.prepare(); player.playWhenReady = true
        show()
    }
    LaunchedEffect(Unit) { focus.focusSoon(100) }
    LaunchedEffect(Unit) {
        while (true) {
            pos = player.currentPosition
            dur = if (player.duration > 0) player.duration else 0L
            tick = System.currentTimeMillis()
            delay(500)
        }
    }
    // Tentativa automática de reconexão em canais ao vivo
    LaunchedEffect(error) { if (error != null && np.live) { delay(4000); player.prepare() } }

    val visible = tick < overlayUntil || !isPlaying || error != null

    Box(
        Modifier.fillMaxSize().background(Color.Black).focusRequester(focus)
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (ev.key) {
                    Key.DirectionCenter, Key.Enter, Key.MediaPlayPause -> { if (player.isPlaying) player.pause() else player.play(); show(); true }
                    Key.MediaPlay -> { player.play(); show(); true }
                    Key.MediaPause -> { player.pause(); show(); true }
                    Key.DirectionLeft, Key.MediaRewind -> { if (!np.live) player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)); show(); true }
                    Key.DirectionRight, Key.MediaFastForward -> { if (!np.live) player.seekTo(player.currentPosition + 10_000); show(); true }
                    Key.DirectionUp, Key.ChannelUp -> { if (np.live) vm.zap(-1); show(); true }
                    Key.DirectionDown, Key.ChannelDown -> { if (np.live) vm.zap(1); show(); true }
                    else -> false
                }
            }
            .focusable()
    ) {
        AndroidView(
            factory = { c -> PlayerView(c).apply { useController = false; this.player = player; isFocusable = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; setShutterBackgroundColor(android.graphics.Color.BLACK) } },
            modifier = Modifier.fillMaxSize()
        )
        if (buffering && error == null) CircularProgressIndicator(color = NX.Crimson, modifier = Modifier.align(Alignment.Center).size(40.dp))
        error?.let { Text(it, color = NX.Crimson, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center).background(Color(0xCC000000)).padding(12.dp)) }
        if (visible) {
            Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent))).padding(24.dp)) {
                Text(np.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))).padding(horizontal = 32.dp, vertical = 20.dp)) {
                if (np.live) {
                    Text("● AO VIVO   •   ↑ ↓ trocar de canal   •   OK pausar", color = Color.White, fontSize = 12.sp)
                } else {
                    Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0x44FFFFFF))) {
                        Box(Modifier.fillMaxWidth(if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f).fillMaxHeight().background(NX.Crimson))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text(fmt(pos), color = Color.White, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text("◀ ▶ ±10s   •   OK pausar/continuar", color = NX.TextSec, fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Text(fmt(dur), color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
