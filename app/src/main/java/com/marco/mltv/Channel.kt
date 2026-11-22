package com.marco.mltv

data class Channel(
    val name: String,
    val logo: String,
    val group: String,
    val url: String,
    val epgId: String = "",
    val streamId: Int = -1,
    val archive: Boolean = false,
    val num: Int = 0
)
