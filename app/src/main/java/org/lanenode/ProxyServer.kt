package org.lanenode

import android.util.Base64
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Listens on the LAN (Wi-Fi) and forwards every accepted connection out through
 * the cellular modem, chained to the upstream HTTP proxy so traffic stays
 * protected and exits from a single stable IP.
 *
 * Accepts both SOCKS5 and HTTP CONNECT on the same port (first byte 0x05 = SOCKS).
 */
class ProxyServer(
    private val cfg: Config,
    private val binder: CellularBinder,
    private val onStat: () -> Unit = {}
) {
    @Volatile private var server: ServerSocket? = null
    @Volatile private var running = false

    private val pool = Executors.newCachedThreadPool()
    val up = AtomicLong(0)
    val down = AtomicLong(0)
    val active = AtomicInteger(0)
    val errors = AtomicInteger(0)

    fun start() {
        if (running) return
        running = true
        val ss = ServerSocket()
        ss.reuseAddress = true
        ss.bind(InetSocketAddress("0.0.0.0", cfg.listenPort), 128)
        server = ss
        pool.execute {
            while (running) {
                try {
                    val client = ss.accept()
                    pool.execute { handle(client) }
                } catch (e: Exception) {
                    if (running) errors.incrementAndGet()
                }
            }
        }
    }

    fun stop() {
        running = false
        runCatching { server?.close() }
        server = null
        pool.shutdownNow()
    }

    // ---------------------------------------------------------------- client

    private fun handle(client: Socket) {
        active.incrementAndGet()
        var upstream: Socket? = null
        try {
            client.tcpNoDelay = true
            client.soTimeout = 30_000
            val cin = client.getInputStream()
            val cout = client.getOutputStream()

            val first = cin.read()
            if (first == -1) return

            val target: Pair<String, Int>
            if (first == 0x05) {
                target = socks5Handshake(cin, cout) ?: return
            } else {
                target = httpHandshake(first, cin, cout) ?: return
            }

            upstream = openUpstream(target.first, target.second)

            if (first == 0x05) {
                // SOCKS success reply, bound addr 0.0.0.0:0
                cout.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                cout.flush()
            } else {
                cout.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                cout.flush()
            }

            client.soTimeout = 0
            upstream.soTimeout = 0
            pump(client, upstream)
        } catch (e: Exception) {
            errors.incrementAndGet()
        } finally {
            runCatching { client.close() }
            runCatching { upstream?.close() }
            active.decrementAndGet()
            onStat()
        }
    }

    /** SOCKS5 greeting + CONNECT. Returns destination host/port. */
    private fun socks5Handshake(cin: InputStream, cout: OutputStream): Pair<String, Int>? {
        val n = cin.read()
        if (n <= 0) return null
        val methods = ByteArray(n)
        readFully(cin, methods)
        cout.write(byteArrayOf(0x05, 0x00))   // no auth
        cout.flush()

        val hdr = ByteArray(4)
        readFully(cin, hdr)
        if (hdr[0].toInt() != 0x05) return null
        if (hdr[1].toInt() != 0x01) {         // only CONNECT
            cout.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
            cout.flush(); return null
        }

        val host: String = when (hdr[3].toInt()) {
            0x01 -> ByteArray(4).also { readFully(cin, it) }
                .joinToString(".") { (it.toInt() and 0xFF).toString() }
            0x03 -> {
                val len = cin.read()
                val b = ByteArray(len); readFully(cin, b); String(b)
            }
            0x04 -> {
                val b = ByteArray(16); readFully(cin, b)
                java.net.InetAddress.getByAddress(b).hostAddress ?: return null
            }
            else -> return null
        }
        val p = ByteArray(2); readFully(cin, p)
        val port = ((p[0].toInt() and 0xFF) shl 8) or (p[1].toInt() and 0xFF)
        return host to port
    }

    /** HTTP CONNECT. Plain (non-CONNECT) requests are rejected — use SOCKS or HTTPS. */
    private fun httpHandshake(first: Int, cin: InputStream, cout: OutputStream): Pair<String, Int>? {
        val sb = StringBuilder()
        sb.append(first.toChar())
        var prev = 0
        while (true) {
            val c = cin.read()
            if (c == -1) return null
            sb.append(c.toChar())
            if (prev == '\r'.code && c == '\n'.code) break
            prev = c
        }
        val line = sb.toString().trim()
        // drain remaining headers
        var blank = false
        while (!blank) {
            val h = readLine(cin) ?: break
            if (h.isEmpty()) blank = true
        }
        val parts = line.split(" ")
        if (parts.size < 2 || !parts[0].equals("CONNECT", true)) {
            cout.write("HTTP/1.1 405 Only CONNECT supported\r\n\r\n".toByteArray())
            cout.flush(); return null
        }
        val hp = parts[1].split(":")
        return hp[0] to (hp.getOrNull(1)?.toIntOrNull() ?: 443)
    }

    // -------------------------------------------------------------- upstream

    private fun openUpstream(host: String, port: Int): Socket {
        if (!cfg.useUpstream) {
            // Direct-over-cellular. No proxy protection — testing only.
            return binder.connect(host, port)
        }

        val s = binder.connect(cfg.upstreamHost, cfg.upstreamPort)
        val out = s.getOutputStream()
        val req = StringBuilder()
            .append("CONNECT $host:$port HTTP/1.1\r\n")
            .append("Host: $host:$port\r\n")
        if (cfg.upstreamUser.isNotEmpty()) {
            val cred = Base64.encodeToString(
                "${cfg.upstreamUser}:${cfg.upstreamPass}".toByteArray(), Base64.NO_WRAP
            )
            req.append("Proxy-Authorization: Basic $cred\r\n")
        }
        req.append("Proxy-Connection: Keep-Alive\r\n\r\n")
        out.write(req.toString().toByteArray())
        out.flush()

        val status = readLine(s.getInputStream())
            ?: throw IllegalStateException("upstream closed")
        if (!status.contains(" 200")) {
            runCatching { s.close() }
            throw IllegalStateException("upstream refused: $status")
        }
        // drain headers to the blank line
        while (true) {
            val h = readLine(s.getInputStream()) ?: break
            if (h.isEmpty()) break
        }
        return s
    }

    // ------------------------------------------------------------------ pipe

    private fun pump(client: Socket, upstream: Socket) {
        val t = Thread {
            copy(client.getInputStream(), upstream.getOutputStream(), up)
            runCatching { upstream.shutdownOutput() }
        }
        t.isDaemon = true
        t.start()
        copy(upstream.getInputStream(), client.getOutputStream(), down)
        runCatching { client.shutdownOutput() }
        t.join(5_000)
    }

    private fun copy(input: InputStream, output: OutputStream, counter: AtomicLong) {
        val buf = ByteArray(32 * 1024)
        try {
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                output.write(buf, 0, n)
                output.flush()
                counter.addAndGet(n.toLong())
            }
        } catch (_: Exception) {
        }
    }

    // ----------------------------------------------------------------- utils

    private fun readFully(input: InputStream, buf: ByteArray) {
        var off = 0
        while (off < buf.size) {
            val n = input.read(buf, off, buf.size - off)
            if (n <= 0) throw IllegalStateException("short read")
            off += n
        }
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val c = input.read()
            if (c == -1) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) return sb.toString().trimEnd('\r')
            sb.append(c.toChar())
        }
    }
}
