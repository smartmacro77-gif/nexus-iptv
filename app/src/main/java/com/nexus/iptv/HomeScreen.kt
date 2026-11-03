package com.nexus.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
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

private data class NavItem(val label: String, val icon: ImageVector, val screen: Screen?)

@Composable
fun HomeScreen(vm: AppVm) {
    LaunchedEffect(Unit) { vm.ensureMovies(); vm.ensureSeries() }
    val featured = remember(vm.movies) {
        vm.movies.filter { it.poster.isNotEmpty() && it.rating >= 6.0 }.maxByOrNull { it.added } ?: vm.movies.firstOrNull()
    }
    val detail by produceState<VodItem?>(null, featured) { value = featured?.let { runCatching { vm.vodDetail(it) }.getOrNull() } }
    val hero = detail ?: featured
    val trending = remember(vm.movies) { vm.movies.sortedByDescending { it.added }.take(25) }
    val topRated = remember(vm.movies) { vm.movies.sortedByDescending { it.rating }.take(25) }
    val newSeries = remember(vm.seriesList) { vm.seriesList.sortedByDescending { it.added }.take(25) }
    val playReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { playReq.focusSoon(300) }
    val nav = listOf(
        NavItem("Search", Icons.Outlined.Search, Screen.Search), NavItem("Home", Icons.Outlined.Home, null),
        NavItem("Live TV", Icons.Outlined.LiveTv, Screen.Live), NavItem("Movies", Icons.Outlined.Movie, Screen.Movies),
        NavItem("TV Shows", Icons.Outlined.Tv, Screen.Series), NavItem("My List", Icons.Outlined.Add, Screen.MyList),
        NavItem("Settings", Icons.Outlined.Settings, Screen.Settings)
    )

    Box(Modifier.fillMaxSize().background(NX.Black)) {
        val img = hero?.backdrop?.ifEmpty { hero.poster } ?: ""
        if (img.isNotEmpty()) AsyncImage(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.55f)
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
                            .nexusFocus(10.dp, selected = n.screen == null) { n.screen?.let { vm.go(it) } }
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
                            Text(hero?.name?.uppercase() ?: "NEXUS IPTV", color = Color.White, fontSize = 28.sp, lineHeight = 31.sp,
                                fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(420.dp))
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (hero != null && Regex("4k|uhd|2160", RegexOption.IGNORE_CASE).containsMatchIn(hero.name)) {
                                    Chip("4K UHD"); Chip("Dolby Vision"); Chip("Dolby Atmos", true)
                                } else { Chip("HD") }
                            }
                            Spacer(Modifier.height(6.dp))
                            val meta = listOfNotNull(hero?.year?.ifEmpty { null }, hero?.rating?.takeIf { it > 0 }?.let { "★ %.1f".format(it) }, hero?.genre?.ifEmpty { null })
                            Text(meta.joinToString("  •  "), color = NX.TextSec, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(hero?.plot ?: "", color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(420.dp))
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                NxButton("Play", Icons.Filled.PlayArrow, true, requester = playReq) { hero?.let { vm.playVod(it) } }
                                NxButton("More Info", Icons.Outlined.Info, false) { hero?.let { vm.push(Screen.Detail(it)) } }
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
fun PosterRow(title: String, items: List<VodItem>, vm: AppVm) {
    if (items.isEmpty()) return
    Column {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
        LazyRow(contentPadding = PaddingValues(end = 20.dp, top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { it ->
                PosterCard(it, Modifier.width(104.dp), onMenu = { vm.toggleMyList(it) }, onClick = { vm.push(Screen.Detail(it)) })
            }
        }
    }
}
