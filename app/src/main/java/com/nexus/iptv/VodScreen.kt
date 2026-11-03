@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.nexus.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun PosterGrid(
    vm: AppVm, items: List<VodItem>, modifier: Modifier = Modifier, restoreId: Int? = null,
    leftReq: FocusRequester? = null, columns: Int = 5, onItemFocus: (VodItem) -> Unit = {}
) {
    val startIdx = remember { restoreId?.let { id -> items.indexOfFirst { it.id == id }.coerceAtLeast(0) } ?: 0 }
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = startIdx)
    val restore = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (restoreId != null) restore.focusSoon(300) }
    LazyVerticalGrid(
        GridCells.Fixed(columns), modifier, state,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items) { index, item ->
            PosterCard(
                item,
                if (leftReq != null && index % columns == 0) Modifier.focusProperties { left = leftReq } else Modifier,
                requester = if (restoreId != null && item.id == restoreId) restore else null,
                onFocusChange = { if (it) onItemFocus(item) },
                onMenu = { vm.toggleMyList(item) },
                onClick = { vm.push(Screen.Detail(item)) }
            )
        }
    }
}

@Composable
fun VodScreen(vm: AppVm, series: Boolean) {
    LaunchedEffect(series) { if (series) vm.ensureSeries() else vm.ensureMovies() }
    val all = if (series) vm.seriesList else vm.movies
    val rawCats = if (series) vm.seriesCats else vm.movieCats
    val loading = if (series) vm.seriesLoading else vm.movieLoading
    val cats = remember(rawCats, all) { listOf(Category("all", "Todos", all.size)) + rawCats }
    var query by remember { mutableStateOf("") }
    val shown = remember(cats, query) { if (query.isBlank()) cats else cats.filter { it.name.contains(query, true) } }
    var catId by remember { mutableStateOf(vm.vodCat[series] ?: "all") }
    val shownItems = remember(catId, all) { if (catId == "all") all else all.filter { it.catId == catId } }
    val selReq = remember { FocusRequester() }
    val restoreId = remember { vm.vodLast[series] }
    LaunchedEffect(Unit) { if (restoreId == null) selReq.focusSoon(300) }
    val selVisible = shown.any { it.id == catId }
    val cur = cats.firstOrNull { it.id == catId }

    NexusBackground {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(200.dp).fillMaxHeight().background(Color(0xF00B0B0E)).padding(12.dp)) {
                Row(Modifier.padding(start = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("NEXUS", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(" IPTV", color = NX.Crimson, fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
                SearchField(query, { query = it }, "Pesquisa em categorias")
                Spacer(Modifier.height(10.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                    items(shown) { c ->
                        CategoryRow(c.name, c.count, null, selected = c.id == catId,
                            requester = if (c.id == catId) selReq else null,
                            onFocusChange = { if (it) { catId = c.id; vm.vodCat[series] = c.id } })
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp), verticalAlignment = Alignment.Bottom) {
                    Text(if (series) "SÉRIES" else (cur?.name ?: "Filmes"), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Text("${if (series) (cur?.name ?: "") + " • " else ""}${shownItems.size} títulos", color = NX.TextSec, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
                    Spacer(Modifier.weight(1f))
                    ClockBar(vm)
                }
                if (shownItems.isEmpty()) Loading(if (loading) null else vm.loadError ?: "Nada por aqui")
                else key(catId) {
                    PosterGrid(vm, shownItems, Modifier.fillMaxSize(), restoreId = if (restoreId != null && catId == vm.vodCat[series]) restoreId else null,
                        leftReq = if (selVisible) selReq else null, onItemFocus = { vm.vodLast[series] = it.id })
                }
            }
        }
    }
}

@Composable
fun SearchScreen(vm: AppVm) {
    LaunchedEffect(Unit) { vm.ensureMovies(); vm.ensureSeries() }
    var q by remember { mutableStateOf("") }
    val results = remember(q, vm.movies, vm.seriesList) { vm.search(q) }
    val req = remember { FocusRequester() }
    LaunchedEffect(Unit) { req.focusSoon(300) }
    NexusBackground {
        Column(Modifier.fillMaxSize().padding(top = 16.dp)) {
            Box(Modifier.padding(horizontal = 24.dp)) { SearchField(q, { q = it }, "Buscar filmes e séries (OK abre o teclado)", requester = req) }
            if (results.isEmpty()) Loading(if (q.isBlank()) "Digite para buscar" else "Sem resultados")
            else PosterGrid(vm, results, Modifier.fillMaxSize(), columns = 6)
        }
    }
}

@Composable
fun MyListScreen(vm: AppVm) {
    NexusBackground {
        Column(Modifier.fillMaxSize().padding(top = 16.dp)) {
            Text("Minha Lista", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
            Text("Menu / Amarelo remove da lista", color = NX.TextSec, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 24.dp))
            if (vm.myList.isEmpty()) Loading("Sua lista está vazia. Use \"+ Minha Lista\" nos detalhes ou a tecla Menu em um pôster.")
            else PosterGrid(vm, vm.myList.toList(), Modifier.fillMaxSize(), columns = 6)
        }
    }
}

@Composable
fun DetailScreen(vm: AppVm, item0: VodItem) {
    val loaded by produceState<Pair<VodItem, Map<Int, List<Episode>>>?>(null, item0) {
        value = runCatching { vm.detail(item0) }.getOrNull() ?: (item0 to emptyMap<Int, List<Episode>>())
    }
    val item = loaded?.first ?: item0
    val episodes = loaded?.second ?: emptyMap()
    var season by remember(episodes) { mutableIntStateOf(episodes.keys.firstOrNull() ?: 1) }
    val playReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { playReq.focusSoon(300) }
    val inList = vm.myList.any { it.id == item.id && it.isSeries == item.isSeries }

    Box(Modifier.fillMaxSize().background(NX.Black)) {
        val img = item.backdrop.ifEmpty { item.poster }
        if (img.isNotEmpty()) AsyncImage(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.35f)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, NX.Black), startY = 200f)))
        LazyColumn(contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    AsyncImage(item.poster.ifEmpty { null }, null, Modifier.width(130.dp).aspectRatio(2f / 3f).background(NX.Card, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop)
                    Column(Modifier.weight(1f)) {
                        Text(item.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (item.rating > 0) Chip("★ %.1f".format(item.rating), true)
                            if (item.year.isNotEmpty()) Chip(item.year)
                            if (item.genre.isNotEmpty()) Chip(item.genre)
                            Chip(if (item.isSeries) "SÉRIE" else "FILME")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(item.plot.ifEmpty { "Sem sinopse disponível." }, color = Color(0xFFD0D0D6), fontSize = 12.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!item.isSeries) NxButton("Assistir", Icons.Filled.PlayArrow, true, requester = playReq) { vm.playVod(item) }
                            else NxButton("Assistir T$season E1", Icons.Filled.PlayArrow, true, requester = playReq) {
                                episodes[season]?.firstOrNull()?.let { vm.playEpisode(item, it) }
                            }
                            NxButton(if (inList) "✓ Na Minha Lista" else "+ Minha Lista", null, false) { vm.toggleMyList(item) }
                            NxButton("Voltar", Icons.AutoMirrored.Filled.ArrowBack, false) { vm.back() }
                        }
                    }
                }
            }
            if (item.isSeries && episodes.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)) {
                        items(episodes.keys.toList()) { s ->
                            Box(Modifier.nexusFocus(8.dp, selected = s == season, onFocusChange = { if (it) season = s }, onClick = { season = s })
                                .padding(horizontal = 14.dp, vertical = 7.dp)) { Text("Temporada $s", color = Color.White, fontSize = 12.sp) }
                        }
                    }
                }
                items(episodes[season].orEmpty()) { ep ->
                    Row(Modifier.fillMaxWidth().nexusFocus(10.dp, onClick = { vm.playEpisode(item, ep) }).padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("E${ep.num}", color = NX.Crimson, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(36.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ep.title, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (ep.plot.isNotEmpty()) Text(ep.plot, color = NX.TextSec, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
