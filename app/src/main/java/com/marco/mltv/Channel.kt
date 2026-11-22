package com.marco.mltv

data class Channel(
    val name: String,
    val logo: String,
    val group: String,
    val url: String,
    val epgId: String = ""
)
