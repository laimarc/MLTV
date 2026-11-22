package com.marco.mltv

/** Liste live courante partagée avec le plein écran (pour zapper haut/bas). */
object LiveSession {
    var channels: List<Channel> = emptyList()
    var index: Int = 0
}
