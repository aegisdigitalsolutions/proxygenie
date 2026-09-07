package org.lanenode

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Holds a persistent handle on the CELLULAR transport while the device's default
 * network stays on Wi-Fi.
 *
 * This is the piece that makes the phone a real second uplink instead of a loop:
 * the listener socket lives on Wi-Fi (so LAN clients can reach it) while every
 * outbound socket is bound to the cellular Network object.
 *
 * NOTE: we use network.bindSocket(socket) per-socket, NOT
 * ConnectivityManager.bindProcessToNetwork(). The latter is process-wide and
 * would drag the Wi-Fi listener onto cellular, killing LAN reachability.
 */
class CellularBinder(private val ctx: Context) {

    private val cm = ctx.getSystemService(ConnectivityManager::class.java)
    private val current = AtomicReference<Network?>(null)
    private var callback: ConnectivityManager.NetworkCallback? = null

    val network: Network? get() = current.get()
    val isUp: Boolean get() = current.get() != null

    /** Requests cellular and blocks up to [timeoutMs] for it to come up. */
    fun start(timeoutMs: Long = 15_000): Boolean {
        if (callback != null) return isUp

        val latch = CountDownLatch(1)
        val req = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                current.set(network)
                latch.countDown()
            }

            override fun onLost(network: Network) {
                if (current.get() == network) current.set(null)
            }

            override fun onUnavailable() {
                latch.countDown()
            }
        }

        callback = cb
        // requestNetwork (not registerNetworkCallback) tells the framework to
        // actively bring the radio up and KEEP it up while we hold the callback.
        cm.requestNetwork(req, cb)

        return latch.await(timeoutMs, TimeUnit.MILLISECONDS) && isUp
    }

    fun stop() {
        callback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
        callback = null
        current.set(null)
    }

    /** DNS resolved ON the cellular network, not the Wi-Fi resolver. */
    fun resolve(host: String): InetAddress {
        val net = current.get() ?: throw IllegalStateException("cellular down")
        return net.getAllByName(host).firstOrNull()
            ?: throw IllegalStateException("no address for $host")
    }

    /** Opens a TCP socket whose egress is the cellular modem. */
    fun connect(host: String, port: Int, timeoutMs: Int = 10_000): Socket {
        val net = current.get() ?: throw IllegalStateException("cellular down")
        val addr = resolve(host)
        val sock = Socket()
        net.bindSocket(sock)                       // <-- the whole point
        sock.tcpNoDelay = true
        sock.keepAlive = true
        sock.connect(InetSocketAddress(addr, port), timeoutMs)
        return sock
    }
}
