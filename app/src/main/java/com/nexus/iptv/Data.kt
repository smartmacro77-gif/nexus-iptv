package com.nexus.iptv

data class Channel(
    val id: String,
    val name: String,
    val logo: String,
    val streamUrl: String,
    val category: String
)

data class VodItem(
    val id: String,
    val title: String,
    val poster: String,
    val backdrop: String,
    val rating: String,
    val category: String,
    val streamUrl: String
)

