package com.fouru.tv

data class Channel(
    val id: String,
    val name: String,
    val category: String,
    val subtitle: String,
    val accent: Int
)

data class PlaybackSession(
    val manifestUrl: String,
    val sessionToken: String?,
    val headers: Map<String, String>
)
