package com.nexus.iptv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private data class NavItem(val label: String, val icon: ImageVector, val screen: Screen?)

private const val HERO_COUNT = 6
private const val HERO_INTERVAL_MS = 6500L

@Composable
fun HomeScreen(vm: AppVm) {
    LaunchedEffect(Unit) { vm.ensureMovies(); vm.ensureSeries() }

    // ---- 6 destaques: mistura de filmes e séries recentes/bem avaliados ----
    val heroes = remember(vm.movies, vm.seriesList) {
        val m = vm.movies.filter { it.poster.isNotEmpty() && it.rating >= 6.0 }.sortedByDescending { it.added }
        val s = vm.seriesList.filter { it.poster.isNotEmpty() && it.rating >= 6.0 }.sortedByDescending { it.added }
        val picked = listOfNotNull(m.getOrNull(0), s.getOrNull(0), m.getOrNull(1), s.getOrNull(1), m.getOrNull(2), m.getOrNull(3)).toMutableList()
        if (picked.size < HERO_COUNT) {
            val fill = (m.drop(4) + s.drop(2) + vm.movies + vm.seriesList)
                .filter { it.poster.isNotEmpty() }
                .distinctBy { "${it.isSeries}-${it.id}" }
                .filter { x -> picked.none { p -> p.id == x.id && p.isSeries == x.isSeries } }
            picked.addAll(fill.take(HERO_COUNT - picked.size))
        }
        picked.take(HERO_COUNT)
    }

    // ---- auto-rotação ----
    var index by remember { mutableIntStateOf(0) }
    var heroFocus by remember { mutableIntStateOf(0) }       // nº de botões do herói com foco
    val paused = heroFocus > 0                                  // foco nos botões = pausa
    LaunchedEffect(heroes.size) { if (index >= heroes.size) index = 0 }
    // Reinicia a contagem sempre que: muda o slide, pausa/retoma ou a lista muda.
    // Ao sair dos botões, o slide atual fica por um intervalo completo antes de avançar.
    LaunchedEffect(heroes.size, paused, index) {
        if (heroes.size > 1 && !paused) {
            delay(HERO_INTERVAL_MS)
            index = (index + 1) % heroes.size
        }
    }
    val current = heroes.getOrNull(index)

    val trending = remember(vm.movies) { vm.movies.sortedByDescending { it.added }.take(25) }
    val topRated = remember(vm.movies) { vm.movies.sortedByDescending { it.rating }.take(25) }
    val newSeries = remember(vm.seriesList) { vm.seriesList.sortedByDescending { it.added }.take(25) }

    // Foco inicial no item "Home" da sidebar: assim o banner roda sozinho ao abrir o app.
    // Quando o usuário vai para a direita e foca em Play/More Info, a rotação pausa.
    val homeReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { homeReq.focusSoon(300) }

    val nav = listOf(
        NavItem("Search", Icons.Outlined.Search, Screen.Search), NavItem("Home", Icons.Outlined.Home, null),
        NavItem("Live TV", Icons.Outlined.LiveTv, Screen.Live), NavItem("Movies", Icons.Outlined.Movie, Screen.Movies),
        NavItem("Series", Icons.Outlined.Tv, Screen.Series), NavItem("My List", Icons.Outlined.Add, Screen.MyList),
        NavItem("Settings", Icons.Outlined.Settings, Screen.Settings)
    )

    Box(Modifier.fillMaxSize().background(NX.Black)) {
        // Fundo: fade suave entre os destaques
        Crossfade(targetState = current, animationSpec = tween(900), label = "heroBg", modifier = Modifier.fillMaxSize()) { slide ->
            if (slide != null) HeroBackdrop(vm, slide)
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(NX.Black, NX.Black.copy(alpha = 0.7f), Color.Transparent))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, NX.Black), startY = 300f)))

        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(170.dp).fillMaxHeight().background(Color(0xE60B0B0E)).padding(horizontal = 10.dp, vertical = 14.dp)) {
                Row(Modifier.padding(start = 6.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("NEXUS", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(" IPTV", color = NX.TextSec, fontSize = 17.sp)
                }
                nav.forEach { n ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            .nexusFocus(10.dp, selected = n.screen == null, requester = if (n.screen == null) homeReq else null) { n.screen?.let { vm.go(it) } }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(n.icon, null, tint = NX.TextSec, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(n.label, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                LazyColumn(contentPadding = PaddingValues(start = 20.dp, top = 8.dp, bottom = 16.dp)) {
                    item {
                        Column(Modifier.fillMaxWidth().height(300.dp), verticalArrangement = Arrangement.Bottom) {
                            // Bloco de informações com altura fixa => os botões nunca "pulam" entre slides
                            AnimatedContent(
                                targetState = current,
                                transitionSpec = {
                                    (fadeIn(tween(600)) + slideInHorizontally(tween(600)) { it / 8 }) togetherWith fadeOut(tween(400))
                                },
                                contentAlignment = Alignment.BottomStart,
                                label = "heroInfo",
                                modifier = Modifier.width(430.dp).height(150.dp)
                            ) { slide -> HeroInfo(vm, slide) }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                val onHeroFocus: (Boolean) -> Unit = { f -> heroFocus = if (f) heroFocus + 1 else (heroFocus - 1).coerceAtLeast(0) }
                                NxButton("Play", Icons.Filled.PlayArrow, true, onFocusChange = onHeroFocus) {
                                    current?.let { if (it.isSeries) vm.push(Screen.Detail(it)) else vm.playVod(it) }
                                }
                                NxButton("More Info", Icons.Outlined.Info, false, onFocusChange = onHeroFocus) {
                                    current?.let { vm.push(Screen.Detail(it)) }
                                }
                                Spacer(Modifier.width(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    for (i in heroes.indices) {
                                        Box(
                                            Modifier.size(width = if (i == index) 16.dp else 6.dp, height = 6.dp)
                                                .background(if (i == index) NX.Crimson else Color(0x66FFFFFF), RoundedCornerShape(3.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { PosterRow("Trending Now", trending, vm) }
                    item { PosterRow("Top Rated", topRated, vm) }
                    item { PosterRow("New Series", newSeries, vm) }
                    item { PosterRow("My List", vm.myList.toList(), vm) }
                }
                ClockBar(vm, Modifier.align(Alignment.TopEnd).padding(14.dp))
                if (vm.movies.isEmpty() && vm.movieLoading) Box(Modifier.align(Alignment.Center)) { Loading(vm.loadError) }
            }
        }
    }
}

@Composable
private fun HeroBackdrop(vm: AppVm, hero: VodItem) {
    val d by produceState<VodItem?>(null, hero) { value = runCatching { vm.vodDetail(hero) }.getOrNull() }
    val img = (d ?: hero).let { x -> x.backdrop.ifEmpty { x.poster } }
    if (img.isNotEmpty()) AsyncImage(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.55f)
}

@Composable
private fun HeroInfo(vm: AppVm, hero: VodItem?) {
    if (hero == null) {
        Text("NEXUS IPTV", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        return
    }
    val d by produceState<VodItem?>(null, hero) { value = runCatching { vm.vodDetail(hero) }.getOrNull() }
    val h = d ?: hero
    Column(verticalArrangement = Arrangement.Bottom) {
        Text(h.name.uppercase(), color = Color.White, fontSize = 28.sp, lineHeight = 31.sp, fontWeight = FontWeight.Black,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (Regex("4k|uhd|2160", RegexOption.IGNORE_CASE).containsMatchIn(h.name)) {
                Chip("4K UHD"); Chip("Dolby Vision"); Chip("Dolby Atmos", true)
            } else Chip("HD")
            Chip(if (h.isSeries) "SÉRIE" else "FILME")
        }
        Spacer(Modifier.height(6.dp))
        val meta = listOfNotNull(h.year.ifEmpty { null }, h.rating.takeIf { it > 0 }?.let { "★ %.1f".format(it) }, h.genre.ifEmpty { null })
        Text(meta.joinToString("  •  "), color = NX.TextSec, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(h.plot, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PosterRow(title: String, list: List<VodItem>, vm: AppVm) {
    if (list.isEmpty()) return
    Column {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
        LazyRow(contentPadding = PaddingValues(end = 20.dp, top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list) { v ->
                PosterCard(v, Modifier.width(104.dp), onMenu = { vm.toggleMyList(v) }, onClick = { vm.push(Screen.Detail(v)) })
            }
        }
    }
}
