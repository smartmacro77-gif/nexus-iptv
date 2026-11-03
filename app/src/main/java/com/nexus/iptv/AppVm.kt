package com.nexus.iptv

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

sealed interface Screen {
    object Login : Screen
    object Home : Screen
    object Live : Screen
    object Movies : Screen
    object Series : Screen
    object Search : Screen
    object MyList : Screen
    object Settings : Screen
    object Player : Screen
    data class Detail(val item: VodItem) : Screen
}

class AppVm(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    var api: XtreamApi? = null
        private set

    // ---- navegação ----
    val stack = mutableStateListOf<Screen>(Screen.Login)
    val screen: Screen get() = stack.last()
    fun go(s: Screen) { stack.clear(); stack.add(Screen.Home); if (s != Screen.Home) stack.add(s) }
    fun push(s: Screen) { stack.add(s) }
    fun back(): Boolean { if (stack.size > 1) { stack.removeAt(stack.lastIndex); return true }; return false }

    // ---- login ----
    var accountInfo by mutableStateOf<AccountInfo?>(null)
    var loginBusy by mutableStateOf(false)
    var loginError by mutableStateOf<String?>(null)

    // ---- dados ----
    var liveCats by mutableStateOf<List<Category>>(emptyList()); private set
    var channels by mutableStateOf<List<Channel>>(emptyList()); private set
    var movieCats by mutableStateOf<List<Category>>(emptyList()); private set
    var movies by mutableStateOf<List<VodItem>>(emptyList()); private set
    var seriesCats by mutableStateOf<List<Category>>(emptyList()); private set
    var seriesList by mutableStateOf<List<VodItem>>(emptyList()); private set
    var liveLoading by mutableStateOf(false); private set
    var movieLoading by mutableStateOf(false); private set
    var seriesLoading by mutableStateOf(false); private set
    var loadError by mutableStateOf<String?>(null)

    val favLive = mutableStateListOf<Int>().apply { addAll(prefs.favLive()) }
    val myList = mutableStateListOf<VodItem>().apply { addAll(prefs.myList()) }
    val epg = mutableStateMapOf<Int, List<EpgEntry>>()
    private val epgSem = Semaphore(4)
    private val detailCache = HashMap<String, Pair<VodItem, Map<Int, List<Episode>>>>()

    // ---- memória de UI (restaurar foco ao voltar) ----
    var liveCatIdx by mutableIntStateOf(1)
    var liveFocusId by mutableStateOf<Int?>(null)
    var livePreviewId by mutableStateOf<Int?>(null)
    val vodCat = HashMap<Boolean, String>()
    val vodLast = HashMap<Boolean, Int>()

    // ---- player ----
    var nowPlaying by mutableStateOf<NowPlaying?>(null)
    private var zapList: List<Channel> = emptyList()
    private var zapIndex = 0

    init {
        if (prefs.autoLogin && prefs.rememberMe && prefs.host.isNotBlank() && prefs.user.isNotBlank()) {
            login(prefs.host, prefs.user, prefs.pass, true)
        }
    }

    private val adult = Regex("adult|xxx|\\+18|18\\+|porn|sexo|erotic|hot", RegexOption.IGNORE_CASE)
    private fun allowed(name: String) = prefs.parentalPin.isEmpty() || !adult.containsMatchIn(name)

    private fun friendly(e: Exception) = when (e) {
        is java.net.UnknownHostException -> "Servidor não encontrado. Verifique a URL."
        is java.net.SocketTimeoutException, is java.net.ConnectException -> "Não foi possível conectar ao servidor."
        is org.json.JSONException -> "Resposta inválida do servidor."
        else -> e.message ?: "Erro desconhecido"
    }

    fun login(hostIn: String, user: String, pass: String, remember: Boolean) {
        if (loginBusy) return
        viewModelScope.launch {
            loginBusy = true; loginError = null
            try {
                var h = hostIn.trim().trimEnd('/')
                if (h.isEmpty() || user.isBlank()) error("Preencha servidor, usuário e senha")
                if (!h.startsWith("http", true)) h = "http://$h"
                val a = XtreamApi(Account(h, user.trim(), pass))
                accountInfo = a.authenticate()
                api = a
                prefs.rememberMe = remember
                if (remember) { prefs.host = h; prefs.user = user.trim(); prefs.pass = pass } else { prefs.host = ""; prefs.user = ""; prefs.pass = "" }
                stack.clear(); stack.add(Screen.Home)
                ensureMovies(); ensureSeries()
            } catch (e: Exception) {
                loginError = friendly(e)
            } finally { loginBusy = false }
        }
    }

    fun logout() {
        prefs.autoLogin = false
        api = null; accountInfo = null
        clearCaches()
        stack.clear(); stack.add(Screen.Login)
    }

    private fun clearCaches() {
        liveCats = emptyList(); channels = emptyList(); movieCats = emptyList(); movies = emptyList()
        seriesCats = emptyList(); seriesList = emptyList(); epg.clear(); detailCache.clear()
    }

    fun refreshAll() { clearCaches(); ensureMovies(); ensureSeries() }

    fun ensureLive() {
        val a = api ?: return
        if (channels.isNotEmpty() || liveLoading) return
        viewModelScope.launch {
            liveLoading = true; loadError = null
            try {
                val cats = a.categories("get_live_categories")
                val ok = cats.filter { allowed(it.second) }.map { it.first }.toSet()
                val list = a.liveStreams().filter { it.catId in ok }
                val cnt = list.groupingBy { it.catId }.eachCount()
                liveCats = cats.filter { it.first in ok && (cnt[it.first] ?: 0) > 0 }.map { Category(it.first, it.second, cnt[it.first] ?: 0) }
                channels = list
            } catch (e: Exception) { loadError = friendly(e) } finally { liveLoading = false }
        }
    }

    private fun loadVod(series: Boolean) {
        val a = api ?: return
        if (series) { if (seriesList.isNotEmpty() || seriesLoading) return; seriesLoading = true }
        else { if (movies.isNotEmpty() || movieLoading) return; movieLoading = true }
        viewModelScope.launch {
            try {
                val cats = a.categories(if (series) "get_series_categories" else "get_vod_categories")
                val items = if (series) a.series() else a.movies()
                val ok = cats.filter { allowed(it.second) }.map { it.first }.toSet()
                val list = items.filter { it.catId in ok }
                val cnt = list.groupingBy { it.catId }.eachCount()
                val cc = cats.filter { it.first in ok && (cnt[it.first] ?: 0) > 0 }.map { Category(it.first, it.second, cnt[it.first] ?: 0) }
                if (series) { seriesCats = cc; seriesList = list } else { movieCats = cc; movies = list }
            } catch (e: Exception) { loadError = friendly(e) } finally { if (series) seriesLoading = false else movieLoading = false }
        }
    }
    fun ensureMovies() = loadVod(false)
    fun ensureSeries() = loadVod(true)

    suspend fun epgFor(id: Int) {
        if (epg.containsKey(id)) return
        val a = api ?: return
        epgSem.withPermit { if (!epg.containsKey(id)) epg[id] = a.shortEpg(id) }
    }

    suspend fun detail(item: VodItem): Pair<VodItem, Map<Int, List<Episode>>> {
        val a = api ?: return item to emptyMap()
        val key = "${item.isSeries}-${item.id}"
        detailCache[key]?.let { return it }
        val d = if (item.isSeries) a.seriesInfo(item.id) else a.vodInfo(item.id)
        val res = item.copy(
            plot = d.plot.ifEmpty { item.plot }, backdrop = d.backdrop.ifEmpty { item.backdrop },
            year = d.year.ifEmpty { item.year }, genre = d.genre.ifEmpty { item.genre }
        ) to d.episodes
        detailCache[key] = res
        return res
    }
    suspend fun vodDetail(item: VodItem) = detail(item).first

    fun search(q: String): List<VodItem> {
        if (q.isBlank()) return emptyList()
        return (movies + seriesList).filter { it.name.contains(q, true) }.take(200)
    }

    // ---- favoritos ----
    fun toggleFavLive(id: Int) { if (!favLive.remove(id)) favLive.add(id); prefs.saveFavLive(favLive.toList()) }
    fun inMyList(i: VodItem) = myList.any { it.id == i.id && it.isSeries == i.isSeries }
    fun toggleMyList(i: VodItem) {
        if (!myList.removeAll { it.id == i.id && it.isSeries == i.isSeries }) myList.add(i)
        prefs.saveMyList(myList.toList())
    }
    fun clearFavorites() { favLive.clear(); myList.clear(); prefs.saveFavLive(emptyList()); prefs.saveMyList(emptyList()) }

    // ---- reprodução ----
    fun liveUrl(ch: Channel) = api!!.liveUrl(ch.id, prefs.streamFormat)

    private fun startExternal(title: String, url: String) {
        val app = getApplication<Application>()
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url), "video/*").putExtra("title", title)
        val intent = if (prefs.externalPkg.isEmpty()) Intent.createChooser(i, title) else i.setPackage(prefs.externalPkg)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { app.startActivity(intent) }.onFailure { Toast.makeText(app, "Player externo não encontrado", Toast.LENGTH_LONG).show() }
    }

    private fun play(np: NowPlaying) {
        if (prefs.externalPlayer) { startExternal(np.title, np.url); return }
        nowPlaying = np; push(Screen.Player)
    }

    fun playLive(list: List<Channel>, idx: Int) {
        zapList = list; zapIndex = idx
        val ch = list[idx]; livePreviewId = ch.id; liveFocusId = ch.id
        play(NowPlaying("${ch.num}  ${ch.name}", liveUrl(ch), true, null))
    }

    fun zap(d: Int) {
        if (zapList.isEmpty()) return
        zapIndex = (zapIndex + d + zapList.size) % zapList.size
        val ch = zapList[zapIndex]
        livePreviewId = ch.id; liveFocusId = ch.id
        nowPlaying = NowPlaying("${ch.num}  ${ch.name}", liveUrl(ch), true, null)
    }

    fun playVod(i: VodItem) = play(NowPlaying(i.name, api!!.movieUrl(i.id, i.ext.ifEmpty { "mp4" }), false, "vod:${i.id}"))
    fun playEpisode(s: VodItem, e: Episode) =
        play(NowPlaying("${s.name} • T${e.season} E${e.num}", api!!.seriesUrl(e.id, e.ext), false, "ep:${e.id}"))
}
