package com.marco.mltv

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Source réseau « tolérante » : ouvre le flux comme VLC/TiViMate, même quand le serveur
 * ne répond pas en HTTP conforme (status line absente / « HTTP 0.9 »), ce qui fait planter
 * la pile standard d'ExoPlayer. Utilisée uniquement pour le rattrapage (replay).
 */
@UnstableApi
class TolerantHttpDataSource(private val userAgent: String) : BaseDataSource(true) {

    private var uri: Uri? = null
    private var socket: Socket? = null
    private var input: InputStream? = null
    private var opened = false
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()

    class Factory(private val userAgent: String) : DataSource.Factory {
        override fun createDataSource(): DataSource = TolerantHttpDataSource(userAgent)
    }

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        var target = dataSpec.uri
        var redirects = 0
        while (true) {
            val r = connectOnce(target, dataSpec.position)
            if (r.redirectTo != null) {
                closeSocketQuietly()
                if (redirects++ >= 5) throw IOException("Trop de redirections")
                target = resolve(target, r.redirectTo)
                continue
            }
            uri = target
            input = r.body
            bytesRemaining = r.length
            break
        }
        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    private class Conn(val body: InputStream?, val length: Long, val redirectTo: String?)

    private fun connectOnce(u: Uri, position: Long): Conn {
        val https = u.scheme.equals("https", true)
        val host = u.host ?: throw IOException("Hôte manquant")
        val port = if (u.port > 0) u.port else if (https) 443 else 80
        val path = buildString {
            append(u.encodedPath ?: "/")
            if (isEmpty()) append("/")
            u.encodedQuery?.let { append("?").append(it) }
        }

        val s = if (https) SSLSocketFactory.getDefault().createSocket() else Socket()
        s.connect(InetSocketAddress(host, port), 15000)
        s.soTimeout = 30000
        if (https) (s as SSLSocket).startHandshake()
        socket = s

        val req = StringBuilder()
        req.append("GET ").append(path).append(" HTTP/1.0\r\n")
        req.append("Host: ").append(host)
        if (u.port > 0) req.append(":").append(u.port)
        req.append("\r\n")
        req.append("User-Agent: ").append(userAgent).append("\r\n")
        req.append("Accept: */*\r\n")
        if (position > 0) req.append("Range: bytes=").append(position).append("-\r\n")
        req.append("Connection: close\r\n\r\n")
        s.getOutputStream().apply { write(req.toString().toByteArray(Charsets.ISO_8859_1)); flush() }

        val bin = BufferedInputStream(s.getInputStream(), 64 * 1024)
        bin.mark(64 * 1024)
        val first = readLine(bin)

        if (first != null && first.startsWith("HTTP/")) {
            val code = first.split(" ").getOrNull(1)?.toIntOrNull() ?: 0
            var length = C.LENGTH_UNSET.toLong()
            var location: String? = null
            var chunked = false
            while (true) {
                val h = readLine(bin) ?: break
                if (h.isEmpty()) break
                val idx = h.indexOf(':')
                if (idx > 0) {
                    val k = h.substring(0, idx).trim().lowercase()
                    val v = h.substring(idx + 1).trim()
                    when (k) {
                        "content-length" -> length = v.toLongOrNull() ?: C.LENGTH_UNSET.toLong()
                        "location" -> location = v
                        "transfer-encoding" -> if (v.contains("chunked", true)) chunked = true
                    }
                }
            }
            if (code in 300..399 && !location.isNullOrEmpty()) return Conn(null, 0, location)
            if (code !in 200..299) throw IOException("HTTP $code")
            val body = if (chunked) ChunkedInputStream(bin) else bin
            return Conn(body, length, null)
        }

        // Pas de status line HTTP : flux brut (HTTP/0.9) → tout est le corps
        bin.reset()
        return Conn(bin, C.LENGTH_UNSET.toLong(), null)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val ins = input ?: return C.RESULT_END_OF_INPUT
        val toRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) length
        else minOf(length.toLong(), bytesRemaining).toInt()
        if (toRead <= 0) return C.RESULT_END_OF_INPUT
        val n = ins.read(buffer, offset, toRead)
        if (n == -1) return C.RESULT_END_OF_INPUT
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= n
        bytesTransferred(n)
        return n
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        try { input?.close() } catch (_: Exception) {}
        closeSocketQuietly()
        input = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    private fun closeSocketQuietly() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }

    private fun resolve(base: Uri, location: String): Uri {
        val l = location.trim()
        return if (l.startsWith("http://") || l.startsWith("https://")) Uri.parse(l)
        else if (l.startsWith("/")) Uri.parse("${base.scheme}://${base.authority}$l")
        else {
            val p = base.encodedPath ?: "/"
            val dir = p.substringBeforeLast('/', "")
            Uri.parse("${base.scheme}://${base.authority}$dir/$l")
        }
    }

    private fun readLine(ins: InputStream): String? {
        val out = ByteArrayOutputStream(128)
        var c = ins.read()
        if (c == -1) return null
        while (c != -1 && c != '\n'.code) {
            if (c != '\r'.code) out.write(c)
            c = ins.read()
        }
        return out.toString("ISO-8859-1")
    }

    /** Dé-chunk minimal pour Transfer-Encoding: chunked. */
    private class ChunkedInputStream(private val src: InputStream) : InputStream() {
        private var remaining = 0
        private var done = false

        override fun read(): Int {
            val b = ByteArray(1)
            val n = read(b, 0, 1)
            return if (n == -1) -1 else b[0].toInt() and 0xff
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (done) return -1
            if (remaining == 0) {
                val line = readSizeLine() ?: run { done = true; return -1 }
                val size = line.trim().substringBefore(';').trim().toIntOrNull(16)
                if (size == null || size == 0) { done = true; return -1 }
                remaining = size
            }
            val toRead = minOf(len, remaining)
            val n = src.read(b, off, toRead)
            if (n == -1) { done = true; return -1 }
            remaining -= n
            if (remaining == 0) { src.read(); src.read() } // CRLF de fin de chunk
            return n
        }

        private fun readSizeLine(): String? {
            val out = ByteArrayOutputStream(16)
            var c = src.read()
            if (c == -1) return null
            while (c != -1 && c != '\n'.code) {
                if (c != '\r'.code) out.write(c)
                c = src.read()
            }
            return out.toString("ISO-8859-1")
        }
    }
}
