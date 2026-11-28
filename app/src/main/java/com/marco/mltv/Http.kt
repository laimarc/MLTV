package com.marco.mltv

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Client HTTP partagé. */
object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Client dédié au téléchargement de l'APK : délais plus longs (fichier de plusieurs Mo)
    private val downloadClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.MINUTES)
        .build()

    fun get(url: String, noCache: Boolean = false): String {
        val b = Request.Builder().url(url)
        if (noCache) b.header("Cache-Control", "no-cache, no-store, max-age=0")
            .header("Pragma", "no-cache")
        client.newCall(b.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            return resp.body?.string() ?: throw RuntimeException("Réponse vide")
        }
    }

    fun download(url: String, dest: java.io.File) {
        val req = Request.Builder().url(url)
            .header("Cache-Control", "no-cache, no-store, max-age=0")
            .header("Pragma", "no-cache")
            .build()
        downloadClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            val body = resp.body ?: throw RuntimeException("Réponse vide")
            val expected = body.contentLength() // -1 si inconnu
            // on écrit dans un fichier temporaire, puis on renomme seulement si complet
            val tmp = java.io.File(dest.parentFile, dest.name + ".part")
            if (tmp.exists()) tmp.delete()
            val written = body.byteStream().use { input ->
                tmp.outputStream().use { out ->
                    val n = input.copyTo(out)
                    out.flush()
                    n
                }
            }
            if (expected > 0 && written != expected)
                throw RuntimeException("Téléchargement incomplet ($written/$expected o)")
            if (written < 500_000L) // un APK fait plusieurs Mo : trop petit = échec/erreur
                throw RuntimeException("Fichier trop petit ($written o)")
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) throw RuntimeException("Finalisation impossible")
        }
    }
}
