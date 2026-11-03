package com.nexus.iptv

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Account(val host: String, val user: String, val pass: String)
data class Category(val id: String, val name: String, val count: Int)
data class Channel(val id: Int, val num: Int, val name: String, val icon: String, val catId: String)
data class VodItem(
    val id: Int, val name: String, val poster: String, val backdrop: String, val rating: Double,
    val catId: String, val ext: String, val isSeries: Boolean, val added: Long,
    val plot: String = "", val year: String = "", val genre: String = ""
)
data class EpgEntry(val title: String, val desc: String, val start: Long, val end: Long)
data class Episode(val id: Int, val season: Int, val num: Int, val title: String, val ext: String, val plot: String)
data class AccountInfo(val status: String, val expDate: Long, val maxConn: String, val activeConn: String, val trial: Boolean)
data class Detail(val plot: String, val backdrop: String, val year: String, val genre: String, val episodes: Map<Int, List<Episode>>)
data class NowPlaying(val title: String, val url: String, val live: Boolean, val resumeKey: String?)

private fun JSONObject.s(k: String): String = if (isNull(k)) "" else optString(k, "")
private fun JSONObject.num(k: String): Double = s(k).toDoubleOrNull() ?: 0.0

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("nexus", Context.MODE_PRIVATE)
    private fun str(k: String, d: String = "") = sp.getString(k, d) ?: d
    private fun put(k: String, v: Any) = sp.edit().apply {
        when (v) { is String -> putString(k, v); is Boolean -> putBoolean(k, v); is Int -> putInt(k, v); is Long -> putLong(k, v); else -> {} }
    }.apply()

    var host: String

        get() = str("host")

        set(v) { put("host", v) }
    var user: String
        get() = str("user")
        set(v) { put("user", v) }
    var pass: String
        get() = str("pass")
        set(v) { put("pass", v) }
    var rememberMe: Boolean
        get() = sp.getBoolean("remember", true)
        set(v) { put("remember", v) }
    var autoLogin: Boolean
        get() = sp.getBoolean("auto", true)
        set(v) { put("auto", v) }
    var streamFormat: String
        get() = str("fmt", "m3u8")
        set(v) { put("fmt", v) }
    var use24h: Boolean
        get() = sp.getBoolean("h24", true)
        set(v) { put("h24", v) }
    var showEpg: Boolean
        get() = sp.getBoolean("epg", true)
        set(v) { put("epg", v) }
    var externalPlayer: Boolean
        get() = sp.getBoolean("ext", false)
        set(v) { put("ext", v) }
    var externalPkg: String
        get() = str("extpkg")
        set(v) { put("extpkg", v) }
    var bufferMode: Int
        get() = sp.getInt("buf", 1)
        set(v) { put("buf", v) }
    var parentalPin: String
        get() = str("pin")
        set(v) { put("pin", v) }

    fun favLive(): List<Int> = str("favlive").split(",").mapNotNull { it.toIntOrNull() }
    fun saveFavLive(l: List<Int>) = put("favlive", l.joinToString(","))

    fun myList(): List<VodItem> = runCatching {
        val a = JSONArray(str("mylist", "[]"))
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            VodItem(o.getInt("id"), o.getString("name"), o.getString("poster"), o.getString("backdrop"), o.getDouble("rating"),
                o.getString("cat"), o.getString("ext"), o.getBoolean("series"), o.getLong("added"))
        }
    }.getOrDefault(emptyList())

    fun saveMyList(l: List<VodItem>) {
        val a = JSONArray()
        l.forEach {
            a.put(JSONObject().put("id", it.id).put("name", it.name).put("poster", it.poster).put("backdrop", it.backdrop)
                .put("rating", it.rating).put("cat", it.catId).put("ext", it.ext).put("series", it.isSeries).put("added", it.added))
        }
        put("mylist", a.toString())
    }

    fun getPos(k: String): Long = sp.getLong("pos_$k", 0L)
    fun setPos(k: String, v: Long) = put("pos_$k", v)
}

class XtreamApi(val acc: Account) {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun fetch(extra: String = ""): String {
        val u = URL("${acc.host}/player_api.php?username=${enc(acc.user)}&password=${enc(acc.pass)}$extra")
        val c = u.openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 90000
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 11; TV) NexusIPTV")
        try { return c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }

    suspend fun authenticate(): AccountInfo = withContext(Dispatchers.IO) {
        val ui = JSONObject(fetch()).optJSONObject("user_info") ?: error("Resposta inválida do servidor")
        if (ui.optInt("auth", 0) != 1) error("Usuário ou senha inválidos")
        AccountInfo(ui.s("status"), ui.s("exp_date").toLongOrNull() ?: 0L, ui.s("max_connections"), ui.s("active_cons"), ui.s("is_trial") == "1")
    }

    suspend fun categories(action: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val a = JSONArray(fetch("&action=$action"))
        List(a.length()) { val o = a.getJSONObject(it); o.s("category_id") to o.s("category_name") }
    }

    suspend fun liveStreams(): List<Channel> = withContext(Dispatchers.IO) {
        val a = JSONArray(fetch("&action=get_live_streams"))
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Channel(o.s("stream_id").toIntOrNull() ?: 0, o.s("num").toIntOrNull() ?: (i + 1), o.s("name"), o.s("stream_icon"), o.s("category_id"))
        }
    }

    suspend fun movies(): List<VodItem> = withContext(Dispatchers.IO) {
        val a = JSONArray(fetch("&action=get_vod_streams"))
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            VodItem(o.s("stream_id").toIntOrNull() ?: 0, o.s("name"), o.s("stream_icon"), "", o.num("rating").coerceAtMost(10.0),
                o.s("category_id"), o.s("container_extension").ifEmpty { "mp4" }, false, o.s("added").toLongOrNull() ?: 0L)
        }
    }

    suspend fun series(): List<VodItem> = withContext(Dispatchers.IO) {
        val a = JSONArray(fetch("&action=get_series"))
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            VodItem(o.s("series_id").toIntOrNull() ?: 0, o.s("name"), o.s("cover"), o.optJSONArray("backdrop_path")?.optString(0) ?: "",
                o.num("rating").coerceAtMost(10.0), o.s("category_id"), "mp4", true, o.s("last_modified").toLongOrNull() ?: 0L,
                o.s("plot"), o.s("releaseDate").take(4), o.s("genre"))
        }
    }

    private fun b64(s: String): String = runCatching { String(Base64.decode(s, Base64.DEFAULT), Charsets.UTF_8) }.getOrDefault(s)

    suspend fun shortEpg(id: Int): List<EpgEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val a = JSONObject(fetch("&action=get_short_epg&stream_id=$id&limit=4")).optJSONArray("epg_listings") ?: JSONArray()
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                EpgEntry(b64(o.s("title")), b64(o.s("description")), o.s("start_timestamp").toLongOrNull() ?: 0L, o.s("stop_timestamp").toLongOrNull() ?: 0L)
            }
        }.getOrDefault(emptyList())
    }

    suspend fun vodInfo(id: Int): Detail = withContext(Dispatchers.IO) {
        val info = JSONObject(fetch("&action=get_vod_info&vod_id=$id")).optJSONObject("info") ?: JSONObject()
        Detail(info.s("plot").ifEmpty { info.s("description") }, info.optJSONArray("backdrop_path")?.optString(0) ?: "",
            info.s("releasedate").take(4), info.s("genre"), emptyMap())
    }

    suspend fun seriesInfo(id: Int): Detail = withContext(Dispatchers.IO) {
        val root = JSONObject(fetch("&action=get_series_info&series_id=$id"))
        val info = root.optJSONObject("info") ?: JSONObject()
        val out = sortedMapOf<Int, MutableList<Episode>>()
        fun add(o: JSONObject, season: Int) {
            out.getOrPut(season) { mutableListOf() }.add(
                Episode(o.s("id").toIntOrNull() ?: 0, season, o.s("episode_num").toIntOrNull() ?: 0,
                    o.s("title"), o.s("container_extension").ifEmpty { "mp4" }, o.optJSONObject("info")?.s("plot") ?: "")
            )
        }
        when (val eps = root.opt("episodes")) {
            is JSONObject -> eps.keys().forEach { k ->
                val arr = eps.optJSONArray(k) ?: JSONArray()
                for (i in 0 until arr.length()) add(arr.getJSONObject(i), k.toIntOrNull() ?: 1)
            }
            is JSONArray -> for (i in 0 until eps.length()) {
                val arr = eps.optJSONArray(i) ?: continue
                for (j in 0 until arr.length()) add(arr.getJSONObject(j), i + 1)
            }
        }
        Detail(info.s("plot"), info.optJSONArray("backdrop_path")?.optString(0) ?: "", info.s("releaseDate").take(4), info.s("genre"),
            out.mapValues { e -> e.value.sortedBy { it.num } })
    }

    fun liveUrl(id: Int, ext: String) = "${acc.host}/live/${acc.user}/${acc.pass}/$id.$ext"
    fun movieUrl(id: Int, ext: String) = "${acc.host}/movie/${acc.user}/${acc.pass}/$id.$ext"
    fun seriesUrl(id: Int, ext: String) = "${acc.host}/series/${acc.user}/${acc.pass}/$id.$ext"
}
