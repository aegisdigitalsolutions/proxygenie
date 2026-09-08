package org.lanenode

/**
 * Simple token-bucket rate limiter. Used to keep aggregate uplink under the
 * cellular bottleneck so bufferbloat / carrier-scheduler latency collapses.
 *
 * Set rateKbps to ~70–80% of your measured upload (e.g. 1400 on a ~2 Mbps cell).
 * rateKbps <= 0 disables limiting.
 */
class RateLimiter(private val rateKbps: Int) {
    @Volatile private var tokens = 0.0
    private var lastNs = System.nanoTime()
    private val capacity: Double
    private val rateBytesPerSec: Double

    init {
        if (rateKbps <= 0) {
            capacity = 0.0
            rateBytesPerSec = 0.0
        } else {
            rateBytesPerSec = rateKbps * 1000.0 / 8.0
            // ~250ms burst so short ACKs / tiny posts aren't over-shaped
            capacity = rateBytesPerSec * 0.25
            tokens = capacity
        }
    }

    val enabled: Boolean get() = rateKbps > 0

    @Synchronized
    fun take(n: Int) {
        if (!enabled || n <= 0) return
        while (true) {
            val now = System.nanoTime()
            val elapsed = (now - lastNs) / 1_000_000_000.0
            lastNs = now
            tokens = (tokens + elapsed * rateBytesPerSec).coerceAtMost(capacity)
            if (tokens >= n) {
                tokens -= n
                return
            }
            val need = n - tokens
            val sleepMs = ((need / rateBytesPerSec) * 1000.0).toLong().coerceIn(1L, 50L)
            try {
                Thread.sleep(sleepMs)
            } catch (_: InterruptedException) {
                return
            }
        }
    }
}
